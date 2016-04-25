package com.tinet.ctilink.bigqueue.inc;

public class CacheKey {
	public static String QUEUE_SCAN_LIST = "queue_scan_list";
	public static String QUEUE_MEMBER = "queue_member_%s_%s";
	public static String QUEUE_AVALIBLE_MEMBER = "queue_avalible_member_%s";
	public static String QUEUE_IDLE_MEMBER = "queue_idle_member_%s";
	public static String MEMBER_DEVICE_STATUS = "member_device_status_%s";
	public static String MEMBER_LOGIN_STATUS = "member_login_status_%s";
	public static String QUEUE_ENTRY = "queue_entry_%s_%s";
	public static String QUEUE_ENTRY_NP = "queue_entry_np_%s_%s";
	public static String QUEUE_ENTRY_INFO = "queue_entry_info_%s";
	
	public static String QUEUE_LOCK = "queue_lock_%s_%s";
	public static String MEMBER_LOCK = "member_device_status_lock_%s_%s";
    public static final String QUEUE_ENTERPRISE_QNO = "cti-link.queue.%d.qno.%s";
}
