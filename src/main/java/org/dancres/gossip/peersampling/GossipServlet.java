package org.dancres.gossip.peersampling;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dancres.gossip.discovery.HostDetails;
import org.dancres.gossip.net.NetworkUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GossipServlet extends HttpServlet {
	private static Logger _logger = LoggerFactory.getLogger(GossipServlet.class);
	private View _view;
	
	public GossipServlet() {				
	}
	
	public GossipServlet(View aView) {
		_view = aView;
	}
	
	
	protected void doGet(HttpServletRequest aReq, HttpServletResponse aResp)
			throws ServletException, IOException {
		
		_logger.info("Asked for my view, returning it");

		Writer myWriter = aResp.getWriter();
		
		View myCurrent = _view.dup();
		
		// The port we were contacted on is the port we're publishing so we can use that in our HostDetails
		//
		myCurrent.add(new HostDetails(NetworkUtils.getWorkableAddress().getHostAddress(),
				aReq.getLocalPort()), 0);
		myCurrent.export(myWriter);	

		aResp.setStatus(HttpServletResponse.SC_OK);
	}


	protected void doPost(HttpServletRequest aReq, HttpServletResponse aResp)
			throws ServletException, IOException {
	
		ServletInputStream myStream = aReq.getInputStream();

		Reader myReader = new InputStreamReader(myStream);
		View myView = new View(myReader);
		
		myView.increaseHopCount();
		
		_logger.info("Received this view (post hop count increment)");
		ByteArrayOutputStream myBuffer = new ByteArrayOutputStream();
		Writer myWriter = new OutputStreamWriter(myBuffer);
		myView.export(myWriter);
		_logger.info(new String(myBuffer.toByteArray()));
		
		_view.merge(myView);

		// The port we were contacted on is the port we're publishing so we can use that in our HostDetails
		//
		_view.discard(new HostDetails(NetworkUtils.getWorkableAddress().getHostAddress(), aReq.getLocalPort()));
		
		_logger.info("Resulting view (post merge)");
		myBuffer = new ByteArrayOutputStream();
		myWriter = new OutputStreamWriter(myBuffer);
		_view.export(myWriter);
		_logger.info(new String(myBuffer.toByteArray()));
		
		aResp.setStatus(HttpServletResponse.SC_OK);
	}		
}

