package com.tinet.ctilink.bigqueue.ami.action;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.tinet.ctilink.ami.action.AmiActionResponse;
import com.tinet.ctilink.control.service.v1.ControlActionService;

@Service
public class MuteActionService {
	
	@Reference
	ControlActionService controlActionService;
	
	public AmiActionResponse mute(Integer sipId, String channel, String direction, String state){
		Map<String, Object> paramsMap = new HashMap<String, Object>();
		paramsMap.put("channel", channel);
		paramsMap.put("direction", direction);
		paramsMap.put("state", state);
		paramsMap.put("sipId", sipId);
		
	    return controlActionService.handleAction("mute", paramsMap);
	}
}
