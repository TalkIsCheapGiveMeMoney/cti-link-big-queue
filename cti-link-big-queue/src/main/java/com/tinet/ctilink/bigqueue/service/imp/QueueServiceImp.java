package com.tinet.ctilink.bigqueue.service.imp;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tinet.ctilink.bigqueue.entity.Queue;
import com.tinet.ctilink.bigqueue.inc.CacheKey;
import com.tinet.ctilink.bigqueue.inc.Const;
import com.tinet.ctilink.cache.RedisService;
import com.tinet.ctilink.json.JSONObject;
import com.tinet.ctilink.util.RedisLock;
import com.tinet.ctilink.util.RedisLockUtil;

@Service
public class QueueServiceImp {
	
	@Autowired
	RedisService redisService;
	
    public JSONObject get(String enterpriseId, String qno){
    	String key = String.format(CacheKey.QUEUE_ENTERPRISE_QNO, enterpriseId, qno);
    	String res = redisService.get(key, String.class, 1);
    	JSONObject object = null;
    	if(StringUtils.isEmpty(res)){
    		object = JSONObject.fromObject(res);
    	}
    	return object;
    }
    
    public Integer join(String enterpriseId, String qno, String customerNumber, String uniqueId, Integer priority, Integer joinTime){
    	Integer res = Const.QUEUE_CODE_JOIN_EMPTY;
    	JSONObject object = get(enterpriseId, qno);
    	if(object != null){
    		Queue queue = object.getBean(Queue.class);
    		Integer avalibleCount = getQueueAvalibleCount(enterpriseId, qno);
    		if(avalibleCount > 0){
    			res = Const.QUEUE_CODE_JOIN_EMPTY;
    			return res;
    		}
    		
    		if(queue.getMaxLen() > 0 && getQueueEntryCount(enterpriseId, qno) >= queue.getMaxLen()){
    			res = Const.QUEUE_CODE_JOIN_FULL;
    			return res;
    		}
    		
    		String queueEntryKey = String.format(CacheKey.QUEUE_ENTRY, enterpriseId, qno);
    		String queueEntryNpKey = String.format(CacheKey.QUEUE_ENTRY_NP, enterpriseId, qno);
    		Integer sort = priority * Const.QUEUE_PRIORITY_MULTIPILER + joinTime % Const.QUEUE_PRIORITY_MULTIPILER;
    		redisService.zadd(dbIndex, key, value, score)
    		redisService.zset.add(queueEntryKey,uniqueId,sort);
    		redisService.zset.add(queueEntryNpKey,uniqueId,sort);
    		
    		String key = String.format(CacheKey.QUEUE_ENTRY_INFO, uniqueId);
    		Integer linpos = redisService.opsForHashed.set(key, "linpos", 1);
    		res = Const.QUEUE_CODE_JOIN_OK;
    		return res;
    	}
    	return res;
    }
    public void leave(String enterpriseId, String qno, String uniqueId){
    	//删除queue_entry_7000001_0001
    	//删除queue_entry_np_7000001_0001
    	String queueEntryKey = String.format(CacheKey.QUEUE_ENTRY, enterpriseId, qno);
		String queueEntryNpKey = String.format(CacheKey.QUEUE_ENTRY_NP, enterpriseId, qno);
		
		redisService.zset.rem(queueEntryKey,uniqueId);
		redisService.zset.rem(queueEntryNpKey,uniqueId);
		
		//删除queue_entry_info_${uniqueId}
		String queueEntryInfoKey =  String.format(CacheKey.QUEUE_ENTRY_INFO, uniqueId);
		redisService.hash.rem(queueEntryInfoKey);
    }
    public JSONObject hangup(String enterpriseId, String qno, String uniqueId){
    	String queueEntryKey = String.format(CacheKey.QUEUE_ENTRY, enterpriseId, qno);
		Object object = redisService.zset.rand(queueEntryKey, uniqueId);
		if(object != null){
			leave(enterpriseId, qno, uniqueId);
		}
    }
    public JSONObject findBest(String enterpriseId, String qno,  ){
    	JSONObject object = null;
    	//return object;
    	penddingEntry();
    	
    }
    private RedisLock lock(String enterpriseId, String qno, Integer timeout){
    	String key = String.format(CacheKey.QUEUE_LOCK, enterpriseId, qno);
    	RedisLock lock = RedisLockUtil.lock(key, Const.QUEUE_LOCK_TIMEOUT);
    	return lock;
    }
    private void unlock(RedisLock lock){
    	RedisLockUtil.unLock(lock);
    }
   
    private getMemberList(String enterpriseId, String qno){
    	
    	//获取队列中
    	String key = String.format(CacheKey.QUEUE_MEMBER, enterpriseId, qno);
    	Set<String> memberSet = redisTemplate.opsForHash().keys(key);
    	
    	
    }
    public boolean isAvalibleMember(Integer deviceStatus, Integer loginStatus, Integer joinEmptyCondition){
    	
		switch(deviceStatus){
		case Const.MEMBER_DEVICE_STATUS_INVALID:
			if((joinEmptyCondition & Const.QUEUE_EMPTY_INVALID) > 0){
				return false;
			}
			break;
		case Const.MEMBER_DEVICE_STATUS_IDLE:
			break;
		case Const.MEMBER_DEVICE_STATUS_LOCKED:
		case Const.MEMBER_DEVICE_STATUS_INVITE:
		case Const.MEMBER_DEVICE_STATUS_RINGING:
			if((joinEmptyCondition & Const.QUEUE_EMPTY_RINGING) > 0){
				return false;
			}
			break;
		case Const.MEMBER_DEVICE_STATUS_INUSE:
			if((joinEmptyCondition & Const.QUEUE_EMPTY_INUSE) > 0){
				return false;
			}
			break;
		}
		
		switch(loginStatus){
		case Const.MEMBER_LOGIN_STATUS_OFFLINE:
			return false;
		case Const.MEMBER_LOGIN_STATUS_READY:
			return true;
		case Const.MEMBER_LOGIN_STATUS_PAUSE:
			if((joinEmptyCondition & Const.QUEUE_EMPTY_PAUSED) > 0){
				return false;
			}
		case Const.MEMBER_LOGIN_STATUS_WRAPUP:
			if((joinEmptyCondition & Const.QUEUE_EMPTY_WRAPUP) > 0){
				return false;
			}
		}
		return true;
    }
    
    public boolean isIdleMember(Integer deviceStatus, Integer loginStatus){
    	boolean avalible = false;
		switch(deviceStatus){
		case Const.MEMBER_DEVICE_STATUS_INVALID:
			break;
		case Const.MEMBER_DEVICE_STATUS_IDLE:
			avalible = true;
			break;
		case Const.MEMBER_DEVICE_STATUS_LOCKED:
		case Const.MEMBER_DEVICE_STATUS_INVITE:
		case Const.MEMBER_DEVICE_STATUS_RINGING:
		case Const.MEMBER_DEVICE_STATUS_INUSE:
			break;
		}
		if(avalible){
			switch(loginStatus){
			case Const.MEMBER_LOGIN_STATUS_OFFLINE:
				avalible = false;
				break;
			case Const.MEMBER_LOGIN_STATUS_READY:
				break;
			case Const.MEMBER_LOGIN_STATUS_PAUSE:
				avalible = false;
				break;
			case Const.MEMBER_LOGIN_STATUS_WRAPUP:
				avalible = false;
				break;
			}
		}
		return avalible;
    }
    
    public void setQueueIdleCount(String enterpriseId, String qno, Integer count){
    	String idleMemberKey = String.format(CacheKey.QUEUE_IDLE_MEMBER, enterpriseId);
    	//redisService.opsForHash().put(idleMemberKey, qno, count);
    }
    public Integer getQueueIdleCount(String enterpriseId, String qno){
    	
    	//redisTemplate.opsForHash().put(avalibleMemberKey, qno, avalibleCount);
    	return 0;
    }
    public void setQueueAvalibleCount(String enterpriseId, String qno, Integer count){
    	//结果写回
    	String avalibleMemberKey = String.format(CacheKey.QUEUE_AVALIBLE_MEMBER, enterpriseId);
   
    	//redisTemplate.opsForHash().put(avalibleMemberKey, qno, avalibleCount);
    }
    public Integer getQueueAvalibleCount(String enterpriseId, String qno){
    	String avalibleMemberKey = String.format(CacheKey.QUEUE_AVALIBLE_MEMBER, enterpriseId);
    	//redisTemplate.opsForHash().put(avalibleMemberKey, qno, avalibleCount);
    	return 0;
    }
    public Integer getQueueEntryCount(String enterpriseId, String qno){
    	String queueEntryKey = String.format(CacheKey.QUEUE_ENTRY, enterpriseId, qno);
    	//redisTemplate.opsForHash().put(avalibleMemberKey, qno, avalibleCount);
    	return 0;
    }
    public void insertQueueEntry(String enterpriseId, String qno, String uniqueId, Integer priority,Integer joinTime){
    	
    }
    
    public void penddingEntry(String enterpriseId, String qno, String uniqueId){
    	
    }
}

