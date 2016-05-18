package com.tinet.ctilink.bigqueue.service.imp;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tinet.ctilink.bigqueue.entity.CallAttemp;
import com.tinet.ctilink.bigqueue.entity.CallMember;
import com.tinet.ctilink.bigqueue.inc.BigQueueCacheKey;
import com.tinet.ctilink.bigqueue.inc.BigQueueConst;
import com.tinet.ctilink.bigqueue.strategy.Strategy;
import com.tinet.ctilink.bigqueue.strategy.StrategyFactory;
import com.tinet.ctilink.cache.CacheKey;
import com.tinet.ctilink.cache.RedisService;
import com.tinet.ctilink.conf.model.Queue;
import com.tinet.ctilink.conf.model.QueueMember;
import com.tinet.ctilink.inc.Const;
import com.tinet.ctilink.json.JSONObject;
import com.tinet.ctilink.util.RedisLock;
import com.tinet.ctilink.util.RedisLockUtil;

@Service
public class QueueServiceImp {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	@Autowired
	RedisService redisService;
	
	@Autowired
	MemberServiceImp memberService;
	
	@Autowired
	AgentServiceImp agentService;
	@Autowired
	QueueEventServiceImp queueEventService;
	
    public Queue getFromConfCache(String enterpriseId, String qno){
    	String key = String.format(CacheKey.QUEUE_ENTERPRISE_ID_QNO, Integer.parseInt(enterpriseId), qno);
    	Queue queue = redisService.get(Const.REDIS_DB_CONF_INDEX, key, Queue.class);
    	return queue;
    }
    
    public Integer join(String enterpriseId, String qno, String customerNumber, String uniqueId, Integer priority, Integer joinTime, Integer startTime, Integer overflow){
    	Integer res = BigQueueConst.QUEUE_CODE_JOIN_EMPTY;
    	Queue queue = getFromConfCache(enterpriseId, qno);
    	if(queue != null){
    		Integer avalibleCount = getQueueAvalibleCount(enterpriseId, qno);
    		if(avalibleCount <= 0){
    			res = BigQueueConst.QUEUE_CODE_JOIN_EMPTY;
    			return res;
    		}
    		
    		if(queue.getMaxLen() > 0 && getQueueEntryCount(enterpriseId, qno) >= queue.getMaxLen()){
    			res = BigQueueConst.QUEUE_CODE_JOIN_FULL;
    			return res;
    		}
    		
    		insertQueueEntry(enterpriseId, qno, uniqueId, priority, joinTime, startTime, overflow);
    		addScan(enterpriseId, qno);
    		
    		Strategy strategy = StrategyFactory.getInstance(queue.getStrategy());
    		strategy.joinHandle(enterpriseId, qno, uniqueId, customerNumber);
    		res = BigQueueConst.QUEUE_CODE_JOIN_OK;
    		
			JSONObject queueEvent = new JSONObject();
			queueEvent.put("event", "join");
			queueEvent.put("enterpriseId", enterpriseId);
			queueEvent.put("qno", qno);
			queueEvent.put("startTime", startTime);
			queueEvent.put("priority", priority);
			queueEvent.put("joinTime", joinTime);
			queueEvent.put("customerNumber", customerNumber);
			queueEvent.put("uniqueId", uniqueId);
			queueEvent.put("overflow", overflow);

			queueEventService.publishEvent(queueEvent);
    		return res;
    	}
    	return res;
    }
    private void recalHoldTime(String enterpriseId, String qno, Integer holdTime){
    	Integer currentHoldTime = (Integer)getQueueStatistic(enterpriseId, qno, "hold_time");
    	Integer newHoldTime = (currentHoldTime * 3 + holdTime) / 4;
    	setQueueStatistic(enterpriseId, qno, "hold_time", newHoldTime);
    }
    public void leave(String enterpriseId, String qno, String uniqueId, Integer leaveCode, String cno){
    	Queue queue = getFromConfCache(enterpriseId, qno);
    	if(queue == null){
    		return;
    	}
    	Integer joinTime = (Integer)getQueueEntryInfo(uniqueId, "join_time", Integer.class);
		Integer holdTime = new Long(new Date().getTime()/1000).intValue() - joinTime;
		
    	JSONObject queueEvent = new JSONObject();
    	switch(leaveCode){
    		case BigQueueConst.LEAVE_CODE_COMPLETE:
    			incQueueStatistic(enterpriseId, qno, "completed", 1);
    			recalHoldTime(enterpriseId, qno, holdTime);
    			if(holdTime <= queue.getServiceLevel()){
    				incQueueStatistic(enterpriseId, qno, "completed_in_sl", 1);
    			}
    			
    			queueEvent.put("event", "complete");
    			queueEvent.put("enterpriseId", enterpriseId);
    			queueEvent.put("qno", qno);
    			queueEvent.put("holdTime", holdTime);
    			queueEvent.put("joinTime", joinTime);
    			queueEvent.put("uniqueId", uniqueId);
    			queueEvent.put("cno", cno);

    			queueEventService.publishEvent(queueEvent);
    			break;
    		case BigQueueConst.LEAVE_CODE_ABANDON:
    			incQueueStatistic(enterpriseId, qno, "abandoned", 1);
    			
    			queueEvent.put("event", "abandon");
    			queueEvent.put("enterpriseId", enterpriseId);
    			queueEvent.put("qno", qno);
    			queueEvent.put("holdTime", holdTime);
    			queueEvent.put("joinTime", joinTime);
    			queueEvent.put("uniqueId", uniqueId);

    			queueEventService.publishEvent(queueEvent);
    			break;
    		case BigQueueConst.LEAVE_CODE_TIMEOUT:
    			incQueueStatistic(enterpriseId, qno, "timeout", 1);
    			
    			queueEvent.put("event", "timeout");
    			queueEvent.put("enterpriseId", enterpriseId);
    			queueEvent.put("qno", qno);
    			queueEvent.put("holdTime", holdTime);
    			queueEvent.put("joinTime", joinTime);
    			queueEvent.put("uniqueId", uniqueId);
    			queueEventService.publishEvent(queueEvent);
    			break;
    		case BigQueueConst.LEAVE_CODE_EMPTY:
    			incQueueStatistic(enterpriseId, qno, "empty", 1);
    			
    			queueEvent.put("event", "empty");
    			queueEvent.put("enterpriseId", enterpriseId);
    			queueEvent.put("qno", qno);
    			queueEvent.put("holdTime", holdTime);
    			queueEvent.put("joinTime", joinTime);
    			queueEvent.put("uniqueId", uniqueId);
    			queueEventService.publishEvent(queueEvent);
    			break;
    	}

    	removeQueueEntry(enterpriseId, qno, uniqueId);

		//如果queueEntry没有等待的了，删除扫描列表
		Integer entryCount = getQueueEntryCount(enterpriseId, qno);
		if(entryCount == 0){
			removeScan(enterpriseId, qno);
		}
    }
    
    public void hangup(String enterpriseId, String qno, String cno, String uniqueId){
    	if(cno != null && StringUtils.isNotEmpty(cno)){
    		memberService.deviceStatusUnlock(enterpriseId, cno);
    	}
		if(getQueueEntryIndex(enterpriseId, qno, uniqueId) != null){
			leave(enterpriseId, qno, uniqueId, BigQueueConst.LEAVE_CODE_ABANDON, "");
		}
    }
    private boolean compareWeight(Integer weight, String enterpriseId, String cno){
    	List<QueueMember> queueMemberList = agentService.getQueueMemberList(enterpriseId, cno);
    	for(QueueMember queueMember: queueMemberList){
    		Queue queue = getFromConfCache(enterpriseId, queueMember.getQno());
    		if(queue != null){
	    		Integer queueEntryCount = getQueueEntryCount(enterpriseId, queueMember.getQno());
	    		Integer queueAvalible = getQueueIdleCount(enterpriseId, queueMember.getQno());
	    		if(queue.getWeight() > weight && queueEntryCount >= queueAvalible){
	    			return false;
	    		}
    		}
    	}
    	return true;
    }
    
    public CallMember findBest(String enterpriseId, String qno, String uniqueId, String customerNumber, String queueRemeberMember){
    	Queue queue = getFromConfCache(enterpriseId, qno);
    	if(queue != null){
    		Strategy strategy = StrategyFactory.getInstance(queue.getStrategy());

    		List<CallAttemp> attempList = strategy.calcMetric(enterpriseId, qno, uniqueId);
    		CallAttemp callAttemp = null;
			if(StringUtils.isNotEmpty(queueRemeberMember)){
				callAttemp = findRemember(attempList, queueRemeberMember);
				if(callAttemp != null){
					callAttemp.setStillGoing(false);
					if(memberService.isAvalibleLock(enterpriseId, callAttemp.getCallMember().getCno())){
						strategy.memberSelectedHandle(enterpriseId, qno, callAttemp.getCallMember().getCno(), uniqueId, customerNumber);
						incQueueEntryDialed(uniqueId, callAttemp.getCallMember().getCno());
    					penddingEntry(enterpriseId, qno, uniqueId);
    					CallMember selectMember = memberService.getCallMember(enterpriseId, qno, callAttemp.getCallMember().getCno());
    					
    					JSONObject queueEvent = new JSONObject();
    	    			queueEvent.put("event", "call");
    	    			queueEvent.put("enterpriseId", enterpriseId);
    	    			queueEvent.put("qno", qno);
    	    			queueEvent.put("cno", callAttemp.getCallMember().getCno());
    	    			queueEvent.put("uniqueId", uniqueId);
    					queueEventService.publishEvent(queueEvent);
    					
    					JSONObject event = new JSONObject();
    					event.put("event", "queueCall");
    					event.put("enterpriseId", enterpriseId);
    					event.put("qno", qno);
    					event.put("cno", callAttemp.getCallMember().getCno());
    					event.put("uniqueId", uniqueId);
    					event.put("customerNumber", customerNumber);
    	    			redisService.convertAndSend(BigQueueCacheKey.AGENT_GATEWAY_EVENT_TOPIC, event);
    					return selectMember;
					}
				}
			}
    		while(true){
    			callAttemp = findBestMetric(attempList);
    			if(callAttemp != null){
    				callAttemp.setStillGoing(false);
    				if(compareWeight(queue.getWeight(), enterpriseId, callAttemp.getCallMember().getCno())){
	    				if(memberService.isAvalibleLock(enterpriseId, callAttemp.getCallMember().getCno())){
	    					strategy.memberSelectedHandle(enterpriseId, qno, callAttemp.getCallMember().getCno(), uniqueId, customerNumber);
	    					incQueueEntryDialed(uniqueId, callAttemp.getCallMember().getCno());
	    					penddingEntry(enterpriseId, qno, uniqueId);
	    					
	    					JSONObject queueEvent = new JSONObject();
	    	    			queueEvent.put("event", "call");
	    	    			queueEvent.put("enterpriseId", enterpriseId);
	    	    			queueEvent.put("qno", qno);
	    	    			queueEvent.put("cno", callAttemp.getCallMember().getCno());
	    	    			queueEvent.put("uniqueId", uniqueId);
	    					queueEventService.publishEvent(queueEvent);
	    					
	    					CallMember selectMember = memberService.getCallMember(enterpriseId, qno, callAttemp.getCallMember().getCno());
	    					return selectMember;
	    				}
    				}
    			}else{
    				break;
    			}
    			
    		}
    	}
    	return null;
    }
    
    public void rna(String enterpriseId, String qno, String cno, String uniqueId){
    	unPenddingEntry(enterpriseId, qno, uniqueId);
    	
    	memberService.deviceStatusUnlock(enterpriseId, cno);
    	
		JSONObject queueEvent = new JSONObject();
		queueEvent.put("event", "rna");
		queueEvent.put("enterpriseId", enterpriseId);
		queueEvent.put("qno", qno);
		queueEvent.put("cno", cno);
		queueEvent.put("uniqueId", uniqueId);
		queueEventService.publishEvent(queueEvent);
    }

    private CallAttemp findRemember(List<CallAttemp> attempList, String rememberCno){
    	for(CallAttemp callAttemp: attempList){
    		if(callAttemp.getCallMember().getCno().equals(rememberCno)){
    			return callAttemp;
    		}
    	}
    	return null;
    }
    
	private CallAttemp findBestMetric(List<CallAttemp> attempList){
		CallAttemp bestCallAttemp = null;
		Integer minMetric = Integer.MAX_VALUE;
		for(CallAttemp callAttemp: attempList){
			if(callAttemp.isStillGoing() == true && callAttemp.getCallMember().getMetic() < minMetric){
				minMetric = callAttemp.getCallMember().getMetic();
				bestCallAttemp = callAttemp;
			}
		}
		return bestCallAttemp;	
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
    	String idleMemberKey = String.format(BigQueueCacheKey.QUEUE_IDLE_MEMBER_ENTERPRISE_ID, enterpriseId);
    	redisService.hset(Const.REDIS_DB_CTI_INDEX, idleMemberKey, qno, count);
    }
    
    public Integer getQueueIdleCount(String enterpriseId, String qno){
    	String idleMemberKey = String.format(BigQueueCacheKey.QUEUE_IDLE_MEMBER_ENTERPRISE_ID, enterpriseId);
    	Integer count = (Integer)redisService.hget(Const.REDIS_DB_CTI_INDEX, idleMemberKey, qno, Integer.class);
    	return (count == null)? 0:count;
    }
    
    public void setQueueAvalibleCount(String enterpriseId, String qno, Integer count){
    	String avalibleMemberKey = String.format(BigQueueCacheKey.QUEUE_AVALIBLE_MEMBER_ENTERPRISE_ID, enterpriseId, qno);
    	redisService.hset(Const.REDIS_DB_CTI_INDEX, avalibleMemberKey, qno, count);
    }
    
    public Integer getQueueAvalibleCount(String enterpriseId, String qno){
    	String avalibleMemberKey = String.format(BigQueueCacheKey.QUEUE_AVALIBLE_MEMBER_ENTERPRISE_ID, enterpriseId, qno);
    	Integer count = (Integer)redisService.hget(Const.REDIS_DB_CTI_INDEX, avalibleMemberKey, qno, Integer.class);
    	return (count == null)? 0:count;
    }
    public Set<String> getQueueEntrySet(String enterpriseId, String qno){
    	String queueEntryKey = String.format(BigQueueCacheKey.QUEUE_ENTRY_ENTERPRISE_ID_QNO, enterpriseId, qno);
    	return redisService.zrange(Const.REDIS_DB_CTI_INDEX, queueEntryKey, Long.MIN_VALUE, Long.MAX_VALUE );
    }
    public <T> Object getQueueEntryInfo(String uniqueId, String field, Class<T> clazz){
		String key = String.format(BigQueueCacheKey.QUEUE_ENTRY_INFO_UNIQUE_ID, uniqueId);
		return redisService.hget(Const.REDIS_DB_CTI_INDEX, key, field, clazz);
    }
    public void setQueueEntryInfo(String uniqueId, String field, Object value){
		String key = String.format(BigQueueCacheKey.QUEUE_ENTRY_INFO_UNIQUE_ID, uniqueId);
		redisService.hset(Const.REDIS_DB_CTI_INDEX, key, field, value);
    }
    public Integer getQueueEntryCount(String enterpriseId, String qno){
    	String queueEntryKey = String.format(BigQueueCacheKey.QUEUE_ENTRY_ENTERPRISE_ID_QNO, enterpriseId, qno);
    	return redisService.zcount(Const.REDIS_DB_CTI_INDEX, queueEntryKey, Double.MIN_VALUE, Double.MAX_VALUE ).intValue();
    }
    
    public void insertQueueEntry(String enterpriseId, String qno, String uniqueId, Integer priority,Integer joinTime, Integer startTime, Integer overflow){
		String queueEntryKey = String.format(BigQueueCacheKey.QUEUE_ENTRY_ENTERPRISE_ID_QNO, enterpriseId, qno);
		String queueEntryNpKey = String.format(BigQueueCacheKey.QUEUE_ENTRY_NP_ENTERPRISE_ID_QNO, enterpriseId, qno);
		Integer score = priority * BigQueueConst.QUEUE_PRIORITY_MULTIPILER + startTime % BigQueueConst.QUEUE_PRIORITY_MULTIPILER;
		
		redisService.zadd(Const.REDIS_DB_CTI_INDEX, queueEntryKey, uniqueId, score);
		redisService.zadd(Const.REDIS_DB_CTI_INDEX, queueEntryNpKey, uniqueId, score);
		
		setQueueEntryInfo(uniqueId, "join_time", joinTime);
		setQueueEntryInfo(uniqueId, "priority", priority);
		setQueueEntryInfo(uniqueId, "start_time", startTime);
		setQueueEntryInfo(uniqueId, "overflow", overflow);
		setQueueEntryInfo(uniqueId, "priority", priority);
    }
    public void removeQueueEntry(String enterpriseId, String qno, String uniqueId){
		//删除queue_entry_7000001_0001
		//删除queue_entry_np_7000001_0001
		String queueEntryKey = String.format(BigQueueCacheKey.QUEUE_ENTRY_ENTERPRISE_ID_QNO, enterpriseId, qno);
		String queueEntryNpKey = String.format(BigQueueCacheKey.QUEUE_ENTRY_NP_ENTERPRISE_ID_QNO, enterpriseId, qno);
		
		redisService.zrem(Const.REDIS_DB_CTI_INDEX, queueEntryKey, uniqueId);
		redisService.zrem(Const.REDIS_DB_CTI_INDEX, queueEntryNpKey,uniqueId);
		
		//删除queue_entry_info_${uniqueId}
		String queueEntryInfoKey =  String.format(BigQueueCacheKey.QUEUE_ENTRY_INFO_UNIQUE_ID, uniqueId);
		redisService.delete(Const.REDIS_DB_CTI_INDEX, queueEntryInfoKey);
    }
    
    private void penddingEntry(String enterpriseId, String qno, String uniqueId){
    	String queueEntryNpKey = String.format(BigQueueCacheKey.QUEUE_ENTRY_NP_ENTERPRISE_ID_QNO, enterpriseId, qno);
    	redisService.zrem(Const.REDIS_DB_CTI_INDEX, queueEntryNpKey, uniqueId);
    }
    
    private void unPenddingEntry(String enterpriseId, String qno, String uniqueId){
    	String queueEntryKey = String.format(BigQueueCacheKey.QUEUE_ENTRY_ENTERPRISE_ID_QNO, enterpriseId, qno);
    	Integer score = redisService.zscore(Const.REDIS_DB_CTI_INDEX, queueEntryKey, uniqueId).intValue();
    	String queueEntryNpKey = String.format(BigQueueCacheKey.QUEUE_ENTRY_NP_ENTERPRISE_ID_QNO, enterpriseId, qno);
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
    	String queueEntryKey = String.format(BigQueueCacheKey.QUEUE_ENTRY_ENTERPRISE_ID_QNO, enterpriseId, qno);
		Long res = redisService.zrank(Const.REDIS_DB_CTI_INDEX, queueEntryKey, uniqueId);
		if(res != null){
			return res.intValue();
		}
		return null;
    }
    
    public Integer getQueueEntryNpIndex(String enterpriseId, String qno, String uniqueId){
    	String queueEntryKey = String.format(BigQueueCacheKey.QUEUE_ENTRY_NP_ENTERPRISE_ID_QNO, enterpriseId, qno);
		Long res = redisService.zrank(Const.REDIS_DB_CTI_INDEX, queueEntryKey, uniqueId);
		if(res != null){
			return res.intValue();
		}
		return null;
    }
    
    public Integer getQueueEntryLinpos(String uniqueId){
		Integer linpos = (Integer) getQueueEntryInfo(uniqueId, "linpos", Integer.class);
		return linpos;
    }
    
    public void setQueueEntryLinpos(String uniqueId, Integer linpos){
 		setQueueEntryInfo(uniqueId, "linpos", linpos);
	}
    
    public Integer getQueueEntryDialed(String uniqueId, String cno){
 		Integer dialedCount = (Integer)getQueueEntryInfo(uniqueId, "dialed_" + cno, Integer.class);
 		return (dialedCount == null)? 0 : dialedCount;
    }
    
    public void incQueueEntryDialed(String uniqueId, String cno){
    	String key = String.format(BigQueueCacheKey.QUEUE_ENTRY_INFO_UNIQUE_ID, uniqueId);
 		redisService.hincrby(Const.REDIS_DB_CTI_INDEX, key, "dialed_" + cno, 1);
    }
    
    public Integer getQueueStatistic(String enterpriseId, String qno, String field){
    	String key = String.format(BigQueueCacheKey.QUEUE_STATISTIC_ENTERPRISE_ID_QNO, enterpriseId, qno);
 		Integer res = (Integer)redisService.hget(Const.REDIS_DB_CTI_INDEX, key, field, Integer.class);
 		if(res == null){
 			redisService.hincrby(Const.REDIS_DB_CTI_INDEX, key, field, 0);
 			return 0;
 		}
 		return res;
    }
    public void setQueueStatistic(String enterpriseId, String qno, String field, Integer value){
    	String key = String.format(BigQueueCacheKey.QUEUE_STATISTIC_ENTERPRISE_ID_QNO, enterpriseId, qno);
    	redisService.hset(Const.REDIS_DB_CTI_INDEX, key, field, value);
    }
    public void incQueueStatistic(String enterpriseId, String qno, String field, Integer value){
    	String key = String.format(BigQueueCacheKey.QUEUE_STATISTIC_ENTERPRISE_ID_QNO, enterpriseId, qno);
    	redisService.hincrby(Const.REDIS_DB_CTI_INDEX, key, field, value);
    }
    
    public Set<String> getMemberSet(String enterpriseId, String qno){
    	//获取队列中
    	String key = String.format(BigQueueCacheKey.QUEUE_MEMBER_ENTERPRISE_ID_QNO, enterpriseId, qno);
    	Set<Object> memberSet = redisService.hkeys(Const.REDIS_DB_CTI_INDEX, key);
    	Set<String> res = new HashSet<String>();
    	for(Object object: memberSet){
    		res.add(object.toString());
    	}
    	return res;
    }
    public List<CallMember> getMembers(String enterpriseId, String qno){
    	String key = String.format(BigQueueCacheKey.QUEUE_MEMBER_ENTERPRISE_ID_QNO, enterpriseId, qno);
    	List<CallMember> list = redisService.hgetList(Const.REDIS_DB_CTI_INDEX, key, CallMember.class);
    	return list;
    }
    
    public CallMember getMember(String enterpriseId, String qno, String cno){
    	String key = String.format(BigQueueCacheKey.QUEUE_MEMBER_ENTERPRISE_ID_QNO, enterpriseId, qno);
    	return redisService.hget(Const.REDIS_DB_CTI_INDEX, key, cno, CallMember.class);
    }
    
    public void setMember(String enterpriseId, QueueMember queueMember){
    	CallMember callMember = getMember(enterpriseId, queueMember.getQno(), queueMember.getCno());
    	if(callMember != null){
    		callMember.setInterface(queueMember.getInterface());
    		callMember.setPenalty(queueMember.getPenalty());
    	}else{
    		callMember = new CallMember();
    		callMember.setInterface(queueMember.getInterface());
    		callMember.setPenalty(queueMember.getPenalty());
    		callMember.setCalls(0);
    		callMember.setCno(queueMember.getCno());
    		callMember.setLastCall(0);
    	}
    	
    	String key = String.format(BigQueueCacheKey.QUEUE_MEMBER_ENTERPRISE_ID_QNO, enterpriseId, queueMember.getQno());
    	redisService.hset(Const.REDIS_DB_CTI_INDEX, key, callMember.getCno(), callMember);
    	return;
    }
    public void delMember(String enterpriseId, String qno, String cno){
    	String key = String.format(BigQueueCacheKey.QUEUE_MEMBER_ENTERPRISE_ID_QNO, enterpriseId, qno);
    	redisService.hdel(Const.REDIS_DB_CTI_INDEX, key, cno);
    }
    
   
    public RedisLock lockQueue(String enterpriseId, String qno){
    	String key = String.format(BigQueueCacheKey.QUEUE_LOCK_ENTERPRISE_ID_QNO, enterpriseId, qno);
    	RedisLock lock = RedisLockUtil.lock(key, BigQueueConst.QUEUE_LOCK_TIMEOUT);
    	return lock;
    }
    
    public void unlockQueue(RedisLock lock){
    	RedisLockUtil.unLock(lock);
    }
}

