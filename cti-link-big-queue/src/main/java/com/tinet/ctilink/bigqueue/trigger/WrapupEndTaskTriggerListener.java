package com.tinet.ctilink.bigqueue.trigger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.davidmarquis.redisscheduler.TaskTriggerListener;

/**
 * @author fengwei //
 * @date 16/4/23 15:38
 */
public class WrapupEndTaskTriggerListener implements TaskTriggerListener {
	private final Logger logger = LoggerFactory.getLogger(getClass());
    @Override
    public void taskTriggered(String taskId) {
        System.out.printf("Task %s is due for execution.", taskId);
        
    }
}
