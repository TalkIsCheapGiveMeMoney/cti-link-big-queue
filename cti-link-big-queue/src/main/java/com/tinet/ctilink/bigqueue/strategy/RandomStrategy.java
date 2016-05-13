package com.tinet.ctilink.bigqueue.strategy;

import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.tinet.ctilink.bigqueue.entity.CallMember;
import com.tinet.ctilink.bigqueue.service.imp.QueueServiceImp;

public class RandomStrategy implements Strategy, InitializingBean{
	
	@Autowired
	QueueServiceImp queueService;
	
	@Override
	public void afterPropertiesSet() throws Exception{
		StrategyFactory.register("rrmemory", this);
	}
	
	@Override
	public List<CallMember> calcMetric(String enterpriseId, String qno, String uniqueId){
		List<CallMember> memberList = queueService.getMembers(enterpriseId, qno);
		Random rand = new Random();
		for(CallMember callMember: memberList){
			Integer metric = callMember.getPenalty() * 1000000 + rand.nextInt(1000);
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
