package com.tinet.ctilink.bigqueue.service;


import java.util.Map;

import com.tinet.ctilink.bigqueue.entity.ActionResponse;


public interface AgentService {

	public ActionResponse login(Map params);
	public ActionResponse logout(Map params);
	public ActionResponse pause(Map params);
	public ActionResponse unpause(Map params);
	public ActionResponse changeBindTel(Map params);
	public ActionResponse queueStatus(Map params);
	
	public ActionResponse barge(Map params);
	public ActionResponse hangup(Map params);
	public ActionResponse consultCancel(Map params);
	public ActionResponse consult(Map params);
	public ActionResponse consultThreeway(Map params);
	public ActionResponse consultTransfer(Map params);
	public ActionResponse directCallStart(Map params);
	public ActionResponse disconnect(Map params);
	public ActionResponse hold(Map params);
	public ActionResponse interact(Map params);
	public ActionResponse investigation(Map params);
	public ActionResponse ivrOutcall(Map params);
	public ActionResponse mute(Map params);
	public ActionResponse pickup(Map params);
	public ActionResponse previewOutcall(Map params);
	public ActionResponse previewOutcallCancel(Map params);
	public ActionResponse refuse(Map params);
	public ActionResponse setPause(Map params);
	public ActionResponse setUnpause(Map params);
	public ActionResponse spy(Map params);
	public ActionResponse threeway(Map params);
	public ActionResponse transfer(Map params);
	public ActionResponse unconsult(Map params);
	public ActionResponse unhold(Map params);
	public ActionResponse unlink(Map params);
	public ActionResponse unspy(Map params);
	public ActionResponse unthreeway(Map params);
	public ActionResponse unwhisper(Map params);
	public ActionResponse whisper(Map params);
}
