package com.tinet.ctilink.bigqueue.ami.event;

import com.tinet.ctilink.json.JSONObject;

public interface EventHandler {
	
	public boolean handle(JSONObject event);
}
