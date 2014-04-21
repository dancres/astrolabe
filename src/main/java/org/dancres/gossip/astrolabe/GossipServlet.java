package org.dancres.gossip.astrolabe;

import org.dancres.gossip.net.ServletUtils;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dancres.gossip.discovery.HostDetails;
import org.dancres.gossip.net.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This servlet handles gossip requests from a remote astrolabe node.  A request URL contains:
 * <ol>
 * <li>The gossip servlet endpoint</li>
 * <li>An additional path element which is the zone on behalf of which the remote node is gossiping</li>
 * <li>A query string providing the remote node's address and port</li>
 * </ol>
 * 
 * The content of the post is a set of MibSummary's for the zone which we compare to the local set.  We return a
 * set of MibSummary's to the remote node listing any it doesn't have or for which there are more up to date versions.
 * We also identify those which are missing locally or are more up to date at the remote node and schedule a job to
 * request them. 
 */
public class GossipServlet extends HttpServlet {
	public static final String MOUNT_POINT = "gossip/*";
	public static final String PATH = "gossip";
	public static final String ASTROLABE_HOST = "host";
	public static final String ASTROLABE_PORT = "port";

	private static Logger _logger = LoggerFactory.getLogger(GossipServlet.class);

	private Service _service;
	
	GossipServlet(Service aService) {
		_service = aService;
	}

	protected void doPost(HttpServletRequest aReq, HttpServletResponse aResp)
		throws ServletException, IOException {
		
		Map<String, String> myQueryParams = ServletUtils.getQueryMap(aReq.getQueryString());
		
		_logger.info("Asked to gossip " + aReq.getPathInfo() + " with " + myQueryParams.get(ASTROLABE_HOST) +
				":" + myQueryParams.get(ASTROLABE_PORT));

		// Zone must be present as it's a common ancestor - but it could be root which means no pathinfo.
		//
		Zone myZone;
		if (aReq.getPathInfo() == null)
			myZone = Zones.getRoot();
		else
			myZone = Zones.getRoot().find(aReq.getPathInfo());
		
		Set<MibSummary> myLocalMibs = myZone.getMibSummaries();
		Set<MibSummary> myRemoteMibs = new MibSummaryCodec().getSummary(aReq.getReader());
		
		// Need the intersection
		Set<MibSummary> myIntersection = new HashSet<>(myLocalMibs);
		myIntersection.retainAll(myRemoteMibs);
		
		// Remove the common summaries - those with equal id and rep and identical isssued timestamps
		//
		myRemoteMibs.removeAll(myIntersection);
		myLocalMibs.removeAll(myIntersection);

		/* 
		 * What's left is summaries that are either older or younger than each other
		 * or exist remotely but not locally
		 * or exist locally but not remotely
		 *
		 * for remote set:
		 *   Iterate through it
		 *     see if summary matches (id and rep only) to the local set
		 *     if there's no match, leave summary
		 *     if summary matches and is older, remove it
		 *     if summary matches and is newer, remove it from other set
		 *     
		 * We require what's left in the remote MIBs, and we send back what's left in local 
		 */ 
		Iterator<MibSummary> myRemoteSeq = myRemoteMibs.iterator();
		while (myRemoteSeq.hasNext()) {
			MibSummary myMib = myRemoteSeq.next();
			MibSummary myMatchingMib = findMatch(myMib, myLocalMibs);
			
			if (myMatchingMib != null) {
				if (myMib.getIssued() < myMatchingMib.getIssued())
					myRemoteSeq.remove();
				else
					myLocalMibs.remove(myMatchingMib);
			}
		}
		
		_logger.info("Think I need these Mibs: " + myRemoteMibs);
		_logger.info("Think they need these Mibs: " + myLocalMibs);
		
		MibDownloader.pull(new HostDetails(myQueryParams.get(ASTROLABE_HOST), 
				Integer.parseInt(myQueryParams.get(ASTROLABE_PORT))), myRemoteMibs, _service);

		new MibSummaryCodec().putSummary(myLocalMibs, aResp.getWriter());
		
		aResp.setStatus(HttpServletResponse.SC_OK);		
	}

	private MibSummary findMatch(MibSummary aMib, Set<MibSummary> aLocalMibSummaries) {
        for (MibSummary mySummary : aLocalMibSummaries) {
			if ((aMib.getId().equals(mySummary.getId()) && (aMib.getRep().equals(mySummary.getRep())))) {
				return mySummary;
			}
		}
		
		return null;
	}

	public class IdRepComparator implements Comparator<MibSummary> {
		public int compare(MibSummary aSummary, MibSummary anotherSummary) {
			if (aSummary.getId().equals(anotherSummary.getId())) {
				return aSummary.getRep().compareTo(anotherSummary.getRep());
			} else {
				return aSummary.getId().compareTo(anotherSummary.getId());
			}
		}

	}
}
