package org.dancres.gossip.astrolabe;

/**
 * Implements the aggregation process.
 */
public class AggregationProcess {
	public void run() {
		Zone myCurrent = Zones.getRoot().find(LocalID.get());
		
		/*
		 * Aggregation happens to each zone in the self chain starting at the bottom thus it's assumed that an
		 * Aggregator instance is run against each zone up the tree in turn and as the System zone will never be
		 * aggregated we must update its issued attribute here.
		 */
		Zone mySys = myCurrent.get(Zone.SYSTEM);
		mySys.getMib().setIssued(mySys.getMib().getIssued() + 1);
		
		do {
			MibAggregator myAgg = new MibAggregator(myCurrent);
			myAgg.run();
		} while ((myCurrent = myCurrent.getParent()) != null);
	}
}
