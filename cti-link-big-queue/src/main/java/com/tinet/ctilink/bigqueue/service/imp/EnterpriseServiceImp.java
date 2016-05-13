package com.tinet.ctilink.bigqueue.service.imp;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tinet.ctilink.cache.CacheKey;
import com.tinet.ctilink.cache.RedisService;
import com.tinet.ctilink.conf.model.Entity;
import com.tinet.ctilink.inc.Const;

@Service
public class EnterpriseServiceImp {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	@Autowired
	RedisService redisService;

	public List<Entity> getAllActive() {
		List<Entity> enterpriseList = redisService.getList(Const.REDIS_DB_CONF_INDEX, CacheKey.ENTITY_ENTERPRISE_ACTIVE, Entity.class);
		
		return enterpriseList;
	}
}
