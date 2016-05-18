package com.tinet.ctilink.bigqueue.strategy;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tinet.ctilink.bigqueue.entity.CallAttemp;
import com.tinet.ctilink.bigqueue.entity.CallMember;
import com.tinet.ctilink.bigqueue.service.imp.QueueServiceImp;
@Component
public class LeastRecentStrategy implements Strategy, InitializingBean{
	
	@Autowired
	QueueServiceImp queueService;
	
	@Override
	public void afterPropertiesSet() throws Exception{
		StrategyFactory.register("rrordered", this);
	}
	
	@Override
	public List<CallAttemp> calcMetric(String enterpriseId, String qno, String uniqueId){
		List<CallAttemp> attempList = new ArrayList<CallAttemp>();
		
		List<CallMember> memberList = queueService.getMembers(enterpriseId, qno);
		for(CallMember callMember: memberList){
			Integer dialedCount = queueService.getQueueEntryDialed(uniqueId, callMember.getCno());
			Integer metric = callMember.getPenalty() * 1000 + callMember.getLastCall() + dialedCount * 10000000;
			callMember.setMetic(metric);
			
			CallAttemp callAttemp = new CallAttemp();
			callAttemp.setStillGoing(true);
			callAttemp.setCallMember(callMember);
			attempList.add(callAttemp);
		}
		memberList = null;
		return attempList;
		
	}

	@Override
	public void memberSelectedHandle(String enterpriseId, String qno, String cno, String uniqueId, String customerNumber){
	}
	
	@Override
	public void joinHandle(String enterpriseId, String qno, String uniqueId, String customerNumber){
	}

}
