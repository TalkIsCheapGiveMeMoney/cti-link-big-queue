package com.tinet.ctilink.bigqueue.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tinet.ctilink.bigqueue.entity.CallMember;
import com.tinet.ctilink.bigqueue.inc.BigQueueChannelVar;
import com.tinet.ctilink.bigqueue.inc.BigQueueConst;
import com.tinet.ctilink.bigqueue.service.imp.QueueServiceImp;
import com.tinet.ctilink.json.JSONObject;
import com.tinet.ctilink.util.ContextUtil;

@WebServlet("/interface/queue/findBest")
public class QueueFindBest extends HttpServlet {

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
        String customerNumber = req.getParameter("customerNumber");
        String queueRemeberMember = req.getParameter("queueRemeberMember");
        JSONObject res = new JSONObject();
        
        CallMember callMember = queueService.findBest(enterpriseId, qno, uniqueId, customerNumber, queueRemeberMember);
        if(callMember != null){
	        res.put(BigQueueChannelVar.QUEUE_CODE, BigQueueConst.QUEUE_CODE_ROUTE_OK);
			res.put(BigQueueChannelVar.QUEUE_DIAL_INTERFACE, callMember.getInterface());
			res.put(BigQueueChannelVar.QUEUE_DIAL_CNO, callMember.getCno());
        }else{
        	res.put(BigQueueChannelVar.QUEUE_CODE, BigQueueConst.QUEUE_CODE_ROUTE_FAIL);
        }

        out.print(res.toString());
        out.flush();
        out.close();
    }
}