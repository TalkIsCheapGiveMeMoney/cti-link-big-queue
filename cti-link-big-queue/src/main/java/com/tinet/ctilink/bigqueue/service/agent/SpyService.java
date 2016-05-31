package com.tinet.ctilink.bigqueue.service.agent;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.pagehelper.StringUtil;
import com.tinet.ctilink.ami.action.AmiActionResponse;
import com.tinet.ctilink.ami.inc.AmiChanVarNameConst;
import com.tinet.ctilink.bigqueue.ami.action.GetVarActionService;
import com.tinet.ctilink.bigqueue.ami.action.OriginateActionService;
import com.tinet.ctilink.bigqueue.entity.ActionResponse;
import com.tinet.ctilink.bigqueue.entity.CallAgent;
import com.tinet.ctilink.bigqueue.inc.BigQueueCacheKey;
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
public class SpyService {
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
	public ActionResponse spy(Map<String,Object> params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String spiedCno = params.get("spiedCno").toString();
		String spyObject = params.get("spyObject").toString();
		String objectType = params.get("objectType").toString();
		
		String spiedChannel = null;
        Integer spiedDeviceStatus = 0;
        String customerNumber = null;
        Integer customerNumberType = 0;
        String customerAreaCode = null;
        String numberTrunk = null;
        String curQueue = null;
        Integer callType = 0; 
        Integer sipId = null;
        
				
		//先获取lock memberService.lockMember(enterpriseId, cno);
		RedisLock spiedMemberLock = memberService.lockMember(enterpriseId, spiedCno);
		if(spiedMemberLock != null){
			try{
				CallAgent callAgent = agentService.getCallAgent(enterpriseId, spiedCno);
				if(callAgent != null){
					spiedChannel = callAgent.getCurrentChannel();
					spiedDeviceStatus = memberService.getDeviceStatus(enterpriseId, spiedCno);
			        customerNumber = callAgent.getCurrentCustomerNumber();
			        customerNumberType = callAgent.getCurrentCustomerNumberType();
			        customerAreaCode = callAgent.getCurrentCustomerNumberAreaCode();
			        numberTrunk = callAgent.getCurrentNumberTrunk();
			        curQueue = callAgent.getCurrentQueue();
			        callType = callAgent.getCurrentCallType(); 
			        sipId = callAgent.getCurrentSipId();
				}
			}catch(Exception e){
				e.printStackTrace();
				response = ActionResponse.createFailResponse(-1, "exception");
				return response;
			}finally{
				memberService.unlockMember(spiedMemberLock);
			}
		}else{
			response = ActionResponse.createFailResponse(-1, "fail to get lock");
			return response;
		}
					
	    if (StringUtil.isEmpty(spiedChannel) || !spiedDeviceStatus.equals(BigQueueConst.MEMBER_DEVICE_STATUS_INUSE)) {
	        response = ActionResponse.createFailResponse(-1, "no spied channel");
			return response;
	    }
	    int routerClidCallType = 0;
        if (callType == Const.CDR_CALL_TYPE_IB || callType ==Const.CDR_CALL_TYPE_OB_WEBCALL ){//呼入
        	routerClidCallType = Const.ROUTER_CLID_CALL_TYPE_IB_RIGHT;
        }else if(callType == Const.CDR_CALL_TYPE_OB || callType == Const.CDR_CALL_TYPE_DIRECT_OB || callType == Const.CDR_CALL_TYPE_PREVIEW_OB){//点击外呼
        	routerClidCallType = Const.ROUTER_CLID_CALL_TYPE_PREVIEW_OB_RIGHT;
        }else if(callType == Const.CDR_CALL_TYPE_PREDICTIVE_OB){//预测外呼
        	routerClidCallType = Const.ROUTER_CLID_CALL_TYPE_PREDICTIVE_OB_RIGHT;
        }
      //获取外显号码
        String clid = ClidUtil.getClid(Integer.parseInt(enterpriseId), routerClidCallType, customerNumber, numberTrunk);
        
        String destInterface = "";
        String gwIp = "";
        if (objectType.equals(Const.OBJECT_TYPE_TEL)) {
        	Caller caller = AreaCodeUtil.updateGetAreaCode(spyObject, "");
            
        	Gateway gateway = RouterUtil.getRouterGateway(Integer.parseInt(enterpriseId), routerClidCallType, caller);
            if (gateway != null) {
                destInterface = "PJSIP/" + gateway.getName()+"/sip:"+gateway.getPrefix() + caller.getCallerNumber() + "@"
                        + gateway.getIpAddr() + ":" + gateway.getPort();
                gwIp = gateway.getIpAddr();
            }
        } else if (objectType.equals(Const.OBJECT_TYPE_EXTEN)) {
        	Gateway gateway = RouterUtil.getRouterGatewayInternal(Integer.parseInt(enterpriseId), routerClidCallType, spyObject);
            if(gateway != null){
            	destInterface = "PJSIP/" + gateway.getName()+"/sip:" + enterpriseId + spyObject + "@"
                    + gateway.getIpAddr() + ":" + gateway.getPort();
            
                gwIp = gateway.getIpAddr();
            }
        } else if (objectType.equals(Const.OBJECT_TYPE_CNO)) {
    		//先获取lock memberService.lockMember(enterpriseId, cno);
    		RedisLock memberLock = memberService.lockMember(enterpriseId, spyObject);
    		if(memberLock != null){
    			try{
    				CallAgent callAgent = agentService.getCallAgent(enterpriseId, spyObject);
    				if(callAgent != null){
    					destInterface = callAgent.getInterface();
    	                gwIp = AgentUtil.getGwIp(destInterface);
    				}
    			}catch(Exception e){
    				e.printStackTrace();
    				response = ActionResponse.createFailResponse(-1, "exception");
    				return response;
    			}finally{
    				memberService.unlockMember(memberLock);
    			}
    		}else{
    			response = ActionResponse.createFailResponse(-1, "fail to get lock");
    			return response;
    		}
        }
        if (destInterface.isEmpty()) {
	        response = ActionResponse.createFailResponse(-1, "bad param");
			return response;
        }                         
        Map<String, Object> varMap = new HashMap<String, Object>();
        varMap.put(AmiChanVarNameConst.SPY_CHAN, spiedChannel); 
        varMap.put("__" + AmiChanVarNameConst.CDR_CUSTOMER_NUMBER, customerNumber); //客户号码
        varMap.put("__" + AmiChanVarNameConst.CDR_CUSTOMER_NUMBER_TYPE, String.valueOf(customerNumberType)); //电话类型
        varMap.put("__" + AmiChanVarNameConst.CDR_CUSTOMER_AREA_CODE, customerAreaCode); //区号
        varMap.put("__" + AmiChanVarNameConst.CUR_QUEUE, curQueue); //
        varMap.put("__" + AmiChanVarNameConst.ENTERPRISE_ID, String.valueOf(enterpriseId));
        if (objectType.equals(Const.OBJECT_TYPE_CNO)) {
        	varMap.put("__" + AmiChanVarNameConst.SPYER_CNO, spyObject);
        	varMap.put(AmiChanVarNameConst.CDR_DETAIL_CNO, spyObject);
        	varMap.put(AmiChanVarNameConst.CHANNEL_CNO, spyObject);
        	varMap.put(AmiChanVarNameConst.PRE_DIAL_RUN, Const.DIALPLAN_CONTEXT_PREVIEW_OUTCALL_PREDIAL);
        }
        varMap.put("__" + AmiChanVarNameConst.SPIED_CNO, spiedCno);
        varMap.put("__" + AmiChanVarNameConst.SPY_OBJECT, spyObject);
        varMap.put("__" + AmiChanVarNameConst.OBJECT_TYPE, objectType);
        
        varMap.put(AmiChanVarNameConst.CDR_DETAIL_GW_IP, gwIp);
        if(routerClidCallType == Const.ROUTER_CLID_CALL_TYPE_IB_RIGHT){
        	varMap.put(AmiChanVarNameConst.CDR_DETAIL_CALL_TYPE, String.valueOf(Const.CDR_CALL_TYPE_IB_SPY));
        }else{
        	varMap.put(AmiChanVarNameConst.CDR_DETAIL_CALL_TYPE, String.valueOf(Const.CDR_CALL_TYPE_OB_SPY));
        }
        varMap.put(AmiChanVarNameConst.CDR_NUMBER_TRUNK, clid);
        varMap.put(AmiChanVarNameConst.CDR_STATUS, String.valueOf(Const.CDR_STATUS_DETAIL_CALL_FAIL));
        
        varMap.put(AmiChanVarNameConst.CDR_ENTERPRISE_ID, String.valueOf(enterpriseId));
        varMap.put(AmiChanVarNameConst.CDR_START_TIME, String.valueOf(new Date().getTime() / 1000));
        

        String mainUniqueId = null;
        Map<String, Object> getVarMap = new HashMap<String, Object>();
    	getVarMap.put(AmiChanVarNameConst.CDR_MAIN_UNIQUE_ID, "0");
    	Map<String, Object> getVarResponse = getVarActionService.getVar(sipId, spiedChannel, getVarMap);
    	if(getVarResponse != null){
    		if(getVarResponse.get(AmiChanVarNameConst.CDR_MAIN_UNIQUE_ID) != null){
    			mainUniqueId = getVarResponse.get(AmiChanVarNameConst.CDR_MAIN_UNIQUE_ID).toString();
    		}else{
    			response = ActionResponse.createFailResponse(-1, "get var fail");
    			return response;
    		}
    	}
        	
        Map<String, Object> actionMap = new HashMap<String, Object>();
        actionMap.put("context", Const.DIALPLAN_CONTEXT_SPY);
        actionMap.put("exten", enterpriseId + spiedCno);
        actionMap.put("priority", 1);
        actionMap.put("otherChannelId", mainUniqueId);
        actionMap.put("channel", destInterface);
        actionMap.put("timeout", 30000);
        actionMap.put("clid", clid);     
                       
        JSONObject actionEvent = null;
        if (objectType.equals(Const.OBJECT_TYPE_CNO)) {
        	actionEvent = new JSONObject();
        	actionEvent.put("event", "originateResponse");
        	actionEvent.put("originateType", "spy");
        	actionEvent.put("enterpriseId", enterpriseId);
        	actionEvent.put("cno", spyObject);
        	actionEvent.put("spiedCno", spiedCno);
        	actionEvent.put("spyObject", spyObject);
        	actionEvent.put("objectType", objectType);
        }
        
        try{
        	if (objectType.equals(Const.OBJECT_TYPE_CNO)) {
        		if(memberService.isAvalibleLock(enterpriseId, spyObject) == false){
        			response = ActionResponse.createFailResponse(-1, "spy agent busy");
                	return response;
        		}
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
        
       
        if (objectType.equals(Const.OBJECT_TYPE_CNO)) {
        	redisService.convertAndSend(BigQueueCacheKey.AGENT_GATEWAY_EVENT_TOPIC, actionEvent);
        }
        response = ActionResponse.createSuccessResponse();

		return response;
	}
}
