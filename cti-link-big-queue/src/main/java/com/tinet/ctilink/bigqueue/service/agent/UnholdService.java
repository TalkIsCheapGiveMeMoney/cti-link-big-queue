package com.tinet.ctilink.bigqueue.service.agent;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tinet.ctilink.ami.action.AmiActionResponse;
import com.tinet.ctilink.ami.inc.AmiParamConst;
import com.tinet.ctilink.bigqueue.ami.action.GetVarActionService;
import com.tinet.ctilink.bigqueue.ami.action.IndicateActionService;
import com.tinet.ctilink.bigqueue.ami.action.OriginateActionService;
import com.tinet.ctilink.bigqueue.entity.ActionResponse;
import com.tinet.ctilink.bigqueue.entity.CallAgent;
import com.tinet.ctilink.bigqueue.inc.BigQueueCacheKey;
import com.tinet.ctilink.bigqueue.inc.BigQueueConst;
import com.tinet.ctilink.bigqueue.service.imp.AgentServiceImp;
import com.tinet.ctilink.bigqueue.service.imp.ChannelServiceImp;
import com.tinet.ctilink.bigqueue.service.imp.MemberServiceImp;
import com.tinet.ctilink.bigqueue.service.imp.QueueEventServiceImp;
import com.tinet.ctilink.bigqueue.service.imp.QueueServiceImp;
import com.tinet.ctilink.cache.RedisService;
import com.tinet.ctilink.json.JSONObject;
import com.tinet.ctilink.scheduler.RedisTaskScheduler;
import com.tinet.ctilink.util.RedisLock;

@Component
public class UnholdService {
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
	@Autowired
	IndicateActionService indicateActionService;
	
	public ActionResponse unhold(Map<String,Object> params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		
		//先获取lock memberService.lockMember(enterpriseId, cno);
		RedisLock memberLock = memberService.lockMember(enterpriseId, cno);
		if(memberLock != null){
			try{
				CallAgent callAgent = agentService.getCallAgent(enterpriseId, cno);
				if(callAgent != null){
					String channel = callAgent.getCurrentChannel();
					Integer sipId = callAgent.getCurrentSipId();
					String queue = callAgent.getCurrentQueue();
					Integer callType = callAgent.getCurrentCallType();
					
					if(StringUtils.isEmpty(channel)){
						response = ActionResponse.createFailResponse(-1, "no channel");
						return response;
					}
					String busyDescription = callAgent.getBusyDescription();
					if(busyDescription == null || !busyDescription.equals("hold")){
						response = ActionResponse.createFailResponse(-1, "not on hold");
						return response;
					}
					
					AmiActionResponse amiResponse = indicateActionService.indicate(sipId, channel, AmiParamConst.INDICATE_UNHOLD);
					if(amiResponse != null && (amiResponse.getCode() == 0)){
						callAgent.setBusyDescription("");
						agentService.saveCallAgent(enterpriseId, cno, callAgent);
						
						JSONObject queueEvent = new JSONObject();
						queueEvent.put("event", "unhold");
						queueEvent.put("enterpriseId", enterpriseId);
						queueEvent.put("qno", queue);
						queueEvent.put("callType", callType);
						queueEventService.publishEvent(queueEvent);
							
						//status事件
						JSONObject statusEvent = new JSONObject();
						statusEvent.put("event", "status");
						statusEvent.put("enterpriseId", enterpriseId);
						statusEvent.put("cno", cno);
						Integer loginStatus = memberService.getLoginStatus(enterpriseId, cno);
						statusEvent.put("loginStatus", memberService.getLoginStatus(enterpriseId, cno));
						statusEvent.put("deviceStatus", memberService.getDeviceStatus(enterpriseId, cno));
						
						if(loginStatus.equals(BigQueueConst.MEMBER_LOGIN_STATUS_PAUSE)){
							statusEvent.put("pauseDescription", callAgent.getPauseDescription());
							statusEvent.put("pauseType", callAgent.getPauseType());
						}
						redisService.convertAndSend(BigQueueCacheKey.AGENT_GATEWAY_EVENT_TOPIC, statusEvent);
						
						response = ActionResponse.createSuccessResponse();
						return response;
					}else{
						response = ActionResponse.createFailResponse(-1, "originate fail");
						return response;
					}
				}else {
					response = ActionResponse.createFailResponse(-1, "no such agent");
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
		}

		return response;
	}
}
