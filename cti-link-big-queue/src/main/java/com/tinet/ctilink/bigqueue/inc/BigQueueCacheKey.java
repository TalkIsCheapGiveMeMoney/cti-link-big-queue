package com.tinet.ctilink.bigqueue.inc;

public class BigQueueCacheKey {
	/* 扫描队列,用于定期扫描队列中坐席状态 set */
	public static String QUEUE_SCAN_LIST = "queue_scan_list";
	/* 队列中的坐席 hash */
	public static String QUEUE_MEMBER_ENTERPRISE_ID_QNO = "queue_member_%s_%s";
	/* 队列中的坐席 hash */
	//public static String QUEUE_MEMBER_ENTERPRISE_ID_CNO = "queue_member_%s_%s";
	/* 队列中可用坐席个数 hash */
	public static String QUEUE_AVALIBLE_MEMBER_ENTERPRISE_ID = "queue_avalible_member_%s"; 
	/* 队列中空闲坐席个数 hash */
	public static String QUEUE_IDLE_MEMBER_ENTERPRISE_ID = "queue_idle_member_%s";
	/* 坐席设备状态 hash */
	public static String MEMBER_DEVICE_STATUS_ENTERPRISE_ID = "member_device_status_%s";
	/* 坐席登录状态 hash */
	public static String MEMBER_LOGIN_STATUS_ENTERPRISE_ID = "member_login_status_%s";
	/* 队列中排队者 zset */
	public static String QUEUE_ENTRY_ENTERPRISE_ID_QNO = "queue_entry_%s_%s";
	/* 队列中排队者去除正在呼叫的 zset*/
	public static String QUEUE_ENTRY_NP_ENTERPRISE_ID_QNO = "queue_entry_np_%s_%s";
	/* 队列排队者信息 hash */
	public static String QUEUE_ENTRY_INFO_UNIQUE_ID = "queue_entry_info_%s";
	/* 队列统计信息rrpos hash */
	public static String QUEUE_STATISTIC_ENTERPRISE_ID_QNO = "queue_statistic_%s_%s";
	/* 队列锁 string */
	public static String QUEUE_LOCK_ENTERPRISE_ID_QNO = "queue_lock_%s_%s";
	/* 坐席锁 string */
	public static String MEMBER_LOCK_ENTERPRISE_ID_CNO = "member_device_status_lock_%s_%s";
    /* 坐席数据 hash*/
    public static final String AGENT_ENTERPRISE_ID = "agent_%s";
    /* 通道存活 */
    public static final String CHANNEL_ALIVE_UNIQUEID ="sip-media-server.channel.%s";

}
