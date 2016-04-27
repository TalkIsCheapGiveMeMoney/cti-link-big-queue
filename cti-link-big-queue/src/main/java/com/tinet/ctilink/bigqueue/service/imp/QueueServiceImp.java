package com.tinet.ctilink.bigqueue.service.imp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tinet.ctilink.bigqueue.entity.CallMember;
import com.tinet.ctilink.bigqueue.entity.Queue;
import com.tinet.ctilink.bigqueue.inc.BigQueueCacheKey;
import com.tinet.ctilink.bigqueue.inc.BigQueueChannelVar;
import com.tinet.ctilink.bigqueue.inc.BigQueueConst;
import com.tinet.ctilink.bigqueue.strategy.Strategy;
import com.tinet.ctilink.bigqueue.strategy.StrategyFactory;
import com.tinet.ctilink.cache.CacheKey;
import com.tinet.ctilink.cache.RedisService;
import com.tinet.ctilink.inc.Const;
import com.tinet.ctilink.json.JSONObject;
import com.tinet.ctilink.util.RedisLock;
import com.tinet.ctilink.util.RedisLockUtil;

@Service
public class QueueServiceImp {
	
	@Autowired
	RedisService redisService;
	
	@Autowired
	MemberServiceImp memberService;
	
    public JSONObject getFromConfCache(String enterpriseId, String qno){
    	String key = String.format(CacheKey.QUEUE_ENTERPRISE_ID_QNO, enterpriseId, qno);
    	String res = redisService.get(Const.REDIS_DB_CONF_INDEX, key, String.class);
    	JSONObject object = null;
    	if(StringUtils.isEmpty(res)){
    		object = JSONObject.fromObject(res);
    	}
    	return object;
    }
    
    public Integer join(String enterpriseId, String qno, String customerNumber, String uniqueId, Integer priority, Integer joinTime){
    	Integer res = BigQueueConst.QUEUE_CODE_JOIN_EMPTY;
    	JSONObject object = getFromConfCache(enterpriseId, qno);
    	if(object != null){
    		Queue queue = object.getBean(Queue.class);
    		Integer avalibleCount = getQueueAvalibleCount(enterpriseId, qno);
    		if(avalibleCount > 0){
    			res = BigQueueConst.QUEUE_CODE_JOIN_EMPTY;
    			return res;
    		}
    		
    		if(queue.getMaxLen() > 0 && getQueueEntryCount(enterpriseId, qno) >= queue.getMaxLen()){
    			res = BigQueueConst.QUEUE_CODE_JOIN_FULL;
    			return res;
    		}
    		
    		insertQueueEntry(enterpriseId, qno, uniqueId, priority, joinTime);
    		addScan(enterpriseId, qno);
    		
    		Strategy strategy = StrategyFactory.getInstance(queue.getStrategy());
    		strategy.joinHandle(enterpriseId, qno, uniqueId, customerNumber);
    		res = BigQueueConst.QUEUE_CODE_JOIN_OK;
    		return res;
    	}
    	return res;
    }
    
    public void leave(String enterpriseId, String qno, String uniqueId){
    	removeQueueEntry(enterpriseId, qno, uniqueId);

		//如果queueEntry没有等待的了，删除扫描列表
		Integer entryCount = getQueueEntryCount(enterpriseId, qno);
		if(entryCount == 0){
			removeScan(enterpriseId, qno);
		}
    }
    
    public void hangup(String enterpriseId, String qno, String uniqueId){
		if(getQueueEntryIndex(enterpriseId, qno, uniqueId) != null){
			leave(enterpriseId, qno, uniqueId);
		}
    }
    public CallMember findBest(String enterpriseId, String qno, String uniqueId, String customerNumber, String queueRemeberMember){
    	JSONObject object = getFromConfCache(enterpriseId, qno);
    	if(object != null){
    		Queue queue = object.getBean(Queue.class);
    		Strategy strategy = StrategyFactory.getInstance(queue.getStrategy());

    		List<CallMember> memberList = strategy.calcMetric(enterpriseId, qno, uniqueId);
			CallMember callMember = null;
			if(StringUtils.isNotEmpty(queueRemeberMember)){
				callMember = findRemember(memberList, queueRemeberMember);
				if(callMember != null){
					if(memberService.isAvalible(enterpriseId, callMember.getCno())){
						strategy.memberSelectedHandle(enterpriseId, qno, callMember.getCno(), uniqueId, customerNumber);
						incQueueEntryDialed(uniqueId, callMember.getCno());
    					penddingEntry(enterpriseId, qno, uniqueId);
    					CallMember selectMember = memberService.getCallMember(enterpriseId, qno, callMember.getCno());
    					return selectMember;
					}
				}
			}
    		while(true){
    			callMember = findBestMetric(memberList);
    			if(callMember != null){
    				if(memberService.isAvalible(enterpriseId, callMember.getCno())){
    					strategy.memberSelectedHandle(enterpriseId, qno, callMember.getCno(), uniqueId, customerNumber);
    					incQueueEntryDialed(uniqueId, callMember.getCno());
    					penddingEntry(enterpriseId, qno, uniqueId);
    					CallMember selectMember = memberService.getCallMember(enterpriseId, qno, callMember.getCno());
    					return selectMember;
    				}
    			}
    		}
    	}
    	return null;
    }

    public CallMember findRemember(List<CallMember> memberList, String rememberCno){
    	for(CallMember callMember: memberList){
    		if(callMember.getCno().equals(rememberCno)){
    			return callMember;
    		}
    	}
    	return null;
    }
    
	public CallMember findBestMetric(List<CallMember> memberList){
		CallMember bestCallMember = null;
		Integer minMetric = Integer.MAX_VALUE;
		for(CallMember callMember: memberList){
			if(callMember.getMetic() < minMetric){
				minMetric = callMember.getMetic();
				bestCallMember = callMember;
			}
		}
		return bestCallMember;	
	}
	
    public boolean isAvalibleMember(Integer deviceStatus, Integer loginStatus, Integer joinEmptyCondition){
    	
		switch(deviceStatus){
		case BigQueueConst.MEMBER_DEVICE_STATUS_INVALID:
			if((joinEmptyCondition & BigQueueConst.QUEUE_EMPTY_INVALID) > 0){
				return false;
			}
			break;
		case BigQueueConst.MEMBER_DEVICE_STATUS_IDLE:
			break;
		case BigQueueConst.MEMBER_DEVICE_STATUS_LOCKED:
		case BigQueueConst.MEMBER_DEVICE_STATUS_INVITE:
		case BigQueueConst.MEMBER_DEVICE_STATUS_RINGING:
			if((joinEmptyCondition & BigQueueConst.QUEUE_EMPTY_RINGING) > 0){
				return false;
			}
			break;
		case BigQueueConst.MEMBER_DEVICE_STATUS_INUSE:
			if((joinEmptyCondition & BigQueueConst.QUEUE_EMPTY_INUSE) > 0){
				return false;
			}
			break;
		}
		
		switch(loginStatus){
		case BigQueueConst.MEMBER_LOGIN_STATUS_OFFLINE:
			return false;
		case BigQueueConst.MEMBER_LOGIN_STATUS_READY:
			return true;
		case BigQueueConst.MEMBER_LOGIN_STATUS_PAUSE:
			if((joinEmptyCondition & BigQueueConst.QUEUE_EMPTY_PAUSED) > 0){
				return false;
			}
		case BigQueueConst.MEMBER_LOGIN_STATUS_WRAPUP:
			if((joinEmptyCondition & BigQueueConst.QUEUE_EMPTY_WRAPUP) > 0){
				return false;
			}
		}
		return true;
    }
    
    public boolean isIdleMember(Integer deviceStatus, Integer loginStatus){
    	boolean avalible = false;
		switch(deviceStatus){
		case BigQueueConst.MEMBER_DEVICE_STATUS_INVALID:
			break;
		case BigQueueConst.MEMBER_DEVICE_STATUS_IDLE:
			avalible = true;
			break;
		case BigQueueConst.MEMBER_DEVICE_STATUS_LOCKED:
		case BigQueueConst.MEMBER_DEVICE_STATUS_INVITE:
		case BigQueueConst.MEMBER_DEVICE_STATUS_RINGING:
		case BigQueueConst.MEMBER_DEVICE_STATUS_INUSE:
			break;
		}
		if(avalible){
			switch(loginStatus){
			case BigQueueConst.MEMBER_LOGIN_STATUS_OFFLINE:
				avalible = false;
				break;
			case BigQueueConst.MEMBER_LOGIN_STATUS_READY:
				break;
			case BigQueueConst.MEMBER_LOGIN_STATUS_PAUSE:
				avalible = false;
				break;
			case BigQueueConst.MEMBER_LOGIN_STATUS_WRAPUP:
				avalible = false;
				break;
			}
		}
		return avalible;
    }
    
    public void setQueueIdleCount(String enterpriseId, String qno, Integer count){
    	String idleMemberKey = String.format(BigQueueCacheKey.QUEUE_IDLE_MEMBER, enterpriseId);
    	redisService.hset(Const.REDIS_DB_CTI_INDEX, idleMemberKey, qno, count);
    }
    
    public Integer getQueueIdleCount(String enterpriseId, String qno){
    	String idleMemberKey = String.format(BigQueueCacheKey.QUEUE_IDLE_MEMBER, enterpriseId);
    	return (Integer)redisService.hget(Const.REDIS_DB_CTI_INDEX, idleMemberKey, qno);
    }
    
    public void setQueueAvalibleCount(String enterpriseId, String qno, Integer count){
    	String avalibleMemberKey = String.format(BigQueueCacheKey.QUEUE_AVALIBLE_MEMBER, enterpriseId);
    	redisService.hset(Const.REDIS_DB_CTI_INDEX, avalibleMemberKey, qno, count);
    }
    
    public Integer getQueueAvalibleCount(String enterpriseId, String qno){
    	String avalibleMemberKey = String.format(BigQueueCacheKey.QUEUE_AVALIBLE_MEMBER, enterpriseId);
    	return (Integer)redisService.hget(Const.REDIS_DB_CTI_INDEX, avalibleMemberKey, qno);
    }
    
    public Integer getQueueEntryCount(String enterpriseId, String qno){
    	String queueEntryKey = String.format(BigQueueCacheKey.QUEUE_ENTRY, enterpriseId, qno);
    	return redisService.zcount(Const.REDIS_DB_CTI_INDEX, queueEntryKey, Double.MIN_VALUE, Double.MAX_VALUE ).intValue();
    }
    
    public void insertQueueEntry(String enterpriseId, String qno, String uniqueId, Integer priority,Integer joinTime){
		String queueEntryKey = String.format(BigQueueCacheKey.QUEUE_ENTRY, enterpriseId, qno);
		String queueEntryNpKey = String.format(BigQueueCacheKey.QUEUE_ENTRY_NP, enterpriseId, qno);
		Integer score = priority * BigQueueConst.QUEUE_PRIORITY_MULTIPILER + joinTime % BigQueueConst.QUEUE_PRIORITY_MULTIPILER;
		
		redisService.zadd(Const.REDIS_DB_CTI_INDEX, queueEntryKey, uniqueId, score);
		redisService.zadd(Const.REDIS_DB_CTI_INDEX, queueEntryNpKey, uniqueId, score);
    }
    public void removeQueueEntry(String enterpriseId, String qno, String uniqueId){
		//删除queue_entry_7000001_0001
		//删除queue_entry_np_7000001_0001
		String queueEntryKey = String.format(BigQueueCacheKey.QUEUE_ENTRY, enterpriseId, qno);
		String queueEntryNpKey = String.format(BigQueueCacheKey.QUEUE_ENTRY_NP, enterpriseId, qno);
		
		redisService.zrem(Const.REDIS_DB_CTI_INDEX, queueEntryKey, uniqueId);
		redisService.zrem(Const.REDIS_DB_CTI_INDEX, queueEntryNpKey,uniqueId);
		
		//删除queue_entry_info_${uniqueId}
		String queueEntryInfoKey =  String.format(BigQueueCacheKey.QUEUE_ENTRY_INFO, uniqueId);
		redisService.delete(Const.REDIS_DB_CTI_INDEX, queueEntryInfoKey);
    }
    
    public void penddingEntry(String enterpriseId, String qno, String uniqueId){
    	String queueEntryNpKey = String.format(BigQueueCacheKey.QUEUE_ENTRY_NP, enterpriseId, qno);
    	redisService.zrem(Const.REDIS_DB_CTI_INDEX, queueEntryNpKey, uniqueId);
    }
    
    public void unPenddingEntry(String enterpriseId, String qno, String uniqueId){
    	String queueEntryKey = String.format(BigQueueCacheKey.QUEUE_ENTRY_NP, enterpriseId, qno);
    	Integer score = redisService.zscore(Const.REDIS_DB_CTI_INDEX, queueEntryKey, uniqueId).intValue();
    	String queueEntryNpKey = String.format(BigQueueCacheKey.QUEUE_ENTRY_NP, enterpriseId, qno);
    	redisService.zadd(Const.REDIS_DB_CTI_INDEX, queueEntryNpKey, uniqueId, score);
    }
    
    public Set<String> getScanSet(){
    	Set<String> queueScaned = redisService.smembers(Const.REDIS_DB_CTI_INDEX, BigQueueCacheKey.QUEUE_SCAN_LIST);
    	return queueScaned;
    }
    
    public void addScan(String enterpriseId, String qno){
    	redisService.sadd(Const.REDIS_DB_CTI_INDEX, BigQueueCacheKey.QUEUE_SCAN_LIST, enterpriseId + qno);
    }
    
    public void removeScan(String enterpriseId, String qno){
    	redisService.srem(Const.REDIS_DB_CTI_INDEX, BigQueueCacheKey.QUEUE_SCAN_LIST, enterpriseId + qno);
    }
    
    public Integer getQueueEntryIndex(String enterpriseId, String qno, String uniqueId){
    	String queueEntryKey = String.format(BigQueueCacheKey.QUEUE_ENTRY, enterpriseId, qno);
		Long res = redisService.zrank(Const.REDIS_DB_CTI_INDEX, queueEntryKey, uniqueId);
		if(res != null){
			return res.intValue();
		}
		return null;
    }
    
    public Integer getQueueEntryNpIndex(String enterpriseId, String qno, String uniqueId){
    	String queueEntryKey = String.format(BigQueueCacheKey.QUEUE_ENTRY_NP, enterpriseId, qno);
		Long res = redisService.zrank(Const.REDIS_DB_CTI_INDEX, queueEntryKey, uniqueId);
		if(res != null){
			return res.intValue();
		}
		return null;
    }
    
    public Integer getQueueEntryLinpos(String uniqueId){
		String key = String.format(BigQueueCacheKey.QUEUE_ENTRY_INFO, uniqueId);
		Integer linpos = (Integer) redisService.hget(Const.REDIS_DB_CTI_INDEX, key, "linpos");
		return linpos;
    }
    
    public void setQueueEntryLinpos(String uniqueId, Integer linpos){
 		String key = String.format(BigQueueCacheKey.QUEUE_ENTRY_INFO, uniqueId);
 		redisService.hset(Const.REDIS_DB_CTI_INDEX, key, "linpos", linpos);
	}
    
    public Integer getQueueEntryDialed(String uniqueId, String cno){
    	String key = String.format(BigQueueCacheKey.QUEUE_ENTRY_INFO, uniqueId);
 		Integer dialedCount = (Integer)redisService.hget(Const.REDIS_DB_CTI_INDEX, key, cno);
 		return dialedCount;
    }
    
    public void incQueueEntryDialed(String uniqueId, String cno){
    	String key = String.format(BigQueueCacheKey.QUEUE_ENTRY_INFO, uniqueId);
 		redisService.hincrby(Const.REDIS_DB_CTI_INDEX, key, cno, 1);
    }
    
    public Integer getQueueRrpos(String enterpriseId, String qno ){
    	String key = String.format(BigQueueCacheKey.QUEUE_RRPOS, enterpriseId);
 		Integer res = (Integer)redisService.hget(Const.REDIS_DB_CTI_INDEX, key, qno);
 		if(res == null){
 			redisService.hincrby(Const.REDIS_DB_CTI_INDEX, key, qno, 0);
 			return 0;
 		}
 		return res;
    }
    
    public void incQueueRrpos(String enterpriseId, String qno){
    	String key = String.format(BigQueueCacheKey.QUEUE_RRPOS, enterpriseId);
 		redisService.hincrby(Const.REDIS_DB_CTI_INDEX, key, qno, 1);
    }
    
    public Set<String> getMemberSet(String enterpriseId, String qno){
    	//获取队列中
    	String key = String.format(BigQueueCacheKey.QUEUE_MEMBER, enterpriseId, qno);
    	Set<Object> memberSet = redisService.hgetallfield(Const.REDIS_DB_CTI_INDEX, key);
    	Set<String> res = new HashSet<String>();
    	for(Object object: memberSet){
    		res.add(object.toString());
    	}
    	return res;
    }
    public List<CallMember> getMembers(String enterpriseId, String qno){
    	String key = String.format(BigQueueCacheKey.QUEUE_MEMBER, enterpriseId, qno);
    	Map<Object,Object> res = redisService.hgetall(Const.REDIS_DB_CTI_INDEX, key);
    	if(res != null){
	    	List<CallMember> list = new ArrayList<CallMember>();
	    	for(Object mapKey: res.keySet()){
	    		String jsonStr = res.get(mapKey).toString();
	    		if(StringUtils.isNotEmpty(jsonStr)){
	    			CallMember callMember = JSONObject.fromObject(res).getBean(CallMember.class);
	    			list.add(callMember);
	    		}
	    	}
	    	return list;
    	}
    	return null;
    }
    public RedisLock lockQueue(String enterpriseId, String qno){
    	String key = String.format(BigQueueCacheKey.QUEUE_LOCK, enterpriseId, qno);
    	RedisLock lock = RedisLockUtil.lock(key, BigQueueConst.QUEUE_LOCK_TIMEOUT);
    	return lock;
    }
    
    public void unlockQueue(RedisLock lock){
    	RedisLockUtil.unLock(lock);
    }
}

