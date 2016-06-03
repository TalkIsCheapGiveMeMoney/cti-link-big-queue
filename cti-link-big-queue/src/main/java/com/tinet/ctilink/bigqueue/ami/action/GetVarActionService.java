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
public class GetVarActionService {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	@Reference
	ControlActionService controlActionService;
	
	public Map<String, String> getVar(Integer sipId, String channel, Map<String, String> varMap){
		Map<String, Object> paramsMap = new HashMap<String, Object>();
		paramsMap.put(AmiParamConst.CHANNEL, channel);
		paramsMap.put(AmiParamConst.VAR_MAP, varMap);
		paramsMap.put(ControlConst.PARAM_SIP_ID, sipId);
		
		logger.info("getVar paramsMap=" + paramsMap.toString());
	    AmiActionResponse response = controlActionService.handleAction("getVar", paramsMap);
		if(response != null){
			logger.info("getVar response=" + response.toString());
			return response.getValues();
		}else{
			logger.info("getVar response=null");
		}
		return null;
	}
}
