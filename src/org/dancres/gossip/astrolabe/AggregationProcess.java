package org.dancres.gossip.astrolabe;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the aggregation process.
 */
public class AggregationProcess {

    private static Logger _logger = LoggerFactory.getLogger(AggregationProcess.class);

    public void run() {
        Zone myCurrentZone = Zones.getRoot().find(LocalID.get());
        Zone mySys = myCurrentZone.get(Zone.SYSTEM);

        /*
         * Aggregation happens to each zone in the self chain starting at the bottom thus it's assumed that an
         * Aggregator instance is run against each zone up the tree in turn and as the System zone will never be
         * aggregated we must update its issued attribute here.
         */
        mySys.getMib().setIssued(mySys.getMib().getIssued() + 1);

        do {
            MibAggregator myAgg = new MibAggregator(myCurrentZone);
            myAgg.run();
        } while ((myCurrentZone = myCurrentZone.getParent()) != null);

        /*
         * Having aggregated all the zones we scan the self-tree to see if any new scripts have been propogated
         * and copy them down into our system zone if the copy flag is set.
         */
        myCurrentZone = Zones.getRoot().find(LocalID.get());
        IMib mySysMib = mySys.getMib();

        do {
            Attributes myCurrentAttrs = myCurrentZone.getMib().getAttributes();
            Iterator<String> myAttrNames = myCurrentAttrs.getKeys();

            while (myAttrNames.hasNext()) {
                String myAttrName = myAttrNames.next();

                if (myAttrName.startsWith(Script.NAME_PREDICATE)) {
                    _logger.debug("Found script: " + myAttrName);
                    Script myScript = (Script) myCurrentAttrs.get(myAttrName);

                    if (myScript.canCopy()) {
                        _logger.debug("Script can be copied: " + myAttrName);
                        Script myCurrentScript = (Script) mySysMib.getAttributes().get(myAttrName);

                        if (myCurrentScript != null) {
                            _logger.debug("Checking for preference");
                            if (myScript.isPreferable(myCurrentScript)) {
                            _logger.debug("Has preference");
                                mySysMib.getAttributes().put(myAttrName, myScript.dup());
                            }
                        } else {
                            _logger.debug("No precdence check required");
                            mySysMib.getAttributes().put(myAttrName, myScript.dup());
                        }
                    }
                }
            }
        } while ((myCurrentZone = myCurrentZone.getParent()) != null);
    }
}

