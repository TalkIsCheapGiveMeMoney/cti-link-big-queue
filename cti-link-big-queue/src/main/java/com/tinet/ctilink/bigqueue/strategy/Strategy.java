package com.tinet.ctilink.bigqueue.strategy;

import java.util.List;

import com.tinet.ctilink.bigqueue.entity.CallMember;

public interface Strategy {
	
	//顺序 linear
	//轮选 rrmemory
	//平均 fewestcalls
	//随机 random
	//最长空闲时间 leastrecent
	//技能优先 rrordered
	public List<CallMember> calcMetric(String enterpriseId, String qno, String uniqueId);
	public void memberSelectedHandle(String enterpriseId, String qno, String cno, String uniqueId, String customerNumber);
	public void joinHandle(String enterpriseId, String qno, String uniqueId, String customerNumber);
}
