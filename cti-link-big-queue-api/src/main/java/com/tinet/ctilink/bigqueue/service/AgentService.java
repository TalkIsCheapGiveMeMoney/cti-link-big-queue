package com.tinet.ctilink.bigqueue.service;




import java.util.Map;

import com.tinet.ctilink.bigqueue.entity.ActionResponse;


public interface AgentService {

	public ActionResponse login(Map<String, Object> params);
	public ActionResponse logout(Map<String, Object> params);
	public ActionResponse status(Map<String, Object> params);
	public ActionResponse pause(Map<String, Object> params);
	public ActionResponse unpause(Map<String, Object> params);
	public ActionResponse changeBindTel(Map<String, Object> params);
	public ActionResponse queueStatus(Map<String, Object> params);
	
	public ActionResponse barge(Map<String, Object> params);
	public ActionResponse consultCancel(Map<String, Object> params);
	public ActionResponse consult(Map<String, Object> params);
	public ActionResponse consultThreeway(Map<String, Object> params);
	public ActionResponse consultTransfer(Map<String, Object> params);
	public ActionResponse directCallStart(Map<String, Object> params);
	public ActionResponse disconnect(Map<String, Object> params);
	public ActionResponse hold(Map<String, Object> params);
	public ActionResponse interact(Map<String, Object> params);
	public ActionResponse investigation(Map<String, Object> params);
	public ActionResponse mute(Map<String, Object> params);
	public ActionResponse pickup(Map<String, Object> params);
	public ActionResponse previewOutcall(Map<String, Object> params);
	public ActionResponse previewOutcallCancel(Map<String, Object> params);
	public ActionResponse refuse(Map<String, Object> params);
	public ActionResponse setPause(Map<String, Object> params);
	public ActionResponse setUnpause(Map<String, Object> params);
	public ActionResponse spy(Map<String, Object> params);
	public ActionResponse threeway(Map<String, Object> params);
	public ActionResponse transfer(Map<String, Object> params);
	public ActionResponse unconsult(Map<String, Object> params);
	public ActionResponse unhold(Map<String, Object> params);
	public ActionResponse unlink(Map<String, Object> params);
	public ActionResponse unspy(Map<String, Object> params);
	public ActionResponse unthreeway(Map<String, Object> params);
	public ActionResponse unwhisper(Map<String, Object> params);
	public ActionResponse whisper(Map<String, Object> params);
}
