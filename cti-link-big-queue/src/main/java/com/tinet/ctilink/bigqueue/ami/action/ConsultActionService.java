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
public class ConsultActionService {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	@Reference
	ControlActionService controlActionService;
	
	public AmiActionResponse consult(Integer sipId, String channel, String context, String exten){
		Map<String, Object> paramsMap = new HashMap<String, Object>();
		paramsMap.put(AmiParamConst.CHANNEL, channel);
		paramsMap.put(AmiParamConst.DIALPLAN_CONTEXT, context);
		paramsMap.put(AmiParamConst.EXTENSION, exten);
		paramsMap.put(ControlConst.PARAM_SIP_ID, sipId);
		
		logger.info("consult paramsMap=" + paramsMap.toString());
		AmiActionResponse response = controlActionService.handleAction("consult", paramsMap);
		if(response != null){
			logger.info("hangup response=" + response.toString());
		}else{
			logger.info("hangup response=null");
		}
		return response;
	}
}
