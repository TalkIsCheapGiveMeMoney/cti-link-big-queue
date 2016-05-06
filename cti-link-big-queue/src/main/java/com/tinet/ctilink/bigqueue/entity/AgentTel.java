package com.tinet.ctilink.bigqueue.entity;

import java.util.Date;

public class AgentTel {

	    private Integer id;

	    private Integer enterpriseId;

	    private Integer agentId;

	    private String tel;

	    private Integer telType;

	    private String areaCode;

	    private Integer isValidity;

	    private Integer isBind;

	    private Date createTime;

	    public Integer getId() {
	        return id;
	    }

	    public void setId(Integer id) {
	        this.id = id;
	    }

	    public Integer getEnterpriseId() {
	        return enterpriseId;
	    }

	    public void setEnterpriseId(Integer enterpriseId) {
	        this.enterpriseId = enterpriseId;
	    }

	    public Integer getAgentId() {
	        return agentId;
	    }

	    public void setAgentId(Integer agentId) {
	        this.agentId = agentId;
	    }

	    public String getTel() {
	        return tel;
	    }

	    public void setTel(String tel) {
	        this.tel = tel == null ? null : tel.trim();
	    }

	    public Integer getTelType() {
	        return telType;
	    }

	    public void setTelType(Integer telType) {
	        this.telType = telType;
	    }

	    public String getAreaCode() {
	        return areaCode;
	    }

	    public void setAreaCode(String areaCode) {
	        this.areaCode = areaCode == null ? null : areaCode.trim();
	    }

	    public Integer getIsValidity() {
	        return isValidity;
	    }

	    public void setIsValidity(Integer isValidity) {
	        this.isValidity = isValidity;
	    }

	    public Integer getIsBind() {
	        return isBind;
	    }

	    public void setIsBind(Integer isBind) {
	        this.isBind = isBind;
	    }

	    public Date getCreateTime() {
	        return createTime;
	    }

	    public void setCreateTime(Date createTime) {
	        this.createTime = createTime;
	    }
}
