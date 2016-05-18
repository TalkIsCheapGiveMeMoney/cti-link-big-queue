package com.tinet.ctilink.bigqueue.service;


import java.util.Map;

import com.tinet.ctilink.bigqueue.entity.ActionResponse;


public interface AgentService {

	public ActionResponse login(Map params);
	public ActionResponse logout(Map params);
	public ActionResponse pause(Map params);
	public ActionResponse unpause(Map params);
	public ActionResponse changeBindTel(Map params);

}
