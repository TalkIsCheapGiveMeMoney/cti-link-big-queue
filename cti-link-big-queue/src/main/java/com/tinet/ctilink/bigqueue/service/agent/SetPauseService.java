package com.tinet.ctilink.bigqueue.service.agent;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tinet.ctilink.bigqueue.ami.action.GetVarActionService;
import com.tinet.ctilink.bigqueue.ami.action.OriginateActionService;
import com.tinet.ctilink.bigqueue.entity.ActionResponse;
import com.tinet.ctilink.bigqueue.inc.BigQueueConst;
import com.tinet.ctilink.bigqueue.service.imp.AgentServiceImp;
import com.tinet.ctilink.bigqueue.service.imp.ChannelServiceImp;
import com.tinet.ctilink.bigqueue.service.imp.MemberServiceImp;
import com.tinet.ctilink.bigqueue.service.imp.QueueEventServiceImp;
import com.tinet.ctilink.bigqueue.service.imp.QueueServiceImp;
import com.tinet.ctilink.cache.RedisService;
import com.tinet.ctilink.scheduler.RedisTaskScheduler;

@Component
public class SetPauseService {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired
	AgentServiceImp agentService;
	
	@Autowired
	RedisService redisService;
	@Autowired
	QueueServiceImp queueService;
	@Autowired
	MemberServiceImp memberService;
	@Autowired
	QueueEventServiceImp queueEventService;
	@Autowired
	private ChannelServiceImp channelService;
	@Autowired
	private RedisTaskScheduler redisTaskScheduler;
	@Autowired
	PauseService pauseService;
	
	@Autowired
	GetVarActionService getVarActionService;
	@Autowired
	OriginateActionService originateActionService;
	public ActionResponse setPause(Map<String,Object> params){
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = (params.get("cno") == null)?"":params.get("cno").toString();
		String monitoredCno = params.get("monitoredCno").toString();
		String monitorCno = params.get("monitorCno").toString();
		
		Map<String,Object> newParams = new HashMap<String, Object>();
		newParams.put("enterpriseId", enterpriseId);
		newParams.put("cno", monitoredCno);
		newParams.put("type", String.valueOf(BigQueueConst.MEMBER_LOGIN_STATUS_PAUSE_TYPE_FORCE));
		newParams.put("description", "管理置忙");
		newParams.put("monitorCno", monitorCno);
		
		return pauseService.pause(newParams);

	}
}
