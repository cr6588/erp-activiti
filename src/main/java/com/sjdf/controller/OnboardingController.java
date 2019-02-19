package com.sjdf.controller;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

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
    
    @RequestMapping("/onboarding")
    public ModelAndView onboarding(ModelAndView view) {
        view.addObject("processEngine", processEngine);
        ProcessInstance processInstance = runtimeService
            .startProcessInstanceByKey("onboarding");
        view.addObject("processInstance", processInstance);
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getProcessInstanceId()).list();
        System.out.println("Active outstanding tasks: [" + tasks.size() + "]");
        Task task = tasks.get(0);
        FormData formData = formService.getTaskFormData(task.getId());
        List<FormProperty> formProperties = formData.getFormProperties();
        view.addObject("formProperties", formProperties);
        return view;
    }
}
