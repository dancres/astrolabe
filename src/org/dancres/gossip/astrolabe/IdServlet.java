package org.dancres.gossip.astrolabe;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Anyone receiving the endpoint (address and port) of this astrolabe node can request our astrolabe id via
 * http://address:port/${Main.ASTROLABE_ROOT}/id.
 */
public class IdServlet extends HttpServlet {
	public static final String MOUNT_POINT = "id";
	public static final String PATH = "id";
	
	private static Logger _logger = LoggerFactory.getLogger(IdServlet.class);

	IdServlet() {
	}
	
	protected void doGet(HttpServletRequest aReq, HttpServletResponse aResp)
		throws ServletException, IOException {
		
		_logger.info("Asked for my id, returning it");

		Writer myWriter = aResp.getWriter();
		myWriter.write(LocalID.get());
		
		aResp.setStatus(HttpServletResponse.SC_OK);		
	}
}
