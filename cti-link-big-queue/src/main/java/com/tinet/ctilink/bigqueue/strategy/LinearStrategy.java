package com.tinet.ctilink.bigqueue.strategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tinet.ctilink.bigqueue.entity.CallAttemp;
import com.tinet.ctilink.bigqueue.entity.CallMember;
import com.tinet.ctilink.bigqueue.service.imp.QueueServiceImp;
@Component
public class LinearStrategy implements Strategy,InitializingBean{
	@Autowired
	QueueServiceImp queueService;
	
	@Override
	public void afterPropertiesSet() throws Exception{
		StrategyFactory.register("linear", this);
	}
	
	@Override
	public List<CallAttemp> calcMetric(String enterpriseId, String qno, String uniqueId){
		List<CallAttemp> attempList = new ArrayList<CallAttemp>();
		
		Set<String> memberSet = queueService.getMemberSet(enterpriseId, qno);
		List<CallMember> memberList=new ArrayList<CallMember>();  
		for(String member: memberSet){
			CallMember callMember = new CallMember();
			callMember.setCno(member);
			memberList.add(callMember);
		}

		Collections.sort(memberList,new Comparator<CallMember>(){
            public int compare(CallMember arg0, CallMember arg1) {
                return Integer.valueOf(arg0.getCno()).compareTo(Integer.valueOf(arg1.getCno()));
            }
        });
		
		Integer linpos = queueService.getQueueEntryLinpos(uniqueId);
		linpos = linpos % memberList.size();
		Integer pos = 0;
		for(CallMember callMember: memberList){
			if(pos < linpos){
				callMember.setMetic(pos + 10000);
			}else{
				callMember.setMetic(pos);
			}
			pos ++;
			
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
		Integer linpos = queueService.getQueueEntryLinpos(uniqueId);
		queueService.setQueueEntryLinpos(uniqueId, linpos++);
	}
	
	@Override
	public void joinHandle(String enterpriseId, String qno, String uniqueId, String customerNumber){
		queueService.setQueueEntryLinpos(uniqueId, 0);
	}

}
