package com.nie;

import com.nie.entity.Leave;
import com.nie.service.LeaveService;
import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngines;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class FlowableSpringbootApplicationTests {

    @Autowired
    private ProcessEngine processEngine;
    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private LeaveService leaveService;

    @Test
    public void saveLeave(){

        Leave leave = new Leave();
        leave.setName("聂永真");
        leave.setStartDate(new Date());
        leave.setNumDays(2);
        leave.setDescription("该吃饭了");
        leave.setIsok("审批中");
        int isok = leaveService.saveLeave(leave);
        System.out.println(isok);
    }





    /***
     * 发起流程
     */
    @Test
    public void contextLoads() {

        Deployment deployment = processEngine.getRepositoryService().createDeployment()
                .name("邮件流程")
                .addClasspathResource("processes\\Email.bpmn20.xml")
                .deploy();
        System.out.println("部署ID" + deployment.getId());

        ProcessInstance pi = processEngine.getRuntimeService()
                .startProcessInstanceByKey("sendEmail");

        System.out.println("流程实例ID" + pi.getId());
        System.out.println("流程定义ID" + pi.getProcessDefinitionId());

        List<Task> taskList = processEngine
                .getTaskService()
                .createTaskQuery()
                .taskAssignee("rengar")
                .orderByTaskCreateTime()
                .desc()
                .list();
        for (Task task : taskList) {
            processEngine.getTaskService().complete(task.getId());
        }

        //9.判断流程是否结束
        ProcessInstance nowPi2 = processEngine.getRuntimeService()
                .createProcessInstanceQuery()
                .processInstanceId(pi.getId())
                .singleResult();
        if (nowPi2 == null) {
            System.out.println("流程结束");
        }

        //3.查询执行对象表,使用流程实例ID和当前活动的名称（receivetask1）
        String processInstanceId = pi.getId();//得到流程实例ID
        Execution execution1 = processEngine.getRuntimeService()
                .createExecutionQuery()
                .processInstanceId(processInstanceId)//流程实例ID
                .activityId("ServiceTask")//当前活动的名称
                .singleResult();

        //4.使用流程变量设置当日的销售额
        processEngine.getRuntimeService().setVariable(execution1.getId(), "当日销售额", 20000);

        //5.向后执行一步
        processEngine.getRuntimeService()
                .signalEventReceived(execution1.getId());


        //9.判断流程是否结束
        ProcessInstance nowPi = processEngine.getRuntimeService()
                .createProcessInstanceQuery()
                .processInstanceId(pi.getId())
                .singleResult();
        if (nowPi == null) {
            System.out.println("流程结束");
        }
    }

    /**
     * 查看已经完成的任务和当前在执行的任务
     */
    @Test
    public void findHistoryTask() {
        ProcessEngine defaultProcessEngine = ProcessEngines.getDefaultProcessEngine();
        //如果只想获取到已经执行完成的，那么就要加入completed这个过滤条件
        List<HistoricTaskInstance> historicTaskInstances1 = defaultProcessEngine.getHistoryService()
                .createHistoricTaskInstanceQuery()
                .taskDeleteReason("completed")
                .list();
        //如果只想获取到已经执行完成的，那么就要加入completed这个过滤条件
        List<HistoricTaskInstance> historicTaskInstances2 = defaultProcessEngine.getHistoryService()
                .createHistoricTaskInstanceQuery()
                .list();
        System.out.println("执行完成的任务：" + historicTaskInstances1.size());
        System.out.println("所有的总任务数（执行完和当前未执行完）：" + historicTaskInstances2.size());
    }

    /**
     * 查询所有的流程定义
     */
    @Test
    public void findProcessDefinition() {
        List<ProcessDefinition> list = processEngine.getRepositoryService()// 与流程定义和部署对象先相关的service
                .createProcessDefinitionQuery()// 创建一个流程定义的查询
                /** 指定查询条件，where条件 */
                // .deploymentId(deploymentId) //使用部署对象ID查询
                // .processDefinitionId(processDefinitionId)//使用流程定义ID查询
                // .processDefinitionNameLike(processDefinitionNameLike)//使用流程定义的名称模糊查询

            /* 排序 */
                .orderByProcessDefinitionVersion().asc()
                // .orderByProcessDefinitionVersion().desc()

            /* 返回的结果集 */
                .list();// 返回一个集合列表，封装流程定义
        // .singleResult();//返回惟一结果集
        // .count();//返回结果集数量
        // .listPage(firstResult, maxResults);//分页查询

        if (list != null && list.size() > 0) {
            for (ProcessDefinition pd : list) {
                System.out.println("流程定义ID:" + pd.getId());// 流程定义的key+版本+随机生成数
                System.out.println("流程定义的名称:" + pd.getName());// 对应helloworld.bpmn文件中的name属性值
                System.out.println("流程定义的key:" + pd.getKey());// 对应helloworld.bpmn文件中的id属性值
                System.out.println("流程定义的版本:" + pd.getVersion());// 当流程定义的key值相同的相同下，版本升级，默认1
                System.out.println("资源名称bpmn文件:" + pd.getResourceName());
                System.out.println("资源名称png文件:" + pd.getDiagramResourceName());
                System.out.println("部署对象ID：" + pd.getDeploymentId());
                System.out.println("是否暂停：" + pd.isSuspended());
                System.out.println("#########################################################");
            }
        } else {
            System.out.println("没有流程正在运行。");
        }
    }

    /**
     * 查询流程状态（判断流程正在执行，还是结束）
     */
    @Test
    public void isProcessEnd() {
        String processInstanceId = "57";
        ProcessInstance pi = processEngine.getRuntimeService()//表示正在执行的流程实例和执行对象
                .createProcessInstanceQuery()//创建流程实例查询
                .processInstanceId(processInstanceId)//使用流程实例ID查询
                .singleResult();

        if (pi == null) {
            System.out.println("流程已经结束");
        } else {
            System.out.println("流程没有结束");
        }
    }

    /**
     * 查询所有的流程定义
     */
    @Test
    public void testQueryAllPD() {
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        List<ProcessDefinition> pdList = processEngine.getRepositoryService()
                .createProcessDefinitionQuery()
                .orderByProcessDefinitionVersion()
                .desc()
                .list();
        for (ProcessDefinition pd : pdList) {
            System.out.println(pd.getName());
            System.out.println(pd.getResourceName());
            System.out.println("* * * * * * * * * *");
        }
    }


    /***
     * 我发起的任务
     * 执行中的任务
     * - [流程实例] -[流程变量]
     */
    @Test
    public void instanceList() {
        List<ProcessInstance> processInstanceList = processEngine.getRuntimeService()
                .createProcessInstanceQuery()
                .startedBy("nie233")
                .list();
        int i = 1;
        for (ProcessInstance processInstance : processInstanceList) {
            System.out.println("=================【 任务" + i + " 】=================");
            System.out.println("Id：" + processInstance.getId());
            Map<String, Object> processVariables = processEngine.getRuntimeService()
                    .getVariables(processInstance.getId());
            System.out.println("流程变量：" + processVariables.toString());
            System.out.println("Name：" + processInstance.getName());
            System.out.println("BusinessKey：" + processInstance.getBusinessKey());
            System.out.println("ProcessVariables：" + processInstance.getProcessVariables().toString());
            System.out.println("StartTime：" + processInstance.getStartTime());
            System.out.println("ActivityId：" + processInstance.getActivityId());
            System.out.println("StartUserId：" + processInstance.getStartUserId());
            i += 1;
        }
    }


    /***
     * 发起人 个人的
     * 历史流程实例   -- [历史变量]
     */
    @Test
    public void historicTaskList() {
        List<HistoricProcessInstance> hpis = processEngine
                .getHistoryService()
                .createHistoricProcessInstanceQuery()
//                .processInstanceBusinessKey("Expensenie233")
                .involvedUser("nie233")
//                .finished()
                .orderByProcessInstanceEndTime()
                .desc()
                .list();
        for (HistoricProcessInstance hpi : hpis) {
            System.out.println("===========================");
            System.out.println("Id==" + hpi.getId());

            List<HistoricVariableInstance> hvis = processEngine.getHistoryService()
                    .createHistoricVariableInstanceQuery()
                    .processInstanceId(hpi.getId())
                    .orderByProcessInstanceId()
                    .desc()
                    .list();
            System.out.println("* * * * * * * * * * * * * * * * * *");
            for (HistoricVariableInstance hvi : hvis) {
                System.out.println("VariableName==" + hvi.getVariableName() + ":   Value==" + hvi.getValue());
            }
            System.out.println("* * * * * * * * * * * * * * * * * *");
            System.out.println("processInstanceId==" + hpi.getProcessDefinitionId());
            System.out.println("StartUserId==" + hpi.getStartUserId());
            System.out.println("BusinessKey==" + hpi.getBusinessKey());
            System.out.println("开始时间==" + hpi.getStartTime());
            System.out.println("结束时间==" + hpi.getEndTime());
            System.out.println("花费时间==" + hpi.getDurationInMillis() + "毫秒");
            System.out.println("Name==" + hpi.getName());
            System.out.println("ProcessVariables==" + hpi.getProcessVariables().toString());
            System.out.println("EndActivityId==" + hpi.getEndActivityId());
        }
    }

    /***
     * 历史变量实例查询
     */
    @Test
    public void hist() {
        List<HistoricVariableInstance> hvis = processEngine.getHistoryService()
                .createHistoricVariableInstanceQuery()
                .processInstanceId("9")     //流程ID
                .orderByVariableName()
                .desc()
                .list();
        for (HistoricVariableInstance hvi : hvis) {
            System.out.println("===========================");
            System.out.println("Id==" + hvi.getId());
            System.out.println("CreateTime==" + hvi.getCreateTime());
            System.out.println("TaskId==" + hvi.getTaskId());
            System.out.println("VariableName==" + hvi.getVariableName() + ":   Value==" + hvi.getValue());
        }
    }


    /**
     * 历史任务实例
     */
    @Test
    public void findTask() {
        List<HistoricTaskInstance> list = processEngine.getHistoryService()//与历史数据（历史表）相关的service
                .createHistoricTaskInstanceQuery()//创建历史任务实例查询
//                .processInstanceId("25001")
                .taskAssignee("rengar")
                .orderByHistoricActivityInstanceId().asc()
                .finished()  //查询已完成
                .list();

        if (list != null && list.size() > 0) {
            int i = 1;
            for (HistoricTaskInstance hti : list) {
                System.out.println("=================【 任务" + i + " 】=================");
                System.out.println("编号：" + hti.getId());
                System.out.println("编号: " + hti.getTaskDefinitionKey());
                System.out.println(hti.getName());
                List<HistoricVariableInstance> hvis = processEngine.getHistoryService()
                        .createHistoricVariableInstanceQuery()
                        .processInstanceId(hti.getProcessInstanceId())
                        .orderByProcessInstanceId()
                        .desc()
                        .list();
                System.out.println("* * * * * * * * * * * * * * * * * *");
                for (HistoricVariableInstance hvi : hvis) {
                    System.out.println("变量名==" + hvi.getVariableName() + ":  变量值==" + hvi.getValue());
                }
                System.out.println("* * * * * * * * * * * * * * * * * *");
                System.out.println("任务负责人：" + hti.getAssignee());
                System.out.println("审批名称：" + hti.getName());
                System.out.println("创建时间：" + hti.getCreateTime());
                System.out.println("流程实例id：" + hti.getProcessInstanceId());
                System.out.println("开始时间：" + hti.getStartTime());
                System.out.println("结束时间：" + hti.getEndTime());
                System.out.println("花费时间：" + hti.getDurationInMillis() + "毫秒");

                i += 1;
            }
        }
    }

    /***
     * 查看个人任务
     */
    @Test
    public void querypersonTask() {
        String assignee = "bx1";
        List<Task> taskList = processEngine
                .getTaskService()
                .createTaskQuery()
                .taskCandidateOrAssigned(assignee)
//                .taskCandidateUser(assignee)  //组成员
//                .taskAssignee(assignee)   //个人审批人
//                            .processDefinitionKey("Expense")   //可指定任务类型
                .orderByTaskCreateTime()
                .desc()
                .list();
        int i = 1;
        for (Task task : taskList) {
            System.out.println("=================【 任务" + i + " 】=================");
            System.out.println("编号：" + task.getId());
            Map<String, Object> processVariables = processEngine.getTaskService()
                    .getVariables(task.getId());
            System.out.println("流程变量：" + processVariables.toString());
            System.out.println("任务负责人：" + task.getAssignee());
            System.out.println("审批名称：" + task.getName());
            System.out.println("创建时间：" + task.getCreateTime());
            System.out.println("流程实例ID:" + task.getProcessInstanceId());
            System.out.println("执行对象ID:" + task.getExecutionId());
            System.out.println("流程定义ID:" + task.getProcessDefinitionId());
            i += 1;
        }
    }

    /***
     * 历史活动实例查询
     * 某一次流程的执行信息
     * @throws Exception
     */
    @Test
    public void queryHistoricActivitiInstance() throws Exception {
        String processInstanceId = "15001";
        List<HistoricActivityInstance> haiList = processEngine.getHistoryService()
                .createHistoricActivityInstanceQuery() // 创建历史活动实例查询
//                .taskAssignee("rengar")
                .processInstanceId(processInstanceId)// 使用流程实例id查询
//                .activityName("开始")
                .orderByHistoricActivityInstanceStartTime().asc()// 排序条件
                .finished()
                .list();// 执行查询
        System.out.println("* * * * * * * * * * * * * * * * * *");
        for (HistoricActivityInstance hai : haiList) {
            System.out.print("活动ID:" + hai.getActivityId() + "，");
            System.out.print("活动名:" + hai.getActivityName() + "，");
            System.out.print("活动类型:" + hai.getActivityType() + "，");
            System.out.print("流程实例ID:" + hai.getProcessInstanceId() + "，");
            System.out.print("审批人:" + hai.getAssignee() + "，");
            System.out.print("开始时间:" + hai.getStartTime() + "，");
            System.out.print("结束时间:" + hai.getEndTime() + "，");
            System.out.println("花费时间:" + hai.getDurationInMillis());
        }
    }

    /***
     * 活动任务执行
     */
    @Test
    public void receiveTask() {
        // 3查询是否有一个执行对象在描述”汇总当日销售额“
        Execution e1 = processEngine.getRuntimeService()//
                .createExecutionQuery()//
                .processInstanceId("9")//
                .activityId("EmailTask")
                .singleResult();
        System.out.println(e1.getId());
        System.out.println(e1.getName());
        processEngine.getRuntimeService().signalEventReceived(e1.getId());
    }

    /***
     * 驳回到指定节点
     */
    @Test
    public void turnDown(){
        //获取当前审批人
        List<Task> tasks = processEngine.getTaskService().createTaskQuery().processInstanceId("2501").list();

//        tasks.forEach(t -> System.out.println(t.getTaskDefinitionKey()));

        List<String> keys = new ArrayList<>();
        tasks.forEach(t -> keys.add(t.getTaskDefinitionKey()));
        keys.forEach(t -> System.out.println(t));
        System.out.println("* * * * * * * * *");

        //获取历史审批节点信息
        List<HistoricTaskInstance> list = processEngine.getHistoryService()//与历史数据（历史表）相关的service
                .createHistoricTaskInstanceQuery()//创建历史任务实例查询
                .processInstanceId("2501")
                .finished()
                .orderByHistoricActivityInstanceId().asc()
                .list();
        List<String> values = new ArrayList<>();
        list.forEach(s -> values.add(s.getTaskDefinitionKey()));
        list.forEach(s -> System.out.println(s.getAssignee()));
        System.out.println("* * * * * * * * * *");
        values.forEach(s -> System.out.println(s));

        //驳回到指定审批人
        processEngine.getRuntimeService().createChangeActivityStateBuilder()
                .processInstanceId("15001")
                .moveActivityIdsToSingleActivityId(keys, "directorTak")
                .changeState();
    }

    /**
     * 可以分配个人任务从一个人到另一个人
     */
    @Test
    public void setAssigneeTask(){
        String taskId = "taskId";
        String userId = "分配人";
        processEngine.getTaskService().setAssignee(taskId, userId);
    }


    /***
     * 添加组成员
     */
    @Test
    public void addUser(){
        Group group = processEngine.getIdentityService().newGroup("BOSS");
        group.setName("BOSS");
        group.setType("manage");
        processEngine.getIdentityService().saveGroup(group);

        User user2 = processEngine.getIdentityService().newUser("pig");
        processEngine.getIdentityService().saveUser(user2);

        processEngine.getIdentityService().createMembership("pig","BOSS");
    }



    /***
     * 获取所有节点 taskkey
     */
    @Test
    public void huoqu() {
        String processDefinitionId = "Expense:2:10006";
            BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
            Process mainProcess = bpmnModel.getMainProcess();
            List<String> nodes = new ArrayList<>();
            Collection<FlowElement> flowElements = mainProcess.getFlowElements();
            String startEventId = "";
            //获取所有节点排除连线
            for (FlowElement flowElement : flowElements) {
                if (flowElement instanceof StartEvent) {
                    StartEvent startEvent = (StartEvent) flowElement;
                    startEventId = startEvent.getId();
                    String startEventName = startEvent.getName();
                    nodes.add(startEventName);
                    break;
                }
            }
            FlowElement flowElement = mainProcess.getFlowElement(startEventId);
            while (!(flowElement instanceof EndEvent)) {
                if (flowElement instanceof FlowNode) {
                    FlowNode flowElementNode = (FlowNode) flowElement;
                    SequenceFlow outgoingFlows = flowElementNode.getOutgoingFlows().get(0);
                    if (outgoingFlows.getTargetRef() != null) {
                        String targetRef = outgoingFlows.getTargetRef();
                        flowElement = mainProcess.getFlowElement(targetRef);
                        String nodesName = flowElement.getName();
                        String nodesid = flowElement.getId();
                        nodes.add(nodesName);
                        nodes.add(nodesid);
                    }
                } else if (flowElement instanceof SequenceFlow) {
                    SequenceFlow sequenceFlow = (SequenceFlow) flowElement;
                    if (sequenceFlow.getTargetRef() != null) {
                        String targetRef = sequenceFlow.getTargetRef();
                        flowElement = mainProcess.getFlowElement(targetRef);
                    }
                }
            }
        System.out.println(nodes.toString());
    }

}

