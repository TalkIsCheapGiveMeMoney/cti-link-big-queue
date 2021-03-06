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
public class MuteActionService {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	@Reference
	ControlActionService controlActionService;
	
	public AmiActionResponse mute(Integer sipId, String channel, String direction, String state){
		Map<String, Object> paramsMap = new HashMap<String, Object>();
		paramsMap.put(AmiParamConst.CHANNEL, channel);
		paramsMap.put(AmiParamConst.MUTE_DIRECTION, direction);
		paramsMap.put(AmiParamConst.MUTE_STATE, state);
		paramsMap.put(ControlConst.PARAM_SIP_ID, sipId);

		logger.info("mute paramsMap=" + paramsMap.toString());
		AmiActionResponse response = controlActionService.handleAction("mute", paramsMap);
		if(response != null){
			logger.info("mute response=" + response.toString());
		}else{
			logger.info("mute response=null");
		}
		return response;
	}
}
