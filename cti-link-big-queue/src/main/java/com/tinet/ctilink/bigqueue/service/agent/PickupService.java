package com.tinet.ctilink.bigqueue.service.agent;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tinet.ctilink.ami.action.AmiActionResponse;
import com.tinet.ctilink.ami.inc.AmiChanVarNameConst;
import com.tinet.ctilink.ami.inc.AmiParamConst;
import com.tinet.ctilink.bigqueue.ami.action.GetVarActionService;
import com.tinet.ctilink.bigqueue.ami.action.OriginateActionService;
import com.tinet.ctilink.bigqueue.entity.ActionResponse;
import com.tinet.ctilink.bigqueue.entity.CallAgent;
import com.tinet.ctilink.bigqueue.inc.BigQueueConst;
import com.tinet.ctilink.bigqueue.service.imp.AgentServiceImp;
import com.tinet.ctilink.bigqueue.service.imp.ChannelServiceImp;
import com.tinet.ctilink.bigqueue.service.imp.MemberServiceImp;
import com.tinet.ctilink.bigqueue.service.imp.QueueEventServiceImp;
import com.tinet.ctilink.bigqueue.service.imp.QueueServiceImp;
import com.tinet.ctilink.cache.RedisService;
import com.tinet.ctilink.conf.entity.Caller;
import com.tinet.ctilink.conf.model.Gateway;
import com.tinet.ctilink.conf.util.AreaCodeUtil;
import com.tinet.ctilink.conf.util.ClidUtil;
import com.tinet.ctilink.conf.util.RouterUtil;
import com.tinet.ctilink.inc.Const;
import com.tinet.ctilink.json.JSONObject;
import com.tinet.ctilink.scheduler.RedisTaskScheduler;
import com.tinet.ctilink.util.AgentUtil;
import com.tinet.ctilink.util.RedisLock;

@Component
public class PickupService {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired
	AgentServiceImp agentService;
	
	@Autowired
	RedisService redisService;
	@Autowired
	QueueServiceImp queueService;
	@Autowired
	MemberServiceImp memberService;
	@Autowired
	QueueEventServiceImp queueEventService;
	@Autowired
	private ChannelServiceImp channelService;
	@Autowired
	private RedisTaskScheduler redisTaskScheduler;
	
	@Autowired
	GetVarActionService getVarActionService;
	@Autowired
	OriginateActionService originateActionService;
	public ActionResponse pickup(Map<String,Object> params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		String pickupCno = params.get("pickupCno").toString();
		//先获取lock memberService.lockMember(enterpriseId, cno);
		RedisLock pickupMemberLock = memberService.lockMember(enterpriseId, pickupCno);
		if(pickupMemberLock != null){
			try{
				CallAgent pickupCallAgent = agentService.getCallAgent(enterpriseId, pickupCno);
				if(pickupCallAgent != null){
					Integer sipId = pickupCallAgent.getCurrentSipId();
					String channel = pickupCallAgent.getCurrentChannel();
					String bridgeChannel = pickupCallAgent.getBargeChannel();
					Integer callType = pickupCallAgent.getCurrentCallType();
					String customerNumber = pickupCallAgent.getCurrentCustomerNumber();
					Integer customerNumberType = pickupCallAgent.getCurrentCustomerNumberType();
					String customerAreaCode = pickupCallAgent.getCurrentCustomerNumberAreaCode();
					String curQno = pickupCallAgent.getCurrentQno();
					String numberTrunk = pickupCallAgent.getCurrentNumberTrunk();
					if(StringUtils.isEmpty(channel)){
						response = ActionResponse.createFailResponse(-1, "no channel");
						return response;
					}
					Integer pickupDeviceStatus = memberService.getDeviceStatus(enterpriseId, pickupCno);
					if(!pickupDeviceStatus.equals(BigQueueConst.MEMBER_DEVICE_STATUS_RINGING)){
						response = ActionResponse.createFailResponse(-1, "not in ring");
						return response;
					}
					int routerClidCallType = 0;
			        if (callType == Const.CDR_CALL_TYPE_IB || callType ==Const.CDR_CALL_TYPE_OB_WEBCALL ){//呼入
			        	routerClidCallType = Const.ROUTER_CLID_CALL_TYPE_IB_RIGHT;
			        }else if(callType == Const.CDR_CALL_TYPE_OB_PREDICTIVE){//预测外呼
			        	routerClidCallType = Const.ROUTER_CLID_CALL_TYPE_PREDICTIVE_OB_RIGHT;
			        }else{
			        	response = ActionResponse.createFailResponse(-1, "callType not allowed");
						return response;
			        }
			      //获取外显号码
			        String clid = ClidUtil.getClid(Integer.parseInt(enterpriseId), routerClidCallType, customerNumber, numberTrunk);
			        
			        String destInterface = "";
			        String gwIp = "";

	
    				CallAgent callAgent = agentService.getCallAgent(enterpriseId, cno);
    				if(callAgent != null){
    					destInterface = callAgent.getInterface();
    	                gwIp = AgentUtil.getGwIp(destInterface);
    				}else{
    					response = ActionResponse.createFailResponse(-1, "no agent");
						return response;
    				}
   
			        if (destInterface.isEmpty()) {
				        response = ActionResponse.createFailResponse(-1, "bad param");
						return response;
			        }                         
			        Map<String, String> varMap = new HashMap<String, String>();
			        varMap.put("__" + AmiChanVarNameConst.MAIN_CHANNEL, bridgeChannel);
			        varMap.put(AmiChanVarNameConst.PICKUP_CHAN, channel); 
			        varMap.put("__" + AmiChanVarNameConst.CDR_CUSTOMER_NUMBER, customerNumber); //客户号码
			        varMap.put("__" + AmiChanVarNameConst.CDR_CUSTOMER_NUMBER_TYPE, String.valueOf(customerNumberType)); //电话类型
			        varMap.put("__" + AmiChanVarNameConst.CDR_CUSTOMER_AREA_CODE, customerAreaCode); //区号
			        varMap.put("__" + AmiChanVarNameConst.CUR_QNO, curQno); //
		        	varMap.put("__" + AmiChanVarNameConst.PICKUPER_CNO, cno);
		        	varMap.put(AmiChanVarNameConst.CDR_CNO, cno);
		        	varMap.put(AmiChanVarNameConst.PRE_DIAL_RUN, "\"" + Const.DIALPLAN_CONTEXT_PREVIEW_OUTCALL_PREDIAL + "," + enterpriseId +",1\"");
		        	varMap.put(AmiChanVarNameConst.PICKUPER_INTERFACE, destInterface);
			       
			        
			        varMap.put(AmiChanVarNameConst.CDR_DETAIL_GW_IP, gwIp);
			        if(routerClidCallType == Const.ROUTER_CLID_CALL_TYPE_IB_RIGHT){
			        	varMap.put(AmiChanVarNameConst.CDR_DETAIL_CALL_TYPE, String.valueOf(Const.CDR_CALL_TYPE_IB_BARGE));
			        }else{
			        	varMap.put(AmiChanVarNameConst.CDR_DETAIL_CALL_TYPE, String.valueOf(Const.CDR_CALL_TYPE_OB_BARGE));
			        }
			        varMap.put(AmiChanVarNameConst.CDR_NUMBER_TRUNK, clid);
			        varMap.put(AmiChanVarNameConst.CDR_STATUS, String.valueOf(Const.CDR_STATUS_DETAIL_CALL_FAIL));
			        
			        varMap.put(AmiChanVarNameConst.CDR_ENTERPRISE_ID, String.valueOf(enterpriseId));
			        varMap.put(AmiChanVarNameConst.CDR_START_TIME, String.valueOf(new Date().getTime() / 1000));
			        varMap.put(AmiChanVarNameConst.CDR_CNO, cno);
			        varMap.put(AmiChanVarNameConst.CDR_DETAIL_GW_IP, gwIp);
			        
			        String mainUniqueId = null;
			        Map<String, String> getVarMap = new HashMap<String, String>();
			    	getVarMap.put(AmiChanVarNameConst.CDR_MAIN_UNIQUE_ID, "0");
			    	Map<String, String> getVarResponse = getVarActionService.getVar(sipId, channel, getVarMap);
			    	if(getVarResponse != null){
			    		if(getVarResponse.get(AmiChanVarNameConst.CDR_MAIN_UNIQUE_ID) != null){
			    			mainUniqueId = getVarResponse.get(AmiChanVarNameConst.CDR_MAIN_UNIQUE_ID);
			    		}else{
			    			response = ActionResponse.createFailResponse(-1, "get var fail");
			    			return response;
			    		}
			    	}
			    	varMap.put(AmiChanVarNameConst.CDR_MAIN_UNIQUE_ID, mainUniqueId);
			    	
			        Map<String, Object> actionMap = new HashMap<String, Object>();
			        actionMap.put(AmiParamConst.DIALPLAN_CONTEXT, Const.DIALPLAN_CONTEXT_PICKUP);
			        actionMap.put(AmiParamConst.EXTENSION, enterpriseId + pickupCno);
			        actionMap.put(AmiParamConst.PRIORITY, 1);
			        actionMap.put(AmiParamConst.OTHER_CHANNEL_ID, mainUniqueId);
			        actionMap.put(AmiParamConst.CHANNEL, destInterface);
			        actionMap.put(AmiParamConst.ORIGINATE_TIMEOUT, 30);
			        actionMap.put(AmiParamConst.CLID, clid);     
			                       
			        JSONObject actionEvent = null;

		        	actionEvent = new JSONObject();
		        	actionEvent.put("event", "originateResponse");
		        	actionEvent.put("originateType", "pickup");
		        	actionEvent.put("enterpriseId", enterpriseId);
		        	actionEvent.put("cno", cno);
		        	actionEvent.put("pickupCno", pickupCno);
			        
			        try{
    	                if(memberService.isAvalibleLock(enterpriseId, cno) == false){
	        				response = ActionResponse.createFailResponse(-1, "pickup agent busy");
	        				return response;
	        			}
			        	AmiActionResponse amiResponse = originateActionService.originate(sipId, actionMap, actionEvent, varMap);
			        	if(amiResponse != null && (amiResponse.getCode() == 0)){
			            	response = ActionResponse.createSuccessResponse();
			            }else{
			            	response = ActionResponse.createFailResponse(-1, "originate fail");
			            	return response;
			            }
			        }catch(Exception e){
			        	e.printStackTrace();
			        	response = ActionResponse.createFailResponse(-1, "originate exception");
			        	return response;
			        }
				}else {
					response = ActionResponse.createFailResponse(-1, "no such agent");
				}
			}catch(Exception e){
				e.printStackTrace();
				response = ActionResponse.createFailResponse(-1, "exception");
				return response;
			}finally{
				memberService.unlockMember(pickupMemberLock);
			}
		}else{
			response = ActionResponse.createFailResponse(-1, "fail to get lock");
		}

		return response;
	}
}
