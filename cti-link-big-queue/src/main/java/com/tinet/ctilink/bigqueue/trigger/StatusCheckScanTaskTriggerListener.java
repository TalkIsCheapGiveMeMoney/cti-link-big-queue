package com.tinet.ctilink.bigqueue.trigger;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.github.davidmarquis.redisscheduler.TaskTriggerListener;
import com.tinet.ctilink.bigqueue.entity.CallAgent;
import com.tinet.ctilink.bigqueue.inc.BigQueueConst;
import com.tinet.ctilink.bigqueue.service.imp.AgentServiceImp;
import com.tinet.ctilink.bigqueue.service.imp.ChannelServiceImp;
import com.tinet.ctilink.bigqueue.service.imp.EnterpriseServiceImp;
import com.tinet.ctilink.bigqueue.service.imp.MemberServiceImp;
import com.tinet.ctilink.cache.RedisService;
import com.tinet.ctilink.conf.model.Entity;

/**
 * @author fengwei //
 * @date 16/4/23 15:38
 */
public class StatusCheckScanTaskTriggerListener implements TaskTriggerListener {
	
	@Autowired
	private RedisService redisService;
	@Autowired
	private MemberServiceImp memberService;
	@Autowired
	private EnterpriseServiceImp enterpriseService;
	@Autowired
	private AgentServiceImp agentService;
	@Autowired
	private ChannelServiceImp channelService;
    @Override
    public void taskTriggered(String taskId) {

        List<Entity> enterpriseList = enterpriseService.getAllActive();
        
        for(Entity entity: enterpriseList){
        	
        	scanStatusCheck(String.valueOf(entity.getEnterpriseId()));
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
    		Integer deviceStatus = (Integer)map.get(cno);
    		String startTimeField = "start_" + field;
    		Integer startTime = (Integer)map.get(startTimeField);
    		switch(deviceStatus){
	    		case BigQueueConst.MEMBER_DEVICE_STATUS_INVALID:
	    			break;
	    		case BigQueueConst.MEMBER_DEVICE_STATUS_IDLE:
	    			break;
	    		case BigQueueConst.MEMBER_DEVICE_STATUS_LOCKED:
	    		case BigQueueConst.MEMBER_DEVICE_STATUS_INVITE:
	    		case BigQueueConst.MEMBER_DEVICE_STATUS_RINGING:
	    			if(((new Date().getTime()/1000) - startTime) > BigQueueConst.MEMBER_STATUS_TRYING_MAX_TIMEOUT){
	    				memberService.setDeviceStatus(enterpriseId, cno, BigQueueConst.MEMBER_DEVICE_STATUS_IDLE);
	    				System.out.printf("StatusCheckScan bad status checked: enterpriseId=%s cno=%s", enterpriseId, field);
	    			}
	    			break;
	    		case BigQueueConst.MEMBER_DEVICE_STATUS_INUSE:
	    			CallAgent agent = agentService.getCallAgent(enterpriseId, cno);
	    			if(channelService.isAlive(agent.getCurrentChannelUniqueId())){
	    				
	    			}else{
	    				memberService.setDeviceStatus(enterpriseId, cno, BigQueueConst.MEMBER_DEVICE_STATUS_IDLE);
	    				System.out.printf("StatusCheckScan bad status checked: enterpriseId=%s cno=%s", enterpriseId, field);
	    			}
	    			break;
    		}
    		
    	}
    }

}
