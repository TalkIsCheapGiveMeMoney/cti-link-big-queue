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
public class LoginService {
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
	public ActionResponse login(Map<String,Object> params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		String bindTel = params.get("bindTel").toString();
		Integer bindType = Integer.parseInt(params.get("bindType").toString());
		Integer loginType = Integer.parseInt(params.get("loginType").toString());
		Integer loginStatus = Integer.parseInt(params.get("loginStatus").toString());
		String pauseDescription = (params.get("pauseDescription") == null)?null:params.get("pauseDescription").toString();
		Integer pauseType = BigQueueConst.MEMBER_LOGIN_STATUS_PAUSE_TYPE_NORMAL;
		
		
		Agent agent = agentService.getAgent(enterpriseId, cno);
		if(agent != null){
			
			List<QueueMember> queueMemberList = agentService.getQueueMemberList(enterpriseId, cno);
			if(queueMemberList.size() == 0){
				response = ActionResponse.createFailResponse(-1, "not in any queue");
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
						callAgent.setLoginType(loginType);
						
						//检查是否需要回位状态
						if(agentService.isNeedResetDeviceStatus(callAgent)){
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
					agentService.saveCallAgent(enterpriseId, cno, callAgent);
				}catch(Exception e){
					e.printStackTrace();
					response = ActionResponse.createFailResponse(-1, "exception");
					return response;
				}finally{
					memberService.unlockMember(memberLock);
				}
				//加入到queue_member中 这样可以让呼叫过来
				agentService.updateQueueMember(enterpriseId, cno, queueMemberList);
				
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
}
