
package com.tinet.ctilink.bigqueue.service.imp;

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

import com.alibaba.dubbo.config.annotation.Reference;
import com.github.pagehelper.StringUtil;
import com.tinet.ctilink.bigqueue.entity.ActionResponse;
import com.tinet.ctilink.bigqueue.entity.CallAgent;
import com.tinet.ctilink.bigqueue.entity.CallMember;
import com.tinet.ctilink.bigqueue.inc.BigQueueCacheKey;
import com.tinet.ctilink.bigqueue.inc.BigQueueConst;
import com.tinet.ctilink.bigqueue.service.AgentService;
import com.tinet.ctilink.cache.CacheKey;
import com.tinet.ctilink.cache.RedisService;
import com.tinet.ctilink.conf.model.Agent;
import com.tinet.ctilink.conf.model.AgentTel;
import com.tinet.ctilink.conf.model.Queue;
import com.tinet.ctilink.conf.model.QueueMember;
import com.tinet.ctilink.conf.util.ClidUtil;
import com.tinet.ctilink.control.service.v1.ControlActionService;
import com.tinet.ctilink.inc.Const;
import com.tinet.ctilink.json.JSONObject;
import com.tinet.ctilink.util.RedisLock;

@com.alibaba.dubbo.config.annotation.Service
public class AgentServiceImp implements AgentService {
	private final Logger logger = LoggerFactory.getLogger(getClass());
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
	
	@Reference
	ControlActionService controlActionService;
	
	public ActionResponse login(Map params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		String bindTel = params.get("bindTel").toString();
		Integer bindType = Integer.parseInt(params.get("bindType").toString());
		Integer loginType = Integer.parseInt(params.get("loginType").toString());
		Integer loginStatus = Integer.parseInt(params.get("loginStatus").toString());
		String pauseDescription = (params.get("pauseDescription") == null)?null:params.get("pauseDescription").toString();
		Integer pauseType = BigQueueConst.MEMBER_LOGIN_STATUS_PAUSE_TYPE_NORMAL;
		
		
		Agent agent = getAgent(enterpriseId, cno);
		if(agent != null){
			List<AgentTel> agentTelList = getAgentBindTel(enterpriseId, cno);
			boolean validBindTel = false;
			for(AgentTel agentTel :agentTelList){
				if(agentTel.getTel().equals(bindTel) && agentTel.getTelType().equals(bindType)){
					validBindTel = true;
					break;
				}
			}
			if(validBindTel == false){
				response = ActionResponse.createFailResponse(-1, "invalid bindTel");
				return response;
			}
			
			//查询是bindTel否在绑定电话里
			List<QueueMember> queueMemberList = getQueueMemberList(enterpriseId, cno);
			if(queueMemberList.size() == 0){
				response = ActionResponse.createFailResponse(-1, "not in any queue");
				return response;
			}
			
			//先获取lock memberService.lockMember(enterpriseId, cno);
			RedisLock memberLock = memberService.lockMember(enterpriseId, cno);
			if(memberLock != null){
				try{
					CallAgent callAgent = getCallAgent(enterpriseId, cno);
					if(callAgent != null){
						callAgent.setInterface(queueMemberList.get(0).getInterface());
						callAgent.setBindTel(bindTel);
						callAgent.setBindType(bindType);
						callAgent.setLoginTime(new Long(new Date().getTime()/1000).intValue());
						callAgent.setLoginType(loginType);
						
						//检查是否需要回位状态
						if(isNeedResetDeviceStatus(callAgent)){
							memberService.setDeviceStatus(enterpriseId, cno, BigQueueConst.MEMBER_DEVICE_STATUS_IDLE);
						}
					}else{
						callAgent = new CallAgent();
						callAgent.setInterface(queueMemberList.get(0).getInterface());
						callAgent.setBindTel(bindTel);
						callAgent.setBindType(bindType);
						callAgent.setCno(cno);
						callAgent.setEnterpriseId(Integer.parseInt(enterpriseId));
						callAgent.setLoginTime(new Long(new Date().getTime()/1000).intValue());
						callAgent.setName(agent.getName());
						callAgent.setLoginType(loginType);
						
						memberService.setDeviceStatus(enterpriseId, cno, BigQueueConst.MEMBER_DEVICE_STATUS_IDLE);
					}
					memberService.setLoginStatus(enterpriseId, cno, loginStatus);
					if(loginStatus == BigQueueConst.MEMBER_LOGIN_STATUS_PAUSE){
						callAgent.setPauseDescription(pauseDescription);
						callAgent.setPauseType(pauseType);
					}
					saveCallAgent(enterpriseId, cno, callAgent);
				}catch(Exception e){
					e.printStackTrace();
				}finally{
					memberService.unlockMember(memberLock);
				}
				//加入到queue_member中 这样可以让呼叫过来
				updateQueueMember(enterpriseId, cno, queueMemberList);
				
				JSONObject queueEvent = new JSONObject();
				queueEvent.put("event", "login");
				queueEvent.put("enterpriseId", enterpriseId);
				queueEvent.put("cno", cno);
				queueEvent.put("bindTel", bindTel);
				queueEvent.put("bindType", bindType);
				queueEvent.put("loginStatus", loginStatus);
				queueEvent.put("loginType", loginType);
				queueEventService.publishEvent(queueEvent);
				
				response = ActionResponse.createSuccessResponse();
			}else{
				response = ActionResponse.createFailResponse(-1, "fail to get lock");
			}
		}else {
			response = ActionResponse.createFailResponse(-1, "no such agent");
		}
		
		return response;
	}
	public ActionResponse logout(Map params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		
		//先获取lock memberService.lockMember(enterpriseId, cno);
		RedisLock memberLock = memberService.lockMember(enterpriseId, cno);
		if(memberLock != null){
			try{
				CallAgent callAgent = getCallAgent(enterpriseId, cno);
				if(callAgent != null){
					Integer deviceStatus = memberService.getDeviceStatus(enterpriseId, cno);
					if(deviceStatus != BigQueueConst.MEMBER_DEVICE_STATUS_IDLE){
						response = ActionResponse.createFailResponse(-1, "status not idle");
						return response;
					}
					memberService.setLoginStatus(enterpriseId, cno, BigQueueConst.MEMBER_LOGIN_STATUS_OFFLINE);
					saveCallAgent(enterpriseId, cno, null);
				}else{
					response = ActionResponse.createFailResponse(-1, "no call agent");
					return response;
				}
			}catch(Exception e){
				e.printStackTrace();
			}finally{
				memberService.unlockMember(memberLock);
			}
			
			
			List<QueueMember> queueMemberList = getQueueMemberList(enterpriseId, cno);
			delQueueMember(enterpriseId, cno, queueMemberList);
			
			JSONObject queueEvent = new JSONObject();
			queueEvent.put("event", "logout");
			queueEvent.put("enterpriseId", enterpriseId);
			queueEvent.put("cno", cno);
			queueEventService.publishEvent(queueEvent);
			
			
			response = ActionResponse.createSuccessResponse();
			return response;
		}else {
			response = ActionResponse.createFailResponse(-1, "no such agent");
		}
		return response;
	}
	public ActionResponse pause(Map params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		String type = params.get("type").toString();
		String description = params.get("description").toString();
		JSONObject queueEvent;
		
		//先获取lock memberService.lockMember(enterpriseId, cno);
		RedisLock memberLock = memberService.lockMember(enterpriseId, cno);
		if(memberLock != null){
			try{
				CallAgent callAgent = getCallAgent(enterpriseId, cno);
				if(callAgent != null){
				
					Integer loginStatus = memberService.getLoginStatus(enterpriseId, cno);
					switch(loginStatus){
					case BigQueueConst.MEMBER_LOGIN_STATUS_OFFLINE:
						response = ActionResponse.createFailResponse(-1, "not logined");
						break;
					case BigQueueConst.MEMBER_LOGIN_STATUS_READY:
					case BigQueueConst.MEMBER_LOGIN_STATUS_PAUSE:
						memberService.setLoginStatus(enterpriseId, cno, BigQueueConst.MEMBER_LOGIN_STATUS_PAUSE);
						callAgent.setPauseDescription(description);
						callAgent.setPauseType(Integer.parseInt(type));
						response = ActionResponse.createSuccessResponse();
						
						queueEvent = new JSONObject();
						queueEvent.put("event", "pause");
						queueEvent.put("enterpriseId", enterpriseId);
						queueEvent.put("cno", cno);
						queueEvent.put("type", type);
						queueEvent.put("description", description);

						queueEventService.publishEvent(queueEvent);
						break;
					case BigQueueConst.MEMBER_LOGIN_STATUS_WRAPUP:
						memberService.setLoginStatus(enterpriseId, cno, BigQueueConst.MEMBER_LOGIN_STATUS_PAUSE);
						callAgent.setPauseDescription(description);
						callAgent.setPauseType(Integer.parseInt(type));
						String taskId = String.format(BigQueueConst.WRAPUP_END_TASK_ID, cno);
						//wrapupEndTaskScheduler.unschedule(taskId);
						response = ActionResponse.createSuccessResponse();
						
						queueEvent = new JSONObject();
						queueEvent.put("event", "pause");
						queueEvent.put("enterpriseId", enterpriseId);
						queueEvent.put("cno", cno);
						queueEvent.put("type", type);
						queueEvent.put("description", description);
						queueEventService.publishEvent(queueEvent);
						
						queueEvent = new JSONObject();
						queueEvent.put("event", "wrapupEnd");
						queueEvent.put("enterpriseId", enterpriseId);
						queueEvent.put("cno", cno);
						queueEventService.publishEvent(queueEvent);
						break;
					}
					saveCallAgent(enterpriseId, cno, callAgent);
				}else{
					response = ActionResponse.createFailResponse(-1, "no such agent");
				}	
			}catch(Exception e){
				e.printStackTrace();
			}finally{
				memberService.unlockMember(memberLock);
			}
		}else{
			response = ActionResponse.createFailResponse(-1, "fail to get lock");
		}
		
		
		return response;
	}
	public ActionResponse unpause(Map params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		JSONObject queueEvent;
		
		//先获取lock memberService.lockMember(enterpriseId, cno);
		RedisLock memberLock = memberService.lockMember(enterpriseId, cno);
		if(memberLock != null){
			try{
				CallAgent callAgent = getCallAgent(enterpriseId, cno);
				if(callAgent != null){
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
						callAgent.setPauseDescription("");
						response = ActionResponse.createSuccessResponse();
						
						queueEvent = new JSONObject();
						queueEvent.put("event", "unpause");
						queueEvent.put("enterpriseId", enterpriseId);
						queueEvent.put("cno", cno);
						queueEventService.publishEvent(queueEvent);
						break;
					case BigQueueConst.MEMBER_LOGIN_STATUS_WRAPUP:
						memberService.setLoginStatus(enterpriseId, cno, BigQueueConst.MEMBER_LOGIN_STATUS_READY);
						String taskId = String.format(BigQueueConst.WRAPUP_END_TASK_ID, cno);
						//wrapupEndTaskScheduler.unschedule(taskId);
						response = ActionResponse.createSuccessResponse();
						
						queueEvent = new JSONObject();
						queueEvent.put("event", "pause");
						queueEvent.put("enterpriseId", enterpriseId);
						queueEvent.put("cno", cno);
						queueEventService.publishEvent(queueEvent);
						
						queueEvent = new JSONObject();
						queueEvent.put("event", "wrapupEnd");
						queueEvent.put("enterpriseId", enterpriseId);
						queueEvent.put("cno", cno);
						queueEventService.publishEvent(queueEvent);
						break;
					}
					saveCallAgent(enterpriseId, cno, callAgent);
				}else {
					response = ActionResponse.createFailResponse(-1, "no such agent");
				}
			}catch(Exception e){
				e.printStackTrace();
			}finally{
				memberService.unlockMember(memberLock);
			}
		}else{
			response = ActionResponse.createFailResponse(-1, "fail to get lock");
		}

		return response;
	}
	public ActionResponse changeBindTel(Map params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		String bindTel = params.get("bindTel").toString();
		Integer bindType = Integer.parseInt(params.get("bindType").toString());
		JSONObject queueEvent;
		
		Agent agent = getAgent(enterpriseId, cno);
		if(agent != null){
			List<AgentTel> agentTelList = getAgentBindTel(enterpriseId, cno);
			boolean validBindTel = false;
			for(AgentTel agentTel :agentTelList){
				if(agentTel.getTel().equals(bindTel) && agentTel.getTelType().equals(bindType)){
					validBindTel = true;
					break;
				}
			}
			if(validBindTel == false){
				response = ActionResponse.createFailResponse(-1, "invalid bindTel");
				return response;
			}
			
			//查询是bindTel否在绑定电话里
			List<QueueMember> queueMemberList = getQueueMemberList(enterpriseId, cno);
			if(queueMemberList.size() == 0){
				response = ActionResponse.createFailResponse(-1, "not in any queue");
				return response;
			}
			Integer deviceStatus = memberService.getDeviceStatus(enterpriseId, cno);
			if(deviceStatus != BigQueueConst.MEMBER_DEVICE_STATUS_IDLE){
				response = ActionResponse.createFailResponse(-1, "status not idle");
				return response;
			}
			//先获取lock memberService.lockMember(enterpriseId, cno);
			RedisLock memberLock = memberService.lockMember(enterpriseId, cno);
			if(memberLock != null){
				try{
					CallAgent callAgent = getCallAgent(enterpriseId, cno);
					if(callAgent != null){
						callAgent.setInterface(queueMemberList.get(0).getInterface());
						callAgent.setBindTel(bindTel);
						callAgent.setBindType(bindType);
						callAgent.setLoginTime(new Long(new Date().getTime()/1000).intValue());
							
						saveCallAgent(enterpriseId, cno, callAgent);
						//加入到queue_member中 这样可以让呼叫过来
						updateQueueMember(enterpriseId, cno, queueMemberList);

						response = ActionResponse.createSuccessResponse();
						
						queueEvent = new JSONObject();
						queueEvent.put("event", "changeBindTel");
						queueEvent.put("enterpriseId", enterpriseId);
						queueEvent.put("cno", cno);
						queueEvent.put("bindTel", bindTel);
						queueEvent.put("bindType", bindType);
						queueEventService.publishEvent(queueEvent);
						
					}else{
						response = ActionResponse.createFailResponse(-1, "no call agent");
						return response;
					}
				}catch(Exception e){
					e.printStackTrace();
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
			CallAgent agent = getCallAgent(enterpriseId, cno);
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
			CallAgent agent = getCallAgent(enterpriseId, cno);
			if(agent != null){	
				List<QueueMember> queueMemberList = getQueueMemberList(enterpriseId, cno);
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
	
	public ActionResponse barge(Map params){
		ActionResponse response = null;
		
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		String bargedCno = params.get("bargedCno").toString();
		String bargeObject = params.get("bargeObject").toString();
		String bargeType = params.get("bargeType").toString();
		
		Boolean paused;
		String location;
		String bindTel;
		Integer bindType;
		//先获取lock memberService.lockMember(enterpriseId, cno);
		RedisLock memberLock = memberService.lockMember(enterpriseId, cno);
		if(memberLock != null){
			try{
				CallAgent callAgent = getCallAgent(enterpriseId, cno);
				if(callAgent != null){
					paused = memberService.isPaused(enterpriseId, bargedCno);
	                location = callAgent.getInterface();
	                bindTel = callAgent.getBindTel();
	                bindType = callAgent.getBindType();
				}
			}catch(Exception e){
				e.printStackTrace();
				response = ActionResponse.createFailResponse(-1, "exception");
				return response;
			}finally{
				memberService.unlockMember(memberLock);
			}
		}else{
			response = ActionResponse.createFailResponse(-1, "fail to get lock");
			return response;
		}
		String bargedChannel = null;
        Integer bargedDeviceStatus = 0;
        String customerNumber = null;
        Integer customerNumberType = 0;
        String customerAreaCode = null;
        String numberTrunk = null;
        String curQueue = null;
        Integer callType = 0; 
		//先获取lock memberService.lockMember(enterpriseId, cno);
		RedisLock bargedMemberLock = memberService.lockMember(enterpriseId, bargedCno);
		if(memberLock != null){
			try{
				CallAgent callAgent = getCallAgent(enterpriseId, cno);
				if(callAgent != null){
					bargedChannel = callAgent.getCurrentChannel();
			        bargedDeviceStatus = memberService.getDeviceStatus(enterpriseId, bargedCno);
			        customerNumber = callAgent.getCurrentCustomerNumber();
			        customerNumberType = callAgent.getCurrentCustomerNumberType();
			        customerAreaCode = callAgent.getCurrentCustomerNumberAreaCode();
			        numberTrunk = callAgent.getCurrentNumberTrunk();
			        curQueue = callAgent.getCurrentQueue();
			        callType = callAgent.getCurrentCallType(); 
				}
			}catch(Exception e){
				e.printStackTrace();
				response = ActionResponse.createFailResponse(-1, "exception");
				return response;
			}finally{
				memberService.unlockMember(memberLock);
			}
		}else{
			response = ActionResponse.createFailResponse(-1, "fail to get lock");
			return response;
		}
					
	    if (StringUtil.isEmpty(bargedChannel) || !bargedDeviceStatus.equals(BigQueueConst.MEMBER_DEVICE_STATUS_INUSE)) {
	        response = ActionResponse.createFailResponse(-1, "no barged channel");
			return response;
	    }
	    int routerClidType = 0;
        if (callType == Const.CDR_CALL_TYPE_IB || callType ==Const.CDR_CALL_TYPE_OB_WEBCALL ){//呼入
            routerClidType = Const.ROUTER_CLID_CALL_TYPE_IB_RIGHT;
        }else if(callType == Const.CDR_CALL_TYPE_OB || callType == Const.CDR_CALL_TYPE_DIRECT_OB || callType == Const.CDR_CALL_TYPE_PREVIEW_OB){//点击外呼
            routerClidType = Const.ROUTER_CLID_CALL_TYPE_PREVIEW_OB_RIGHT;
        }else if(callType == Const.CDR_CALL_TYPE_PREDICTIVE_OB){//预测外呼
            routerClidType = Const.ROUTER_CLID_CALL_TYPE_PREDICTIVE_OB_RIGHT;
        }
        /* 
        ClidUtil.getClid(enterpriseId, routerClidCallType, customerNumber, clidBack)
        //获取外显号码
        EnterpriseClidService enterpriseClidService=(EnterpriseClidService) ContextUtil.getContext().getBean("enterpriseClidService");
        String clid = enterpriseClidService.getClid(enterpriseId, routerClidType, customerNumber, numberTrunk);
        
        long timeout = 30000;

        String destInterface = "";
        String gwIp = "";
        if (objectType.equals(Const.OBJECT_TYPE_TEL)) {
            AreaCodeService areaCodeService = (AreaCodeService) ContextUtil.getContext().getBean("areaCodeService");
            Caller caller =areaCodeService.updateGetAreaCode(bargeObject, "");
            EnterpriseRouterService enterpriseRouterService = (EnterpriseRouterService) ContextUtil.getContext()
                    .getBean("enterpriseRouterService");
            Router router = enterpriseRouterService.getRouter(enterpriseId, routerClidType, caller);
            if (router != null) {
                destInterface = "SIP/" + router.getGateway().getPrefix() + caller.getCallerNumber() + "@"
                        + router.getGateway().getIpAddr();
                gwIp = router.getGateway().getIpAddr();
            }
        } else if (objectType.equals(Const.OBJECT_TYPE_EXTEN)) {
            destInterface = "SIP/" + enterpriseId + "-" + bargeObject;
            SipConfService sipConfService = (SipConfService) ContextUtil.getContext().getBean("sipConfService");
            List<SipConf> sipConfs = (List<SipConf>)sipConfService.findBy("name", enterpriseId + "-" + bargeObject);
            if(sipConfs.size() > 0){
                gwIp = sipConfs.get(0).getIpAddr();
            }
        } else if (objectType.equals(Const.OBJECT_TYPE_CNO)) {
            destInterface = location;
            if(bindType == Const.BIND_TYPE_TEL){//座席绑定固话
                SipConfService sipConfService = (SipConfService) ContextUtil.getContext().getBean("sipConfService");
                List<SipConf> sipConfs = (List<SipConf>)sipConfService.findBy("name", enterpriseId + cno);
                if(sipConfs.size() > 0){
                    gwIp = sipConfs.get(0).getIpAddr();
                }
            }else{//座席绑定分机或软电话
                SipConfService sipConfService = (SipConfService) ContextUtil.getContext().getBean("sipConfService");
                List<SipConf> sipConfs = (List<SipConf>)sipConfService.findBy("name", enterpriseId + "-" + bindTel);
                if(sipConfs.size() > 0){
                    gwIp = sipConfs.get(0).getIpAddr();
                }
            }
        }
        if (destInterface.isEmpty()) {
	        response = ActionResponse.createFailResponse(-1, "bad param");
			return response;
        } else {
                                  
                Map<String, String> varMap = new HashMap<String, String>();
                varMap.put(AmiChanVarNameConst.BARGE_CHAN, bargedChannel); 
                varMap.put("__" + AmiChanVarNameConst.CDR_CUSTOMER_NUMBER, customerNumber); //客户号码
                varMap.put("__" + AmiChanVarNameConst.CDR_CUSTOMER_NUMBER_TYPE, customerNumberType); //电话类型
                varMap.put("__" + AmiChanVarNameConst.CDR_CUSTOMER_AREA_CODE, customerAreaCode); //区号
                varMap.put("__" + AmiChanVarNameConst.CUR_QUEUE, curQueue); //
                varMap.put("__" + AmiChanVarNameConst.ENTERPRISE_ID, String.valueOf(enterpriseId));
                varMap.put("__" + AmiChanVarNameConst.BARGER_CNO, cno);
                varMap.put("__" + AmiChanVarNameConst.BARGED_CNO, bargedCno);
                varMap.put("__" + AmiChanVarNameConst.BARGER_INTERFACE, destInterface);
                
                varMap.put(AmiChanVarNameConst.CDR_DETAIL_GW_IP, gwIp);
                if(routerClidType == Const.ROUTER_CLID_CALL_TYPE_IB_RIGHT){
                	varMap.put(AmiChanVarNameConst.CDR_DETAIL_CALL_TYPE, String.valueOf(Const.CDR_CALL_TYPE_IB_BARGE));
                }else{
                	varMap.put(AmiChanVarNameConst.CDR_DETAIL_CALL_TYPE, String.valueOf(Const.CDR_CALL_TYPE_OB_BARGE));
                }
                varMap.put(AmiChanVarNameConst.CDR_NUMBER_TRUNK, clid);
                varMap.put(AmiChanVarNameConst.CDR_STATUS, String.valueOf(Const.CDR_STATUS_DETAIL_CALL_FAIL));
                
                varMap.put(AmiChanVarNameConst.CDR_ENTERPRISE_ID, String.valueOf(enterpriseId));
                varMap.put(AmiChanVarNameConst.CDR_START_TIME, String.valueOf(new Date().getTime() / 1000));
                varMap.put(AmiChanVarNameConst.CDR_DETAIL_CNO, cno);
                try {
                    GetVarAction getVarAction = new GetVarAction(bargedChannel, AmiChanVarNameConst.CDR_MAIN_UNIQUE_ID);
                    amiActionService.handleAction("getVar",context, exten, priority, clid, location, actionObject, varMap);
                    GetVarResponse re = (GetVarResponse) AmiManager.getManager(Integer.valueOf(ctiId)).sendAction(getVarAction, 1000);
                    varMap.put(AmiChanVarNameConst.CDR_MAIN_UNIQUE_ID, re.getValue());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                varMap.put(AmiChanVarNameConst.BARGE_OBJECT, bargeObject);
                varMap.put(AmiChanVarNameConst.OBJECT_TYPE, objectType);
                
                JSONObject actionObject = new JSONObject();
                actionObject.put("type", "barge");
                
                
                amiActionService.handleAction("originate", actionMap, actionObject, varMap);
                
                originateAction.setChannel(destInterface); // set
                // channel
                originateAction.setContext(Const.DIALPLAN_CONTEXT_BARGE); // set
                // context
                originateAction.setExten(enterpriseId + cno);
                originateAction.setVariables(variables);
                originateAction.setTimeout(timeout);
                originateAction.setPriority(new Integer(1)); // set
                originateAction.setCallerId(clid);
                ManagerResponse res = AmiManager.getManager(member.getCti().getId()).sendAction(
                        originateAction, 60000);
                if (res.getResponse().equals("Success")) // call success
                {
                
                } else // call failed
                {
                    // generate an event consult_link
                    Event event = new Event(Action.VARIABLE_EVENT);
                    event.putResponse(Action.VARIABLE_NAME, Event.BARGE_ERROR);
                    event.putResponse(Action.VARIABLE_ENTERPRISE_ID, enterpriseId);
                    event.putResponse(Action.VARIABLE_CNO, cno);
                    event.putResponse(Action.VARIABLE_BARGED_CNO, bargedCno);
                    event.putResponse(Action.VARIABLE_BARGE_OBJECT, bargeObject);
                    event.putResponse(Action.VARIABLE_OBJECT_TYPE, objectType);
                    AmiEventEngine.pushEvent(event);
                }
            } catch (Exception e) {
                e.printStackTrace();
                // broadcast to all other jwsClients
                Event event = new Event(Action.VARIABLE_EVENT);
                event.putResponse(Action.VARIABLE_NAME, Event.BARGE_ERROR);
                event.putResponse(Action.VARIABLE_ENTERPRISE_ID, enterpriseId);
                event.putResponse(Action.VARIABLE_CNO, cno);
                event.putResponse(Action.VARIABLE_BARGED_CNO, bargedCno);
                event.putResponse(Action.VARIABLE_BARGE_OBJECT, bargeObject);
                event.putResponse(Action.VARIABLE_OBJECT_TYPE, objectType);
                AmiEventEngine.pushEvent(event);
            }
					
				}
				*/
        return response;
	}
	public ActionResponse hangup(Map params){
		ActionResponse response = null;
		String uniqueId = params.get("uniqueId").toString();
		
		return response;
	}
	public ActionResponse consultCancel(Map params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		
		return response;
	}
	public ActionResponse consult(Map params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		
		return response;
	}
	public ActionResponse consultThreeway(Map params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		
		return response;
	}
	public ActionResponse consultTransfer(Map params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		
		return response;
	}
	public ActionResponse directCallStart(Map params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		
		return response;
	}
	public ActionResponse disconnect(Map params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		
		return response;
	}
	public ActionResponse hold(Map params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		
		return response;
	}
	public ActionResponse interact(Map params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		
		return response;
	}
	public ActionResponse investigation(Map params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		
		return response;
	}
	public ActionResponse ivrOutcall(Map params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		
		return response;
	}
	public ActionResponse mute(Map params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		
		return response;
	}
	public ActionResponse pickup(Map params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		
		return response;
	}
	public ActionResponse previewOutcall(Map params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		
		return response;
	}
	public ActionResponse previewOutcallCancel(Map params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		
		return response;
	}
	public ActionResponse refuse(Map params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		
		return response;
	}
	public ActionResponse setPause(Map params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		
		return response;
	}
	public ActionResponse setUnpause(Map params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		
		return response;
	}
	public ActionResponse spy(Map params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		
		return response;
	}
	public ActionResponse threeway(Map params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		
		return response;
	}
	public ActionResponse transfer(Map params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		
		return response;
	}
	public ActionResponse unconsult(Map params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		
		return response;
	}
	public ActionResponse unhold(Map params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		
		return response;
	}
	public ActionResponse unlink(Map params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		
		return response;
	}
	public ActionResponse unspy(Map params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		
		return response;
	}
	public ActionResponse unthreeway(Map params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		
		return response;
	}
	public ActionResponse unwhisper(Map params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		
		return response;
	}		
	public ActionResponse whisper(Map params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		
		return response;
	}
	
	/**
	 * 其他方法
	 */
	/**
	 * 
	 * @param enterpriseId
	 * @param cno
	 * @return
	 */
	public Agent getAgent(String enterpriseId, String cno){
		String agentKey = String.format(CacheKey.AGENT_ENTERPRISE_ID_CNO, Integer.parseInt(enterpriseId), cno);
		Agent agent = redisService.get(Const.REDIS_DB_CONF_INDEX, agentKey, Agent.class);
		return agent;
	}
	public CallAgent getCallAgent(String enterpriseId, String cno){
		String agentKey = String.format(BigQueueCacheKey.AGENT_ENTERPRISE_ID, enterpriseId);
		CallAgent agent = redisService.hget(Const.REDIS_DB_CTI_INDEX, agentKey, cno, CallAgent.class);
		return agent;
	}
	
	public void saveCallAgent(String enterpriseId, String cno, CallAgent callAgent){
		if(callAgent == null){
			String agentKey = String.format(BigQueueCacheKey.AGENT_ENTERPRISE_ID, enterpriseId);
			redisService.hdel(Const.REDIS_DB_CTI_INDEX, agentKey, cno);
		}else{
			String agentKey = String.format(BigQueueCacheKey.AGENT_ENTERPRISE_ID, callAgent.getEnterpriseId());
			redisService.hset(Const.REDIS_DB_CTI_INDEX, agentKey, callAgent.getCno(), callAgent);
		}
	}
	private List<AgentTel> getAgentBindTel(String enterpriseId, String cno){
		String key = String.format(CacheKey.AGENT_TEL_ENTERPRISE_ID_CNO, Integer.parseInt(enterpriseId), cno);
		List<AgentTel> agentTelList = redisService.getList(Const.REDIS_DB_CONF_INDEX, key, AgentTel.class);

		return agentTelList;
	}
	
	private void updateQueueMember(String enterpriseId, String cno, List<QueueMember> queueMemberList){
		for(QueueMember queueMember: queueMemberList){
			 queueService.setMember(enterpriseId, queueMember);
		}
	}
	private void delQueueMember(String enterpriseId, String cno, List<QueueMember> queueMemberList){
		for(QueueMember queueMember: queueMemberList){
			queueService.delMember(enterpriseId, queueMember.getQno(), cno);
		}
	}
	
	public List<QueueMember> getQueueMemberList(String enterpriseId, String cno){
		String confKey = String.format(CacheKey.QUEUE_MEMBER_ENTERPRISE_ID_CNO, Integer.parseInt(enterpriseId), cno);
		List<QueueMember> queueMemberList = redisService.getList(Const.REDIS_DB_CONF_INDEX, confKey, QueueMember.class);
		return queueMemberList;
	}
	private boolean isNeedResetDeviceStatus(CallAgent callAgent){
		boolean res = false;
		Integer deviceStatus = memberService.getDeviceStatus(String.valueOf(callAgent.getEnterpriseId()), callAgent.getCno());
		switch(deviceStatus){
			case BigQueueConst.MEMBER_DEVICE_STATUS_IDLE:
				break;
			case BigQueueConst.MEMBER_DEVICE_STATUS_INVALID:
				res = true;
				break;
			case BigQueueConst.MEMBER_DEVICE_STATUS_LOCKED:
				res = true;
				break;
			case BigQueueConst.MEMBER_DEVICE_STATUS_INVITE:
			case BigQueueConst.MEMBER_DEVICE_STATUS_RINGING:
			case BigQueueConst.MEMBER_DEVICE_STATUS_INUSE:
				if(!channelService.isAlive(callAgent.getCurrentChannelUniqueId())){
					res = true;
				}
				break;
		}
		return res;
	}
}
