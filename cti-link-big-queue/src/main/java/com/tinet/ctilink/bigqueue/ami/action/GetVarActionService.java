package com.tinet.ctilink.bigqueue.ami.action;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.tinet.ctilink.ami.action.AmiActionResponse;
import com.tinet.ctilink.control.service.v1.ControlActionService;

@Service
public class GetVarActionService {
	
	@Reference
	ControlActionService controlActionService;
	
	public Map getVar(String channel, Map varMap){
		Map getVarParamsMap = new HashMap();
		getVarParamsMap.put("channel", channel);
		getVarParamsMap.put("varMap", varMap);
		
	    AmiActionResponse response =controlActionService.handleAction("getVar", getVarParamsMap);
		if(response != null){
			return response.getValues();
		}
		return null;
	}
}
