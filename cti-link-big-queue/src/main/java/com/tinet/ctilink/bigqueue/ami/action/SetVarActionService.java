package com.tinet.ctilink.bigqueue.ami.action;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.tinet.ctilink.ami.action.AmiActionResponse;
import com.tinet.ctilink.control.service.v1.ControlActionService;

@Service
public class SetVarActionService {
	
	@Reference
	ControlActionService controlActionService;
	
	public AmiActionResponse setVar(Integer sipId, String channel, Map<String, Object> varMap){
		Map<String, Object> paramsMap = new HashMap<String, Object>();
		paramsMap.put("channel", channel);
		paramsMap.put("varMap", varMap);
		paramsMap.put("sipId", sipId);
		
	    return controlActionService.handleAction("setVar", paramsMap);
	}
}
