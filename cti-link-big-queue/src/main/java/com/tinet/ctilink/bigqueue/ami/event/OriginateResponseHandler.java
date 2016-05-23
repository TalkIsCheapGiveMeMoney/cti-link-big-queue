package com.tinet.ctilink.bigqueue.ami.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.tinet.ctilink.ami.inc.AmiEventTypeConst;
import com.tinet.ctilink.ami.inc.AmiParamConst;
import com.tinet.ctilink.bigqueue.inc.BigQueueCacheKey;
import com.tinet.ctilink.cache.RedisService;
import com.tinet.ctilink.json.JSONObject;

public class OriginateResponseHandler implements EventHandler, InitializingBean{
	private final Logger logger = LoggerFactory.getLogger(getClass());
	@Autowired
    private RedisService redisService;
	
	@Override
	public void afterPropertiesSet() throws Exception{
		EventHandlerFactory.register(AmiEventTypeConst.ORIGINATE_RESPONSE, this);
	}
	
	public boolean handle(JSONObject event){
		String originateType = event.getString("originateType").toString();
		event.remove("originateType");
		
		EventHandler handler = OriginateResponseHandlerFactory.getInstance(originateType);
		if(handler != null){
			event.put("event", originateType);
			return handler.handle(event);
		}
		return false;
	}
}
