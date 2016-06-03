package com.tinet.ctilink.bigqueue.ami.action;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.tinet.ctilink.ami.action.AmiActionResponse;
import com.tinet.ctilink.ami.inc.AmiParamConst;
import com.tinet.ctilink.control.inc.ControlConst;
import com.tinet.ctilink.control.service.v1.ControlActionService;

@Service
public class IndicateActionService {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	@Reference
	ControlActionService controlActionService;
	
	public AmiActionResponse indicate(Integer sipId, String channel, Integer code){
		Map<String, Object> paramsMap = new HashMap<String, Object>();
		paramsMap.put(AmiParamConst.CHANNEL, channel);
		paramsMap.put(AmiParamConst.INDICATE_CODE, code);
		paramsMap.put(ControlConst.PARAM_SIP_ID, sipId);
		
		logger.info("indicate paramsMap=" + paramsMap.toString());
		AmiActionResponse response = controlActionService.handleAction("indicate", paramsMap);
		if(response != null){
			logger.info("indicate response=" + response.toString());
		}else{
			logger.info("indicate response=null");
		}
		return response;
	}
}
