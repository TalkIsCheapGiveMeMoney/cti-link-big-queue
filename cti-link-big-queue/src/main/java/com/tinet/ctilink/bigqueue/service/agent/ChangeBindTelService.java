package com.tinet.ctilink.bigqueue.service.agent;

import java.util.Date;
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
import com.tinet.ctilink.conf.model.Agent;
import com.tinet.ctilink.conf.model.AgentTel;
import com.tinet.ctilink.conf.model.QueueMember;
import com.tinet.ctilink.json.JSONObject;
import com.tinet.ctilink.scheduler.RedisTaskScheduler;
import com.tinet.ctilink.util.RedisLock;

@Component
public class ChangeBindTelService {
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
	public ActionResponse changeBindTel(Map params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		String bindTel = params.get("bindTel").toString();
		Integer bindType = Integer.parseInt(params.get("bindType").toString());
		JSONObject queueEvent;
		
		Agent agent = agentService.getAgent(enterpriseId, cno);
		if(agent != null){
			List<AgentTel> agentTelList = agentService.getAgentBindTel(enterpriseId, cno);
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
			List<QueueMember> queueMemberList = agentService.getQueueMemberList(enterpriseId, cno);
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
					CallAgent callAgent = agentService.getCallAgent(enterpriseId, cno);
					if(callAgent != null){
						callAgent.setInterface(queueMemberList.get(0).getInterface());
						callAgent.setBindTel(bindTel);
						callAgent.setBindType(bindType);
						callAgent.setLoginTime(new Long(new Date().getTime()/1000).intValue());
							
						agentService.saveCallAgent(enterpriseId, cno, callAgent);
						//加入到queue_member中 这样可以让呼叫过来
						agentService.updateQueueMember(enterpriseId, cno, queueMemberList);

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
}
