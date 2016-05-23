package com.tinet.ctilink.bigqueue.agent.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tinet.ctilink.bigqueue.entity.ActionResponse;
import com.tinet.ctilink.bigqueue.service.imp.AgentServiceImp;
import com.tinet.ctilink.conf.model.Queue;
import com.tinet.ctilink.json.JSONObject;
import com.tinet.ctilink.util.ContextUtil;

@WebServlet("/interface/agent/pause")
public class AgentPause extends HttpServlet {

	AgentServiceImp agentService;
	@Override
	public void init() throws ServletException {
		agentService = ContextUtil.getBean(AgentServiceImp.class);
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
        
        
        Map params = new HashMap();
        params.put("enterpriseId", req.getParameter("enterpriseId"));
        params.put("cno", req.getParameter("cno"));
        params.put("type", req.getParameter("type"));
        params.put("description", req.getParameter("description"));
        
        ActionResponse res = agentService.pause(params);
        if(res != null){
        	out.print(res.toString());
        }
        out.flush();
        out.close();
    }
}