package org.dancres.gossip.astrolabe;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.dancres.gossip.discovery.HostDetails;
import org.dancres.gossip.net.NetworkUtils;
import org.dancres.gossip.net.Service;
import org.dancres.gossip.peersampling.RemotePeerSampler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Gossip at a particular level:</p>
 * 
 * <p>"Each agent periodically runs the gossip algorithm. First, the agent updates 
 * the issued attribute in the MIB of its virtual system zone, and re-evaluates the 
 * AFCs that depend on this attribute. Next, the agent has to decide at which 
 * levels (in the zone tree) it will gossip. For this decision, the agent traverses the 
 * list of records in Figure 3. An agent gossips on behalf of those zones for which 
 * it is a contact, as calculated by the aggregation function for that zone. The rate 
 * of gossip at each level can be set individually (using the &config certificate 
 * described in Section 6.2). </p>
 *
 * <p>When it is time to gossip within some zone, the agent picks one of the child
 * zones, other than its own, from the list at random. Next the agent looks up the 
 * contacts attribute for this child zone, and picks a random contact agent from
 * the set of hosts in this attribute. (Gossips always are between different child 
 * zones, thus if there is only one child zone at a level, no gossip will occur.) The
 * gossiping agent then sends the chosen agent the id, rep, and issued attributes 
 * of all the child zones at that level, and does the same thing for the higher levels 
 * in the tree up until the root level. The recipient compares the information with 
 * the MIBs that it has in its memory, and can determine which of the gossiper's 
 * entries are out-of-date, and which of its own entries are. It sends the updates 
 * for the first category back to the gossiper, and requests updates for the second 
 * category."</p>
 * 
 * <p>Updating of zones and running of AFCs is done by {@link AggregationProcess}.  This class performs the gossip
 * steps for a particular zone.  It assumes that if it is being invoked, some other entity has determined that
 * it is appropriate to gossip at this level (specifically that we are a contact for this zone and should gossip.</p>
 * 
 * <p>This class connects via http to a GossipServlet instance using an appropriately formatted payload and url.  See 
 * {@link GossipServlet} for more details.
 */
public class ZoneGossiper {
    private static Logger _logger = LoggerFactory.getLogger(ZoneGossiper.class);
	
	private Service _service;
	private Zone _zone;
	private Random _random = new Random();
	private HostDetails _contactDetails;
	
	public ZoneGossiper(Zone aZone, Service aService) throws IOException {
		_zone = aZone;
		_service = aService;
    	_contactDetails = _service.getContactDetails();
	}
	
	/**
	 * @todo Fix up the random zone selection - could currently take a while if we got unlucky with the RNG results.
	 */
	public void run() {
		_logger.info("Looking to gossip about: " + _zone.getId() + "(" + _zone.getName() + ")");
		
		ArrayList<Zone> myChildZones = new ArrayList<Zone>(_zone.getChildren());
		
		// If there's only one child, it must be ours as we gossip along the self-chain and so no gossiping required
		//
		if (myChildZones.size() == 1) {
			_logger.info("Not going to gossip about (only child): " + _zone.getId() + "(" + _zone.getName() + ")");
			return;
		}
		
		Zone myChosen = null;
		
		// Pick some zone at random, that isn't ours
		//
		do {
			myChosen = myChildZones.get(_random.nextInt(myChildZones.size()));
		} while (myChosen.isSelf());
		
		// Pick a random contact
		//
		ArrayList myContacts = new ArrayList(myChosen.getMib().getContacts());
		HostDetails myContact = (HostDetails) myContacts.get(_random.nextInt(myContacts.size()));
		
		// Assemble relevant MIB details
		//
		Set<MibSummary> mySummaries = _zone.getMibSummaries();
		HttpResponse myResponse = null;
		Reader myReader = null;	        	
		HttpPost myPostMethod = null;
		URL myURL = null;
		
		try {
			// Hit the gossip endpoint, includes the zone we're gossiping on behalf of
			// 
			myURL = new URL("http://" + myContact.getHostName() + ":"
					+ myContact.getPort() + _service.getRoot(GossipServlet.PATH) + _zone.getId() + "?" +
					GossipServlet.ASTROLABE_HOST + "=" + _contactDetails.getHostName() + "&" + 
					GossipServlet.ASTROLABE_PORT + "=" + _contactDetails.getPort());

			_logger.info("Connecting for gossip with: " + myURL);
			
			myPostMethod = new HttpPost(myURL.toString());			
			MibSummaryCodec myCodec = new MibSummaryCodec();
			ByteArrayOutputStream myBuffer = new ByteArrayOutputStream();
			Writer myWriter = new OutputStreamWriter(myBuffer);
			myCodec.putSummary(mySummaries, myWriter);
			myWriter.close();

			ByteArrayEntity myEntity = new ByteArrayEntity(myBuffer.toByteArray());
			myEntity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "java/app"));
			myPostMethod.setEntity(myEntity);

			myResponse = _service.getClient().execute(myPostMethod);
			if (myResponse.getStatusLine().getStatusCode() != 200) {
				myPostMethod.abort();
				throw new IOException("Failed to post Mib summary: " + myResponse.getStatusLine());
			}

			/*
			 *  Need to process response which will be another set of mib summaries. 
			 */
			HttpEntity entity = myResponse.getEntity();

			if (entity != null) {
				myReader = new InputStreamReader(entity.getContent());
				mySummaries = myCodec.getSummary(myReader);
				myReader.close();
				
				MibDownloader.pull(myContact, mySummaries, _service);
			} else {
				_logger.warn("Didn't get a response back - might be bad");
			}
		} catch (Exception anE) {
			_logger.warn("Failed to gossip summary: " + myURL, anE);
			if (myResponse != null)
				myPostMethod.abort();
		}
	}
}
