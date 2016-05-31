package com.tinet.ctilink.bigqueue.ami.action;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.tinet.ctilink.ami.action.AmiActionResponse;
import com.tinet.ctilink.ami.inc.AmiParamConst;
import com.tinet.ctilink.control.inc.ControlConst;
import com.tinet.ctilink.control.service.v1.ControlActionService;
import com.tinet.ctilink.json.JSONObject;

@Service
public class OriginateActionService {
	
	@Reference
	ControlActionService controlActionService;
	
	public AmiActionResponse originate(Integer sipId, Map<String, Object> actionMap, JSONObject actionEvent, Map<String, String> varMap){
		Map<String, Object> paramsMap = new HashMap<String, Object>();
		paramsMap.put(AmiParamConst.ACTION_MAP, actionMap);
		paramsMap.put(AmiParamConst.VAR_MAP, varMap);
		paramsMap.put(AmiParamConst.ACTION_EVENT, actionEvent);
		if(sipId != null){
			paramsMap.put(ControlConst.PARAM_SIP_ID, sipId);
		}
	    return controlActionService.handleAction("originate", paramsMap);
	}
}
