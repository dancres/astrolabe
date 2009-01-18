package org.dancres.gossip.astrolabe;

import org.dancres.gossip.net.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Gossiper implements Runnable {
	private static Logger _logger = LoggerFactory.getLogger(Gossiper.class);
	
	private Service _service;
	private long _gossipInterval;

	public Gossiper(Service aService, long aGossipInterval) {
		_service = aService;
        _gossipInterval = aGossipInterval;
	}
	
	public void run() {
		while (true) {
			try {
				Thread.sleep(_gossipInterval);
			} catch (InterruptedException anIE) {
				_logger.warn("Got an interruption, ignoring", anIE);
			}
			
			new GossipProcess(_service).run();
		}
	}
}
