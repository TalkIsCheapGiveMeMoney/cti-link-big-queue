package com.tinet.ctilink.bigqueue.ami.event;

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
public class BargeUnlinkHandler implements EventHandler, InitializingBean{
	private final Logger logger = LoggerFactory.getLogger(getClass());
	@Autowired
    private RedisService redisService;
	@Autowired
    private AgentServiceImp agentService;
	@Autowired
    private MemberServiceImp memberService;
	
    @Override
	public void afterPropertiesSet() throws Exception{
		EventHandlerFactory.register(AmiEventTypeConst.BARGE_UNLINK, this);
	}
	
	public boolean handle(JSONObject event){
		try{
			String enterpriseId = event.getString("enterpriseId");
			String cno = event.getString("cno");
			
			//先获取lock memberService.lockMember(enterpriseId, cno);
			RedisLock memberLock = memberService.lockMember(enterpriseId, cno);
			if(memberLock != null){
				try{
					CallAgent callAgent = agentService.getCallAgent(enterpriseId, cno);
					if(callAgent != null){
						callAgent.setBargeChannel("");
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
			redisService.convertAndSend(BigQueueCacheKey.AGENT_GATEWAY_EVENT_TOPIC, event);
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
