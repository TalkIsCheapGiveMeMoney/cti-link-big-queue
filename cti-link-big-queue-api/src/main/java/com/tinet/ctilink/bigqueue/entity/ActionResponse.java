package com.tinet.ctilink.bigqueue.entity;

import java.util.HashMap;
import java.util.Map;

/**
 * @author fengwei //
 * @date 16/4/25 15:49
 */
public class ActionResponse {
    private int code;
    private String msg;
    private Map<String, Object> values;

    public ActionResponse() {
        this.code = 0;
        this.msg = "ok";
        values = new HashMap<String, Object>();
    }

    /**
     * @param code
     * @param msg
     */
    public ActionResponse(int code, String msg) {
        this();
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public void setValues(Map<String, Object> values) {
        this.values = values;
    }

    @Override
    public String toString() {
        return "AmiActionReponse [code=" + code + ", msg=" + msg + ",values="+values+"]";
    }


    public static ActionResponse createFailResponse(int code, String msg) {
        ActionResponse response = new ActionResponse();
        response.setCode(code);
        response.setMsg(msg);
        return response;
    }

    public static ActionResponse createSuccessResponse() {
        ActionResponse response = new ActionResponse();
        response.setCode(0);
        response.setMsg("ok");
        return response;
    }
}
