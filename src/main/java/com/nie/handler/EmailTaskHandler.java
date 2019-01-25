package com.nie.handler;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;

/**
 *
 * Created by Administrator on 2019/1/17.
 */
public class EmailTaskHandler implements JavaDelegate {

    @Override
    public void execute(DelegateExecution delegateExecution) {

        System.out.println(delegateExecution.getProcessInstanceBusinessKey());

        System.out.println("执行serviceTask---->调用外部Email服务完成");
    }
}
