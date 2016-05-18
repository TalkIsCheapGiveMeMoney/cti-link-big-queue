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
import org.springframework.stereotype.Service;

import com.github.pagehelper.StringUtil;
import com.tinet.ctilink.bigqueue.entity.ActionResponse;
import com.tinet.ctilink.bigqueue.entity.CallAgent;
import com.tinet.ctilink.bigqueue.entity.CallMember;
import com.tinet.ctilink.bigqueue.inc.BigQueueCacheKey;
import com.tinet.ctilink.bigqueue.inc.BigQueueConst;
import com.tinet.ctilink.bigqueue.inc.BigQueueMacro;
import com.tinet.ctilink.cache.CacheKey;
import com.tinet.ctilink.cache.RedisService;
import com.tinet.ctilink.conf.model.Agent;
import com.tinet.ctilink.conf.model.AgentTel;
import com.tinet.ctilink.conf.model.Queue;
import com.tinet.ctilink.conf.model.QueueMember;
import com.tinet.ctilink.inc.Const;
import com.tinet.ctilink.json.JSONObject;
import com.tinet.ctilink.util.RedisLock;

@Service
public class AgentServiceImp {
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
	
	
	public ActionResponse login(Map params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		String bindTel = params.get("bindTel").toString();
		Integer bindType = Integer.parseInt(params.get("bindType").toString());
		Integer loginType = Integer.parseInt(params.get("loginType").toString());
		Integer loginStatus = Integer.parseInt(params.get("loginStatus").toString());
		String pauseDescription = (params.get("pauseDescription") == null)?params.get("pauseDescription").toString():null;
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
		String agentKey = String.format(CacheKey.AGENT_ENTERPRISE_ID_CNO, enterpriseId, cno);
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
			redisService.hset(Const.REDIS_DB_CTI_INDEX, agentKey, callAgent.getCno(), CallAgent.class);
		}
	}
	private List<AgentTel> getAgentBindTel(String enterpriseId, String cno){
		String key = String.format(CacheKey.AGENT_TEL_ENTERPRISE_ID_CNO, enterpriseId, cno);
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
	
	private List<QueueMember> getQueueMemberList(String enterpriseId, String cno){
		String confKey = String.format(CacheKey.QUEUE_MEMBER_ENTERPRISE_ID_CNO, enterpriseId, cno);
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
