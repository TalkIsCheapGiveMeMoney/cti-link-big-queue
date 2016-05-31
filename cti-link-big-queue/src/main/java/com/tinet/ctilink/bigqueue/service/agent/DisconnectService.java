package com.tinet.ctilink.bigqueue.service.agent;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.pagehelper.StringUtil;
import com.tinet.ctilink.ami.action.AmiActionResponse;
import com.tinet.ctilink.ami.inc.AmiChanVarNameConst;
import com.tinet.ctilink.bigqueue.ami.action.GetVarActionService;
import com.tinet.ctilink.bigqueue.ami.action.HangupActionService;
import com.tinet.ctilink.bigqueue.ami.action.OriginateActionService;
import com.tinet.ctilink.bigqueue.ami.action.SetVarActionService;
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
import com.tinet.ctilink.inc.Const;
import com.tinet.ctilink.json.JSONObject;
import com.tinet.ctilink.scheduler.RedisTaskScheduler;
import com.tinet.ctilink.util.RedisLock;

@Component
public class DisconnectService {
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
	SetVarActionService setVarActionService;
	@Autowired
	OriginateActionService originateActionService;
	@Autowired
	HangupActionService hangupActionService;
	
	public ActionResponse disconnect(Map<String,Object> params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		String disconnectedCno = params.get("disconnectedCno").toString();
		//先获取lock memberService.lockMember(enterpriseId, cno);
		RedisLock memberLock = memberService.lockMember(enterpriseId, disconnectedCno);
		if(memberLock != null){
			try{
				CallAgent callAgent = agentService.getCallAgent(enterpriseId, cno);
				if(callAgent != null){
					Integer sipId = callAgent.getCurrentSipId();
					String channel = callAgent.getCurrentChannel();
					String uniqueId = callAgent.getCurrentChannelUniqueId();
					Integer callType = callAgent.getCurrentCallType();
					if(StringUtil.isEmpty(channel)){
						response = ActionResponse.createFailResponse(-1, "no channel");
						return response;
					}
					String destChannel = callAgent.getCurrentChannel();;
					Map<String, String> varMap = new HashMap<String,String>();
					varMap.put(AmiChanVarNameConst.CDR_FORCE_DISCONNECT, "1");
					 if (callType == Const.CDR_CALL_TYPE_IB || callType ==Const.CDR_CALL_TYPE_OB_WEBCALL || callType == Const.CDR_CALL_TYPE_PREDICTIVE_OB){//呼入
						 destChannel = callAgent.getBridgedChannel();
				     }else if(callType == Const.CDR_CALL_TYPE_OB || callType == Const.CDR_CALL_TYPE_DIRECT_OB || callType == Const.CDR_CALL_TYPE_PREVIEW_OB){//点击外呼
				    	 destChannel = callAgent.getCurrentChannel();
				     }
					setVarActionService.setVar(sipId, destChannel, varMap);
					
					AmiActionResponse amiResponse = hangupActionService.hangup(sipId, channel, Const.CDR_HANGUP_CAUSE_DISCONNECT);
    				if(amiResponse != null && (amiResponse.getCode() == 0)){
    					JSONObject event = new JSONObject();
                        event.put("event", "disconnectUnlink");
                        event.put("enterpriseId", enterpriseId);
                        event.put("cno", disconnectedCno);
                        event.put("disconnectorCno", cno);
                        redisService.convertAndSend(BigQueueCacheKey.AGENT_GATEWAY_EVENT_TOPIC, event);
                        
    					response = ActionResponse.createSuccessResponse();
    					return response;
    				}else{
    					response = ActionResponse.createFailResponse(-1, "hangup exception");
    					return response;
    				}
				}else {
					response = ActionResponse.createFailResponse(-1, "no such agent");
					return response;
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
