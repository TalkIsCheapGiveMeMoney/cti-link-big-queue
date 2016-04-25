package com.tinet.ctilink.bigqueue.strategy;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.tinet.ctilink.bigqueue.entity.CallMember;

public class LinearStrategy implements Strategy{
	
	@Override
	public void calcMetric(String enterpriseId, String qno, String uniqueId, List<CallMember> list){
		Collections.sort(list,new Comparator<CallMember>(){
            public int compare(CallMember arg0, CallMember arg1) {
                return arg0.getCno().compareTo(arg1.getCno());
            }
        });
		
		String key = String.format(CacheKey.QUEUE_ENTRY_INFO, uniqueId);
		Integer linpos = redisService.opsForHashed.get(key, "linpos");
		Integer pos = 1;
		for(CallMember callMember: list){
			if(pos < linpos){
				callMember.setMetic(pos + 10000);
			}else{
				callMember.setMetic(pos);
			}
			pos ++;
		}
	}
	public void memberSelected(String enterpriseId, String qno, String cno){
		String key = String.format(CacheKey.MEMBER_LOCK, enterpriseId, cno);
		Member.locked();
		Member.setDeviceStatus(locked);
		
		String key = String.format(CacheKey.QUEUE_ENTRY_INFO, uniqueId);
		Integer linpos = redisService.opsForHashed.get(key, "linpos");
		
		String key = String.format(CacheKey.QUEUE_ENTRY_INFO, uniqueId);
		Integer linpos = redisService.opsForHashed.get(key, "linpos", linpos++);
	}
}
