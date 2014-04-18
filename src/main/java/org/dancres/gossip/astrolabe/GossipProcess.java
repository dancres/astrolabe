package org.dancres.gossip.astrolabe;

import java.io.IOException;
import java.util.Set;

import org.dancres.gossip.net.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the overall gossip process
 */
public class GossipProcess {
	private static Logger _logger = LoggerFactory.getLogger(GossipProcess.class);

	private Service _service;

	public GossipProcess(Service aService) {
		_service = aService;
	}
	
	public void run() {
		// First run the aggregation process
		new AggregationProcess().run();

		try {
			// Starting at our own zone, walk back up the "self" tree
			//
			Zone myCurrent = Zones.getRoot().find(LocalID.get());

			do {
				// If we're a contact for the zone, gossip it
				//
				Set myZoneContacts = myCurrent.getMib().getContacts();

				if (myZoneContacts.contains(_service.getContactDetails())) {
					ZoneGossiper myGossiper = new ZoneGossiper(myCurrent, _service);
					myGossiper.run();
				}
			} while ((myCurrent = myCurrent.getParent()) != null);
		} catch (IOException anIOE) {
			_logger.warn("Failed to run gossip proces:", anIOE);
		}
	}
}
