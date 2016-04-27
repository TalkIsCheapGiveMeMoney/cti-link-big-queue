package com.tinet.ctilink.bigqueue.entity;

public class CallMember {
	private String cno;
	private Integer penalty;
	private Integer calls;
	private Integer lastCall;
	private Integer metic;
	private String _interface;
	
	public String getCno() {
		return cno;
	}
	public void setCno(String cno) {
		this.cno = cno;
	}
	public Integer getPenalty() {
		return penalty;
	}
	public void setPenalty(Integer penalty) {
		this.penalty = penalty;
	}
	public Integer getCalls() {
		return calls;
	}
	public void setCalls(Integer calls) {
		this.calls = calls;
	}
	public Integer getMetic() {
		return metic;
	}
	public void setMetic(Integer metic) {
		this.metic = metic;
	}
	
	public String getInterface(){
		return _interface;
	}
	public void setInterface(String _interface){
		this._interface = _interface;
	}
	public Integer getLastCall() {
		return lastCall;
	}
	public void setLastCall(Integer lastCall) {
		this.lastCall = lastCall;
	}
	
}
