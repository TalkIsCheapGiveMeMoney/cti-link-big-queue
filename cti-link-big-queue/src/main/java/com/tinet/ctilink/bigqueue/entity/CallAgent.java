package com.tinet.ctilink.bigqueue.entity;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tinet.ctilink.util.SipMediaServerUtil;
@JsonIgnoreProperties(ignoreUnknown = true)
public class CallAgent {

	public static final Integer MONITOR_TYPE_SPY = 1;
	public static final Integer MONITOR_TYPE_WHISPER = 2;
	public static final Integer MONITOR_TYPE_THREEWAY = 3;
	
	public static final Integer LOGIN_TYPE_FRONT = 1;
	public static final Integer LOGIN_TYPE_BACKEND = 2;
	
	private Integer loginType;
	private String bindTel;
	private Integer bindType;
	private String _interface;
	private String name;
	private String cno;
	private Integer enterpriseId;
	private String currentChannel;
	private String currentChannelUniqueId;
	private String bridgedChannel;
	private String bridgedChannelUniqueId;
	private Integer currentSipId;
	private Integer currentDetailCallType;
	private Integer currentCallType;
	private String currentCustomerNumber;
	private Integer currentCustomerNumberType;
	private String currentCustomerNumberAreaCode;
	private String currentNumberTrunk;
	private String currentHotline;
	private String currentQno;
	private Integer pauseType;
	private String pauseDescription;
	private String busyDescription;
	private String monitoredObject;
	private Integer monitoredObjectType;
	private String consultChannel; // 咨询时对方的channel
	private String consulterCno;
	private String transferCno;
	
	private String spyChannel; // 监听者的channel
	private String whisperChannel; // 耳语者channel
	private String threewayChannel; // 三方channel
	private String bargeChannel;
	private Integer loginTime;
	private boolean rnaPause;
	private Integer rnaPauseType;
	private String rnaPauseDescription;
	
	public Integer getLoginType() {
		return loginType;
	}
	public void setLoginType(Integer loginType) {
		this.loginType = loginType;
	}
	public String getBindTel() {
		return bindTel;
	}
	public void setBindTel(String bindTel) {
		this.bindTel = bindTel;
	}
	public String getInterface() {
		return _interface;
	}
	public void setInterface(String _interface) {
		this._interface = _interface;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getCno() {
		return cno;
	}
	public void setCno(String cno) {
		this.cno = cno;
	}
	public Integer getEnterpriseId() {
		return enterpriseId;
	}
	public void setEnterpriseId(Integer enterpriseId) {
		this.enterpriseId = enterpriseId;
	}
	public String getCurrentChannel() {
		return currentChannel;
	}
	public void setCurrentChannel(String currentChannel) {
		this.currentChannel = currentChannel;
		
	}
	public String getCurrentChannelUniqueId() {
		return currentChannelUniqueId;
	}
	public void setCurrentChannelUniqueId(String currentChannelUniqueId) {
		this.currentChannelUniqueId = currentChannelUniqueId;
		this.currentSipId = SipMediaServerUtil.getSipId(currentChannelUniqueId);
	}
	public String getBridgedChannel() {
		return bridgedChannel;
	}
	public void setBridgedChannel(String bridgedChannel) {
		this.bridgedChannel = bridgedChannel;
	}
	public String getBridgedChannelUniqueId() {
		return bridgedChannelUniqueId;
	}
	public void setBridgedChannelUniqueId(String bridgedChannelUniqueId) {
		this.bridgedChannelUniqueId = bridgedChannelUniqueId;
	}
	public Integer getCurrentSipId() {
		return currentSipId;
	}

	public Integer getBindType() {
		return bindType;
	}
	public void setBindType(Integer bindType) {
		this.bindType = bindType;
	}
	public Integer getCurrentDetailCallType() {
		return currentDetailCallType;
	}
	public void setCurrentDetailCallType(Integer currentDetailCallType) {
		this.currentDetailCallType = currentDetailCallType;
	}
	public Integer getCurrentCallType() {
		return currentCallType;
	}
	public void setCurrentCallType(Integer currentCallType) {
		this.currentCallType = currentCallType;
	}
	public String getCurrentCustomerNumber() {
		return currentCustomerNumber;
	}
	public void setCurrentCustomerNumber(String currentCustomerNumber) {
		this.currentCustomerNumber = currentCustomerNumber;
	}
	public Integer getCurrentCustomerNumberType() {
		return currentCustomerNumberType;
	}
	public void setCurrentCustomerNumberType(Integer currentCustomerNumberType) {
		this.currentCustomerNumberType = currentCustomerNumberType;
	}
	public String getCurrentCustomerNumberAreaCode() {
		return currentCustomerNumberAreaCode;
	}
	public void setCurrentCustomerNumberAreaCode(String currentCustomerNumberAreaCode) {
		this.currentCustomerNumberAreaCode = currentCustomerNumberAreaCode;
	}
	public String getCurrentNumberTrunk() {
		return currentNumberTrunk;
	}
	public void setCurrentNumberTrunk(String currentNumberTrunk) {
		this.currentNumberTrunk = currentNumberTrunk;
	}
	public String getCurrentHotline() {
		return currentHotline;
	}
	public void setCurrentHotline(String currentHotline) {
		this.currentHotline = currentHotline;
	}
	
	public String getCurrentQno() {
		return currentQno;
	}
	public void setCurrentQno(String currentQno) {
		this.currentQno = currentQno;
	}
	public String getPauseDescription() {
		return pauseDescription;
	}
	public void setPauseDescription(String pauseDescription) {
		this.pauseDescription = pauseDescription;
	}
	public String getBusyDescription() {
		return busyDescription;
	}
	public void setBusyDescription(String busyDescription) {
		this.busyDescription = busyDescription;
	}
	public String getMonitoredType() {
		if (StringUtils.isNotEmpty(spyChannel)) {
			return "spy";
		} else if (StringUtils.isNotEmpty(whisperChannel)) {
			return "whisper";
		} else if (StringUtils.isNotEmpty(threewayChannel)) {
			return "threeway";
		}
		return "";
	}
	public String getMonitoredObject() {
		return monitoredObject;
	}
	public void setMonitoredObject(String monitoredObject) {
		this.monitoredObject = monitoredObject;
	}
	public Integer getMonitoredObjectType() {
		return monitoredObjectType;
	}
	public void setMonitoredObjectType(Integer monitoredObjectType) {
		this.monitoredObjectType = monitoredObjectType;
	}
	public Integer getLoginTime() {
		return loginTime;
	}
	public void setLoginTime(Integer loginTime) {
		this.loginTime = loginTime;
	}
	public Integer getPauseType() {
		return pauseType;
	}
	public void setPauseType(Integer pauseType) {
		this.pauseType = pauseType;
	}
	
	public String getBargeChannel() {
		return bargeChannel;
	}
	public void setBargeChannel(String bargeChannel) {
		this.bargeChannel = bargeChannel;
	}
	
	public String getConsultChannel() {
		return consultChannel;
	}
	public void setConsultChannel(String consultChannel) {
		this.consultChannel = consultChannel;
	}
	public String getSpyChannel() {
		return spyChannel;
	}
	public void setSpyChannel(String spyChannel) {
		this.spyChannel = spyChannel;
	}
	public String getWhisperChannel() {
		return whisperChannel;
	}
	public void setWhisperChannel(String whisperChannel) {
		this.whisperChannel = whisperChannel;
	}
	public String getThreewayChannel() {
		return threewayChannel;
	}
	public void setThreewayChannel(String threewayChannel) {
		this.threewayChannel = threewayChannel;
	}
	
	public boolean getRnaPause() {
		return rnaPause;
	}
	public void setRnaPause(boolean rnaPause) {
		this.rnaPause = rnaPause;
	}
	public Integer getRnaPauseType() {
		return rnaPauseType;
	}
	public void setRnaPauseType(Integer rnaPauseType) {
		this.rnaPauseType = rnaPauseType;
	}
	public String getRnaPauseDescription() {
		return rnaPauseDescription;
	}
	public void setRnaPauseDescription(String rnaPauseDescription) {
		this.rnaPauseDescription = rnaPauseDescription;
	}
	
	public String getConsulterCno() {
		return consulterCno;
	}
	public void setConsulterCno(String consulterCno) {
		this.consulterCno = consulterCno;
	}
	public String getTransferCno() {
		return transferCno;
	}
	public void setTransferCno(String transferCno) {
		this.transferCno = transferCno;
	}
	public void clearCall(){
		currentChannel = "";
		currentChannelUniqueId = "";
		bridgedChannel = "";
		bridgedChannelUniqueId = "";
		currentSipId = -1;
		currentDetailCallType = 0;
		currentCallType = 0;
		currentCustomerNumber = "";
		currentCustomerNumberType = 0;
		currentCustomerNumberAreaCode = "";
		currentNumberTrunk = "";
		currentHotline = "";
		currentQno = "";
		busyDescription = "";
		monitoredObject = "";
		monitoredObjectType = 0;
		consultChannel = "";
		spyChannel = "";
		whisperChannel = "";
		threewayChannel = "";
		bargeChannel = "";
		consulterCno = "";
		transferCno = "";
	}
}
