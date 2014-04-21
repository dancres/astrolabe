package org.dancres.gossip.astrolabe;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.dancres.gossip.discovery.HostDetails;
import org.dancres.gossip.net.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An active object that accepts requires for Mibs from remote astrolabe nodes and populates them into the local
 * zone hierarchy.  It talks to a remote MibServlet instance using a particular 
 * format of URL, see {@link MibServlet} for details.
 */
public class MibDownloader {
	private static ExecutorService _executor = Executors.newFixedThreadPool(1);
	private static Logger _logger = LoggerFactory.getLogger(MibDownloader.class);
	
	public static void pull(HostDetails aContact, Set<MibSummary> aSummaries, Service aService) {
		if (aSummaries.size() == 0) {
			_logger.debug("Refused download, no summaries");
			return;
		}
	
		_logger.debug("Submitting summaries");
		_executor.execute(new UpdateTask(aContact, aSummaries, aService));
	}
	
	private static class UpdateTask implements Runnable {
		private Set<MibSummary> _summaries;
		private Service _service;
		private HostDetails _contact;
		
		UpdateTask(HostDetails aContact, Set<MibSummary> aSummaries, Service aService) {
			_summaries = aSummaries;
			_service = aService;
			_contact = aContact;
		}
		
		public void run() {
			_logger.debug("UpdateTask is active: " + _summaries.size());

            for (MibSummary mySummary : _summaries) {

				_logger.debug("Got a summary to process");
				
				HttpGet myGetMethod = null;
				HttpResponse myResponse = null;
				URL myURL = null;
				
				try {
                    Zone myLocal = Zones.getRoot().find(mySummary.getId());

                    if ((myLocal != null) && (myLocal.isSelf())) {
                        _logger.debug("Not going to pull a self zone: " + myLocal.getId());
                        continue;
                    }
					
					_logger.debug("Processing summary: " + mySummary);

					// Hit the MIB endpoint, includes the zone we're gossiping on behalf of and the representative
					// which generated the MIB we want
					// 
					myURL = new URL("http://" + _contact.getHostName() + ":" + _contact.getPort() +
							_service.getRoot(MibServlet.PATH) + mySummary.getId() + "?" + 
							MibServlet.REP_QUERY + "=" + mySummary.getRep());

					_logger.debug("URL for summmary is: " + myURL);
					
					myGetMethod = new HttpGet(myURL.toString());
					
					_logger.debug("Built get method");
					
					myResponse = _service.getClient().execute(myGetMethod);
					
					_logger.debug("Submitted request: " + myResponse.getStatusLine() + ", " + 
							myResponse.getStatusLine().getStatusCode());
					
					if (myResponse.getStatusLine().getStatusCode() != 200) {
						// A 404 response means the MIB expired or similar event, that's part of the protocol and ok.
						//
						if (myResponse.getStatusLine().getStatusCode() == 404) {
							myGetMethod.abort();
							continue;
						}
						
						// Response failed but does not form part of the expected protocol so....
						//
						throw new IOException("Failed to obtain Mib: " + myResponse.getStatusLine());
					}

					HttpEntity entity = myResponse.getEntity();

					if (entity != null) {
						Reader myReader = new InputStreamReader(entity.getContent());
						MibImpl myMib = new MibImpl(myReader);
						myReader.close();

						_logger.debug("Got back Mib: " + myMib.getAttributes());
						Zone myZone = Zones.getRoot().find(mySummary.getId());
						if (myZone == null) {
							_logger.debug("No zone exists, inserting one: " + mySummary.getId());
							myZone = new Zone(mySummary.getId());
							myZone.add(myMib);
							Zones.getRoot().add(myZone);
						} else {
							_logger.debug("Zone exists, inserting Mib: " + mySummary.getId());
							myZone.add(myMib);
						}

					} else {
						_logger.warn("Didn't get a response back - might be bad");
					}
				} catch (Throwable anE) {
					_logger.warn("Failed to recover Mib: " + myURL + " -> " + mySummary.getId() + " from " + mySummary.getRep(), anE);
					if (myResponse != null)
						myGetMethod.abort();
				}
			}
			
		}
	}
}
