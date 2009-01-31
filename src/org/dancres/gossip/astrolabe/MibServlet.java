package org.dancres.gossip.astrolabe;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles requests for MIBs.  It expects a URL of the form:
 * <ol>
 * <li>The MibServlet endpoint</li>
 * <li>An additional path item specifying the zone of the MIB</li>
 * <li>An optional query parameter specifying a specific representative's MIB</li>
 * </ol>
 * 
 * If the query parameter is not provided, it is assumed that the most recently updated MIB (in accordance with
 * the behaviour of <code>Zone.getMib()</code> is satisfactory.  If the requested MIB is not present (perhaps because
 * it's been expired out due to a lack of updates from it's representative) a 404 http error is returned.
 */
public class MibServlet extends HttpServlet {
	public static final String MOUNT_POINT = "mib/*";
	public static final String PATH = "mib";
	public static final String REP_QUERY = "rep";
	
	private static Logger _logger = LoggerFactory.getLogger(MibServlet.class);
	
	MibServlet() {
	}
	
	protected void doGet(HttpServletRequest aReq, HttpServletResponse aResp)
		throws ServletException, IOException {

		_logger.info("Asked for my mib: " + aReq.getPathInfo() + " from " + aReq.getQueryString() + ", returning it");

		Writer myWriter = aResp.getWriter();
		
		Zone myZone = Zones.getRoot().find(aReq.getPathInfo());
		if (myZone == null) {
			aResp.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		// Request for a specific representative's version of the MIB?
		//
		Mib myMib = null;
		
		if (aReq.getQueryString() == null) 
			myMib = myZone.getMib();
		else {
			Map<String, String> myQueries = ServletUtils.getQueryMap(aReq.getQueryString());
			String myRep = myQueries.get(REP_QUERY);
			
			if (myRep == null)
				myMib = myZone.getMib();
			else {
				myMib = myZone.getMib(myRep);
			}
		}
		

		// If the MIB isn't present
		//
		if (myMib == null) {
			aResp.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;					
		} else {
			myMib.export(myWriter);
		}
		
		aResp.setStatus(HttpServletResponse.SC_OK);		
	}	
}
