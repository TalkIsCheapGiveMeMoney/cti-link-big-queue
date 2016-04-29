package com.tinet.ctilink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import com.github.davidmarquis.redisscheduler.RedisTaskScheduler;

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
    @Qualifier("statusScanTaskScheduler")
    private RedisTaskScheduler statusScanScheduler;
	@Autowired
    @Qualifier("statusCheckScanTaskScheduler")
    private RedisTaskScheduler statusCheckScanScheduler;
	
	@Override
	public void onApplicationEvent(final ContextRefreshedEvent event) {
		
		// 设置JVM的DNS缓存时间
		// http://docs.amazonaws.cn/AWSSdkDocsJava/latest/DeveloperGuide/java-dg-jvm-ttl.html
		java.security.Security.setProperty("networkaddress.cache.ttl", "60");

		statusScanScheduler.schedule("queueMemberStatusScan", null);
		statusCheckScanScheduler.schedule("queueMemberStatusCheckScan", null);
		
		
		logger.info("cti-link-big-queue启动成功");
		System.out.println("cti-link-big-queue启动成功");
	}
}