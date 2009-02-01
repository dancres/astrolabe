package org.dancres.gossip.astrolabe;

import java.io.IOException;
import java.io.Writer;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventServlet extends HttpServlet {
	public static final String MOUNT_POINT = "events/*";
	public static final String PATH = "events";

	private static Logger _logger = LoggerFactory.getLogger(EventServlet.class);

    public EventServlet() {
    }

	protected void doGet(HttpServletRequest aReq, HttpServletResponse aResp)
		throws ServletException, IOException {

		_logger.info("Asked for events concerning: " + aReq.getPathInfo());

		Writer myWriter = aResp.getWriter();

        String myZoneId = aReq.getPathInfo();
        if (myZoneId == null)
            myZoneId = "";

        Zone myZone = Zones.getRoot().find(myZoneId);
		if (myZone == null) {
			aResp.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

        myZone.getQueue().export(myWriter);
		aResp.setStatus(HttpServletResponse.SC_OK);
	}
}
