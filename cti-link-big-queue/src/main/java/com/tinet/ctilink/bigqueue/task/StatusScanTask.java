package com.tinet.ctilink.bigqueue.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.github.davidmarquis.redisscheduler.RedisTaskScheduler;

public class StatusScanTask {
	
	static String taskId = "queueMemberStatusScan";
	
    @Autowired
    @Qualifier("periodTaskScheduler")
    private RedisTaskScheduler scheduler;
    
    public void start(){
    	scheduler.schedule(taskId, null);
    }
}
