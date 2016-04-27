package com.tinet.ctilink.bigqueue.strategy;

import java.util.HashMap;
import java.util.Map;

public class StrategyFactory {
	private static Map<String, Strategy> list = new HashMap<String, Strategy>();
	
	public static void register(String strategyName, Strategy strategyObject ){
		list.put(strategyName, strategyObject);
		
	}
	public static Strategy getInstance(String strategyName){
		return list.get(strategyName);
	}
}
