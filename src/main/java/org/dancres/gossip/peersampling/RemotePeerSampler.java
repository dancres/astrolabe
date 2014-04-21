package org.dancres.gossip.peersampling;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.URL;
import java.util.Properties;
import java.util.Random;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.dancres.gossip.core.Peer;
import org.dancres.gossip.discovery.DiscoveryListener;
import org.dancres.gossip.discovery.HostDetails;
import org.dancres.gossip.discovery.RegistrarFactory;
import org.dancres.gossip.net.NetworkUtils;
import org.dancres.gossip.net.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemotePeerSampler implements DiscoveryListener, PeerSampler {
	private static String ROOT =  "peer";
	private static String TYPE = "_peersampler._tcp";
    private static Logger _logger = LoggerFactory.getLogger(RemotePeerSampler.class);

    private View _view;
    private Gossiper _gossiper;

	private Service _service;
	
	public static void main(String anArgs[]) {
		try {
			Service myService = new Service("/gossip");
			RemotePeerSampler mySampler = new RemotePeerSampler(myService, 16, true, null);

			if (anArgs.length == 2) {
				String myHost = anArgs[0];
				int myPort = Integer.parseInt(anArgs[1]);

				mySampler.seed(new HostDetails(myHost, myPort));
			}

			Object myBlocker = new Object();
			try {
				synchronized(myBlocker) {
                    while(true) {
                        myBlocker.wait();
                    }
				}
			} catch (InterruptedException anIE) {
                anIE.printStackTrace(System.err);
			}
		} catch (Exception anE) {
			// Any exception is logged where it happened, so we just exit silently
			//
            anE.printStackTrace(System.err);
			return;
		}
	}
	
	/**
	 * 
	 * @param aService
	 * @param aSize is the maximum number of nodes to track in this sampler
	 * @param doLocalAdvert determines whether or not the sampler advertises itself via multicast.  If this option is
	 * not enabled then the PeerSampler will need seeding.
	 * @param anAttrs contains a list of attributes to publish via multicast advert should it be enabled or 
	 * <code>null</code>
	 * @throws Exception if initialisation fails
	 */
	public RemotePeerSampler(Service aService, int aSize, boolean doLocalAdvert, Properties anAttrs) throws Exception {
		_service = aService;
		_view = new View(aSize);
		
        _service.add(new GossipServlet(_view), ROOT);
        
        // Only advertise when we're configured and all connected up
        //
        if (doLocalAdvert) {
        	_logger.info("Doing local advert as: " + TYPE + " : " + NetworkUtils.getWorkableInterface() + " : " +
        			_service.getPort());
        	
        	RegistrarFactory.getRegistrar().sample(TYPE, this);
        	RegistrarFactory.getRegistrar().register(TYPE, NetworkUtils.getWorkableInterface(), 
        			_service.getPort(), anAttrs);
        }
        
		_gossiper = new Gossiper();
		_gossiper.start();
	}

	public void found(HostDetails aHostDetails) {
		try {
			_logger.info("Discovered: " + aHostDetails);
			
			// Don't add ourselves
			//
			String myHost = aHostDetails.getHostName();
			InetAddress myAddr = InetAddress.getByName(myHost);
			
			if ((NetworkUtils.isLocalInterface(myAddr)) && (_service.getPort() == aHostDetails.getPort())) {				
				_logger.warn("Dumping our own address from view");
				return;
			}

			// Now pick a suitable address from those this host has (which will be one on our chosen subnet)
			//
            for (InetAddress myCandidate : InetAddress.getAllByName(myHost)) {
				myAddr = myCandidate;
				
				if (NetworkUtils.isWorkableSubnet(myAddr))
					break;
			}
			
			HostDetails mySanitized = new HostDetails(myAddr.getHostAddress(), aHostDetails.getPort());
			_logger.info("Updating view: " + mySanitized);

			_view.add(mySanitized, 1);
		} catch (IOException anIOE) {
			_logger.warn("Failed to add host", anIOE);
		}
	}

	public Peer getPeer() {
		return _view.getPeer();
	}
	
	public void seed(HostDetails aDetails) throws Exception {
		try {
			URL myURL = new URL("http://" + aDetails.getHostName() + ":"
					+ aDetails.getPort() + _service.getRoot(ROOT));
			
			HttpGet myGet = new HttpGet(myURL.toString());
			HttpResponse myResponse = _service.getClient().execute(myGet);
			
		    if (myResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
		    	throw new IOException("Connection failed: " + myResponse);
		    
		    HttpEntity myEntity = myResponse.getEntity();
		    InputStream myStream = myEntity.getContent();
			View mySeedView = new View(new InputStreamReader(myStream));
			
			myStream.close();
			
			mySeedView.increaseHopCount();

			_logger.info("Seeding from this view (post hop count increment)");
			ByteArrayOutputStream myBuffer = new ByteArrayOutputStream();
			Writer myWriter = new OutputStreamWriter(myBuffer);
			mySeedView.export(myWriter);
			_logger.info(new String(myBuffer.toByteArray()));
			
			/*
			 *  Just in case - if we're using seeding then chances are we can't find other nodes but if local
			 *  discovery is partially working, we might get our own address back
			 */
			mySeedView.discard(new HostDetails(NetworkUtils.getWorkableAddress().getHostAddress(),
					_service.getPort()));
			_view.merge(mySeedView);

			_logger.info("Resulting view (post merge)");
			myBuffer = new ByteArrayOutputStream();
			myWriter = new OutputStreamWriter(myBuffer);
			_view.export(myWriter);
			_logger.info(new String(myBuffer.toByteArray()));
			
		} catch (Exception anE) {
			_logger.error("Failed to seed", anE);
		}	
	}
	
	private class Gossiper extends Thread {
		/*
		 * @todo If we can't connect to this peer, should we junk it, increment a fail count and try a few more times
		 * or something else?
		 */
		public void run() {
			Random myRandom = new Random();
			
			while (true) {
				try {
					Thread.sleep(60000 + (myRandom.nextInt(30) * 1000));
				} catch (InterruptedException anIE) {
					_logger.error("Unexpected exception", anIE);
				}
				
				try {
					Peer myPeer = _view.getPeer();
					
					if (myPeer == null)
						continue;
					
					_logger.info("Gossiping with: " + myPeer);
					
					View myCurrent = _view.dup();
					myCurrent.add(new HostDetails(NetworkUtils.getWorkableAddress().getHostAddress(),
							_service.getPort()), 0);

					URL myURL = new URL("http://" + myPeer.getHostName() + ":"
							+ myPeer.getPort() + _service.getRoot(ROOT));

					HttpResponse myResponse = null;
					
					try {
						HttpPost myPostMethod = new HttpPost(myURL.toString());
						
						_logger.info("Exporting view as follows:");
						ByteArrayOutputStream myBuffer = new ByteArrayOutputStream();
						Writer myWriter = new OutputStreamWriter(myBuffer);
						myCurrent.export(myWriter);
						_logger.info(new String(myBuffer.toByteArray()));
						
						myBuffer = new ByteArrayOutputStream();
						myWriter = new OutputStreamWriter(myBuffer);
						
						myCurrent.export(myWriter);

						ByteArrayEntity myEntity = new ByteArrayEntity(myBuffer.toByteArray());
						myEntity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "java/app"));
						myPostMethod.setEntity(myEntity);

						myResponse = _service.getClient().execute(myPostMethod);

						if (myResponse.getStatusLine().getStatusCode() != 200)
							throw new IOException("Upload failed: " + myResponse.getStatusLine());

					} finally {
						if (myResponse != null) {
							HttpEntity myEntity = myResponse.getEntity();
							if (myEntity != null) {
								myEntity.consumeContent();
								myEntity.getContent().close();
								_logger.info("Closed entity");
							} else
								_logger.info("No entity to close");
						}
					}	
				} catch (Exception anE) {
					_logger.error("Failed to gossip", anE);
				}
			}
		}
	}
}
