package com.tinet.ctilink.bigqueue.ami.action;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.tinet.ctilink.ami.action.AmiActionResponse;
import com.tinet.ctilink.control.service.v1.ControlActionService;
import com.tinet.ctilink.json.JSONObject;

@Service
public class OriginateActionService {
	
	@Reference
	ControlActionService controlActionService;
	
	public AmiActionResponse originate(Integer sipId, Map actionMap, JSONObject actionEvent, Map varMap){
		Map paramsMap = new HashMap();
		paramsMap.put("actionMap", actionMap);
		paramsMap.put("varMap", varMap);
		paramsMap.put("actionEvent", actionEvent);
		if(sipId != null){
			paramsMap.put("sipId", sipId);
		}
	    return controlActionService.handleAction("originate", paramsMap);
	}
}
