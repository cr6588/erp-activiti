package com.sjdf.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.engine.FormService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.form.FormData;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.impl.persistence.entity.VariableInstance;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.image.ProcessDiagramGenerator;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.sjdf.util.PagerInfo;
import com.sjdf.util.PagerResolver;

@Controller
public class OnboardingController {

    @Autowired
    ProcessEngine processEngine;
    @Autowired
    RuntimeService runtimeService;
    @Autowired
    TaskService taskService;
    @Autowired
    FormService formService;
    @Autowired
    HttpServletRequest request;

    @RequestMapping("/onboarding")
    public ModelAndView onboarding(ModelAndView view, @RequestParam(value = "processId", required = false) String processId) {
        view.addObject("processEngine", processEngine);
        ProcessInstance processInstance = null;
        if (null == processId) {
            processInstance = runtimeService.startProcessInstanceByKey("onboarding");
        } else {
            processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processId).singleResult();
        }
        view.addObject("processInstance", processInstance);
        if (processInstance == null || processInstance.isEnded()) {
            return view;
        }
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getProcessInstanceId()).list();
        System.out.println("Active outstanding tasks: [" + tasks.size() + "]");
        Task task = tasks.get(0);
        if(null != processId) {
            completeTask(task.getId());
            processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processId).singleResult();
        }
        if(processInstance == null || processInstance.isEnded()) {
            return view;
        } 
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getProcessInstanceId()).list();
        task = tasks.get(0);
        FormData formData = formService.getTaskFormData(task.getId());
        List<FormProperty> formProperties = formData.getFormProperties();
        view.addObject("formProperties", formProperties);
        return view;
    }

    private void completeTask(String taskId) {
        FormData formData = formService.getTaskFormData(taskId);
        Map<String, Object> variables = new HashMap<String, Object>();
        for (FormProperty formProperty : formData.getFormProperties()) {
            variables.put(formProperty.getId(), request.getParameter(formProperty.getId()));
        }
        taskService.complete(taskId, variables);
    }

    
    @RequestMapping("/user/upcomingTaskList")
    public ModelAndView upcomingTaskList(ModelAndView view, @PagerResolver PagerInfo pager,
        @RequestParam(value = "startDate", required = false) String startDate,
        @RequestParam(value = "endDate", required = false) String endDate,
        @RequestParam(value = "name", required = false) String name
    ) {
        long comId = getComId();
        Map<String, Object> param = new HashMap<>();
        param.put("startDate", startDate);
        param.put("endDate", endDate);
        param.put("name", name);

        ResultParamVo<List<Task>, PagerInfo> res = activitiService.getTaskList(comId, getCandidateGroups(), pager, param);
        List<Task> tasks = res.result();
        view.addObject("tasks", tasks);
        view.addObject(ConstBusiness.PAGER_INFO, pager);

        if(PlatformUtils.isNotEmpty(tasks)) {
            List<? extends Dictionary> process = ConfigManager.getInstance().getDictionaryList(GoodsOnlineProcess.class);
            Map<String, String> taskKeyUrl = new HashMap<>();
            process.forEach(p -> taskKeyUrl.put(p.getEnName(), p.getValue()));
            view.addObject("taskKeyUrl", taskKeyUrl);

            Map<String, Object> taskIdUrl = new HashMap<>();
//            前台需要审核时加入
//            Map<String, Object> auditMap = new HashMap<>();
            Map<String, ProcessInstance> processMap = new HashMap<>();
            Map<String, Object> createUserMap = new HashMap<>();
            Set<String> processInstanceIds = new HashSet<>();
            tasks.forEach(t -> {
                processInstanceIds.add(t.getProcessInstanceId());
//                if(t.getTaskDefinitionKey().contains("审核")) {
//                    auditMap.put(t.getId(), true);
//                }
                processInstanceIds.add(t.getProcessInstanceId());

                //executionId为t.getProcessInstanceId()与t.getExecutionId()都可
                //runtimeService.getVariable(t.getProcessInstanceId(), ConstActivitiBusiness.CREATE_USER);
            });

            List<ProcessInstance> processList = runtimeService.createProcessInstanceQuery().processInstanceIds(processInstanceIds).list();
            processList.forEach(p -> {
                processMap.put(p.getProcessInstanceId(), p);
                p.getProcessVariables();
            });


            //这里的executionIds是processInstanceIds，不是task中executionId
            List<VariableInstance> variableInstance =  runtimeService.getVariableInstancesByExecutionIds(processInstanceIds);
            variableInstance.forEach(v -> {
                if(ConstActivitiBusiness.CREATE_USER.equals(v.getName())) {
                    createUserMap.put(v.getProcessInstanceId(), v.getValue());
                }
            });
            tasks.forEach(t -> {
                ProcessInstance processInstance = processMap.get(t.getProcessInstanceId());
                String businessKey = processInstance.getBusinessKey();
                String url = taskKeyUrl.get(t.getTaskDefinitionKey());
                url = String.format(url, businessKey) + "&taskId=" + t.getId();
                taskIdUrl.put(t.getId(), url);
            });

            view.addObject("taskIdUrl", taskIdUrl);
//            view.addObject("auditMap", auditMap);
            view.addObject("processMap", processMap);
            view.addObject("createUserMap", createUserMap);

        }
        return view;
    }

    /**
     * @Note 读取流程资源
     * @param processDefinitionId 流程定义ID
     */
    @RequestMapping(value = "/user/readResource")
    public void readResource(String processDefinitionId, String processInstanceId, HttpServletResponse response)
            throws Exception {
        // 设置页面不缓存
        response.setHeader("Pragma", "No-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        ProcessDefinitionQuery pdq = repositoryService.createProcessDefinitionQuery();
        ProcessDefinition pd = pdq.processDefinitionId(processDefinitionId).singleResult();
        String resourceName = "goodsOnline.bpmn20.png";
        if (resourceName.endsWith(".png") && PlatformUtils.hasText(processInstanceId)) {
            try(InputStream imageStream = getActivitiProccessImage(processInstanceId);
                OutputStream os = response.getOutputStream();
            ) {
                response.setContentType("image/png");
                int bytesRead = 0;
                byte[] buffer = new byte[8192];
                while ((bytesRead = imageStream.read(buffer, 0, 8192)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }

            }
            return;
        }
        // 通过接口读取
        InputStream resourceAsStream = repositoryService.getResourceAsStream(pd.getDeploymentId(), resourceName);
        // 输出资源内容到相应对象
        byte[] b = new byte[1024];
        int len = -1;
        while ((len = resourceAsStream.read(b, 0, 1024)) != -1) {
            response.getOutputStream().write(b, 0, len);
        }
    }

    private List<HistoricActivityInstance> getHistory(String processInstanceId) {
        // 获取流程历史中已执行节点，并按照节点在流程中执行先后顺序排序
        List<HistoricActivityInstance> historicActivityInstanceList = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId).orderByHistoricActivityInstanceId().asc().list();
        return historicActivityInstanceList;
    }

    /**
     * 获取流程图像，已执行节点和流程线高亮显示
     */
    public InputStream getActivitiProccessImage(String processInstanceId) {
        //  获取历史流程实例
        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId).singleResult();
        if (historicProcessInstance == null) {
            return null;
        }
        // 获取流程历史中已执行节点，并按照节点在流程中执行先后顺序排序
        List<HistoricActivityInstance> historicActivityInstanceList = getHistory(processInstanceId);
        // 已执行的节点ID集合
        List<String> executedActivityIdList = new ArrayList<String>();
        for (HistoricActivityInstance activityInstance : historicActivityInstanceList) {
            executedActivityIdList.add(activityInstance.getActivityId());
        }
        BpmnModel bpmnModel = repositoryService.getBpmnModel(historicProcessInstance.getProcessDefinitionId());
        // 已执行的线集合
        List<String> flowIds = getHighLightedFlows(bpmnModel, historicActivityInstanceList);

        //获取流程图图像字符流
        ProcessDiagramGenerator pec = processEngine.getProcessEngineConfiguration().getProcessDiagramGenerator();
        //配置字体
        InputStream imageStream = pec.generateDiagram(bpmnModel, "png", executedActivityIdList, flowIds, "微软雅黑", "微软雅黑", "微软雅黑", null, 2.0);
        return imageStream;
    }

    public List<String> getHighLightedFlows(BpmnModel bpmnModel, List<HistoricActivityInstance> historicActivityInstances) {
        List<String> highFlows = new ArrayList<>();// 用以保存高亮的线flowId
        int size = historicActivityInstances.size();
        Process mainProcess = bpmnModel.getMainProcess();
        Map<String, String> highFlowsMap = new HashMap<>();
        Map<String, String> activityIdStartTimeMap = new HashMap<>();

        //将所有活动以活动id与开始时间为key放入map中
        for (int i = 0; i < size; i++) {
            HistoricActivityInstance activityInstance = historicActivityInstances.get(i);
            String activityId = activityInstance.getActivityId();
            String key = activityId + DateUtils.formatDateTime(activityInstance.getStartTime());
            activityIdStartTimeMap.put(key, key);
        }
        //遍历当前任务出去的线是否被执行，所以最后的节点出去的线不不需要遍历
        for (int i = 0; i < size - 1; i++) {
            // 对历史流程节点进行遍历
            // 得到节点定义的详细信息
            HistoricActivityInstance activityInstance = historicActivityInstances.get(i);

            String activityId = activityInstance.getActivityId();
            FlowNode flowNode = (FlowNode) mainProcess.getFlowElement(activityId);
            List<SequenceFlow> flows = flowNode.getOutgoingFlows();
            if(PlatformUtils.isEmpty(flows)) {
                continue;
            }
            //在activityIdStartTimeMap中若有以当前任务出去的节点id与结束时间作为key的对象则该条线是被执行过的
            String endTime = DateUtils.formatDateTime(activityInstance.getEndTime());
            for (SequenceFlow f :flows) {
                String key = f.getTargetRef() + endTime;
                if(activityIdStartTimeMap.get(key) != null && highFlowsMap.get(f.getId()) == null) {
                    highFlows.add(f.getId());
                    highFlowsMap.put(f.getId(), f.getId());
                }
            }
        }
        return highFlows;
    }

    /**
     * 查看进度
     * @param view
     * @param processDefinitionId
     * @param processInstanceId
     * @return
     */
    @RequestMapping("/user/viewProgress")
    public ModelAndView viewProgress(ModelAndView view, String processDefinitionId, String processInstanceId) {
        //  获取历史流程实例
        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId).singleResult();
        if (historicProcessInstance == null) {
            return null;
        }
        // 获取流程历史中已执行节点，并按照节点在流程中执行先后顺序排序
        List<HistoricActivityInstance> historicActivityInstanceList = getHistory(processInstanceId);
        // 已执行的节点ID集合
        List<String> executedActivityIdList = new ArrayList<>();
        for (HistoricActivityInstance activityInstance : historicActivityInstanceList) {
            executedActivityIdList.add(activityInstance.getActivityId());
        }
        BpmnModel bpmnModel = repositoryService.getBpmnModel(historicProcessInstance.getProcessDefinitionId());
        // 已执行的线集合
        List<String> flowIds = getHighLightedFlows(bpmnModel, historicActivityInstanceList);
        //获取流程图图像字符流
        ProcessDiagramGenerator pec = processEngine.getProcessEngineConfiguration().getProcessDiagramGenerator();
        //配置字体
        try (InputStream imageStream = pec.generateDiagram(bpmnModel, "png", executedActivityIdList, flowIds, "微软雅黑", "微软雅黑", "微软雅黑", null, 2.0);
             ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ){
            Base64.encode(imageStream, stream);
            String base64str = new String(stream.toByteArray());
            view.addObject("base64str", "data:image/png;base64," + base64str);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
        view.addObject("history", historicActivityInstanceList);
        return view;
    }

    @RequestMapping("/user/deleteProcess")
    @ResponseBody
    public RequestResult<String> deleteProcess(String processInstanceId) {
        activitiService.deleteProcessInstance(processInstanceId, null);
        return RequestResult.createSucc();
    }

    @RequestMapping("/user/completeTask")
    @ResponseBody
    public RequestResult<String> completeTask(String taskId, @RequestParam(value = ConstActivitiBusiness.AUDIT, required = false) Boolean audit) {
        Map<String, Object> variables = null;
        if(audit != null) {
            variables = new HashMap<>();
            variables.put(ConstActivitiBusiness.AUDIT, audit);
        }
        try {
            activitiService.completeTask(taskId, variables, getCandidateGroups(), getComId());
            return RequestResult.createSucc();
        } catch (Exception e) {
            LOGGER.error("任务完成出错,taskId=" + taskId + ",audit=" + audit + "_" + e.getMessage(), e);
            return RequestResult.createErr(e.getMessage());
        }
    }

    @RequestMapping("/user/startProcess")
    @ResponseBody
    public RequestResult<String> startProcess() {
        long comId = getComId();
        activitiService.startProcessInstanceByKey(ConstActivitiProcessKey.GOODS_ONLINE, IdGeneratorUtils.generateId(), comId, getUserName());
        return RequestResult.createSucc();
    }
}
