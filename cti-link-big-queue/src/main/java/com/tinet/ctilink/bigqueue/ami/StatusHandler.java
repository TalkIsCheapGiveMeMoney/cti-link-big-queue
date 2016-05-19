package com.tinet.ctilink.bigqueue.ami;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.tinet.ctilink.ami.inc.AmiEventTypeConst;
import com.tinet.ctilink.bigqueue.entity.CallAgent;
import com.tinet.ctilink.bigqueue.inc.BigQueueCacheKey;
import com.tinet.ctilink.bigqueue.inc.BigQueueConst;
import com.tinet.ctilink.bigqueue.service.imp.AgentServiceImp;
import com.tinet.ctilink.bigqueue.service.imp.MemberServiceImp;
import com.tinet.ctilink.cache.RedisService;
import com.tinet.ctilink.json.JSONObject;
import com.tinet.ctilink.util.RedisLock;

public class StatusHandler implements EventHandler, InitializingBean{
	private static final Logger logger = LoggerFactory.getLogger(StatusHandler.class);
	
	@Autowired
	private MemberServiceImp memberService;
	
	@Autowired
	private AgentServiceImp agentService;
	 
    @Autowired
    private RedisService redisService;
    
    
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
						Integer oldDeviceStatus = memberService.getDeviceStatus(enterpriseId, cno);
						Integer statusInt = Integer.parseInt(status);
						Integer deviceStatus;
						switch(statusInt){
							case 1:
								if(oldDeviceStatus != BigQueueConst.MEMBER_DEVICE_STATUS_IDLE){
									logger.error(String.format("bad status received, new=%d old=%d", statusInt, oldDeviceStatus));
									return false;
								}
								deviceStatus = BigQueueConst.MEMBER_DEVICE_STATUS_INVITE;
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
								
								break;
							case 2:
								if(oldDeviceStatus != BigQueueConst.MEMBER_DEVICE_STATUS_INVITE){
									logger.error(String.format("bad status received, new=%d old=%d", statusInt, oldDeviceStatus));
									return false;
								}
								deviceStatus = BigQueueConst.MEMBER_DEVICE_STATUS_RINGING;
								break;
							case 3:
								deviceStatus = BigQueueConst.MEMBER_DEVICE_STATUS_IDLE;
								callAgent.clearCall();
								break;
							case 4:
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
