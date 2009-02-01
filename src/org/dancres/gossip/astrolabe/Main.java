package org.dancres.gossip.astrolabe;

import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;

import org.dancres.gossip.discovery.HostDetails;
import org.dancres.gossip.discovery.RegistrarFactory;
import org.dancres.gossip.net.NetworkUtils;
import org.dancres.gossip.net.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bsh.Interpreter;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Startup class for the astrolabe implementation.
 */
public class Main {
	private static Logger _logger = LoggerFactory.getLogger(Main.class);
	
	private static final String ASTROLABE_ROOT = "/astrolabe";
	private static final String TYPE = "_astrolabe._tcp";
	private static final String INTERACTIVE_PROP = "interactive";
	
	/**
	 * DO NOT REFERENCE THIS DIRECTLY - FOR USE IN BEANSHELL-BASED DEBUGGING ONLY
	 */
	public static Service _service;
	
	/**
	 * @param anArgs a list of peer id or peer id and seed specification which is <code>[host:port]*</code>
	 */
	public static void main(String[] anArgs) throws Exception {
		String myId = null;
		LinkedList<HostDetails> mySeedDetails = new LinkedList<HostDetails>();
		
		if (anArgs.length == 1) {
			myId = anArgs[0];
			
		} else if (anArgs.length > 1) {
			myId = anArgs[0];

            for (int i = 1; i < anArgs.length; i++) {
                mySeedDetails.add(HostDetails.parse(anArgs[i]));
            }
		} else {
			System.err.println("Usage: <peerId> | <peerId> [<seed URL>]*");
			return;
		}
		
		LocalID.set(myId);
		
		_service = new Service(ASTROLABE_ROOT);
		
		_service.add(new IdServlet(), IdServlet.MOUNT_POINT);
		_service.add(new MibServlet(), MibServlet.MOUNT_POINT);
		_service.add(new GossipServlet(_service), GossipServlet.MOUNT_POINT);

    	_logger.info("Doing local advert as: " + TYPE + " : " + NetworkUtils.getWorkableInterface() + " : " +
    			_service.getPort());

    	HostDetails myContactDetails = _service.getContactDetails();
    	HashSet<HostDetails> myContactsSet = new HashSet<HostDetails>();
    	myContactsSet.add(myContactDetails);
    	
		Zone myRoot = new Zone();
		
		MibImpl myMib = new MibImpl(myRoot, myId);
		myRoot.add(myMib);
		myMib.setIssued(0);
		myMib.setNMembers(0);
		
		Zone myMachineZone = new Zone(myId);
		
		myMib = new MibImpl(myMachineZone, myId);
		myMachineZone.add(myMib);
		myMib.setIssued(System.currentTimeMillis());
		myMib.setNMembers(1);
		
		Zone mySystemZone = new Zone(myId + "/" + Zone.SYSTEM);

		myMib = new MibImpl(mySystemZone, myId);
		mySystemZone.add(myMib);
		myMib.setIssued(System.currentTimeMillis());
		myMib.setNMembers(1);
		myMib.setContacts(myContactsSet);
		myMib.setServers(myContactsSet);		
		
		myMachineZone.add(mySystemZone);
		myRoot.add(myMachineZone);		
    	
		Zones.setRoot(myRoot);

        Iterator<HostDetails> mySeeds = mySeedDetails.iterator();
        while (mySeeds.hasNext()) {
            try {
                Zones.addHost(SeedDetails.discover(_service, mySeeds.next()));
            } catch (IOException anIOE) {
                // Discard, already reported and nothing to be done for this host
            }
        }

    	if (isInteractive()) {
    		// Basics are now setup, start the shell and leave everything else to the user
    		//
    		new Interpreter().eval("desktop()");
    	} else {
    		new Interpreter().source("config/init.bsh");
    		
    		publishAdvert();
    		
    		Thread myGossiper = new Thread(new Gossiper(_service, 30000));
    		myGossiper.start();

            Thread myCuller = new Thread(new Culler(60000));
            myCuller.start();

    		/*
    		  Thread myDumper = new MibDumper();
    		  myDumper.start();
    		*/
    	}
	}

	static boolean isInteractive() {
		return (System.getProperty(INTERACTIVE_PROP) != null);
	}

	public static void publishAdvert() throws IOException {
		Properties myProps = new Properties();
		myProps.put(NodeListener.ADVERT_ID_FIELD, LocalID.get());
	
    	RegistrarFactory.getRegistrar().sample(TYPE, new NodeListener(_service, _service.getPort()));
    	RegistrarFactory.getRegistrar().register(TYPE, NetworkUtils.getWorkableInterface(), 
    			_service.getPort(), myProps);	    	
	}
	
	private static class MibDumper extends Thread {
		MibDumper() {
		}
		
		public void run() {
			while (true) {
				try {
					Thread.sleep(30000);
				} catch (InterruptedException anIE) {
					//					
				}
				
				Zones.getRoot().dumpTree("");
			}
		}
	}
}
