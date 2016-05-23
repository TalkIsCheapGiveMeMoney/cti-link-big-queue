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

public class PreviewOutcallResponseHandler implements EventHandler, InitializingBean{
	private final Logger logger = LoggerFactory.getLogger(getClass());
	@Autowired
    private RedisService redisService;
	
	@Override
	public void afterPropertiesSet() throws Exception{
		OriginateResponseHandlerFactory.register("previewOutcall", this);
	}
	
	public boolean handle(JSONObject event){
		String result = event.getString("result");
		//根据请求结果，设置坐席置忙置闲等操作
		if(result.equals(AmiParamConst.ORIGINATE_RESPONSE_RESULT_SUCCESS)){
			
		}else{
			
		}
		try{
			redisService.convertAndSend(BigQueueCacheKey.AGENT_GATEWAY_EVENT_TOPIC, event);
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
