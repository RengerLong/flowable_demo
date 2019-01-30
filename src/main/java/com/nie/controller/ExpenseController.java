package com.nie.controller;

import com.nie.entity.Leave;
import com.nie.entity.LeaveApplication;
import com.nie.service.LeaveService;
import org.apache.ibatis.annotations.Param;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.*;
import org.flowable.engine.common.impl.identity.Authentication;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.*;

/**
 *
 * @author
 * @date 2018/12/19
 */
@Controller
@RequestMapping(value = "expense")
public class ExpenseController {
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private ProcessEngine processEngine;
    @Autowired
    private IdentityService identityService;
    @Autowired
    private LeaveService leaveService;


    @RequestMapping("/test")
    @ResponseBody
    public List<LeaveApplication> test(){
        LeaveApplication leaveApplication = new LeaveApplication();
        leaveApplication.setUserId("1");
        leaveApplication.setUserName("111");
        leaveApplication.setStartDate(new Date());
        leaveApplication.setNumDays(5);
        leaveApplication.setDescription("描述");

        LeaveApplication leaveApplication2 = new LeaveApplication();
        leaveApplication2.setUserId("1");
        leaveApplication2.setUserName("111");
        leaveApplication2.setStartDate(new Date());
        leaveApplication2.setNumDays(5);
        leaveApplication2.setDescription("描述");

        List<LeaveApplication> leaveApplicationlist = new ArrayList<>();
        leaveApplicationlist.add(leaveApplication);
        leaveApplicationlist.add(leaveApplication2);

        return leaveApplicationlist;
    }


    /**
     * 添加报销
     *
     */
    @RequestMapping(value = "/add")
    public String addExpense(Leave leave) {

        String processDefinitionKey = "Expense";

        //启动流程
        HashMap<String, Object> map = new HashMap<>();
        map.put("taskUser", leave.getName());
        map.put("numDays", leave.getNumDays());
        int nId = leaveService.saveLeave(leave);
        //设置发起人
        Authentication.setAuthenticatedUserId(leave.getName());
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processDefinitionKey, String.valueOf(nId),map);
        System.out.println("提交成功.流程实例Id为：" + processInstance.getId());
        Authentication.setAuthenticatedUserId(null);


        //流程图上设置第一个审批人为flow，这里直接审批通过
        List<Task> tasks = taskService.createTaskQuery().taskAssignee(leave.getName()).orderByTaskCreateTime()
                .asc().list();
        Task task = tasks.get(tasks.size()-1);
        HashMap<String, Object> taskUserMap = new HashMap<>();
        taskUserMap.put("taskUser", leave.getName());
        taskService.complete(task.getId(),taskUserMap);
        System.out.println("flow审批通过！！");
        return "index";
    }


    /**
     * 获取审批管理列表
     */
    @RequestMapping(value = "/list")
    @ResponseBody
    public List<LeaveApplication> list(@Param("userId") String userId) {
        List<Task> tasks = taskService.createTaskQuery()
//                .taskAssignee(userId)   //指定个人任务查询
//                .or()
//                .taskCandidateUser(userId)    //指定组任务查询
                .taskCandidateOrAssigned(userId)
                .orderByTaskCreateTime()
                .desc()
                .list();

        List<LeaveApplication> leaveApplications = new ArrayList<>();
        for (Task task : tasks) {
            LeaveApplication leaveApplication = new LeaveApplication();
            Map<String, Object> processVariables = taskService.getVariables(task.getId());
            leaveApplication.setTaskId(task.getId());
            leaveApplication.setProcessInstanceID(task.getProcessInstanceId());
            leaveApplication.setUserId(String.valueOf(processVariables.get("taskUser")));
            leaveApplication.setUserName(String.valueOf(processVariables.get("userName")));
            leaveApplication.setStartDate((Date) processVariables.get("startDate"));
            leaveApplication.setNumDays(Integer.valueOf(String.valueOf(processVariables.get("numDays"))));
            leaveApplication.setDescription(String.valueOf(processVariables.get("description")));
            leaveApplications.add(leaveApplication);
        }
        System.out.println("* * * 获取审批管理列表 * * *");
        return leaveApplications;
    }

    /**
     * 获取审批管理列表
     */
    @RequestMapping(value = "/turnDown")
    @ResponseBody
    public List<String> turnDown(@Param("processInstanceID") String processInstanceID) {
        List<String> keys = new ArrayList<>();

        //获取历史审批节点信息
        List<HistoricTaskInstance> list = processEngine.getHistoryService()//与历史数据（历史表）相关的service
                .createHistoricTaskInstanceQuery()//创建历史任务实例查询
                .processInstanceId(processInstanceID)
                .finished()
                .orderByHistoricActivityInstanceId()
                .asc()
                .list();
        list.forEach(s -> keys.add(s.getTaskDefinitionKey()));  //偷懒，直接返回节点名

        List<String> keysTemplate = new ArrayList<>();
        for(int i=0;i<keys.size();i++){                   //驳回后，查询历史审批人会有重复
            if(!keysTemplate.contains(keys.get(i))){      //去重复。。感觉有点多余反正这样写有问题
                keysTemplate.add(keys.get(i));            //肯定在查询的时候可以优化
            }
        }

        return keysTemplate;
    }

    @RequestMapping("/rejectNode/{processInstanceID}/{taskName}")
    public String rejectNode(@PathVariable String processInstanceID,@PathVariable String taskName){
        //获取当前审批人
        List<Task> tasks = processEngine.getTaskService().createTaskQuery().processInstanceId(processInstanceID).list();
        List<String> keys = new ArrayList<>();
        tasks.forEach(t -> keys.add(t.getTaskDefinitionKey()));
        processEngine.getRuntimeService().createChangeActivityStateBuilder()
                .processInstanceId(processInstanceID)
                .moveActivityIdsToSingleActivityId(keys, taskName)
                .changeState();
        return "index";
    }


    /**
     * 获取审批管理列表
     */
    @RequestMapping(value = "/personlist")
    @ResponseBody
    public List<LeaveApplication> personlist(@Param("userId") String userId) throws IllegalAccessException {

        //，查询审批未完成的
        List<ProcessInstance> processInstanceList = processEngine.getRuntimeService()
                .createProcessInstanceQuery()
                .startedBy(userId)
                .list();
        List<LeaveApplication> leaveApplications = new ArrayList<>();

        int i = 1;
        for (ProcessInstance processInstance : processInstanceList) {
            Map<String, Object> processVariables = processEngine.getRuntimeService()
                    .getVariables(processInstance.getId());
            LeaveApplication leaveApplication = new LeaveApplication();
            leaveApplication.setProcessInstanceID(processInstance.getId());
            leaveApplication.setUserId(String.valueOf(processVariables.get("taskUser")));
            leaveApplication.setUserName(String.valueOf(processVariables.get("userName")));
            leaveApplication.setStartDate((Date) processVariables.get("startDate"));
            leaveApplication.setNumDays(Integer.valueOf(String.valueOf(processVariables.get("numDays"))));
            leaveApplication.setDescription(String.valueOf(processVariables.get("description")));
            leaveApplication.setIsok("待审批");

            leaveApplications.add(leaveApplication);
            i += 1;
        }

        //审批完成的
        List<HistoricProcessInstance> hpis = processEngine
                .getHistoryService()
                .createHistoricProcessInstanceQuery()
                .involvedUser(userId)
                .finished()
                .orderByProcessInstanceEndTime()
                .desc()
                .list();
        for (HistoricProcessInstance hpi : hpis) {

            LeaveApplication leaveApplication = new LeaveApplication();

            List<HistoricVariableInstance> hvis = processEngine.getHistoryService()
                    .createHistoricVariableInstanceQuery()
                    .processInstanceId(hpi.getId())
                    .orderByProcessInstanceId()
                    .desc()
                    .list();

            Class leaveClass = leaveApplication.getClass();

            Field[] fs = leaveClass.getDeclaredFields();


            for (HistoricVariableInstance hvi : hvis) {
                for (Field f : fs) {
                    f.setAccessible(true);
                    if (f.getName().equals(hvi.getVariableName())) {
                        f.set(leaveApplication, hvi.getValue());
                    }
                }
            }
            leaveApplication.setIsok("审批通过");
            leaveApplications.add(leaveApplication);
        }



        return leaveApplications;
    }



    /**
     * 批准
     *
     * @param taskId 任务ID
     */
    @RequestMapping(value = "apply")
    public String apply(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("流程不存在");
        }
        //通过审核
        HashMap<String, Object> map = new HashMap<>();
        map.put("outcome", "通过");
        taskService.complete(taskId, map);
        System.out.println("审批通过! ! !");
        return "Approval";
    }

    /**
     * 拒绝
     */
    @RequestMapping(value = "reject")
    public String reject(String taskId) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("outcome", "驳回");
        taskService.complete(taskId, map);
        System.out.println("审批驳回! ! !");
        return "Approval";
    }

    /**
     * 生成流程图
     *
     * @param processId 任务ID
     */
    @RequestMapping(value = "processDiagram")
    public void genProcessDiagram(HttpServletResponse httpServletResponse, String processId) throws Exception {
        ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(processId).singleResult();

        //流程走完的不显示图
        if (pi == null) {
            return;
        }
//        Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        //使用流程实例ID，查询正在执行的执行对象表，返回流程实例对象
//        String InstanceId = task.getProcessInstanceId();
        List<Execution> executions = runtimeService
                .createExecutionQuery()
                .processInstanceId(processId)
                .list();

        //得到正在执行的Activity的Id
        List<String> activityIds = new ArrayList<>();
        List<String> flows = new ArrayList<>();
        for (Execution exe : executions) {
            List<String> ids = runtimeService.getActiveActivityIds(exe.getId());
            activityIds.addAll(ids);
        }

        //获取流程图
        BpmnModel bpmnModel = repositoryService.getBpmnModel(pi.getProcessDefinitionId());
        ProcessEngineConfiguration engconf = processEngine.getProcessEngineConfiguration();
        ProcessDiagramGenerator diagramGenerator = engconf.getProcessDiagramGenerator();
        InputStream in = diagramGenerator.generateDiagram(bpmnModel, "png", activityIds, flows, engconf.getActivityFontName(), engconf.getLabelFontName(), engconf.getAnnotationFontName(), engconf.getClassLoader(), 1.0);
        OutputStream out = null;
        byte[] buf = new byte[1024];
        int legth = 0;
        try {
            out = httpServletResponse.getOutputStream();
            while ((legth = in.read(buf)) != -1) {
                out.write(buf, 0, legth);
            }
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }
}
