package com.tinet.ctilink.bigqueue.ami.event;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OriginateResponseHandlerFactory {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private static Map<String, EventHandler> list = new HashMap<String, EventHandler>();
	
	public static void register(String originateType, EventHandler handler ){
		list.put(originateType, handler);
		
	}
	public static EventHandler getInstance(String originateType){
		return list.get(originateType);
	}
}
