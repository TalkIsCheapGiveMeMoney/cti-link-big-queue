package com.tinet.ctilink.bigqueue;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.tinet.ctilink.cache.RedisService;

/**
 * @author fengwei //
 * @date 16/4/23 15:24
 */
public class RedisEventListener {
    @Autowired
    private RedisService redisService;

    public void handleMessage(Map<String, Object> event, String channel) {
        System.out.println("channel=" + channel);
    }

    /***
     * @param event
     */
    private void sendMessage(Map<String, Object> event) {
        //channel
        String channel = "channel:xxxevent";
        redisService.convertAndSend(channel, "");
    }
}
