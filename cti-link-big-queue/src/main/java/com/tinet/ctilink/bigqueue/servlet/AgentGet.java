package com.tinet.ctilink.bigqueue.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.github.pagehelper.StringUtil;
import com.tinet.ctilink.bigqueue.entity.ActionResponse;
import com.tinet.ctilink.bigqueue.entity.CallAgent;
import com.tinet.ctilink.bigqueue.inc.BigQueueChannelVar;
import com.tinet.ctilink.bigqueue.inc.BigQueueConst;
import com.tinet.ctilink.bigqueue.service.imp.AgentServiceImp;
import com.tinet.ctilink.bigqueue.service.imp.MemberServiceImp;
import com.tinet.ctilink.bigqueue.service.imp.QueueServiceImp;
import com.tinet.ctilink.conf.model.Agent;
import com.tinet.ctilink.conf.model.Queue;
import com.tinet.ctilink.json.JSONObject;
import com.tinet.ctilink.util.AgentUtil;
import com.tinet.ctilink.util.ContextUtil;
import com.tinet.ctilink.util.RedisLock;

@WebServlet("/interface/agent/get")
public class AgentGet extends HttpServlet {

	AgentServiceImp agentService;
	MemberServiceImp memberService;
	@Override
	public void init() throws ServletException {
		agentService = ContextUtil.getBean(AgentServiceImp.class);
		memberService = ContextUtil.getBean(MemberServiceImp.class);
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
        String cno = req.getParameter("cno");
        
       
        //先获取lock memberService.lockMember(enterpriseId, cno);
  		RedisLock bargedMemberLock = memberService.lockMember(enterpriseId, cno);
  		if(bargedMemberLock != null){
  			try{
  				CallAgent callAgent = agentService.getCallAgent(enterpriseId, cno);
  				if(callAgent != null){
  			        Integer deviceStatus = memberService.getDeviceStatus(enterpriseId, cno);
		        	if(deviceStatus.equals(BigQueueConst.MEMBER_DEVICE_STATUS_LOCKED)){
		        		JSONObject object = new JSONObject();
  			        	object.put(BigQueueChannelVar.DIAL_INTERFACE, callAgent.getInterface());
  			        	object.put(BigQueueChannelVar.DIAL_TEL, callAgent.getBindTel());
  			        	object.put(BigQueueChannelVar.DIAL_GW_IP, AgentUtil.getGwIp(callAgent.getInterface()));
  						Agent agent = agentService.getAgent(enterpriseId, cno);
  			        	object.put(BigQueueChannelVar.AGENT_IS_RECORD, agent.getIbRecord());
  			        	out.print(object.toString());
  			        	return;
		        	}
  				}
  			}catch(Exception e){
  				e.printStackTrace();
  			}finally{
  				memberService.unlockMember(bargedMemberLock);
  			}
  		}else{
  		}
        JSONObject object = new JSONObject();
        object.put("result", "-1");
        out.print(object.toString());	
        out.flush();
        out.close();
    }
}