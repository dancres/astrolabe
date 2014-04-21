package org.dancres.gossip.astrolabe;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles requests for children of a specified Zone.  It expects a URL of the form:
 * <ol>
 * <li>The ZoneServlet endpoint</li>
 * <li>An additional path item specifying the zone</li>
 * </ol>
 *
 * If the requested Zone is not present (perhaps because it's been expired out due to a lack of updates
 * from it's representative) a 404 http error is returned.
 */
public class ZoneServlet extends HttpServlet {
	public static final String MOUNT_POINT = "zone/*";
	public static final String PATH = "zone";

	private static Logger _logger = LoggerFactory.getLogger(ZoneServlet.class);

	ZoneServlet() {
	}

	protected void doGet(HttpServletRequest aReq, HttpServletResponse aResp)
		throws ServletException, IOException {

		_logger.info("Asked for zone children: " + aReq.getPathInfo() + " , returning it");

		Writer myWriter = aResp.getWriter();

        String myZoneId = aReq.getPathInfo();
        if (myZoneId == null)
            myZoneId = "";

		Zone myZone = Zones.getRoot().find(myZoneId);

		if (myZone == null) {
			aResp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

        for (Zone myChild : myZone.getChildren()) {
            myWriter.write(myChild.getName());
            myWriter.write("\n");
        }

		aResp.setStatus(HttpServletResponse.SC_OK);
    }
}
