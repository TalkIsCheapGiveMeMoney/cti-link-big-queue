package com.tinet.ctilink.bigqueue.ami.action;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.tinet.ctilink.ami.action.AmiActionResponse;
import com.tinet.ctilink.ami.inc.AmiParamConst;
import com.tinet.ctilink.control.inc.ControlConst;
import com.tinet.ctilink.control.service.v1.ControlActionService;

@Service
public class HangupActionService {
	
	@Reference
	ControlActionService controlActionService;
	
	public AmiActionResponse hangup(Integer sipId, String channel, Integer cause){
		Map<String, Object> paramsMap = new HashMap<String, Object>();
		paramsMap.put(AmiParamConst.CHANNEL, channel);
		paramsMap.put(AmiParamConst.HANGUP_CAUSE, cause);
		paramsMap.put(ControlConst.PARAM_SIP_ID, sipId);
		
		return controlActionService.handleAction("hangup", paramsMap);
	}
}
