package com.tinet.ctilink.bigqueue.servlet.v1;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;

import com.tinet.ctilink.bigqueue.inc.ChannelVar;
import com.tinet.ctilink.bigqueue.service.imp.QueueServiceImp;
import com.tinet.ctilink.json.JSONObject;

@WebServlet("/v1/queue/get")
public class QueueJoin extends HttpServlet {

	@Autowired
	QueueServiceImp queueService;
	
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
        String uniqueId = req.getParameter("uniqueId");
        String customerNumber = req.getParameter("customerNumber");
        Integer priority = Integer.parseInt(req.getParameter("priority"));
        Integer joinTime = Integer.parseInt(req.getParameter("joinTime"));
        
        JSONObject res = new JSONObject();
        Integer queueCode = queueService.join(enterpriseId, qno, customerNumber, uniqueId, priority, joinTime);
        res.put(ChannelVar.QUEUE_CODE, queueCode);
        out.print(res.toString());
        out.flush();
        out.close();
    }
}