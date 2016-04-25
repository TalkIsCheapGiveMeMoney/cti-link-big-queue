package com.tinet.ctilink.bigqueue.trigger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import com.github.davidmarquis.redisscheduler.TaskTriggerListener;
import com.tinet.ctilink.bigqueue.inc.CacheKey;
import com.tinet.ctilink.bigqueue.inc.Const;
import com.tinet.ctilink.bigqueue.service.imp.QueueServiceImp;
import com.tinet.ctilink.json.JSONObject;

/**
 * @author fengwei //
 * @date 16/4/23 15:38
 */
public class StatusScanTaskTriggerListener implements TaskTriggerListener {
	@Autowired
	private RedisTemplate redisTemplate;
	@Autowired
	private QueueServiceImp queueService;
	
	private Map<String, Integer> memberDeviceStatusMap = new HashMap<String, Integer>();
	private Map<String, Integer> memberLoginStatusMap = new HashMap<String, Integer>();
	
    @Override
    public void taskTriggered(String taskId) {
        System.out.printf("Task %s is due for execution.", taskId);
        memberDeviceStatusMap.clear();
        memberLoginStatusMap.clear();
        
        Set<String> queueScaned = redisTemplate.opsForSet().members(CacheKey.QUEUE_SCAN_LIST);
        for(String queue: queueScaned){
        	String enterpriseId = queue.substring(0, Const.ENTERPRISE_ID_LEN);
        	String qno = queue.substring(Const.ENTERPRISE_ID_LEN);
        	scanStatus(enterpriseId, qno);
        }
    }
    private void scanStatus(String enterpriseId, String qno){
    	Integer idleCount = 0;
    	Integer avalibleCount = 0;
    	//获取队列joinEmpty设置
    	JSONObject queueObject = queueService.get(enterpriseId, qno);
    	Integer joinEmpty = (Integer)queueObject.get("joinEmpty");
    	
    	//获取队列中
    	String key = String.format(CacheKey.QUEUE_MEMBER, enterpriseId, qno);
    	Set<String> memberSet = redisTemplate.opsForHash().keys(key);
    	//循环每个坐席
    	for(String member: memberSet){
    		//获取deviceStatus
    		Integer deviceStatus;
    		if(memberDeviceStatusMap.containsKey(member)){
    			deviceStatus = memberDeviceStatusMap.get(member);
    		}else {
    			String deviceStatusKey = String.format(CacheKey.MEMBER_DEVICE_STATUS, enterpriseId);
    			Object value = redisTemplate.opsForHash().get(deviceStatusKey, member);
    			if(value != null){
    				deviceStatus = Integer.valueOf(value.toString());
    			}else {
    				deviceStatus = Const.MEMBER_DEVICE_STATUS_INVALID;
    			}
    			memberDeviceStatusMap.put(member, deviceStatus);
    		}
    		//获取loginStatus
    		Integer loginStatus;
    		if(memberLoginStatusMap.containsKey(member)){
    			loginStatus = memberLoginStatusMap.get(member);
    		}else {
    			String loginStatusKey = String.format(CacheKey.MEMBER_DEVICE_STATUS, enterpriseId);
    			Object value = redisTemplate.opsForHash().get(loginStatusKey, member);
    			if(value != null){
    				loginStatus = Integer.valueOf(value.toString());
    			}else {
    				loginStatus = Const.MEMBER_LOGIN_STATUS_OFFLINE;
    			}
    			memberLoginStatusMap.put(member, loginStatus);
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
