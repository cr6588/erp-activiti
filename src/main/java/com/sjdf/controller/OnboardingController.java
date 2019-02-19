package com.sjdf.controller;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.activiti.engine.FormService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.form.FormData;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.impl.form.DateFormType;
import org.activiti.engine.impl.form.LongFormType;
import org.activiti.engine.impl.form.StringFormType;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

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
}
