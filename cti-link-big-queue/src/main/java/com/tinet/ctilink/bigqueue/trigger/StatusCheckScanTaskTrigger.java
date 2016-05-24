package com.tinet.ctilink.bigqueue.trigger;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tinet.ctilink.bigqueue.entity.CallAgent;
import com.tinet.ctilink.bigqueue.inc.BigQueueConst;
import com.tinet.ctilink.bigqueue.service.imp.AgentServiceImp;
import com.tinet.ctilink.bigqueue.service.imp.ChannelServiceImp;
import com.tinet.ctilink.bigqueue.service.imp.EnterpriseServiceImp;
import com.tinet.ctilink.bigqueue.service.imp.MemberServiceImp;
import com.tinet.ctilink.cache.RedisService;
import com.tinet.ctilink.conf.model.Entity;
import com.tinet.ctilink.scheduler.TaskSchedulerTrigger;

/**
 * @author fengwei //
 * @date 16/4/23 15:38
 */
@Component
public class StatusCheckScanTaskTrigger implements TaskSchedulerTrigger {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private MemberServiceImp memberService;
	@Autowired
	private EnterpriseServiceImp enterpriseService;
	@Autowired
	private AgentServiceImp agentService;
	@Autowired
	private ChannelServiceImp channelService;

    
    public void taskTriggered(String taskId, Map<String, Object> param) {

        List<Entity> enterpriseList = enterpriseService.getAllActive();
        
        for(Entity entity: enterpriseList){
        	try{
        		scanStatusCheck(String.valueOf(entity.getEnterpriseId()));
        	}catch(Exception e){
        		e.printStackTrace();
        	}
        }
    }
    private void scanStatusCheck(String enterpriseId){

    	Map<Object, Object> map = memberService.getDeviceStatusAll(enterpriseId);
    	//循环每个坐席
    	for(Object object: map.keySet()){
    		String field = object.toString();
    		if(field.startsWith("start_")){
    			continue;
    		}
    		String cno = field;
    		//获取deviceStatus
    		Integer deviceStatus = Integer.parseInt(map.get(cno).toString());
    		String startTimeField = "start_" + field;
    		Integer startTime = Integer.parseInt(map.get(startTimeField).toString());
    		switch(deviceStatus){
	    		case BigQueueConst.MEMBER_DEVICE_STATUS_INVALID:
	    			break;
	    		case BigQueueConst.MEMBER_DEVICE_STATUS_IDLE:
	    			break;
	    		case BigQueueConst.MEMBER_DEVICE_STATUS_LOCKED:
	    			if(((new Date().getTime()/1000) - startTime) > BigQueueConst.MEMBER_STATUS_LOCKED_MAX_TIMEOUT){
	    				memberService.setDeviceStatus(enterpriseId, cno, BigQueueConst.MEMBER_DEVICE_STATUS_IDLE);
	    				logger.error(String.format("StatusCheckScan bad status checked: enterpriseId=%s cno=%s", enterpriseId, field));
	    			}
	    			break;
	    		case BigQueueConst.MEMBER_DEVICE_STATUS_INVITE:
	    		case BigQueueConst.MEMBER_DEVICE_STATUS_RINGING:
	    			if(((new Date().getTime()/1000) - startTime) > BigQueueConst.MEMBER_STATUS_TRYING_MAX_TIMEOUT){
	    				memberService.setDeviceStatus(enterpriseId, cno, BigQueueConst.MEMBER_DEVICE_STATUS_IDLE);
	    				logger.error(String.format("StatusCheckScan bad status checked: enterpriseId=%s cno=%s", enterpriseId, field));
	    			}
	    			break;
	    		case BigQueueConst.MEMBER_DEVICE_STATUS_INUSE:
	    			CallAgent agent = agentService.getCallAgent(enterpriseId, cno);
	    			if(channelService.isAlive(agent.getCurrentChannelUniqueId())){
	    				
	    			}else{
	    				memberService.setDeviceStatus(enterpriseId, cno, BigQueueConst.MEMBER_DEVICE_STATUS_IDLE);
	    				logger.error("StatusCheckScan bad status checked: enterpriseId=%s cno=%s", enterpriseId, field);
	    			}
	    			break;
    		}
    		
    	}
    }

}
