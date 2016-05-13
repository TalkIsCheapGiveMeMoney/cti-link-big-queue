package com.tinet.ctilink.bigqueue.ami;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventHandlerFactory {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private static Map<String, EventHandler> list = new HashMap<String, EventHandler>();
	
	public static void register(String event, EventHandler strategyObject ){
		list.put(event, strategyObject);
		
	}
	public static EventHandler getInstance(String event){
		return list.get(event);
	}
}
