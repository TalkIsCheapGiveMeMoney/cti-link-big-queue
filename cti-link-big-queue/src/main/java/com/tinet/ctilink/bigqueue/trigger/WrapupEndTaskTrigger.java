package com.tinet.ctilink.bigqueue.trigger;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tinet.ctilink.bigqueue.entity.CallAgent;
import com.tinet.ctilink.bigqueue.inc.BigQueueConst;
import com.tinet.ctilink.bigqueue.service.imp.AgentServiceImp;
import com.tinet.ctilink.bigqueue.service.imp.ChannelServiceImp;
import com.tinet.ctilink.bigqueue.service.imp.MemberServiceImp;
import com.tinet.ctilink.bigqueue.service.imp.QueueEventServiceImp;
import com.tinet.ctilink.bigqueue.service.imp.QueueServiceImp;
import com.tinet.ctilink.json.JSONObject;
import com.tinet.ctilink.scheduler.TaskSchedulerTrigger;
import com.tinet.ctilink.util.RedisLock;

/**
 * @author fengwei //
 * @date 16/4/23 15:38
 */
@Component
public class WrapupEndTaskTrigger implements TaskSchedulerTrigger {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	@Autowired
	private QueueServiceImp queueService;
	@Autowired
	private MemberServiceImp memberService;
	@Autowired
	private ChannelServiceImp channelService;
	@Autowired
	private AgentServiceImp agentService;
	@Autowired
	QueueEventServiceImp queueEventService;
	
    public void taskTriggered(String taskId, Map param) {
    	String enterpriseId = param.get("enterpriseId").toString();
    	String cno = param.get("cno").toString();
    	
    	//先获取lock memberService.lockMember(enterpriseId, cno);
		RedisLock memberLock = memberService.lockMember(enterpriseId, cno);
		if(memberLock != null){
			try{
				CallAgent callAgent = agentService.getCallAgent(enterpriseId, cno);
				if(callAgent != null){
					Integer loginStatus = memberService.getLoginStatus(enterpriseId, cno);
					if(loginStatus.equals(BigQueueConst.MEMBER_LOGIN_STATUS_WRAPUP)){
						memberService.setLoginStatus(enterpriseId, cno, BigQueueConst.MEMBER_LOGIN_STATUS_WRAPUP);
						
						JSONObject queueEvent = new JSONObject();
						queueEvent.put("event", "wrapupEnd");
						queueEvent.put("enterpriseId", enterpriseId);
						queueEvent.put("cno", callAgent.getCno());
				
						queueEventService.publishEvent(queueEvent);
					}else{
						logger.warn(String.format("agent login status is not wrapup when WrapupEndTrigger run. enterpriseId=%s cno=%s loginStatus=%d",enterpriseId, cno, loginStatus));
					}
				}else{
					logger.warn(String.format("no CallAgent when WrapupEndTrigger run. enterpriseId=%s cno=%s", enterpriseId, cno));
				}
			}catch(Exception e){
				e.printStackTrace();
			}finally{
				memberService.unlockMember(memberLock);
			}
		}

    }
}
