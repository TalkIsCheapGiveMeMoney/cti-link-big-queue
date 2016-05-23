package com.tinet.ctilink.bigqueue.ami.action;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.tinet.ctilink.ami.action.AmiActionResponse;
import com.tinet.ctilink.control.service.v1.ControlActionService;

@Service
public class RedirectActionService {
	
	@Reference
	ControlActionService controlActionService;
	
	public AmiActionResponse redirect(Integer sipId, String channel, String context, String exten, Integer priority){
		Map<String, Object> paramsMap = new HashMap<String, Object>();
		paramsMap.put("channel", channel);
		paramsMap.put("context", context);
		paramsMap.put("exten", exten);
		paramsMap.put("priority", priority);
		paramsMap.put("sipId", sipId);
		
	    return controlActionService.handleAction("redirect", paramsMap);
	}
}
