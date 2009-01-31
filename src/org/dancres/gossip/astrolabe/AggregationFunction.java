package org.dancres.gossip.astrolabe;

import java.util.Collection;

/**
 * Interface to be implemented by bean shell scripts so that they may be used as script attributes of a Mib in a Zone.
 */
public interface AggregationFunction {
	/**
	 * @param aScript is the enclosing Script object executing the bean shell script
	 * @param aListOfMibs is a list of Mibs to aggregate
	 * @param aTarget is the Mib to put the aggregation in
	 */
	public void aggregate(Script aScript, Collection<Mib> aListOfMibs, Mib aTarget);
}
