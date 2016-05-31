package com.tinet.ctilink.bigqueue.service.agent;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.pagehelper.StringUtil;
import com.tinet.ctilink.ami.inc.AmiChanVarNameConst;
import com.tinet.ctilink.bigqueue.ami.action.GetVarActionService;
import com.tinet.ctilink.bigqueue.ami.action.OriginateActionService;
import com.tinet.ctilink.bigqueue.ami.action.RedirectActionService;
import com.tinet.ctilink.bigqueue.ami.action.SetVarActionService;
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
public class DirectCallStartService {
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
	SetVarActionService setVarActionService;
	@Autowired
	OriginateActionService originateActionService;
	@Autowired
	RedirectActionService redirectActionService;
	
	public ActionResponse directCallStart(Map<String,Object> params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		String customerNumber = params.get("customerNumber").toString();
		
		//先获取lock memberService.lockMember(enterpriseId, cno);
		RedisLock memberLock = memberService.lockMember(enterpriseId, cno);
		if(memberLock != null){
			try{
				CallAgent callAgent = agentService.getCallAgent(enterpriseId, cno);
				if(callAgent != null){
					Integer sipId = callAgent.getCurrentSipId();
					String channel = callAgent.getCurrentChannel();
					
					if(StringUtil.isEmpty(channel)){
						response = ActionResponse.createFailResponse(-1, "no channel");
						return response;
					}
					
					String directCallReadWaitStatus = null;
			        Map<String, Object> getVarMap = new HashMap<String, Object>();
			    	getVarMap.put(AmiChanVarNameConst.DIRECT_CALL_READ_STATUS, "1");
			    	Map<String, Object> getVarResponse = getVarActionService.getVar(sipId, channel, getVarMap);
			    	if(getVarResponse != null){
			    		if(getVarResponse.get(AmiChanVarNameConst.DIRECT_CALL_READ_STATUS) != null){
			    			directCallReadWaitStatus = getVarResponse.get(AmiChanVarNameConst.DIRECT_CALL_READ_STATUS).toString();
			    		}else{
			    			response = ActionResponse.createFailResponse(-1, "get var fail");
			    			return response;
			    		}
			    	}
	                if ("1".equals(directCallReadWaitStatus)) {
	                    	Map<String, Object> varMap = new HashMap<String, Object>();
	                    	varMap.put(AmiChanVarNameConst.DIRECT_CALL_READ_DONE, "1");
	                    	varMap.put(AmiChanVarNameConst.CDR_CUSTOMER_NUMBER, customerNumber);
	                    	setVarActionService.setVar(sipId, channel, varMap);
	                    	  
	                    	redirectActionService.redirect(sipId, channel, "direct_call_read", "~~s~~", 1,
	                    			null,null,null,null);
	                    	response = ActionResponse.createSuccessResponse();
	                    	return response;
		                } else {
		                	response = ActionResponse.createFailResponse(-1, "callback timeout");
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
