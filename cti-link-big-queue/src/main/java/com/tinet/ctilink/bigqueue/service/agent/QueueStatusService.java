package com.tinet.ctilink.bigqueue.service.agent;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.pagehelper.StringUtil;
import com.tinet.ctilink.bigqueue.ami.action.GetVarActionService;
import com.tinet.ctilink.bigqueue.ami.action.OriginateActionService;
import com.tinet.ctilink.bigqueue.entity.ActionResponse;
import com.tinet.ctilink.bigqueue.entity.CallAgent;
import com.tinet.ctilink.bigqueue.entity.CallMember;
import com.tinet.ctilink.bigqueue.inc.BigQueueConst;
import com.tinet.ctilink.bigqueue.service.imp.AgentServiceImp;
import com.tinet.ctilink.bigqueue.service.imp.ChannelServiceImp;
import com.tinet.ctilink.bigqueue.service.imp.MemberServiceImp;
import com.tinet.ctilink.bigqueue.service.imp.QueueEventServiceImp;
import com.tinet.ctilink.bigqueue.service.imp.QueueServiceImp;
import com.tinet.ctilink.cache.RedisService;
import com.tinet.ctilink.conf.model.Queue;
import com.tinet.ctilink.conf.model.QueueMember;
import com.tinet.ctilink.scheduler.RedisTaskScheduler;

@Component
public class QueueStatusService {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired
	AgentServiceImp agentService;
	
	@Autowired
	RedisService redisService;
	@Autowired
	QueueServiceImp queueService;
	@Autowired
	MemberServiceImp memberService;
	@Autowired
	QueueEventServiceImp queueEventService;
	@Autowired
	private ChannelServiceImp channelService;
	@Autowired
	private RedisTaskScheduler redisTaskScheduler;
	
	@Autowired
	GetVarActionService getVarActionService;
	@Autowired
	OriginateActionService originateActionService;
	private List<Map<String, Object>> queueEntry(String enterpriseId, String qno){
		List<Map<String, Object>> queueEntryList = new ArrayList<Map<String, Object>>();
		Set<String> entrySet = queueService.getQueueEntrySet(enterpriseId, qno);
		Integer position = 1;
		for(String entry: entrySet){
			Map<String, Object> queueEntryMap = new HashMap<String, Object>();
			String uniqueId = entry;
			queueEntryMap.put("position", position);
			position++;
			Integer joinTime = (Integer)queueService.getQueueEntryInfo(uniqueId, "join_time", Integer.class);
			queueEntryMap.put("joinTime", joinTime);//排队客户加入时间
			Integer waitTime = new Long(new Date().getTime()/1000).intValue() - joinTime;
			if(waitTime < 0) waitTime = 0;
			queueEntryMap.put("waitTime", waitTime);//排队客户等待时间
			Integer priority = (Integer)queueService.getQueueEntryInfo(uniqueId, "priority", Integer.class);
			queueEntryMap.put("priority", priority);//排队客户VIP级别
			String customerNumber = (String)queueService.getQueueEntryInfo(uniqueId, "customer_number", String.class);
			queueEntryMap.put("customerNumber", customerNumber);
			queueEntryMap.put("uniqueId", uniqueId);
			Integer startTime = (Integer)queueService.getQueueEntryInfo(uniqueId, "start_time", Integer.class);
			queueEntryMap.put("startTime", startTime);
			Integer overflow = (Integer)queueService.getQueueEntryInfo(uniqueId, "overflow", Integer.class);
			queueEntryMap.put("overflow", overflow);
			
			Integer npIndex = queueService.getQueueEntryNpIndex(enterpriseId, qno, uniqueId);
			if(npIndex != null){
				queueEntryMap.put("callStatus", "going");
			}
			queueEntryList.add(queueEntryMap);
		}
		return queueEntryList;
	}
	private Map<String, Object> queueParams(String enterpriseId, String qno){
		Queue queue = queueService.getFromConfCache(enterpriseId, qno);
		Map<String, Object> queueParamsMap = new HashMap<String, Object>();
		queueParamsMap.put("max", queue.getMaxLen()); //队列中最大等待座席数
		Integer calls = queueService.getQueueEntryCount(enterpriseId, qno);
		queueParamsMap.put("calls", calls); //队列当前等待电话数
		Integer holdTime = queueService.getQueueStatistic(enterpriseId, qno, "hold_time");
		queueParamsMap.put("holdTime", holdTime);//队列中电话接通平均等待时长
		Integer talkTime = queueService.getQueueStatistic(enterpriseId, qno, "talk_time");
		queueParamsMap.put("talkTime", talkTime);//队列中电话接通平均通话时长
		Integer abandoned = queueService.getQueueStatistic(enterpriseId, qno, "abandoned");
		queueParamsMap.put("abandoned", abandoned);//队列中接通电话数
		Integer completed = queueService.getQueueStatistic(enterpriseId, qno, "completed");
		queueParamsMap.put("completed", completed);//队列中放弃电话数
		queueParamsMap.put("serviceLevel",queue.getServiceLevel());//队列服务水平描述
		Integer completedInSl = queueService.getQueueStatistic(enterpriseId, qno, "completed_in_sl");
		Integer serviceLevelPerf = 100;
		if(completedInSl > 0){
			serviceLevelPerf = completedInSl * 100 / completed;
		}
		queueParamsMap.put("serviceLevelPerf", serviceLevelPerf);//队列服务水平
		queueParamsMap.put("weight", queue.getWeight()); //队列优先级
		return queueParamsMap;
	}
	
    	
	private List<Map<String, Object>> memberStatus(String enterpriseId, String qno){
		List<Map<String, Object>> memberStatusList = new ArrayList<Map<String, Object>>();
		List<CallMember> memberList = queueService.getMembers(enterpriseId, qno);
		for(CallMember member: memberList){
			String cno = member.getCno();
			CallAgent agent = agentService.getCallAgent(enterpriseId, cno);
			Map<String, Object> memberStatusMap = new HashMap<String, Object>();
			memberStatusMap.put("cno", cno);
			memberStatusMap.put("bindTel", agent.getBindTel());
			Integer loginStatus = memberService.getLoginStatus(enterpriseId, cno);
			memberStatusMap.put("loginStatus", loginStatus);
			Integer loginStatusStartTime = memberService.getDeviceStatusStartTime(enterpriseId, cno);
			Integer duration = new Long(new Date().getTime()/1000).intValue() - loginStatusStartTime;
			if(duration < 0){duration = 0;}
			memberStatusMap.put("loginStatusDuration", duration);
			memberStatusMap.put("calls", member.getCalls());
			if(loginStatus.equals(BigQueueConst.MEMBER_LOGIN_STATUS_OFFLINE)){
				
			}else{
				Integer deviceStatus = memberService.getDeviceStatus(enterpriseId, cno);
				memberStatusMap.put("deviceStatus", deviceStatus);
				Integer deviceStatusStartTime = memberService.getDeviceStatusStartTime(enterpriseId, cno);
				duration = new Long(new Date().getTime()/1000).intValue() - deviceStatusStartTime;
				if(duration < 0){duration = 0;}
				memberStatusMap.put("deviceStatusDuration", duration);
				if(loginStatus.equals(BigQueueConst.MEMBER_LOGIN_STATUS_PAUSE)){
					memberStatusMap.put("pauseType", agent.getPauseType());
					memberStatusMap.put("pauseDescription", agent.getPauseDescription());
				}
				if(StringUtil.isNotEmpty(agent.getMonitoredType())){
					memberStatusMap.put("monitoredType", agent.getMonitoredType());
					memberStatusMap.put("monitoredObject", agent.getMonitoredObject());
					memberStatusMap.put("monitoredObjectType", agent.getMonitoredObjectType());
				}
				if(deviceStatus.equals(BigQueueConst.MEMBER_DEVICE_STATUS_INUSE)){
					memberStatusMap.put("busyDescription", agent.getBusyDescription());
					memberStatusMap.put("customerNumber", agent.getCurrentCustomerNumber());
					memberStatusMap.put("customerNumberType", agent.getCurrentCustomerNumberType());//通话号码
					memberStatusMap.put("customerNumberAreaCode", agent.getCurrentCustomerNumberAreaCode());//通话号码类型
					memberStatusMap.put("numberTrunk", agent.getCurrentNumberTrunk());
					memberStatusMap.put("hotline", agent.getCurrentHotline());
					memberStatusMap.put("callType", agent.getCurrentCallType());
					memberStatusMap.put("qno", agent.getCurrentQueue());
				}
				memberStatusMap.put("loginTime", agent.getLoginTime());
			}
			memberStatusList.add(memberStatusMap);
		}
		return memberStatusList;

	}
	
	public ActionResponse queueStatus(Map params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String qno = params.get("qno").toString();
		String cno = params.get("cno").toString();
		Map<String, Object> queueStatusMap = new HashMap<String, Object>();
		if(StringUtils.isNotEmpty(qno)){
			response = ActionResponse.createSuccessResponse();
			Map<String, Object> thisQueueStatusMap = new HashMap<String, Object>();
			List<Map<String, Object>> thisMemberStatusMap = memberStatus(enterpriseId, qno);
			List<Map<String, Object>> thisQueueEntryList = queueEntry(enterpriseId, qno);
			Map<String, Object> thisQueueParamsMap = queueParams(enterpriseId, qno);
			thisQueueStatusMap.put("memberStatus", thisMemberStatusMap);
			thisQueueStatusMap.put("queueEntry", thisQueueEntryList);
			thisQueueStatusMap.put("queueParams", thisQueueParamsMap);
			queueStatusMap.put(qno, thisQueueStatusMap);
			response.setValues(queueStatusMap);
		}else{
			CallAgent agent = agentService.getCallAgent(enterpriseId, cno);
			if(agent != null){	
				List<QueueMember> queueMemberList = agentService.getQueueMemberList(enterpriseId, cno);
		    	for(QueueMember queueMember: queueMemberList){
					Map<String, Object> thisQueueStatusMap = new HashMap<String, Object>();
					List<Map<String, Object>> thisMemberStatusMap = memberStatus(enterpriseId, queueMember.getQno());
					List<Map<String, Object>> thisQueueEntryList = queueEntry(enterpriseId, queueMember.getQno());
					Map<String, Object> thisQueueParamsMap = queueParams(enterpriseId, queueMember.getQno());
					thisQueueStatusMap.put("memberStatus", thisMemberStatusMap);
					thisQueueStatusMap.put("queueEntry", thisQueueEntryList);
					thisQueueStatusMap.put("queueParams", thisQueueParamsMap);
					queueStatusMap.put(qno, thisQueueStatusMap);
				}
				response = ActionResponse.createSuccessResponse();
				response.setValues(queueStatusMap);
	    	}
		}
		return response;
	}
}
