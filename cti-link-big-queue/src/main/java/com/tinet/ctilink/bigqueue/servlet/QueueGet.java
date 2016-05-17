package com.tinet.ctilink.bigqueue.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;

import com.tinet.ctilink.bigqueue.service.imp.QueueServiceImp;
import com.tinet.ctilink.conf.model.Queue;
import com.tinet.ctilink.json.JSONObject;
import com.tinet.ctilink.util.ContextUtil;

@WebServlet("/interface/queue/get")
public class QueueGet extends HttpServlet {

	QueueServiceImp queueService;
	@Override
	public void init() throws ServletException {
		queueService = ContextUtil.getBean(QueueServiceImp.class);
	}
	
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();
        
        String enterpriseId = req.getParameter("enterpriseId");
        String qno = req.getParameter("qno");
        
        Queue queue = queueService.getFromConfCache(enterpriseId, qno);
        
        if(queue != null){
        	JSONObject object = new JSONObject();
        	object.put("queue_qno", queue.getQno());
        	object.put("queue_moh", queue.getMusicClass());
        	object.put("queue_timeout", queue.getQueueTimeout());
        	object.put("queue_say_agentno", queue.getSayAgentno());
        	object.put("queue_member_timeout", queue.getMemberTimeout());
        	object.put("queue_retry", queue.getRetry());
        	object.put("queue_vip_support", queue.getVipSupport());
        	object.put("queue_announce_sound", queue.getAnnounceSound());
        	object.put("queue_announce_sound_frequency", queue.getAnnounceSoundFrequency());
        	object.put("queue_announce_sound_file", queue.getAnnounceSoundFile());
        	object.put("queue_announce_position", queue.getAnnouncePosition());
        	object.put("queue_announce_position_youarenext", queue.getAnnouncePositionYouarenext());
        	object.put("queue_announce_position_frequency", queue.getAnnouncePositionFrequency());
        	object.put("queue_announce_position_param", queue.getAnnouncePositionParam());

        	out.print(object.toString());
        }
        out.flush();
        out.close();
    }
}