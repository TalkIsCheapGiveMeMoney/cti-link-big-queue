package com.tinet.ctilink.bigqueue.trigger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import com.github.davidmarquis.redisscheduler.TaskTriggerListener;
import com.tinet.ctilink.bigqueue.entity.Queue;
import com.tinet.ctilink.bigqueue.inc.BigQueueConst;
import com.tinet.ctilink.bigqueue.inc.BigQueueMacro;
import com.tinet.ctilink.bigqueue.service.imp.ChannelServiceImp;
import com.tinet.ctilink.bigqueue.service.imp.MemberServiceImp;
import com.tinet.ctilink.bigqueue.service.imp.QueueServiceImp;
import com.tinet.ctilink.cache.RedisService;

/**
 * @author fengwei //
 * @date 16/4/23 15:38
 */
public class StatusScanTaskTriggerListener implements TaskTriggerListener {
	
	@Autowired
	private RedisService redisService;
	@Autowired
	private QueueServiceImp queueService;
	@Autowired
	private MemberServiceImp memberService;
	@Autowired
	private ChannelServiceImp channelService;
	
	private Map<String, Integer> memberDeviceStatusMap = new HashMap<String, Integer>();
	private Map<String, Integer> memberLoginStatusMap = new HashMap<String, Integer>();
	
	
	
    @Override
    public void taskTriggered(String taskId) {
        System.out.printf("Task %s is due for execution.", taskId);
        memberDeviceStatusMap.clear();
        memberLoginStatusMap.clear();
        
        Map<String, Set<String>> memberQueueMap = BigQueueMacro.getReplaceMemberQueueMap();
        
        Set<String> queueScaned = queueService.getScanSet();
        
        for(String queue: queueScaned){
        	String enterpriseId = queue.substring(0, BigQueueConst.ENTERPRISE_ID_LEN);
        	String qno = queue.substring(BigQueueConst.ENTERPRISE_ID_LEN);
        	scanStatus(enterpriseId, qno, memberQueueMap);
        	scanEntry(enterpriseId, qno);
        }
        BigQueueMacro.replaceMemberQueueMap();
    }
    private void scanEntry(String enterpriseId, String qno){

    	Set<String> entrySet = queueService.getQueueEntrySet(enterpriseId, qno);
		for(String entry: entrySet){
			String uniqueId = entry;
			if(channelService.isAlive(uniqueId)){
				
			}else{
				System.out.printf("发现宕机造成的通道不存在，%s %s %s", enterpriseId, qno, uniqueId);
				queueService.hangup(enterpriseId, qno, uniqueId);
			}
		}
    }
    private void scanStatus(String enterpriseId, String qno, Map<String, Set<String>> memberQueueMap){
    	Integer idleCount = 0;
    	Integer avalibleCount = 0;
    	//获取队列joinEmpty设置
    	Queue queue = queueService.getFromConfCache(enterpriseId, qno);
    	if(queue == null){
    		return;
    	}
    	Integer joinEmpty = queue.getJoinEmpty();
    	
    	//获取队列中
    	Set<String> memberSet = queueService.getMemberSet(enterpriseId, qno);
    	
    	//循环每个坐席
    	for(String member: memberSet){
    		String cid = enterpriseId + member;
    		Set<String> memberQueueSet = memberQueueMap.get(cid);
    		if(memberQueueSet != null){
    			memberQueueSet.add(qno);
    		}else{
    			memberQueueSet = new HashSet<String>();
    			memberQueueSet.add(qno);
    			memberQueueMap.put(cid, memberQueueSet);
    		}
    		//获取deviceStatus
    		Integer deviceStatus;
    		if(memberDeviceStatusMap.containsKey(member)){
    			deviceStatus = memberDeviceStatusMap.get(member);
    		}else {
    			Object value = memberService.getDeviceStatus(enterpriseId, member);
    			if(value != null){
    				deviceStatus = Integer.valueOf(value.toString());
    			}else {
    				deviceStatus = BigQueueConst.MEMBER_DEVICE_STATUS_INVALID;
    			}
    			memberDeviceStatusMap.put(member.toString(), deviceStatus);
    		}
    		//获取loginStatus
    		Integer loginStatus;
    		if(memberLoginStatusMap.containsKey(member)){
    			loginStatus = memberLoginStatusMap.get(member);
    		}else {
    			Object value = memberService.getLoginStatus(enterpriseId, member);
    			if(value != null){
    				loginStatus = Integer.valueOf(value.toString());
    			}else {
    				loginStatus = BigQueueConst.MEMBER_LOGIN_STATUS_OFFLINE;
    			}
    			memberLoginStatusMap.put(member.toString(), loginStatus);
    		}
    			
    		//判断是否avalible
    		if(queueService.isAvalibleMember(deviceStatus, loginStatus, joinEmpty)){
    			avalibleCount ++;
    		}
    		//判断是否idle
    		if(queueService.isIdleMember(deviceStatus, loginStatus) == true){
    			idleCount ++;
    		}
    	}
    	//结果写回
    	queueService.setQueueAvalibleCount(enterpriseId, qno, avalibleCount);
    	queueService.setQueueIdleCount(enterpriseId, qno, idleCount);
    }

}
