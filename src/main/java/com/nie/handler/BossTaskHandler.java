package com.nie.handler;


import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author
 * @date 2018/12/19
 */
public class BossTaskHandler implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {
        System.out.println("指定老板审批！！！");
        System.out.println(delegateTask.getId());
        System.out.println(delegateTask.getName());
        delegateTask.setAssignee("老板");
    }

}
