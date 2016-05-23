package com.tinet.ctilink.bigqueue.trigger;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tinet.ctilink.bigqueue.ami.action.GetVarActionService;
import com.tinet.ctilink.bigqueue.ami.action.HangupActionService;
import com.tinet.ctilink.bigqueue.ami.action.OriginateActionService;
import com.tinet.ctilink.bigqueue.inc.BigQueueConst;
import com.tinet.ctilink.bigqueue.service.imp.AgentServiceImp;
import com.tinet.ctilink.bigqueue.service.imp.ChannelServiceImp;
import com.tinet.ctilink.bigqueue.service.imp.MemberServiceImp;
import com.tinet.ctilink.bigqueue.service.imp.QueueEventServiceImp;
import com.tinet.ctilink.bigqueue.service.imp.QueueServiceImp;
import com.tinet.ctilink.scheduler.RedisTaskScheduler;
import com.tinet.ctilink.scheduler.TaskSchedulerTrigger;
import com.tinet.ctilink.util.DateUtil;

/**
 * @author fengwei //
 * @date 16/4/23 15:38
 */
@Component
public class LimitTimeTaskTrigger implements TaskSchedulerTrigger {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	@Autowired
	QueueEventServiceImp queueEventService;
	@Autowired
	GetVarActionService getVarActionService;
	@Autowired
	OriginateActionService originateActionService;	
	@Autowired
	private RedisTaskScheduler redisTaskScheduler;
	@Autowired
	HangupActionService hangupActionService;
	
    public void taskTriggered(String taskId, Map<String, Object> param) {
    	Integer sipId = Integer.parseInt(param.get("sipId").toString());
    	String channel = param.get("channel").toString();
    	String uniqueId = param.get("uniqueId").toString();
    	Integer alertSecond = Integer.parseInt(param.get("alertSecond").toString());
    	String file = param.get("file").toString();
    	
    	Map<String, Object> varParam = new HashMap<String, Object>();
    	varParam.put("CHANNEL(state)", "channelState");
    	
    	Map<String, Object> getVarResponse = getVarActionService.getVar(sipId, channel, varParam);
    	if(getVarResponse != null){
    		if(getVarResponse.get("channelState") != null){
    			String channelState = getVarResponse.get("channelState").toString();
    			if(!channelState.equals("Up")){
    				return;
    			}
    		}else{
    			return;
    		}
    	}
		if(alertSecond > 0){
			String context = "mix_sound";
			String extension = "s";
			String targetContext = "global_channel_spy";
			String targetExtension = "s";
			Map<String, Object> varMap = new HashMap<String, Object>();
			varMap.put("mix_sound", file);
			varMap.put("spied_channel", channel);
			varMap.put("spied_unique_id", uniqueId);
			
	        Map<String, Object> actionMap = new HashMap<String, Object>();
	        actionMap.put("context", targetContext);
	        actionMap.put("exten", targetExtension);
	        actionMap.put("priority", 1);
	        actionMap.put("channel", "Local/" + extension + "@" + context);
	        actionMap.put("timeout", 5000);
	        actionMap.put("clid", "");     
	
			originateActionService.originate(sipId, actionMap, null, varMap);
			
			Map<String, Object> limitTimeParams = new HashMap<String, Object>();
			limitTimeParams.put("channel", channel);
			limitTimeParams.put("uniqueId", uniqueId);
			limitTimeParams.put("alertSecond", 0);
			limitTimeParams.put("file", file);
	
 
			Date triggerTime = DateUtil.addSecond(new Date(), alertSecond);
			redisTaskScheduler.scheduleTimed("limitTimeTaskSchedulerGroup",
					String.format(BigQueueConst.LIMIT_TIME_TASK_ID, uniqueId), 
					"LimitTimeTaskTrigger", 
					limitTimeParams,
					triggerTime.getTime()/1000);
		}else{
			hangupActionService.hangup(sipId, channel, new Integer(16));
		}
    }
}
