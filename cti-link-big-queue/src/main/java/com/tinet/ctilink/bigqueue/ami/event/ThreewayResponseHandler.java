package com.tinet.ctilink.bigqueue.ami.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tinet.ctilink.ami.inc.AmiEventTypeConst;
import com.tinet.ctilink.bigqueue.inc.BigQueueCacheKey;
import com.tinet.ctilink.cache.RedisService;
import com.tinet.ctilink.json.JSONObject;
@Component
public class ThreewayResponseHandler implements EventHandler, InitializingBean{
	private final Logger logger = LoggerFactory.getLogger(getClass());
	@Autowired
    private RedisService redisService;
	
	@Override
	public void afterPropertiesSet() throws Exception{
		OriginateResponseHandlerFactory.register("threeway", this);
	}
	
	public boolean handle(JSONObject event){
		try{
			redisService.convertAndSend(BigQueueCacheKey.AGENT_GATEWAY_EVENT_TOPIC, event);
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
