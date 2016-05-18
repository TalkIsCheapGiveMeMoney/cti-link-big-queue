package com.tinet.ctilink.bigqueue.strategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tinet.ctilink.bigqueue.entity.CallAttemp;
import com.tinet.ctilink.bigqueue.entity.CallMember;
import com.tinet.ctilink.bigqueue.service.imp.QueueServiceImp;
@Component
public class RRMemoryStrategy implements Strategy, InitializingBean{
	
	@Autowired
	QueueServiceImp queueService;
	
	@Override
	public void afterPropertiesSet() throws Exception{
		StrategyFactory.register("rrmemory", this);
	}
	
	@Override
	public List<CallAttemp> calcMetric(String enterpriseId, String qno, String uniqueId){
		List<CallAttemp> attempList = new ArrayList<CallAttemp>();
		
		List<CallMember> memberList = queueService.getMembers(enterpriseId, qno);
		if(memberList.size() == 0){
			return attempList; 
		}
		Collections.sort(memberList,new Comparator<CallMember>(){
            public int compare(CallMember arg0, CallMember arg1) {
                return Integer.valueOf(arg0.getCno()).compareTo(Integer.valueOf(arg1.getCno()));
            }
        });
		
		Integer rrpos = queueService.getQueueStatistic(enterpriseId, qno, "rrpos");
		rrpos = rrpos % memberList.size();
		Integer pos = 0;
		
		for(CallMember callMember: memberList){
			Integer metric;
			if(pos < rrpos){
				metric = 10000 + pos;
			}else{
				metric = pos;
			}
			metric += callMember.getPenalty() * 1000000;
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
		queueService.incQueueStatistic(enterpriseId, qno, "rrpos", 1);
	}
	
	@Override
	public void joinHandle(String enterpriseId, String qno, String uniqueId, String customerNumber){
	}

}
