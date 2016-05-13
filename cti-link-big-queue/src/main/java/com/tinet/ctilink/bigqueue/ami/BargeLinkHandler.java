package com.tinet.ctilink.bigqueue.ami;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.tinet.ctilink.ami.inc.AmiEventConst;
import com.tinet.ctilink.bigqueue.entity.CallAgent;
import com.tinet.ctilink.bigqueue.inc.BigQueueConst;
import com.tinet.ctilink.bigqueue.service.imp.AgentServiceImp;
import com.tinet.ctilink.bigqueue.service.imp.MemberServiceImp;
import com.tinet.ctilink.cache.RedisService;
import com.tinet.ctilink.json.JSONObject;
import com.tinet.ctilink.util.RedisLock;

public class BargeLinkHandler implements EventHandler, InitializingBean{
	private final Logger logger = LoggerFactory.getLogger(getClass());
	@Autowired
    private RedisService redisService;
	@Autowired
    private AgentServiceImp agentService;
	@Autowired
    private MemberServiceImp memberService;
	
    @Override
	public void afterPropertiesSet() throws Exception{
		EventHandlerFactory.register(AmiEventConst.BARGE_LINK, this);
	}
	
	public boolean handle(JSONObject event){
		try{
			String enterpriseId = event.getString("enterpriseId");
			String cno = event.getString("cno");
			String channel = event.getString("channel");
			String bargedCno = event.getString("bargedCno");
			String bargeObject = event.getString("bargeObject");
			String objectType = event.getString("objectType");
			
			
			//先获取lock memberService.lockMember(enterpriseId, cno);
			RedisLock memberLock = memberService.lockMember(enterpriseId, cno);
			if(memberLock != null){
				try{
					CallAgent callAgent = agentService.getCallAgent(enterpriseId, cno);
					if(callAgent != null){
						callAgent.setBargeChannel(channel);
						agentService.saveCallAgent(enterpriseId, cno, callAgent);
					}else{
						logger.error("no such callAgent when dispatch BargeLinkEvent");
					}
				}catch(Exception e){
					e.printStackTrace();
				}finally{
					memberService.unlockMember(memberLock);
				}
			}else{
				logger.error("fail to get lock when dispatch BargeLinkEvent");
			}
			
			JSONObject bargeeEvent = new JSONObject();
			bargeeEvent.put("event", event.getString("event"));
			bargeeEvent.put("enterpriseId", event.getString("enterpriseId"));
			bargeeEvent.put("cno", event.getString("cno"));
			bargeeEvent.put("bargedCno", event.getString("bargedCno"));
			redisService.convertAndSend(BigQueueConst.AGENT_GATEWAY_EVENT_TOPIC, event);
			//发送事件给被强插者
			if(StringUtils.isNotEmpty(cno)){
				JSONObject bargerEvent = new JSONObject();
				bargeeEvent.put("event", event.getString("event"));
				bargerEvent.put("enterpriseId", event.getString("enterpriseId"));
				bargerEvent.put("cno", event.getString("cno"));
				bargerEvent.put("bargedCno", event.getString("bargedCno"));
				bargerEvent.put("bargeObject", event.getString("bargeObject"));
				bargerEvent.put("objectType", event.getString("objectType"));
				redisService.convertAndSend(BigQueueConst.AGENT_GATEWAY_EVENT_TOPIC, event);
				//发送事件给发起者
			}
			
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
