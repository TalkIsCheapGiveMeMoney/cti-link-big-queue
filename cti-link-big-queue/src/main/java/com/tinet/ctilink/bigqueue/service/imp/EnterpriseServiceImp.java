package com.tinet.ctilink.bigqueue.service.imp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinet.ctilink.bigqueue.entity.Enterprise;
import com.tinet.ctilink.cache.CacheKey;
import com.tinet.ctilink.cache.RedisService;
import com.tinet.ctilink.inc.Const;

@Service
public class EnterpriseServiceImp {
	@Autowired
	RedisService redisService;

	public List<Enterprise> getAllActive() {
		String jsonStr = redisService.get(Const.REDIS_DB_CONF_INDEX, CacheKey.ENTITY_ENTERPRISE_ACTIVE);
		List<Enterprise> enterpriseList = null;
		if (jsonStr != null) {
			ObjectMapper mapper = new ObjectMapper();

			JavaType valueType = mapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class,
					Enterprise.class);
			try {
				enterpriseList = mapper.readValue(jsonStr, valueType);
			} catch (JsonParseException e) {
				e.printStackTrace();
			} catch (JsonMappingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		return enterpriseList;
	}
}
