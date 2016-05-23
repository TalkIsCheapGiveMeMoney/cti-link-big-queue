package com.tinet.ctilink.bigqueue.ami.action;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.tinet.ctilink.ami.action.AmiActionResponse;
import com.tinet.ctilink.control.service.v1.ControlActionService;

@Service
public class HangupActionService {
	
	@Reference
	ControlActionService controlActionService;
	
	public Map hangup(Integer sipId, String channel, Integer cause){
		Map paramsMap = new HashMap();
		paramsMap.put("channel", channel);
		paramsMap.put("cause", cause);
		paramsMap.put("sipId", sipId);
		
	    AmiActionResponse response =controlActionService.handleAction("hangup", paramsMap);
		if(response != null){
			return response.getValues();
		}
		return null;
	}
}
