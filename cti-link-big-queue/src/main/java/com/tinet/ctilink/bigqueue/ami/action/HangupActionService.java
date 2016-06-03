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
public class HangupActionService {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	@Reference
	ControlActionService controlActionService;
	
	public AmiActionResponse hangup(Integer sipId, String channel, Integer cause){
		Map<String, Object> paramsMap = new HashMap<String, Object>();
		paramsMap.put(AmiParamConst.CHANNEL, channel);
		paramsMap.put(AmiParamConst.HANGUP_CAUSE, cause);
		paramsMap.put(ControlConst.PARAM_SIP_ID, sipId);
		
		logger.info("hangup paramsMap=" + paramsMap.toString());
		AmiActionResponse response = controlActionService.handleAction("hangup", paramsMap);
		if(response != null){
			logger.info("hangup response=" + response.toString());
		}else{
			logger.info("hangup response=null");
		}
		return response;
	}
}
