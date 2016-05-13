package com.tinet.ctilink.bigqueue.strategy;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.tinet.ctilink.bigqueue.entity.CallMember;
import com.tinet.ctilink.bigqueue.service.imp.QueueServiceImp;

public class RRMemoryStrategy implements Strategy, InitializingBean{
	
	@Autowired
	QueueServiceImp queueService;
	
	@Override
	public void afterPropertiesSet() throws Exception{
		StrategyFactory.register("rrmemory", this);
	}
	
	@Override
	public List<CallMember> calcMetric(String enterpriseId, String qno, String uniqueId){
		List<CallMember> memberList = queueService.getMembers(enterpriseId, qno);
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
		}
		return memberList;
		
	}

	@Override
	public void memberSelectedHandle(String enterpriseId, String qno, String cno, String uniqueId, String customerNumber){
		queueService.incQueueStatistic(enterpriseId, qno, "rrpos", 1);
	}
	
	@Override
	public void joinHandle(String enterpriseId, String qno, String uniqueId, String customerNumber){
	}

}
