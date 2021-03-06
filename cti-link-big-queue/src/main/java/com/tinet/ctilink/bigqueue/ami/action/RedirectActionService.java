package com.tinet.ctilink.bigqueue.ami.action;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.tinet.ctilink.ami.action.AmiActionResponse;
import com.tinet.ctilink.ami.inc.AmiParamConst;
import com.tinet.ctilink.control.inc.ControlConst;
import com.tinet.ctilink.control.service.v1.ControlActionService;

@Service
public class RedirectActionService {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	@Reference
	ControlActionService controlActionService;
	
	public AmiActionResponse redirect(Integer sipId, String channel, String context, String exten, Integer priority, 
			String extraChannel, String extraContext, String extraExten, Integer extraPriority){
		Map<String, Object> paramsMap = new HashMap<String, Object>();
		paramsMap.put(AmiParamConst.CHANNEL, channel);
		paramsMap.put(AmiParamConst.DIALPLAN_CONTEXT, context);
		paramsMap.put(AmiParamConst.EXTENSION, exten);
		paramsMap.put(AmiParamConst.PRIORITY, priority);
		paramsMap.put(ControlConst.PARAM_SIP_ID, sipId);
		if(StringUtils.isNotEmpty(extraChannel)){
			paramsMap.put(AmiParamConst.EXTRA_CHANNEL, channel);
			paramsMap.put(AmiParamConst.EXTRA_CONTEXT, extraContext);
			paramsMap.put(AmiParamConst.EXTRA_EXTEN, extraExten);
			paramsMap.put(AmiParamConst.EXTRA_PRIORITY, extraPriority);
		}
		
		logger.info("originate paramsMap=" + paramsMap.toString());
		AmiActionResponse response = controlActionService.handleAction("redirect", paramsMap);
		if(response != null){
			logger.info("redirect response=" + response.toString());
		}else{
			logger.info("redirect response=null");
		}
		return response;
	}
}
