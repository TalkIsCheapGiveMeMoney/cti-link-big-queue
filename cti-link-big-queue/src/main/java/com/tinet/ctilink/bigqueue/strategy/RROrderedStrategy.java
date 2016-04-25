package com.tinet.ctilink.bigqueue.strategy;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.tinet.ctilink.bigqueue.entity.CallMember;

public class RROrderedStrategy implements Strategy{
	
	@Override
	public void calcMetric(String enterpriseId, String qno, List<CallMember> list){
		String key = String.format(CacheKey.QUEUE_ENTRY_INFO, )
		Integer metric = 1;
		for(CallMember callMember: list){
			Integer metric = callMember.getPenalty() * 100 + random()%100;
			callMember.setMetic(metric++);
		}
	}
	public Member findBest(String enterpriseId, String qno, List<CallMember> list){
		calcMetric(enterpriseId, qno, list);
		String key = String.format(CacheKey.QUEUE_ENTRY_INFO, )
	}
}
