package com.tinet.ctilink.bigqueue.service.imp;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tinet.ctilink.bigqueue.entity.CallMember;
import com.tinet.ctilink.bigqueue.inc.BigQueueCacheKey;
import com.tinet.ctilink.bigqueue.inc.BigQueueConst;
import com.tinet.ctilink.cache.RedisService;
import com.tinet.ctilink.inc.Const;
import com.tinet.ctilink.json.JSONObject;
import com.tinet.ctilink.util.RedisLock;
import com.tinet.ctilink.util.RedisLockUtil;

@Service
public class MemberServiceImp {
	@Autowired
	RedisService redisService;
	
    public RedisLock lockMember(String enterpriseId, String cno){
    	String key = String.format(BigQueueCacheKey.MEMBER_LOCK_ENTERPRISE_ID_CNO, enterpriseId, cno);
    	RedisLock lock = RedisLockUtil.lock(key, BigQueueConst.MEMBER_LOCK_TIMEOUT);
    	return lock;
    }
    
    public void unlockMember(RedisLock lock){
    	RedisLockUtil.unLock(lock);
    }
    
    /**
     * 被调用时，必须先lockMember成功
     * @param enterpriseId
     * @param cno
     * @param deviceStatus
     */
    public void setDeviceStatus(String enterpriseId, String cno, Integer deviceStatus){
    	String deviceStatusKey = String.format(BigQueueCacheKey.MEMBER_DEVICE_STATUS_ENTERPRISE_ID, enterpriseId);
    	redisService.hset(Const.REDIS_DB_CTI_INDEX, deviceStatusKey, cno, deviceStatus);
    	redisService.hset(Const.REDIS_DB_CTI_INDEX, deviceStatusKey, "start_" + cno, redisService.time());
    }
    public Integer getDeviceStatus(String enterpriseId, String cno){
    	String deviceStatusKey = String.format(BigQueueCacheKey.MEMBER_DEVICE_STATUS_ENTERPRISE_ID, enterpriseId);
    	Object value = redisService.hget(Const.REDIS_DB_CTI_INDEX, deviceStatusKey, cno);
    	return (Integer) value;
    }
    public Integer getDeviceStatusStartTime(String enterpriseId, String cno){
    	String deviceStatusKey = String.format(BigQueueCacheKey.MEMBER_DEVICE_STATUS_ENTERPRISE_ID, enterpriseId);
    	Object value = redisService.hget(Const.REDIS_DB_CTI_INDEX, deviceStatusKey, "start_" + cno);
    	return (Integer) value;
    }
    public Map<Object, Object> getDeviceStatusAll(String enterpriseId){
    	String deviceStatusKey = String.format(BigQueueCacheKey.MEMBER_DEVICE_STATUS_ENTERPRISE_ID, enterpriseId);
    	Map<Object, Object> map = redisService.hgetall(Const.REDIS_DB_CTI_INDEX, deviceStatusKey);
    	return map;
    }
    /**
     * 被调用时，必须先lockMember成功
     * @param enterpriseId
     * @param cno
     * @param loginStatus
     */
    public void setLoginStatus(String enterpriseId, String cno, Integer loginStatus){
    	String loginStatusKey = String.format(BigQueueCacheKey.MEMBER_LOGIN_STATUS_ENTERPRISE_ID, enterpriseId);
    	redisService.hset(Const.REDIS_DB_CTI_INDEX, loginStatusKey, cno, loginStatus);
    	redisService.hset(Const.REDIS_DB_CTI_INDEX, loginStatusKey, "start_" + cno, redisService.time());
    }
    public Integer getLoginStatus(String enterpriseId, String cno){
    	String loginStatusKey = String.format(BigQueueCacheKey.MEMBER_LOGIN_STATUS_ENTERPRISE_ID, enterpriseId);
    	Object value = redisService.hget(Const.REDIS_DB_CTI_INDEX, loginStatusKey, cno);
    	return (Integer) value;
    }
    public Integer getLoginStatusStartTime(String enterpriseId, String cno){
    	String loginStatusKey = String.format(BigQueueCacheKey.MEMBER_LOGIN_STATUS_ENTERPRISE_ID, enterpriseId);
    	Object value = redisService.hget(Const.REDIS_DB_CTI_INDEX, loginStatusKey, "start_" + cno);
    	return (Integer) value;
    }
    public CallMember getCallMember(String enterpriseId, String qno, String cno){
    	String key = String.format(BigQueueCacheKey.QUEUE_MEMBER_ENTERPRISE_ID_QNO, enterpriseId, qno);
    	Object res = redisService.hget(Const.REDIS_DB_CTI_INDEX, key, cno);
    	if(res != null){
	    	JSONObject object = null;
	    	if(StringUtils.isEmpty(res.toString())){
	    		object = JSONObject.fromObject(res);
	    		return object.getBean(CallMember.class);
	    	}
    	}
    	return null;
    }
    /**
     * 
     * @param enterpriseId
     * @param cno
     * @return true 如果可用，并且锁定坐席状态到locked 返回false不可用，不锁定坐席状态
     */
    public boolean isAvalible(String enterpriseId, String cno){
    	 Boolean res = false;
    	 RedisLock lock = lockMember(enterpriseId, cno);
    	 if(lock != null){
    		 Integer deviceStatus = getDeviceStatus(enterpriseId, cno);
    		 switch (deviceStatus){
	    		 case BigQueueConst.MEMBER_DEVICE_STATUS_INVALID:
	    			 break;
	    		 case BigQueueConst.MEMBER_DEVICE_STATUS_IDLE:
	    			 res = true;
	    			 break;
	    		 case BigQueueConst.MEMBER_DEVICE_STATUS_LOCKED:
	    		 case BigQueueConst.MEMBER_DEVICE_STATUS_INVITE:
	    		 case BigQueueConst.MEMBER_DEVICE_STATUS_RINGING:
	    		 case BigQueueConst.MEMBER_DEVICE_STATUS_INUSE:
	    			 break;
    		 }
    		 if(res == true){
    			 Integer loginStatus = getLoginStatus(enterpriseId, cno);
    			 switch (loginStatus){
		    		 case BigQueueConst.MEMBER_LOGIN_STATUS_OFFLINE:
		    			 res = false;
		    			 break;
		    		 case BigQueueConst.MEMBER_LOGIN_STATUS_READY:
		    			 break;
		    		 case BigQueueConst.MEMBER_LOGIN_STATUS_PAUSE:
		    		 case BigQueueConst.MEMBER_LOGIN_STATUS_WRAPUP:
		    			 res = false;
		    			 break;
    			 }
    		 }
    		 if(res == true){
    			 setDeviceStatus(enterpriseId, cno, BigQueueConst.MEMBER_DEVICE_STATUS_LOCKED);
    		 }
    		 unlockMember(lock);
    	 }
    	 return false;
    }
}
