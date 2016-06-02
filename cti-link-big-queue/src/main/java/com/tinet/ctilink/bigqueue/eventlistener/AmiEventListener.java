package com.tinet.ctilink.bigqueue.eventlistener;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.tinet.ctilink.ami.inc.AmiEventTypeConst;
import com.tinet.ctilink.bigqueue.ami.event.EventHandler;
import com.tinet.ctilink.bigqueue.ami.event.EventHandlerFactory;
import com.tinet.ctilink.bigqueue.inc.BigQueueCacheKey;
import com.tinet.ctilink.cache.RedisService;
import com.tinet.ctilink.inc.Const;
import com.tinet.ctilink.json.JSONObject;

/**
 * @author fengwei //
 * @date 16/4/23 15:24
 */
public class AmiEventListener extends Thread{
	private final Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private RedisService redisService;
    private Integer executorThreadCount = 10;
    private ExecutorService executorService = Executors.newFixedThreadPool(executorThreadCount);
    
    private long AMI_EVENT_TIMEOUT = 50;
    private boolean terminate = false;
    
    public void run(){
    	while(!terminate){
    		String popRes = redisService.brpop(Const.REDIS_DB_CTI_INDEX, AmiEventTypeConst.AMI_EVENT_LIST, AMI_EVENT_TIMEOUT, TimeUnit.MILLISECONDS);
    		if(StringUtils.isNotEmpty(popRes)){
    			JSONObject jsonObject = JSONObject.fromObject(popRes);
    			if(jsonObject != null){
    				String event = jsonObject.get("event").toString();
    				logger.info("[AmiEventListenr] " + jsonObject.toString());
    				EventHandler handler = EventHandlerFactory.getInstance(event);
    				if(handler != null){
    					executorService.execute(new Runnable() {
							@Override
							public void run() {
								boolean res = handler.handle(jsonObject);
								if(!res){
									logger.error(String.format("AmiEventListener event handler fail! event=%s", jsonObject.toString()));
								}
							}
    					});
    					
    				}else{
    					logger.error(String.format("AmiEventListener event has no handler! event=%s", jsonObject.toString()));
    				}
    			}
    		}
    	}
    }
    
    public void terminate(){
    	this.terminate = true;
    }
}
