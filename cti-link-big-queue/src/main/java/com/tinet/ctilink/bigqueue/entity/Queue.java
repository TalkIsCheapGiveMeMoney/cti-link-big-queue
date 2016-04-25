package com.tinet.ctilink.bigqueue.entity;

import java.util.Date;


public class Queue {
    private Integer id;

    private Integer enterpriseId;

    private String qno;

    private String description;

    private String musicClass;

    private Integer queueTimeout;

    private Boolean sayAgentno;

    private Integer memberTimeout;

    private Integer retry;

    private Integer wrapupTime;

    private Integer maxLen;

    private String strategy;

    private Integer serviceLevel;

    private Integer weight;

    private Integer vipSupport;

    private String joinEmpty;

    private Integer announceYouarenext;

    private Integer announceLessThen;

    private Integer announceLargeThen;

    private String announceThankyou;

    private Integer announceFrequency;

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

    public String getQno() {
        return qno;
    }

    public void setQno(String qno) {
        this.qno = qno == null ? null : qno.trim();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? null : description.trim();
    }

    public String getMusicClass() {
        return musicClass;
    }

    public void setMusicClass(String musicClass) {
        this.musicClass = musicClass == null ? null : musicClass.trim();
    }

    public Integer getQueueTimeout() {
        return queueTimeout;
    }

    public void setQueueTimeout(Integer queueTimeout) {
        this.queueTimeout = queueTimeout;
    }

    public Boolean getSayAgentno() {
        return sayAgentno;
    }

    public void setSayAgentno(Boolean sayAgentno) {
        this.sayAgentno = sayAgentno;
    }

    public Integer getMemberTimeout() {
        return memberTimeout;
    }

    public void setMemberTimeout(Integer memberTimeout) {
        this.memberTimeout = memberTimeout;
    }

    public Integer getRetry() {
        return retry;
    }

    public void setRetry(Integer retry) {
        this.retry = retry;
    }

    public Integer getWrapupTime() {
        return wrapupTime;
    }

    public void setWrapupTime(Integer wrapupTime) {
        this.wrapupTime = wrapupTime;
    }

    public Integer getMaxLen() {
        return maxLen;
    }

    public void setMaxLen(Integer maxLen) {
        this.maxLen = maxLen;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy == null ? null : strategy.trim();
    }

    public Integer getServiceLevel() {
        return serviceLevel;
    }

    public void setServiceLevel(Integer serviceLevel) {
        this.serviceLevel = serviceLevel;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public Integer getVipSupport() {
        return vipSupport;
    }

    public void setVipSupport(Integer vipSupport) {
        this.vipSupport = vipSupport;
    }

    public String getJoinEmpty() {
        return joinEmpty;
    }

    public void setJoinEmpty(String joinEmpty) {
        this.joinEmpty = joinEmpty == null ? null : joinEmpty.trim();
    }

    public Integer getAnnounceYouarenext() {
        return announceYouarenext;
    }

    public void setAnnounceYouarenext(Integer announceYouarenext) {
        this.announceYouarenext = announceYouarenext;
    }

    public Integer getAnnounceLessThen() {
        return announceLessThen;
    }

    public void setAnnounceLessThen(Integer announceLessThen) {
        this.announceLessThen = announceLessThen;
    }

    public Integer getAnnounceLargeThen() {
        return announceLargeThen;
    }

    public void setAnnounceLargeThen(Integer announceLargeThen) {
        this.announceLargeThen = announceLargeThen;
    }

    public String getAnnounceThankyou() {
        return announceThankyou;
    }

    public void setAnnounceThankyou(String announceThankyou) {
        this.announceThankyou = announceThankyou == null ? null : announceThankyou.trim();
    }

    public Integer getAnnounceFrequency() {
        return announceFrequency;
    }

    public void setAnnounceFrequency(Integer announceFrequency) {
        this.announceFrequency = announceFrequency;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
