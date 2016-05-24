
package com.tinet.ctilink.bigqueue.service.imp;


import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.tinet.ctilink.bigqueue.ami.action.GetVarActionService;
import com.tinet.ctilink.bigqueue.ami.action.OriginateActionService;
import com.tinet.ctilink.bigqueue.entity.ActionResponse;
import com.tinet.ctilink.bigqueue.entity.CallAgent;
import com.tinet.ctilink.bigqueue.inc.BigQueueCacheKey;
import com.tinet.ctilink.bigqueue.inc.BigQueueConst;
import com.tinet.ctilink.bigqueue.service.AgentService;
import com.tinet.ctilink.bigqueue.service.agent.BargeService;
import com.tinet.ctilink.bigqueue.service.agent.ChangeBindTelService;
import com.tinet.ctilink.bigqueue.service.agent.ConsultCancelService;
import com.tinet.ctilink.bigqueue.service.agent.ConsultService;
import com.tinet.ctilink.bigqueue.service.agent.ConsultThreewayService;
import com.tinet.ctilink.bigqueue.service.agent.ConsultTransferService;
import com.tinet.ctilink.bigqueue.service.agent.DirectCallStartService;
import com.tinet.ctilink.bigqueue.service.agent.DisconnectService;
import com.tinet.ctilink.bigqueue.service.agent.HoldService;
import com.tinet.ctilink.bigqueue.service.agent.InteractService;
import com.tinet.ctilink.bigqueue.service.agent.InvestigationService;
import com.tinet.ctilink.bigqueue.service.agent.LoginService;
import com.tinet.ctilink.bigqueue.service.agent.LogoutService;
import com.tinet.ctilink.bigqueue.service.agent.MuteService;
import com.tinet.ctilink.bigqueue.service.agent.PauseService;
import com.tinet.ctilink.bigqueue.service.agent.PickupService;
import com.tinet.ctilink.bigqueue.service.agent.PreviewOutcallCancelService;
import com.tinet.ctilink.bigqueue.service.agent.PreviewOutcallService;
import com.tinet.ctilink.bigqueue.service.agent.QueueStatusService;
import com.tinet.ctilink.bigqueue.service.agent.RefuseService;
import com.tinet.ctilink.bigqueue.service.agent.SetPauseService;
import com.tinet.ctilink.bigqueue.service.agent.SetUnpauseService;
import com.tinet.ctilink.bigqueue.service.agent.SpyService;
import com.tinet.ctilink.bigqueue.service.agent.StatusService;
import com.tinet.ctilink.bigqueue.service.agent.ThreewayService;
import com.tinet.ctilink.bigqueue.service.agent.TransferService;
import com.tinet.ctilink.bigqueue.service.agent.UnconsultService;
import com.tinet.ctilink.bigqueue.service.agent.UnholdService;
import com.tinet.ctilink.bigqueue.service.agent.UnlinkService;
import com.tinet.ctilink.bigqueue.service.agent.UnpauseService;
import com.tinet.ctilink.bigqueue.service.agent.UnspyService;
import com.tinet.ctilink.bigqueue.service.agent.UnthreewayService;
import com.tinet.ctilink.bigqueue.service.agent.UnwhisperService;
import com.tinet.ctilink.bigqueue.service.agent.WhisperService;
import com.tinet.ctilink.cache.CacheKey;
import com.tinet.ctilink.cache.RedisService;
import com.tinet.ctilink.conf.model.Agent;
import com.tinet.ctilink.conf.model.AgentTel;
import com.tinet.ctilink.conf.model.QueueMember;
import com.tinet.ctilink.inc.Const;
import com.tinet.ctilink.scheduler.RedisTaskScheduler;

@com.alibaba.dubbo.config.annotation.Service
public class AgentServiceImp implements AgentService {
	private final Logger logger = LoggerFactory.getLogger(getClass());
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
	LoginService loginService;
	@Autowired
	LogoutService logoutService;
	@Autowired
	PauseService pauseService;
	@Autowired
	UnpauseService unpauseService;
	@Autowired
	ChangeBindTelService changeBindTelService;
	@Autowired
	QueueStatusService queueStatusService;
	@Autowired
	BargeService bargeService;
	@Autowired
	ConsultCancelService consultCancelService;
	@Autowired
	ConsultService consultService;
	@Autowired
	ConsultThreewayService consultThreewayService;
	@Autowired
	ConsultTransferService consultTransferService;
	@Autowired
	DirectCallStartService directCallStartService;
	@Autowired
	DisconnectService disconnectService;
	@Autowired
	HoldService holdService;
	@Autowired
	InteractService interactService;
	@Autowired
	InvestigationService investigationService;

	@Autowired
	MuteService muteService;
	@Autowired
	PickupService pickupService;
	@Autowired
	PreviewOutcallService previewOutcallService;
	@Autowired
	PreviewOutcallCancelService previewOutcallCancelService;
	@Autowired
	RefuseService refuseService;
	@Autowired
	SetPauseService setPauseService;
	@Autowired
	SetUnpauseService setUnpauseService;
	@Autowired
	SpyService spyService;
	@Autowired
	ThreewayService threewayService;
	@Autowired
	TransferService transferService;
	@Autowired
	UnconsultService unconsultService;
	@Autowired
	UnholdService unholdService;
	@Autowired
	UnlinkService unlinkService;
	@Autowired
	UnspyService unspyService;
	@Autowired
	UnthreewayService unthreewayService;
	@Autowired
	UnwhisperService unwhisperService;
	@Autowired
	WhisperService whisperService;
	@Autowired
	StatusService statusService;
	
	public ActionResponse login(Map<String, Object> params){
		return loginService.login(params);
	}
	public ActionResponse logout(Map<String, Object> params){
		return logoutService.logout(params);
	}	
	public ActionResponse status(Map<String, Object> params){
		return statusService.status(params);
	}
	public ActionResponse pause(Map<String, Object> params){
		return pauseService.pause(params);
	}
	public ActionResponse unpause(Map<String, Object> params){
		return unpauseService.unpause(params);
	}
	public ActionResponse changeBindTel(Map<String, Object> params){
		return changeBindTelService.changeBindTel(params);
	}
	
	public ActionResponse queueStatus(Map<String, Object> params){
		return queueStatusService.queueStatus(params);
	}
	
	public ActionResponse barge(Map<String, Object> params){
		return bargeService.barge(params);
	}
	
	public ActionResponse consultCancel(Map<String, Object> params){
		return consultCancelService.consultCancel(params);
	}
	public ActionResponse consult(Map<String, Object> params){
		return consultService.consult(params);
	}
	public ActionResponse consultThreeway(Map<String, Object> params){
		return consultThreewayService.consultThreeway(params);
	}
	public ActionResponse consultTransfer(Map<String, Object> params){
		return consultTransferService.consultTransfer(params);
	}
	public ActionResponse directCallStart(Map<String, Object> params){
		return directCallStartService.directCallStart(params);
	}
	public ActionResponse disconnect(Map<String, Object> params){
		return disconnectService.disconnect(params);
	}
	public ActionResponse hold(Map<String, Object> params){
		return holdService.hold(params);
	}
	public ActionResponse interact(Map<String, Object> params){
		return interactService.interact(params);
	}
	public ActionResponse investigation(Map<String, Object> params){
		return investigationService.investigation(params);
	}
	public ActionResponse mute(Map<String, Object> params){
		return muteService.mute(params);
	}
	public ActionResponse pickup(Map<String, Object> params){
		return pickupService.pickup(params);
	}
	public ActionResponse previewOutcall(Map<String, Object> params){
		return previewOutcallService.previewOutcall(params);
	}
	public ActionResponse previewOutcallCancel(Map<String, Object> params){
		return previewOutcallCancelService.previewOutcallCancel(params);
	}
	public ActionResponse refuse(Map<String, Object> params){
		return refuseService.refuse(params);
	}
	public ActionResponse setPause(Map<String, Object> params){
		return setPauseService.setPause(params);
	}
	public ActionResponse setUnpause(Map<String, Object> params){
		return setUnpauseService.setUnpause(params);
	}
	public ActionResponse spy(Map<String, Object> params){
		return spyService.spy(params);
	}
	public ActionResponse threeway(Map<String, Object> params){
		return threewayService.threeway(params);
	}
	public ActionResponse transfer(Map<String, Object> params){
		return transferService.transfer(params);
	}
	public ActionResponse unconsult(Map<String, Object> params){
		return unconsultService.unconsult(params);
	}
	public ActionResponse unhold(Map<String, Object> params){
		return unholdService.unhold(params);
	}
	public ActionResponse unlink(Map<String, Object> params){
		return unlinkService.unlink(params);
	}
	public ActionResponse unspy(Map<String, Object> params){
		return unspyService.unspy(params);
	}
	public ActionResponse unthreeway(Map<String, Object> params){
		return unthreewayService.unthreeway(params);
	}
	public ActionResponse unwhisper(Map<String, Object> params){
		return unwhisperService.unwhisper(params);
	}		
	public ActionResponse whisper(Map<String, Object> params){
		return whisperService.whisper(params);
	}
	
	/**
	 * 其他方法
	 */
	/**
	 * 
	 * @param enterpriseId
	 * @param cno
	 * @return
	 */
	public Agent getAgent(String enterpriseId, String cno){
		String agentKey = String.format(CacheKey.AGENT_ENTERPRISE_ID_CNO, Integer.parseInt(enterpriseId), cno);
		Agent agent = redisService.get(Const.REDIS_DB_CONF_INDEX, agentKey, Agent.class);
		return agent;
	}
	public CallAgent getCallAgent(String enterpriseId, String cno){
		String agentKey = String.format(BigQueueCacheKey.AGENT_ENTERPRISE_ID, enterpriseId);
		CallAgent agent = redisService.hget(Const.REDIS_DB_CTI_INDEX, agentKey, cno, CallAgent.class);
		return agent;
	}
	
	public void saveCallAgent(String enterpriseId, String cno, CallAgent callAgent){
		if(callAgent == null){
			String agentKey = String.format(BigQueueCacheKey.AGENT_ENTERPRISE_ID, enterpriseId);
			redisService.hdel(Const.REDIS_DB_CTI_INDEX, agentKey, cno);
		}else{
			String agentKey = String.format(BigQueueCacheKey.AGENT_ENTERPRISE_ID, callAgent.getEnterpriseId());
			redisService.hset(Const.REDIS_DB_CTI_INDEX, agentKey, callAgent.getCno(), callAgent);
		}
	}
	public List<AgentTel> getAgentBindTel(String enterpriseId, String cno){
		String key = String.format(CacheKey.AGENT_TEL_ENTERPRISE_ID_CNO, Integer.parseInt(enterpriseId), cno);
		List<AgentTel> agentTelList = redisService.getList(Const.REDIS_DB_CONF_INDEX, key, AgentTel.class);

		return agentTelList;
	}
	
	public void updateQueueMember(String enterpriseId, String cno, List<QueueMember> queueMemberList){
		for(QueueMember queueMember: queueMemberList){
			 queueService.setMember(enterpriseId, queueMember);
		}
	}
	public void delQueueMember(String enterpriseId, String cno, List<QueueMember> queueMemberList){
		for(QueueMember queueMember: queueMemberList){
			queueService.delMember(enterpriseId, queueMember.getQno(), cno);
		}
	}
	
	public List<QueueMember> getQueueMemberList(String enterpriseId, String cno){
		String confKey = String.format(CacheKey.QUEUE_MEMBER_ENTERPRISE_ID_CNO, Integer.parseInt(enterpriseId), cno);
		List<QueueMember> queueMemberList = redisService.getList(Const.REDIS_DB_CONF_INDEX, confKey, QueueMember.class);
		return queueMemberList;
	}
	public boolean isNeedResetDeviceStatus(CallAgent callAgent){
		boolean res = false;
		Integer deviceStatus = memberService.getDeviceStatus(String.valueOf(callAgent.getEnterpriseId()), callAgent.getCno());
		switch(deviceStatus){
			case BigQueueConst.MEMBER_DEVICE_STATUS_IDLE:
				break;
			case BigQueueConst.MEMBER_DEVICE_STATUS_INVALID:
				res = true;
				break;
			case BigQueueConst.MEMBER_DEVICE_STATUS_LOCKED:
				res = true;
				break;
			case BigQueueConst.MEMBER_DEVICE_STATUS_INVITE:
			case BigQueueConst.MEMBER_DEVICE_STATUS_RINGING:
			case BigQueueConst.MEMBER_DEVICE_STATUS_INUSE:
				if(!channelService.isAlive(callAgent.getCurrentChannelUniqueId())){
					res = true;
				}
				break;
		}
		return res;
	}
}
