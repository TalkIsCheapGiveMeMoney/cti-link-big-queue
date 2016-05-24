package com.tinet.ctilink.bigqueue.ami.event;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tinet.ctilink.ami.inc.AmiEventTypeConst;
import com.tinet.ctilink.bigqueue.entity.CallAgent;
import com.tinet.ctilink.bigqueue.inc.BigQueueCacheKey;
import com.tinet.ctilink.bigqueue.service.imp.AgentServiceImp;
import com.tinet.ctilink.bigqueue.service.imp.MemberServiceImp;
import com.tinet.ctilink.cache.RedisService;
import com.tinet.ctilink.json.JSONObject;
import com.tinet.ctilink.util.RedisLock;
@Component
public class SpyLinkHandler implements EventHandler, InitializingBean{
	private final Logger logger = LoggerFactory.getLogger(getClass());
	@Autowired
    private RedisService redisService;
    private AgentServiceImp agentService;
	@Autowired
    private MemberServiceImp memberService;
    @Override
	public void afterPropertiesSet() throws Exception{
		EventHandlerFactory.register(AmiEventTypeConst.SPY_LINK, this);
	}
	
	public boolean handle(JSONObject event){
		try{
			String enterpriseId = event.getString("enterpriseId");
			String cno = event.getString("cno");
			String channel = event.getString("channel");
			String spyObject = event.getString("spyObject");
			String objectType = event.getString("objectType");
			String spiedCno = event.getString("spiedCno");
			
			//先获取lock memberService.lockMember(enterpriseId, cno);
			RedisLock memberLock = memberService.lockMember(enterpriseId, spiedCno);
			if(memberLock != null){
				try{
					CallAgent callAgent = agentService.getCallAgent(enterpriseId, spiedCno);
					if(callAgent != null){
						callAgent.setSpyChannel(channel);
						callAgent.setMonitoredObject(spyObject);
						callAgent.setMonitoredObjectType(Integer.parseInt(objectType));
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
			if(StringUtils.isNotEmpty(cno)){
				redisService.convertAndSend(BigQueueCacheKey.AGENT_GATEWAY_EVENT_TOPIC, event);
			}
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
