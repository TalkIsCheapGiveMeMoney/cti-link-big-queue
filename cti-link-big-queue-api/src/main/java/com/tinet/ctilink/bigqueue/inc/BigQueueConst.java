package com.tinet.ctilink.bigqueue.inc;

public class BigQueueConst {
	public static final int MEMBER_DEVICE_STATUS_INVALID = -1;
	public static final int MEMBER_DEVICE_STATUS_IDLE = 0;
	public static final int MEMBER_DEVICE_STATUS_LOCKED = 1;
	public static final int MEMBER_DEVICE_STATUS_INVITE = 2;
	public static final int MEMBER_DEVICE_STATUS_RINGING = 3;
	public static final int MEMBER_DEVICE_STATUS_INUSE = 4;

	
	public static final int MEMBER_LOGIN_STATUS_OFFLINE = 0;
	public static final int MEMBER_LOGIN_STATUS_READY = 1;
	public static final int MEMBER_LOGIN_STATUS_PAUSE = 2;
	public static final int MEMBER_LOGIN_STATUS_WRAPUP = 3;
	
	public static final int MEMBER_LOGIN_STATUS_PAUSE_TYPE_NORMAL = 1;
	public static final int MEMBER_LOGIN_STATUS_PAUSE_TYPE_REST = 2;
	public static final int MEMBER_LOGIN_STATUS_PAUSE_TYPE_IM = 3;
	
	public static final int ENTERPRISE_ID_LEN = 7;
	
	public static final int QUEUE_EMPTY_PAUSED = (1<<0);
	public static final int QUEUE_EMPTY_INUSE = (1<<1);
	public static final int QUEUE_EMPTY_RINGING = (1<<2);
	public static final int QUEUE_EMPTY_INVALID = (1<<3);
	public static final int QUEUE_EMPTY_WRAPUP = (1<<4);
	
	public static final int QUEUE_CODE_TIMEOUT = 1;
	public static final int QUEUE_CODE_LEAVE_EMPTY = 2;
	public static final int QUEUE_CODE_JOIN_EMPTY = 4;
	public static final int QUEUE_CODE_JOIN_FULL = 3;
	public static final int QUEUE_CODE_JOIN_OK = 5;
	public static final int QUEUE_CODE_OUR_TURN = 6;
	public static final int QUEUE_CODE_ROUTE_OK = 7;
	public static final int QUEUE_CODE_COMPLETE = 8;
	public static final int QUEUE_CODE_ROUTE_FAIL = 9;
	
	public static final int QUEUE_PRIORITY_MULTIPILER = 1000000;
	public static final int QUEUE_LOCK_TIMEOUT = 2000;
	public static final int MEMBER_LOCK_TIMEOUT = 2000;
	
	public static final int MEMBER_STATUS_TRYING_MAX_TIMEOUT = 80;
	public static final int MEMBER_STATUS_LOCKED_MAX_TIMEOUT = 10;
	
    public static String WRAPUP_END_TASK_ID = "wrapup_end_task_id_%s_%s";//wrapup_end_task_id_{enterpriseId}_{cno}
    public static String LIMIT_TIME_TASK_ID = "limit_time_task_id_%s";//wrapup_end_task_id_{uniqueId}
    
    public static final int LEAVE_CODE_COMPLETE = 1;
    public static final int LEAVE_CODE_ABANDON = 2;
    public static final int LEAVE_CODE_TIMEOUT = 3;
    public static final int LEAVE_CODE_EMPTY = 4;
    
}
