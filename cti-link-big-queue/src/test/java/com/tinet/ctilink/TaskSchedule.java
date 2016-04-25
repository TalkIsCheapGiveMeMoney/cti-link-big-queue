package com.tinet.ctilink;

import com.github.davidmarquis.redisscheduler.RedisTaskScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Calendar;
import java.util.Date;

/**
 * @author fengwei //
 * @date 16/4/23 15:54
 */
public class TaskSchedule {
    private static ClassPathXmlApplicationContext context;

    static {
        context = new ClassPathXmlApplicationContext("classpath:spring/*.xml");
    }

    public void test() {
        RedisTaskScheduler redisTaskScheduler =
        context.getBean(RedisTaskScheduler.class);
        RedisTemplate redisTemplate = context.getBean(RedisTemplate.class);
        System.out.println(redisTemplate);
        Date date = new Date();
        date.setTime(1461398936);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        redisTaskScheduler.schedule("Mytask", calendar);
    }


    public static void main(String[] args) {
        (new TaskSchedule()).test();
    }
}
