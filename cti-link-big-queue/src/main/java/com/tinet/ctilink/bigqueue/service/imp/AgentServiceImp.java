package com.tinet.ctilink.bigqueue.service.imp;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.github.davidmarquis.redisscheduler.RedisTaskScheduler;
import com.tinet.ctilink.bigqueue.entity.ActionResponse;
import com.tinet.ctilink.bigqueue.entity.Agent;
import com.tinet.ctilink.bigqueue.entity.CallMember;
import com.tinet.ctilink.bigqueue.entity.Queue;
import com.tinet.ctilink.bigqueue.inc.BigQueueCacheKey;
import com.tinet.ctilink.bigqueue.inc.BigQueueConst;
import com.tinet.ctilink.bigqueue.inc.BigQueueMacro;
import com.tinet.ctilink.cache.RedisService;
import com.tinet.ctilink.inc.Const;
import com.tinet.ctilink.json.JSONObject;
import com.tinet.ctilink.util.RedisLock;

@Service
public class AgentServiceImp {
	@Autowired
	RedisService redisService;
	@Autowired
	QueueServiceImp queueService;
	@Autowired
	MemberServiceImp memberService;
	@Autowired
    @Qualifier("wrapupEndTaskScheduler")
    private RedisTaskScheduler wrapupEndTaskScheduler;
	
	public ActionResponse login(Map params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		String bindTel = params.get("bindTel").toString();
		String bindType = params.get("bindType").toString();
		
		Agent agent = getAgent(enterpriseId, cno);
		
		if(agent != null){
			//查询是bindTel否在绑定电话里
			
			//从路由逻辑中获取inteface
			//更新bind_tel的绑定状态
			//加入到queue_member中
			//检查坐席是否在任何队列中

			
			response = ActionResponse.createSuccessResponse();
			return response;
		}else {
			response = ActionResponse.createFailResponse(-1, "no such agent");
		}
		
		return response;
	}
	public ActionResponse logout(Map params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		return response;
	}
	public ActionResponse pause(Map params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		String description = params.get("description").toString();
		Agent agent = getAgent(enterpriseId, cno);
		
		if(agent != null){
			RedisLock memberLock = memberService.lockMember(enterpriseId, cno);
			if(memberLock != null){
				try{
					Integer loginStatus = memberService.getLoginStatus(enterpriseId, cno);
					switch(loginStatus){
					case BigQueueConst.MEMBER_LOGIN_STATUS_OFFLINE:
						response = ActionResponse.createFailResponse(-1, "not logined");
						break;
					case BigQueueConst.MEMBER_LOGIN_STATUS_READY:
					case BigQueueConst.MEMBER_LOGIN_STATUS_PAUSE:
						memberService.setLoginStatus(enterpriseId, cno, BigQueueConst.MEMBER_LOGIN_STATUS_PAUSE);
						agent.setPauseDescription(description);
						response = ActionResponse.createSuccessResponse();
						break;
					case BigQueueConst.MEMBER_LOGIN_STATUS_WRAPUP:
						memberService.setLoginStatus(enterpriseId, cno, BigQueueConst.MEMBER_LOGIN_STATUS_PAUSE);
						agent.setPauseDescription(description);
						String taskId = String.format(BigQueueConst.WRAPUP_END_TASK_ID, cno);
						wrapupEndTaskScheduler.unschedule(taskId);
						response = ActionResponse.createSuccessResponse();
						break;
					}
				}finally{
					memberService.unlockMember(memberLock);
				}
			}else{
				response = ActionResponse.createFailResponse(-1, "fail to get lock");
			}
		}else {
			response = ActionResponse.createFailResponse(-1, "no such agent");
		}
		
		return response;
	}
	public ActionResponse unpause(Map params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		Agent agent = getAgent(enterpriseId, cno);
		
		if(agent != null){
			RedisLock memberLock = memberService.lockMember(enterpriseId, cno);
			if(memberLock != null){
				try{
					Integer loginStatus = memberService.getLoginStatus(enterpriseId, cno);
					switch(loginStatus){
					case BigQueueConst.MEMBER_LOGIN_STATUS_OFFLINE:
						response = ActionResponse.createFailResponse(-1, "not logined");
						break;
					case BigQueueConst.MEMBER_LOGIN_STATUS_READY:
						response = ActionResponse.createFailResponse(-1, "already ready");
						break;
					case BigQueueConst.MEMBER_LOGIN_STATUS_PAUSE:
						memberService.setLoginStatus(enterpriseId, cno, BigQueueConst.MEMBER_LOGIN_STATUS_READY);
						response = ActionResponse.createSuccessResponse();
						break;
					case BigQueueConst.MEMBER_LOGIN_STATUS_WRAPUP:
						memberService.setLoginStatus(enterpriseId, cno, BigQueueConst.MEMBER_LOGIN_STATUS_READY);
						String taskId = String.format(BigQueueConst.WRAPUP_END_TASK_ID, cno);
						wrapupEndTaskScheduler.unschedule(taskId);
						response = ActionResponse.createSuccessResponse();
						break;
					}
				}finally{
					memberService.unlockMember(memberLock);
				}
			}else{
				response = ActionResponse.createFailResponse(-1, "fail to get lock");
			}
		}else {
			response = ActionResponse.createFailResponse(-1, "no such agent");
		}
		
		return response;
	}
	public ActionResponse changeBindTel(Map params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		String description = params.get("description").toString();
		
		return response;
	}

	private List<Map<String, Object>> queueEntry(String enterpriseId, String qno){
		List<Map<String, Object>> queueEntryList = new ArrayList<Map<String, Object>>();
		Set<String> entrySet = queueService.getQueueEntrySet(enterpriseId, qno);
		Integer position = 1;
		for(String entry: entrySet){
			Map<String, Object> queueEntryMap = new HashMap<String, Object>();
			String uniqueId = entry;
			queueEntryMap.put("position", position);
			position++;
			Integer joinTime = (Integer)queueService.getQueueEntryInfo(uniqueId, "join_time");
			queueEntryMap.put("joinTime", joinTime);//排队客户加入时间
			Integer waitTime = new Long(new Date().getTime()/1000).intValue() - joinTime;
			if(waitTime < 0) waitTime = 0;
			queueEntryMap.put("waitTime", waitTime);//排队客户等待时间
			Integer priority = (Integer)queueService.getQueueEntryInfo(uniqueId, "priority");
			queueEntryMap.put("priority", priority);//排队客户VIP级别
			String customerNumber = (String)queueService.getQueueEntryInfo(uniqueId, "customer_number");
			queueEntryMap.put("customerNumber", customerNumber);
			queueEntryMap.put("uniqueId", uniqueId);
			Integer startTime = (Integer)queueService.getQueueEntryInfo(uniqueId, "start_time");
			queueEntryMap.put("startTime", startTime);
			Integer overflow = (Integer)queueService.getQueueEntryInfo(uniqueId, "overflow");
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
			Agent agent = getAgent(enterpriseId, cno);
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
				if(agent.getMonitoredType() != null){
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
			Agent agent = getAgent(enterpriseId, cno);
			if(agent != null){	
				Set<String> queueSet = BigQueueMacro.getCurrentMemberQueueMap().get(enterpriseId + cno);
				for(String thisQno: queueSet){
					Map<String, Object> thisQueueStatusMap = new HashMap<String, Object>();
					List<Map<String, Object>> thisMemberStatusMap = memberStatus(enterpriseId, qno);
					List<Map<String, Object>> thisQueueEntryList = queueEntry(enterpriseId, qno);
					Map<String, Object> thisQueueParamsMap = queueParams(enterpriseId, qno);
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
	
	public Agent getAgent(String enterpriseId, String cno){
		String agentKey = String.format(BigQueueCacheKey.AGENT, enterpriseId);
		String res = (String) redisService.hget(Const.REDIS_DB_CTI_INDEX, agentKey, cno);
		if(StringUtils.isNotEmpty(res)){
			JSONObject json = new JSONObject();
			json.fromObject(res);
			return json.getBean(Agent.class);
		}
		return null;
	}
	private void initAgent(String enterpriseId, String cno){
		//MEMBER_LOGIN_STATUS
		//QUEUE_MEMBER
		//MEMBER_DEVICE_STATUS
		//MEMBER_LOGIN_STATUS
		//MEMBER_LOCK
		
	}
	
}
