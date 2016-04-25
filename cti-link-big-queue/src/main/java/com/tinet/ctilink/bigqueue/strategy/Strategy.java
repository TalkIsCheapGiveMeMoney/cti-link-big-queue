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
	public void calcMetric(String enterpriseId, String qno, List<CallMember> list);
	public void memberSelected(String enterpriseId, String qno, String cno);
}
