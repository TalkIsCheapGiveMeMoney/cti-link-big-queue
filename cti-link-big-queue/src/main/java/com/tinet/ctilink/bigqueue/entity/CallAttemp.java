package com.tinet.ctilink.bigqueue.entity;

public class CallAttemp {
	private CallMember callMember;
	private boolean stillGoing;
	
	public CallMember getCallMember() {
		return callMember;
	}
	public void setCallMember(CallMember callMember) {
		this.callMember = callMember;
	}
	public boolean isStillGoing() {
		return stillGoing;
	}
	public void setStillGoing(boolean stillGoing) {
		this.stillGoing = stillGoing;
	}
	
	 
}
