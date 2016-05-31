package com.tinet.ctilink.bigqueue.service.agent;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tinet.ctilink.ami.action.AmiActionResponse;
import com.tinet.ctilink.bigqueue.ami.action.ConsultActionService;
import com.tinet.ctilink.bigqueue.ami.action.GetVarActionService;
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
public class InteractService {
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
	ConsultActionService consultActionService;
	public ActionResponse interact(Map<String,Object> params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		
		//先获取lock memberService.lockMember(enterpriseId, cno);
		RedisLock memberLock = memberService.lockMember(enterpriseId, cno);
		if(memberLock != null){
			try{
				CallAgent callAgent = agentService.getCallAgent(enterpriseId, cno);
				if(callAgent != null){
					Integer sipId = callAgent.getCurrentSipId();
					String channel = callAgent.getCurrentChannel();
					String bridgeChannel = callAgent.getBargeChannel();
					Integer callType = callAgent.getCurrentCallType();
					if(StringUtils.isEmpty(channel)){
						response = ActionResponse.createFailResponse(-1, "no channel");
						return response;
					}
					
		            String interactObject = params.get("interactObject").toString();       //ivrId,ivrNode
		            String extension = interactObject + "#";
		            
		            Map<String, String> varMap = new HashMap<String, String>();
		            
	                String setVarChannel;
	                if(callType.equals(Const.CDR_CALL_TYPE_PREVIEW_OB) || callType.equals(Const.CDR_CALL_TYPE_OB)){
	                	setVarChannel = channel;
            			varMap.put("interact_channel", channel);
            			varMap.put("interact_cno", cno);
	                }else{
	                	setVarChannel = bridgeChannel;
            			varMap.put("interact_channel", channel);
            			varMap.put("interact_cno", cno);
	                }
	                setVarActionService.setVar(sipId, setVarChannel, varMap);
	                
		            AmiActionResponse amiResponse = consultActionService.consult(sipId, channel, Const.DIALPLAN_CONTEXT_INTERACT, extension);
		            if(amiResponse != null && (amiResponse.getCode() == 0)){
						//返回
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
