package com.tinet.ctilink.bigqueue.service.imp;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tinet.ctilink.bigqueue.inc.BigQueueCacheKey;
import com.tinet.ctilink.cache.RedisService;
import com.tinet.ctilink.inc.Const;

@Service
public class ChannelServiceImp {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	@Autowired
	RedisService redisService;
	
	public boolean isAlive(String uniqueId){
		String key = String.format(BigQueueCacheKey.CHANNEL_ALIVE_UNIQUEID, uniqueId);
		String res = redisService.get(Const.REDIS_DB_CTI_INDEX, key);
		if(StringUtils.isEmpty(res)){
			return false;
		}
		return true;
	}
   
}

