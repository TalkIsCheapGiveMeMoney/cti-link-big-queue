package com.tinet.ctilink.bigqueue.service.agent;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tinet.ctilink.bigqueue.ami.action.GetVarActionService;
import com.tinet.ctilink.bigqueue.ami.action.OriginateActionService;
import com.tinet.ctilink.bigqueue.entity.ActionResponse;
import com.tinet.ctilink.bigqueue.entity.CallAgent;
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
public class PauseService {
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
	
	public ActionResponse pause(Map<String,Object> params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		Integer type = Integer.parseInt(params.get("type").toString());
		String description = params.get("description").toString();
		
		//先获取lock memberService.lockMember(enterpriseId, cno);
		RedisLock memberLock = memberService.lockMember(enterpriseId, cno);
		if(memberLock != null){
			try{
				return pauseNolock(enterpriseId, cno, description, type);
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
	
	public ActionResponse pauseNolock(String enterpriseId, String cno, String description, Integer type){
		ActionResponse response = null;
		JSONObject queueEvent;
		CallAgent callAgent = agentService.getCallAgent(enterpriseId, cno);
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
				callAgent.setPauseType(type);
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
				callAgent.setPauseType(type);
				String taskId = String.format(BigQueueConst.WRAPUP_END_TASK_ID, enterpriseId, cno);
				redisTaskScheduler.unschedule(taskId);
				
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
			agentService.saveCallAgent(enterpriseId, cno, callAgent);
			response = ActionResponse.createSuccessResponse();
		}else{
			response = ActionResponse.createFailResponse(-1, "no such agent");
		}	
		return response;
	}
}
