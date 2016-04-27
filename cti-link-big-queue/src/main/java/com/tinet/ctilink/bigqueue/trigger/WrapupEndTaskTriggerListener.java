package com.tinet.ctilink.bigqueue.trigger;

import com.github.davidmarquis.redisscheduler.TaskTriggerListener;

/**
 * @author fengwei //
 * @date 16/4/23 15:38
 */
public class WrapupEndTaskTriggerListener implements TaskTriggerListener {
	
    @Override
    public void taskTriggered(String taskId) {
        System.out.printf("Task %s is due for execution.", taskId);
        
    }
}
