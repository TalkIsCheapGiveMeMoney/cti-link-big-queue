package com.tinet.ctilink.bigqueue.service.agent;

import java.util.List;
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
import com.tinet.ctilink.conf.model.QueueMember;
import com.tinet.ctilink.json.JSONObject;
import com.tinet.ctilink.scheduler.RedisTaskScheduler;
import com.tinet.ctilink.util.RedisLock;

@Component
public class LogoutService {
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
	
	public ActionResponse logout(Map<String,Object> params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		
		//先获取lock memberService.lockMember(enterpriseId, cno);
		RedisLock memberLock = memberService.lockMember(enterpriseId, cno);
		if(memberLock != null){
			try{
				CallAgent callAgent = agentService.getCallAgent(enterpriseId, cno);
				if(callAgent != null){
					Integer deviceStatus = memberService.getDeviceStatus(enterpriseId, cno);
					if(deviceStatus != BigQueueConst.MEMBER_DEVICE_STATUS_IDLE){
						response = ActionResponse.createFailResponse(-1, "status not idle");
						return response;
					}
					memberService.setLoginStatus(enterpriseId, cno, BigQueueConst.MEMBER_LOGIN_STATUS_OFFLINE);
					agentService.saveCallAgent(enterpriseId, cno, null);
				}else{
					response = ActionResponse.createFailResponse(-1, "no call agent");
					return response;
				}
			}catch(Exception e){
				e.printStackTrace();
				response = ActionResponse.createFailResponse(-1, "exception");
				return response;
			}finally{
				memberService.unlockMember(memberLock);
			}
			
			
			List<QueueMember> queueMemberList = agentService.getQueueMemberList(enterpriseId, cno);
			agentService.delQueueMember(enterpriseId, cno, queueMemberList);
			
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
}
