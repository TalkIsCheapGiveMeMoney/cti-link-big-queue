package com.tinet.ctilink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import com.tinet.ctilink.bigqueue.eventlistener.AmiEventListener;
import com.tinet.ctilink.bigqueue.trigger.StatusScanTaskTrigger;
import com.tinet.ctilink.scheduler.RedisTaskScheduler;
import com.tinet.ctilink.scheduler.TaskSchedulerGroup;

/**
 * 应用程序启动器
 * 
 * @author Jiangsl
 *
 */
@Component
public class ApplicationStarter implements ApplicationListener<ContextRefreshedEvent> {
	private static Logger logger = LoggerFactory.getLogger(ApplicationStarter.class);

	@Autowired
	AmiEventListener amiEventListener;
	@Autowired
	private RedisTaskScheduler redisTaskScheduler;
	
	@Override
	public void onApplicationEvent(final ContextRefreshedEvent event) {
		
		// 设置JVM的DNS缓存时间
		// http://docs.amazonaws.cn/AWSSdkDocsJava/latest/DeveloperGuide/java-dg-jvm-ttl.html
		java.security.Security.setProperty("networkaddress.cache.ttl", "60");

		//注册整理任务的group
		redisTaskScheduler.registerTaskSchedulerGroup(new TaskSchedulerGroup("warpupTaskSchedulerGroup", 10));
		//注册整理任务的group
		redisTaskScheduler.registerTaskSchedulerGroup(new TaskSchedulerGroup("limitTimeTaskSchedulerGroup", 3));
				
		//启动statusScanTask
		redisTaskScheduler.schedulePeriod("statusScanTask", "statusScanTaskTrigger", null, 1000, 1);

		//启动statusCheckScanTask
		redisTaskScheduler.schedulePeriod("statusCheckScanTask", "statusCheckScanTaskTrigger", null, 5000, 1);
		
		amiEventListener.start();
		
		logger.info("cti-link-big-queue启动成功");
		System.out.println("cti-link-big-queue启动成功");
	}
}