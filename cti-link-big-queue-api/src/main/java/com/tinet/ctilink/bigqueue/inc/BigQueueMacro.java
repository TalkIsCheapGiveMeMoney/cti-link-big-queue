package com.tinet.ctilink.bigqueue.inc;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BigQueueMacro {
	private static Map<String, Set<String>> memberQueueMap1 = new HashMap<String, Set<String>>();
	private static Map<String, Set<String>> memberQueueMap2 = new HashMap<String, Set<String>>();
	private static int currentMemberQueueMapIndex = 1;
	
	public static Map<String, Set<String>> getCurrentMemberQueueMap(){
		if(currentMemberQueueMapIndex == 1){
			return memberQueueMap1;
		}else{
			return memberQueueMap2;
		}
	}
	public static Map<String, Set<String>> getReplaceMemberQueueMap(){
		if(currentMemberQueueMapIndex == 1){
			memberQueueMap2.clear();
			return memberQueueMap2;
		}else{
			memberQueueMap1.clear();
			return memberQueueMap1;
		}
	}
	public synchronized static void replaceMemberQueueMap(){
		if(currentMemberQueueMapIndex == 1){
			currentMemberQueueMapIndex = 2;
		}else{
			currentMemberQueueMapIndex = 1;
		}
	}
}
