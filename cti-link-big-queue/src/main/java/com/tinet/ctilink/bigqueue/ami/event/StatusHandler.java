package com.tinet.ctilink.bigqueue.ami.event;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.tinet.ctilink.ami.inc.AmiChannelStatusConst;
import com.tinet.ctilink.ami.inc.AmiEventTypeConst;
import com.tinet.ctilink.bigqueue.entity.CallAgent;
import com.tinet.ctilink.bigqueue.inc.BigQueueCacheKey;
import com.tinet.ctilink.bigqueue.inc.BigQueueConst;
import com.tinet.ctilink.bigqueue.service.imp.AgentServiceImp;
import com.tinet.ctilink.bigqueue.service.imp.MemberServiceImp;
import com.tinet.ctilink.bigqueue.service.imp.QueueEventServiceImp;
import com.tinet.ctilink.bigqueue.service.imp.QueueServiceImp;
import com.tinet.ctilink.cache.RedisService;
import com.tinet.ctilink.conf.model.Agent;
import com.tinet.ctilink.conf.model.Queue;
import com.tinet.ctilink.inc.Const;
import com.tinet.ctilink.json.JSONObject;
import com.tinet.ctilink.scheduler.RedisTaskScheduler;
import com.tinet.ctilink.util.DateUtil;
import com.tinet.ctilink.util.RedisLock;

public class StatusHandler implements EventHandler, InitializingBean{
	private static final Logger logger = LoggerFactory.getLogger(StatusHandler.class);
	
	@Autowired
	private MemberServiceImp memberService;
	
	@Autowired
	private AgentServiceImp agentService;
	@Autowired
	private QueueServiceImp queueService;
    @Autowired
    private RedisService redisService;
	@Autowired
	private RedisTaskScheduler redisTaskScheduler;
	@Autowired
	QueueEventServiceImp queueEventService;
	
	@Override
	public void afterPropertiesSet() throws Exception{
		EventHandlerFactory.register(AmiEventTypeConst.STATUS, this);
	}
	
	public boolean handle(JSONObject event){
		try{
			String enterpriseId = event.getString("enterpriseId");
			String cno = event.getString("cno");
			String status = event.getString("status");
			
			
			//先获取lock memberService.lockMember(enterpriseId, cno);
			RedisLock memberLock = memberService.lockMember(enterpriseId, cno);
			if(memberLock != null){
				try{
					CallAgent callAgent = agentService.getCallAgent(enterpriseId, cno);
					if(callAgent != null){
						JSONObject ringingEvent = null;
						Integer oldDeviceStatus = memberService.getDeviceStatus(enterpriseId, cno);
						Integer statusInt = Integer.parseInt(status);
						Integer deviceStatus;
						switch(statusInt){
							case AmiChannelStatusConst.TRYING:
								if(oldDeviceStatus != BigQueueConst.MEMBER_DEVICE_STATUS_IDLE){
									logger.error(String.format("bad status received, new=%d old=%d", statusInt, oldDeviceStatus));
									return false;
								}
								deviceStatus = BigQueueConst.MEMBER_DEVICE_STATUS_INVITE;
								break;
							case AmiChannelStatusConst.RING:
								if(oldDeviceStatus != BigQueueConst.MEMBER_DEVICE_STATUS_INVITE){
									logger.error(String.format("bad status received, new=%d old=%d", statusInt, oldDeviceStatus));
									return false;
								}
								deviceStatus = BigQueueConst.MEMBER_DEVICE_STATUS_RINGING;
								
								String channel = event.getString("channel");
								String uniqueId = event.getString("uniqueId");
								Integer callType = event.getInt("callType");
								String customerNumber = event.getString("customerNumber");
								String customerNumberAreaCode = event.getString("customerNumberAreaCode");
								Integer customerNumberType = event.getInt("customerNumberType");
								Integer detailCallType = event.getInt("detailCallType");
								String hotline = event.getString("hotline");
								String numberTrunk = event.getString("numberTrunk");
								String queue = event.getString("queue");
								String bridgedChannel = event.getString("bridgedChannel");
								String bridgedChannelUniqueId = event.getString("bridgedUniqueId");
								
								callAgent.setCurrentCallType(callType);
								callAgent.setCurrentChannel(channel);
								callAgent.setCurrentChannelUniqueId(uniqueId);
								callAgent.setBridgedChannel(bridgedChannel);
								callAgent.setBridgedChannelUniqueId(bridgedChannelUniqueId);
								callAgent.setCurrentCustomerNumber(customerNumber);
								callAgent.setCurrentCustomerNumberAreaCode(customerNumberAreaCode);
								callAgent.setCurrentCustomerNumberType(customerNumberType);
								callAgent.setCurrentDetailCallType(detailCallType);
								callAgent.setCurrentHotline(hotline);
								callAgent.setCurrentNumberTrunk(numberTrunk);
								callAgent.setCurrentQueue(queue);
								
								ringingEvent = new JSONObject();
								ringingEvent.put("event", "ringing");
								ringingEvent.put("enterpriseId", enterpriseId);
								ringingEvent.put("cno", cno);
								
								JSONObject variables = event.getJSONObject("variables");
								ringingEvent.put("variables", variables);
								break;
							case AmiChannelStatusConst.IDLE:
								if(oldDeviceStatus == BigQueueConst.MEMBER_DEVICE_STATUS_INUSE){//准备整理
									Integer wrapupTime = -1;
									switch(callAgent.getCurrentCallType()){
										case Const.CDR_CALL_TYPE_IB:
											if(StringUtils.isNotEmpty(callAgent.getCurrentQueue())){
												 Queue queueConf = queueService.getFromConfCache(enterpriseId, callAgent.getCurrentQueue());
											     if(queueConf != null){
											    	 wrapupTime = queueConf.getWrapupTime();
											     }
											}
											break;
										case Const.CDR_CALL_TYPE_OB_WEBCALL:
											if(StringUtils.isNotEmpty(callAgent.getCurrentQueue())){
												 Queue queueConf = queueService.getFromConfCache(enterpriseId, callAgent.getCurrentQueue());
											     if(queueConf != null){
											    	 wrapupTime = queueConf.getWrapupTime();
											     }
											}
											break;
										case Const.CDR_CALL_TYPE_DIRECT_OB:
										case Const.CDR_CALL_TYPE_OB:
										case Const.CDR_CALL_TYPE_PREVIEW_OB:
											break;
										case Const.CDR_CALL_TYPE_PREDICTIVE_OB:
											if(StringUtils.isNotEmpty(callAgent.getCurrentQueue())){
												 Queue queueConf = queueService.getFromConfCache(enterpriseId, callAgent.getCurrentQueue());
											     if(queueConf != null){
											    	 wrapupTime = queueConf.getWrapupTime();
											     }
											}
											break;
									}
									if(wrapupTime == -1){
										Agent agent = agentService.getAgent(enterpriseId, cno);
										if(agent != null){
											wrapupTime = agent.getWrapup();
										}
									}
									if(wrapupTime > 0){
										Map<String, Object> wrapupParams = new HashMap<String, Object>();
										wrapupParams.put("enterpriseId", enterpriseId);
										wrapupParams.put("cno", cno);
										
										Date triggerTime = DateUtil.addSecond(new Date(), wrapupTime);
										redisTaskScheduler.scheduleTimed("warpupTaskSchedulerGroup",
												String.format(BigQueueConst.WRAPUP_END_TASK_ID, enterpriseId, cno), 
												"WrapupEndTaskTrigger", 
												wrapupParams,
												triggerTime.getTime()/1000);
										
										memberService.setLoginStatus(enterpriseId, cno, BigQueueConst.MEMBER_LOGIN_STATUS_WRAPUP);
										
										JSONObject queueEvent = new JSONObject();
										queueEvent.put("event", "wrapupStart");
										queueEvent.put("enterpriseId", enterpriseId);
										queueEvent.put("qno", callAgent.getCurrentQueue());
										queueEvent.put("wrapupTime", wrapupTime);
										queueEvent.put("cno", callAgent.getCno());
										queueEvent.put("uniqueId", callAgent.getCurrentChannelUniqueId());

										queueEventService.publishEvent(queueEvent);
									}
								}
								if(callAgent.getBusyDescription().equals("hold")){
									JSONObject queueEvent = new JSONObject();
									queueEvent.put("event", "unhold");
									queueEvent.put("enterpriseId", enterpriseId);
									queueEvent.put("qno", callAgent.getCurrentQueue());
									queueEvent.put("callType", callAgent.getCurrentCallType());
	
									queueEventService.publishEvent(queueEvent);
								}
								deviceStatus = BigQueueConst.MEMBER_DEVICE_STATUS_IDLE;
								callAgent.clearCall();
								break;
							case AmiChannelStatusConst.UP:
								if(oldDeviceStatus == BigQueueConst.MEMBER_DEVICE_STATUS_IDLE){
									logger.error(String.format("bad status received, new=%d old=%d", statusInt, oldDeviceStatus));
									return false;
								}
								deviceStatus = BigQueueConst.MEMBER_DEVICE_STATUS_INUSE;
								break;
							default:
								deviceStatus = BigQueueConst.MEMBER_DEVICE_STATUS_INVALID;
						}
		
						memberService.setDeviceStatus(enterpriseId, cno, deviceStatus);
						agentService.saveCallAgent(enterpriseId, cno, callAgent);

						Integer loginStatus = memberService.getLoginStatus(enterpriseId, cno);
						
						JSONObject statusEvent = new JSONObject();
						statusEvent.put("event", "status");
						statusEvent.put("enterpriseId", enterpriseId);
						statusEvent.put("cno", cno);
						statusEvent.put("loginStatus", loginStatus);
						statusEvent.put("deviceStatus", deviceStatus);
						if(loginStatus.equals(BigQueueConst.MEMBER_LOGIN_STATUS_PAUSE)){
							statusEvent.put("pauseDescription", callAgent.getPauseDescription());
							statusEvent.put("pauseType", callAgent.getPauseType());
						}
						if(deviceStatus.equals(BigQueueConst.MEMBER_DEVICE_STATUS_INUSE) 
								|| deviceStatus.equals(BigQueueConst.MEMBER_DEVICE_STATUS_RINGING) 
								|| deviceStatus.equals(BigQueueConst.MEMBER_DEVICE_STATUS_INVITE)){
							statusEvent.put("busyDescription", callAgent.getBusyDescription());
						}
						redisService.convertAndSend(BigQueueCacheKey.AGENT_GATEWAY_EVENT_TOPIC, statusEvent);
						if(ringingEvent != null){
							redisService.convertAndSend(BigQueueCacheKey.AGENT_GATEWAY_EVENT_TOPIC, ringingEvent);
						}
						
					}else{
						return false;
					}
				}catch(Exception e){
					e.printStackTrace();
				}finally{
					memberService.unlockMember(memberLock);
				}
			}
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
