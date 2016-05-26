package com.tinet.ctilink.bigqueue.service.agent;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tinet.ctilink.ami.action.AmiActionResponse;
import com.tinet.ctilink.ami.inc.AmiChanVarNameConst;
import com.tinet.ctilink.bigqueue.ami.action.ConsultActionService;
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
import com.tinet.ctilink.inc.Const;
import com.tinet.ctilink.json.JSONObject;
import com.tinet.ctilink.scheduler.RedisTaskScheduler;
import com.tinet.ctilink.util.RedisLock;

@Component
public class ConsultService {
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
	ConsultActionService consultActionService;
	
	public ActionResponse consult(Map<String,Object> params){
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
					
					if(StringUtils.isEmpty(channel)){
						response = ActionResponse.createFailResponse(-1, "no channel");
						return response;
					}
					String consultObject = params.get("consultObject").toString();     //电话号码    座席号    分机号
		            String objectType = params.get("objectType").toString();           //0.电话  1.座席号  2.分机
		            String extension = objectType + consultObject + "#";
		            if(objectType.equals("1")){
		        		CallAgent consultedCallAgent = agentService.getCallAgent(enterpriseId, consultObject);
		        		if(consultedCallAgent != null){
	                		if(memberService.isAvalibleLock(enterpriseId, consultObject) == false){
		        				response = ActionResponse.createFailResponse(-1, "consulted agent busy");
		        				return response;
		        			}
		        		}else{
		        			response = ActionResponse.createFailResponse(-1, "no such consult agent");
							return response;
		        		}
		            }
		            AmiActionResponse amiResponse = consultActionService.consult(sipId, channel, "web_consult", extension);
    				if(amiResponse != null && (amiResponse.getCode() == 0)){
    					response = ActionResponse.createSuccessResponse();
    	            	return response;
    	            }else{
    	            	response = ActionResponse.createFailResponse(-1, "originate fail");
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
