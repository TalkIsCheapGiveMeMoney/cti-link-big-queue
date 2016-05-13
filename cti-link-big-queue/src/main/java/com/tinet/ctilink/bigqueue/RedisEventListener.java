package com.tinet.ctilink.bigqueue;

import com.tinet.ctilink.cache.RedisService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

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
