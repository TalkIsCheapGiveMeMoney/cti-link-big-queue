package com.tinet.ctilink.bigqueue.service.imp;

import java.util.Date;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tinet.ctilink.bigqueue.inc.BigQueueCacheKey;
import com.tinet.ctilink.cache.RedisService;
import com.tinet.ctilink.json.JSONObject;

@Service
public class QueueEventServiceImp {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	@Autowired
	RedisService redisService;
	@Autowired
	QueueServiceImp queueService;
	@Autowired
	MemberServiceImp memberService;
	
	@Autowired
	private ChannelServiceImp channelService;
	
	public void publishEvent(JSONObject event){
		try{
			event.put("id", UUID.randomUUID().toString());
			event.put("eventTime", new Long(new Date().getTime()/1000).intValue());
			redisService.convertAndSend(BigQueueCacheKey.QUEUE_EVENT_TOPIC, event);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
