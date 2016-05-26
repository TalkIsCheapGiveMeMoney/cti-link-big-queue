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
import com.tinet.ctilink.bigqueue.ami.action.OriginateActionService;
import com.tinet.ctilink.bigqueue.ami.action.TransferActionService;
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
public class TransferService {
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
	TransferActionService transferActionService;
	public ActionResponse transfer(Map<String,Object> params){
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
	
					if(StringUtil.isEmpty(channel)){
						response = ActionResponse.createFailResponse(-1, "no channel");
						return response;
					}
					String transferObject = params.get("transferObject").toString();     //电话号码    座席号    分机号
		            String objectType = params.get("objectType").toString();           //0：普通电话1：座席号 2：IVR id+IVR 节点
		            String extension = objectType + transferObject + "#"; // 
		            
					if(objectType.equals("1")){
		        		CallAgent tranferedCallAgent = agentService.getCallAgent(enterpriseId, transferObject);
		        		if(tranferedCallAgent != null){
	                		if(memberService.isAvalibleLock(enterpriseId, transferObject) == false){
	                			response = ActionResponse.createFailResponse(-1, "transfer agent busy");
	                        	return response;
	                		}
		        		}else{
		        			response = ActionResponse.createFailResponse(-1, "no such transfer agent");
							return response;
		        		}
		            }
					AmiActionResponse amiResponse = transferActionService.transfer(sipId, channel, "web_transfer", extension);
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
