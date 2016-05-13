package com.tinet.ctilink.bigqueue.servlet.v1;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;

import com.tinet.ctilink.bigqueue.service.imp.QueueServiceImp;

@WebServlet("/v1/queue/get")
public class QueueRna extends HttpServlet {

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
        String cno = req.getParameter("cno");
        
        queueService.rna(enterpriseId, qno, cno, uniqueId);
        out.flush();
        out.close();
    }
}