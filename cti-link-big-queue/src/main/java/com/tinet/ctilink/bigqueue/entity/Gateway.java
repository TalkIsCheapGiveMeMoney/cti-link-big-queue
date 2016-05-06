package com.tinet.ctilink.bigqueue.entity;

import java.util.Date;


public class Gateway {

    private Integer id;

    private String name;

    private String prefix;

    private String ipAddr;

    private Integer port;

    private String areaCode;

    private String description;

    private Integer callLimit;

    private String disallow;

    private String allow;

    private String dtmfMode;

    private Date createTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix == null ? null : prefix.trim();
    }

    public String getIpAddr() {
        return ipAddr;
    }

    public void setIpAddr(String ipAddr) {
        this.ipAddr = ipAddr == null ? null : ipAddr.trim();
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getAreaCode() {
        return areaCode;
    }

    public void setAreaCode(String areaCode) {
        this.areaCode = areaCode == null ? null : areaCode.trim();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? null : description.trim();
    }

    public Integer getCallLimit() {
        return callLimit;
    }

    public void setCallLimit(Integer callLimit) {
        this.callLimit = callLimit;
    }

    public String getDisallow() {
        return disallow;
    }

    public void setDisallow(String disallow) {
        this.disallow = disallow == null ? null : disallow.trim();
    }

    public String getAllow() {
        return allow;
    }

    public void setAllow(String allow) {
        this.allow = allow == null ? null : allow.trim();
    }

    public String getDtmfMode() {
        return dtmfMode;
    }

    public void setDtmfMode(String dtmfMode) {
        this.dtmfMode = dtmfMode == null ? null : dtmfMode.trim();
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}