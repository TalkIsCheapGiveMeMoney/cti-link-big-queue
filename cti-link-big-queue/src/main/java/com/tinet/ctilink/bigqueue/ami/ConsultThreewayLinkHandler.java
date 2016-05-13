package com.tinet.ctilink.bigqueue.ami;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.tinet.ctilink.ami.inc.AmiEventConst;
import com.tinet.ctilink.bigqueue.inc.BigQueueCacheKey;
import com.tinet.ctilink.bigqueue.service.imp.AgentServiceImp;
import com.tinet.ctilink.bigqueue.service.imp.MemberServiceImp;
import com.tinet.ctilink.cache.RedisService;
import com.tinet.ctilink.json.JSONObject;

public class ConsultThreewayLinkHandler implements EventHandler, InitializingBean{
	private final Logger logger = LoggerFactory.getLogger(getClass());
	@Autowired
    private RedisService redisService;
    private AgentServiceImp agentService;
	@Autowired
    private MemberServiceImp memberService;
    @Override
	public void afterPropertiesSet() throws Exception{
		EventHandlerFactory.register(AmiEventConst.CONSULT_THREEWAY, this);
	}
	
	public boolean handle(JSONObject event){
		try{
			String enterpriseId = event.getString("enterpriseId");
			String cno = event.getString("cno");
			String consulterCno = event.getString("consulterCno");
			
			// 发送事件到被咨询的那个人，通知被咨询那个人是谁咨询的他，所以需要发送给前台，前台需要显示谁咨询的。
			if (StringUtils.isNotEmpty(cno)) {
				JSONObject consulteeEvent = new JSONObject();
				consulteeEvent.put("event", event.getString("event"));
				consulteeEvent.put("enterpriseId", event.getString("enterpriseId"));
				consulteeEvent.put("cno", event.getString("cno"));
				consulteeEvent.put("consulterCno", event.getString("consulterCno"));
				redisService.convertAndSend(BigQueueCacheKey.AGENT_GATEWAY_EVENT_TOPIC, event);
			}

			//发送事件到咨询发起者的那个人
			JSONObject consulteeEvent = new JSONObject();
			consulteeEvent.put("event", event.getString("event"));
			consulteeEvent.put("enterpriseId", event.getString("enterpriseId"));
			consulteeEvent.put("cno", event.getString("consulterCno"));
			redisService.convertAndSend(BigQueueCacheKey.AGENT_GATEWAY_EVENT_TOPIC, event);
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
