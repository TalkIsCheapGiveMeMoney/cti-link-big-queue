package com.tinet.ctilink.bigqueue.strategy;

import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.tinet.ctilink.bigqueue.entity.CallMember;
import com.tinet.ctilink.bigqueue.service.imp.QueueServiceImp;

public class LeastRecentStrategy implements Strategy, InitializingBean{
	
	@Autowired
	QueueServiceImp queueService;
	
	@Override
	public void afterPropertiesSet() throws Exception{
		StrategyFactory.register("rrordered", this);
	}
	
	@Override
	public List<CallMember> calcMetric(String enterpriseId, String qno, String uniqueId){
		List<CallMember> memberList = queueService.getMembers(enterpriseId, qno);
		for(CallMember callMember: memberList){
			Integer dialedCount = queueService.getQueueEntryDialed(uniqueId, callMember.getCno());
			Integer metric = callMember.getPenalty() * 1000 + callMember.getLastCall() + dialedCount * 10000000;
			callMember.setMetic(metric);
		}
		return memberList;
		
	}

	@Override
	public void memberSelectedHandle(String enterpriseId, String qno, String cno, String uniqueId, String customerNumber){
	}
	
	@Override
	public void joinHandle(String enterpriseId, String qno, String uniqueId, String customerNumber){
	}

}
