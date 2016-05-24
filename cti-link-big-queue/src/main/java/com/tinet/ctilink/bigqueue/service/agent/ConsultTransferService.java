package com.tinet.ctilink.bigqueue.service.agent;

import java.util.Date;
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
import com.tinet.ctilink.util.DateUtil;
import com.tinet.ctilink.util.RedisLock;

@Component
public class ConsultTransferService {
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
	HangupActionService hangupActionService;
	@Autowired
	OriginateActionService originateActionService;
	public ActionResponse consultTransfer(Map<String,Object> params){
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
					String uniqueId = callAgent.getCurrentChannelUniqueId();
					String consultChannel = callAgent.getConsultChannel();
					if(StringUtil.isEmpty(channel)){
						response = ActionResponse.createFailResponse(-1, "no channel");
						return response;
					}
					if(StringUtil.isEmpty(consultChannel)){
						response = ActionResponse.createFailResponse(-1, "no consult channel");
						return response;
					}
					AmiActionResponse amiResponse = hangupActionService.hangup(sipId, channel, new Integer(3));
    				if(amiResponse != null && (amiResponse.getCode() == 0)){
    					if(params.get("limitTimeSecond") != null){
    	    	            String limitTimeSecond = params.get("limitTimeSecond").toString();
    	    	            String limitTimeAlertSecond = params.get("limitTimeAlertSecond").toString();
    	    	            String limitTimeFile = params.get("limitTimeFile").toString();
    	    	            if(StringUtil.isNotEmpty(limitTimeSecond)){

    	    	                if(StringUtil.isEmpty(limitTimeAlertSecond)){
    	    	                    limitTimeAlertSecond = "60";
    	    	                }
    	    	                Integer alertSecond = Integer.parseInt(limitTimeAlertSecond);
    	    	                Integer limitSecond = Integer.parseInt(limitTimeSecond);
    	    	                if(alertSecond >= limitSecond){
    	    	                    alertSecond = 0;
    	    	                }
    	    	                if(StringUtil.isEmpty(limitTimeFile));{
    	    	                    limitTimeFile="1_minute_left";
    	    	                }
    	    	                Map<String, Object> limitTimeParams = new HashMap<String, Object>();
    	    	                limitTimeParams.put("channel", consultChannel);
    	    	                limitTimeParams.put("uniqueId", uniqueId);
    	    	                limitTimeParams.put("alertSecond", alertSecond);
    	    	                limitTimeParams.put("file", limitTimeFile);
    	    	                
    							Date triggerTime = DateUtil.addSecond(new Date(), (limitSecond - alertSecond));
    							redisTaskScheduler.scheduleTimed("limitTimeTaskSchedulerGroup",
    									String.format(BigQueueConst.LIMIT_TIME_TASK_ID, uniqueId), 
    									"limitTimeTaskTrigger", 
    									limitTimeParams,
    									triggerTime.getTime());
    	    	            }
        	            }
    					
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
