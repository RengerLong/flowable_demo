package com.nie.handler;

import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;

/**
 * 
 * @author
 * @date 2018/12/19
 */
public class ManagerTaskHandler implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {
        System.out.println("经理审批！！！");
        delegateTask.setAssignee("经理");
    }

}
