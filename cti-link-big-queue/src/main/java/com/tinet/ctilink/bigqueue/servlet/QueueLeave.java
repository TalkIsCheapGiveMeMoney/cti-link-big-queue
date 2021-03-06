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
import com.tinet.ctilink.json.JSONObject;
import com.tinet.ctilink.util.ContextUtil;

@WebServlet("/interface/queue/leave")
public class QueueLeave extends HttpServlet {

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
        String uniqueId = req.getParameter("uniqueId");
        Integer leaveCode = Integer.parseInt(req.getParameter("leaveCode"));
        String cno = req.getParameter("cno");
        
        queueService.leave(enterpriseId, qno, uniqueId, leaveCode, cno);
        JSONObject object = new JSONObject();
        object.put("result", "0");
        out.print(object.toString());
        out.flush();
        out.close();
    }
}