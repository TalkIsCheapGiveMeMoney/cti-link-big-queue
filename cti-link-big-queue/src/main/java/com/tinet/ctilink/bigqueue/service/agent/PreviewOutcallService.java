package com.tinet.ctilink.bigqueue.service.agent;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.tinet.ctilink.inc.EnterpriseSettingConst;
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
import com.tinet.ctilink.cache.CacheKey;
import com.tinet.ctilink.cache.RedisService;
import com.tinet.ctilink.conf.entity.Caller;
import com.tinet.ctilink.conf.model.Agent;
import com.tinet.ctilink.conf.model.EnterpriseSetting;
import com.tinet.ctilink.conf.model.Gateway;
import com.tinet.ctilink.conf.util.AreaCodeUtil;
import com.tinet.ctilink.conf.util.ClidUtil;
import com.tinet.ctilink.conf.util.RestrictTelUtil;
import com.tinet.ctilink.conf.util.RouterUtil;
import com.tinet.ctilink.inc.Const;
import com.tinet.ctilink.inc.EnterpriseSettingConst;
import com.tinet.ctilink.json.JSONObject;
import com.tinet.ctilink.scheduler.RedisTaskScheduler;
import com.tinet.ctilink.util.AgentUtil;
import com.tinet.ctilink.util.RedisLock;

@Component
public class PreviewOutcallService {
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
	@Autowired
	UnpauseService unpauseService;
	public ActionResponse previewOutcall(Map<String,Object> params){
		ActionResponse response = null;
		String enterpriseId = params.get("enterpriseId").toString();
		String cno = params.get("cno").toString();
		
		//先获取lock memberService.lockMember(enterpriseId, cno);
		RedisLock memberLock = memberService.lockMember(enterpriseId, cno);
		if(memberLock != null){
			try{
				CallAgent callAgent = agentService.getCallAgent(enterpriseId, cno);
				if(callAgent != null){
					Integer deviceStatus = memberService.getDeviceStatus(enterpriseId, cno);
					Integer loginStatus = memberService.getLoginStatus(enterpriseId, cno);
					String pauseDescription = callAgent.getPauseDescription();
					Integer pauseType = callAgent.getPauseType();
	        		if(!deviceStatus.equals(BigQueueConst.MEMBER_DEVICE_STATUS_IDLE)){
	        			response = ActionResponse.createFailResponse(-1, "previewOutcall agent busy");
	                	return response;
	        		}

	        		Integer timeout;
	        		String timeoutStr = params.get("timeout").toString(); 
	        		if(StringUtils.isNotEmpty(timeoutStr)){
	        			timeout = Integer.parseInt(timeoutStr);
	        		}else{
	        			timeout = 45;
	        		}
	                String previewOutcallTel = params.get("previewOutcallTel").toString();//外呼要拨打的电话号码
	                if (StringUtils.isNotEmpty(previewOutcallTel)) {
	                    previewOutcallTel = previewOutcallTel.replaceAll("\\s","").replaceAll("-",""); //除去空格等特殊字符和中横线
	                }
	                if (StringUtils.isEmpty(previewOutcallTel) || !StringUtils.isNumeric(previewOutcallTel)) {
	                	response = ActionResponse.createFailResponse(-1, "bad param");
	                	return response;
	                }
	               
	                //
	                Caller caller = AreaCodeUtil.updateGetAreaCode(previewOutcallTel, "");
	                if (caller.getCallerNumber().equals(Const.UNKNOWN_NUMBER) || caller.getAreaCode().equals("")) {
	                	response = ActionResponse.createFailResponse(-1, "bad tel");
	                	return response;
	                }
	                //是否有外呼权限EnterpriseSetting里查
	                if(!checkCallPower(Integer.parseInt(enterpriseId), cno, caller)){
	                	response = ActionResponse.createFailResponse(-1, "permission denied");
	                	return response;
	                }
	                
	                //检查黑名单 电话号码为黑名单
	                if(RestrictTelUtil.isRestrictTel(Integer.parseInt(enterpriseId), previewOutcallTel,Const.RESTRICT_TEL_TYPE_OB)){
	                	response = ActionResponse.createFailResponse(-1, "restrict tel");
	                	return response;
	                }
	                int routerClidCallType = Const.ROUTER_CLID_CALL_TYPE_PREVIEW_OB_RIGHT;
	                
	        		//检查传入的clid是否合法
	                String obClidLeft;
	        		String obClidLeftNumber = params.get("obClidLeftNumber").toString();
	        		if(StringUtils.isNotEmpty(obClidLeftNumber)){
		        		if(!ClidUtil.isClidValid(Integer.parseInt(enterpriseId), Const.ROUTER_CLID_CALL_TYPE_PREVIEW_OB_LEFT, previewOutcallTel, obClidLeftNumber)){
		        			response = ActionResponse.createFailResponse(-1, "bad obClidLeftNumber");
		                	return response;
		        		}
		        		obClidLeft = obClidLeftNumber;
	        		}else{
	        			obClidLeft = ClidUtil.getClid(Integer.parseInt(enterpriseId), Const.ROUTER_CLID_CALL_TYPE_PREVIEW_OB_LEFT, previewOutcallTel, "");
	        		}
	        		
	                String gwIp = AgentUtil.getGwIp(callAgent.getInterface());
	                String clidRight = ClidUtil.getClid(Integer.parseInt(enterpriseId), routerClidCallType, previewOutcallTel, "");
	        		
	                if(loginStatus.equals(BigQueueConst.MEMBER_LOGIN_STATUS_PAUSE)  || 
	                        (loginStatus.equals(BigQueueConst.MEMBER_LOGIN_STATUS_WRAPUP))){//置忙外呼就不再置忙preivewoutcalling,如果在整理要调用置忙让整理结束
	                	
	                	unpauseService.unpauseNolock(enterpriseId, cno, null);
	                	
	                	if(loginStatus.equals(BigQueueConst.MEMBER_LOGIN_STATUS_PAUSE)){
		                	callAgent.setRnaPause(true);
		                	callAgent.setRnaPauseDescription(pauseDescription);
		                	callAgent.setRnaPauseType(pauseType);
		                	agentService.saveCallAgent(enterpriseId, cno, callAgent);
	                	}
	                }
	                Map<String, String> varMap = new HashMap<String, String>();

	                varMap.put("__" + AmiChanVarNameConst.CDR_CUSTOMER_NUMBER, caller.getCallerNumber()); //客户号码
	                varMap.put("__" + AmiChanVarNameConst.CDR_CUSTOMER_NUMBER_TYPE, String.valueOf(caller.getTelType())); //电话类型
	                varMap.put("__" + AmiChanVarNameConst.CDR_CUSTOMER_AREA_CODE, caller.getAreaCode()); //区号
	           
	                varMap.put("__" + AmiChanVarNameConst.CDR_ENTERPRISE_ID, String.valueOf(enterpriseId));
	                varMap.put(AmiChanVarNameConst.CDR_CNO, cno);
	                varMap.put("__" + AmiChanVarNameConst.CDR_CALL_TYPE, String.valueOf(Const.CDR_CALL_TYPE_OB_PREVIEW));
	                
	                //判断是否打开号码状态语音识别
	                String enterpriseSettingKey = String.format(CacheKey.ENTERPRISE_SETTING_ENTERPRISE_ID_NAME, Integer.parseInt(enterpriseId), EnterpriseSettingConst.ENTERPRISE_SETTING_NAME_TEL_STATUS_IDENTIFICATION);
	        		EnterpriseSetting setting = redisService.get(Const.REDIS_DB_CONF_INDEX, enterpriseSettingKey, EnterpriseSetting.class);
	                if(setting!=null && "1".equals(setting.getValue())){
	                	varMap.put("__" + AmiChanVarNameConst.IS_TSI, "1");
	                }
	                setting = redisService.get(Const.REDIS_DB_CONF_INDEX, String.format(CacheKey.ENTERPRISE_SETTING_ENTERPRISE_ID_NAME, Integer.parseInt(enterpriseId)
							, EnterpriseSettingConst.ENTERPRISE_SETTING_NAME_MP3_RATIO), EnterpriseSetting.class);
					if (setting != null ) { 
						varMap.put("__" + AmiChanVarNameConst.MP3_RATIO, setting.getValue());
					}
	                //获取是否外呼录音
	                String agentKey = String.format(CacheKey.AGENT_ENTERPRISE_ID_CNO, Integer.parseInt(enterpriseId), cno);
	                Agent agent = redisService.get(Const.REDIS_DB_CONF_INDEX, agentKey, Agent.class);
	                Integer obRecord = agent.getObRecord();
	                
	                varMap.put(AmiChanVarNameConst.IS_OB_RECORD, String.valueOf(obRecord));
	                varMap.put(AmiChanVarNameConst.CDR_CALLEE_NUMBER, callAgent.getBindTel());
	                if(callAgent.getBindType().equals(Const.BIND_TYPE_EXTEN) || callAgent.getBindType().equals(Const.BIND_TYPE_SOFT_PHONE)){
	                	varMap.put(AmiChanVarNameConst.CDR_EXTEN, callAgent.getBindTel());
	                }
	                Date startDate = new Date();
	                long callStartTime =startDate.getTime()/1000;
	                varMap.put(AmiChanVarNameConst.CDR_START_TIME, String.valueOf(callStartTime));
	                //member
	                varMap.put(AmiChanVarNameConst.CDR_STATUS, String.valueOf(Const.CDR_STATUS_OB_PREVIEW_AGENT_NO_ANSWER));
	                
	                setting = redisService.get(Const.REDIS_DB_CONF_INDEX, String.format(CacheKey.ENTERPRISE_SETTING_ENTERPRISE_ID_NAME, Integer.parseInt(enterpriseId)
							, EnterpriseSettingConst.ENTERPRISE_SETTING_NAME_RECORD_FILE_USERFIELD), EnterpriseSetting.class);
					if (setting != null ) { 
						varMap.put(AmiChanVarNameConst.RECORD_FILE_USERFIELD, setting.getValue());
						varMap.put(AmiChanVarNameConst.MONITOR_TYPE, setting.getProperty());
					}
					
	                //获取是否自动满意度调查
	                Integer isInvestigationAuto = 0;
	                enterpriseSettingKey = String.format(CacheKey.ENTERPRISE_SETTING_ENTERPRISE_ID_NAME, Integer.parseInt(enterpriseId), EnterpriseSettingConst.ENTERPRISE_SETTING_NAME_AUTO_INVESTIGATION_OB);
	        		setting = redisService.get(Const.REDIS_DB_CONF_INDEX, enterpriseSettingKey, EnterpriseSetting.class);
	                if(setting!=null && "1".equals(setting.getValue())){
	                	isInvestigationAuto = 1;
	                }
	                
	                varMap.put(AmiChanVarNameConst.CDR_GW_IP, gwIp);
	                varMap.put(AmiChanVarNameConst.CDR_CLID, clidRight);                 //座席侧外显号码
	                varMap.put("__" + AmiChanVarNameConst.IS_INVESTIGATION_AUTO, String.valueOf(isInvestigationAuto));
	                varMap.put(AmiChanVarNameConst.PREVIEW_OUTCALL_LEFT_CLID, obClidLeft);               //客户侧外显号码
	                
	                varMap.put(AmiChanVarNameConst.DIAL_TIMEOUT, "60");                  //外呼等待时长  60秒
	                varMap.put(AmiChanVarNameConst.PRE_DIAL_RUN, "\"" + Const.DIALPLAN_CONTEXT_PREVIEW_OUTCALL_PREDIAL + "," + enterpriseId +",1\"");
	                Map<String, Object> userFieldMap = (Map<String, Object>)(params.get("userField"));
	                for(String key: userFieldMap.keySet()){
	                	varMap.put(key, userFieldMap.get(key).toString()); 
	                }
	                
	                Map<String, Object> actionMap = new HashMap<String, Object>();
	                actionMap.put(AmiParamConst.DIALPLAN_CONTEXT, Const.DIALPLAN_CONTEXT_PREVIEW_OUTCALL);
	                actionMap.put(AmiParamConst.EXTENSION, enterpriseId + cno);
	                actionMap.put(AmiParamConst.PRIORITY, 1);
	                actionMap.put(AmiParamConst.CHANNEL, callAgent.getInterface());
	                actionMap.put(AmiParamConst.ORIGINATE_TIMEOUT, timeout);
	                actionMap.put(AmiParamConst.CLID, clidRight);     
	                               
	                JSONObject actionEvent = null;

                	actionEvent = new JSONObject();
                	actionEvent.put("event", "originateResponse");
                	actionEvent.put("originateType", "previewOutcall");
                	actionEvent.put("enterpriseId", enterpriseId);
                	actionEvent.put("cno", cno);
                	actionEvent.put("previewOutcallTel", previewOutcallTel);

	                try{
	                	memberService.setDeviceStatus(enterpriseId, cno, BigQueueConst.MEMBER_DEVICE_STATUS_LOCKED);
	                	
	                	AmiActionResponse amiResponse = originateActionService.originate(null, actionMap, actionEvent, varMap);
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

	                response = ActionResponse.createSuccessResponse();
	                return response;
	                
				}else {
					response = ActionResponse.createFailResponse(-1, "no such agent");
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
		}

		return response;
	}
	
	public Boolean checkCallPower(Integer enterpriseId, String cno, Caller caller) {
		
		String agentKey = String.format(CacheKey.AGENT_ENTERPRISE_ID_CNO, enterpriseId, cno);
		Agent agent = redisService.get(Const.REDIS_DB_CONF_INDEX, agentKey, Agent.class);

		if (agent != null) {
			
			switch (agent.getCallPower()) {
			case Const.CLIENT_CALL_POWER_ALL:
				return true;
			case Const.CLIENT_CALL_POWER_NATIONAL:
				if (caller.getAreaCode().equals("00")) {
					return false;
				} else {
					return true;
				}
			case Const.CLIENT_CALL_POWER_LOCAL:
				if (caller.getAreaCode().equals(agent.getAreaCode())) {
					return true;
				} else {
					return false;
				}
			case Const.CLIENT_CALL_POWER_INTERNAL:
				if (caller.getAreaCode().equals("")) {// 取不到areaCode表示分机
					return true;
				} else {
					return false;
				}
			}
		}
		return false;
	}
}
