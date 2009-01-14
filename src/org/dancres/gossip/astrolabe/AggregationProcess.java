package org.dancres.gossip.astrolabe;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the aggregation process.
 *
 * @todo When propogating scripts into the system zone make sure that appropriate issued precedence is observed.
 * @todo Implement the full Astrolabe precedence rules
 */
public class AggregationProcess {
	private static Logger _logger = LoggerFactory.getLogger(AggregationProcess.class);

	public void run() {
		Zone myCurrent = Zones.getRoot().find(LocalID.get());
		Zone mySys = myCurrent.get(Zone.SYSTEM);
		
		/*
		 * Aggregation happens to each zone in the self chain starting at the bottom thus it's assumed that an
		 * Aggregator instance is run against each zone up the tree in turn and as the System zone will never be
		 * aggregated we must update its issued attribute here.
		 */
		mySys.getMib().setIssued(mySys.getMib().getIssued() + 1);
		
		do {
			MibAggregator myAgg = new MibAggregator(myCurrent);
			myAgg.run();
		} while ((myCurrent = myCurrent.getParent()) != null);

        /*
         * Having aggregated all the zones we scan the self-tree to see if any new scripts have been propogated
         * and copy them down into our system zone if the copy flag is set.
         */
        Map<String, Script> myScripts = new HashMap<String, Script>();
        myCurrent = Zones.getRoot().find(LocalID.get());
        Mib mySysMib = mySys.getMib();

        do {
            Map<String, Object> myCurrentAttrs = myCurrent.getMib().getAttributes();
            Iterator<String> myAttrNames = myCurrentAttrs.keySet().iterator();

            while (myAttrNames.hasNext()) {
                String myAttrName = myAttrNames.next();
                if (myAttrName.startsWith(Script.SCRIPT_NAME_PREDICATE)) {
                    _logger.info("Found script: " + myAttrName);
                    Script myScript = (Script) myCurrentAttrs.get(myAttrName);
                    if (myScript.canCopy()) {
                        _logger.info("Copying script: " + myAttrName);
                        myScripts.put(myAttrName, ((Script) myCurrentAttrs.get(myAttrName)).dup());
                    }
                }
            }
        } while ((myCurrent = myCurrent.getParent()) != null);

        Iterator<String> myScriptNames = myScripts.keySet().iterator();

        while (myScriptNames.hasNext()) {
            String myScriptName = myScriptNames.next();

            _logger.info("Possible candidate: " + myScriptName);
            if (! mySysMib.getAttributes().containsKey(myScriptName)) {
                _logger.info("Inserting candidate: " + myScriptName);
                mySysMib.getAttributes().put(myScriptName, myScripts.get(myScriptName));
            }
        }
	}
}
