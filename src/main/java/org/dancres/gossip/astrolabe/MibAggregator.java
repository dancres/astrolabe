package org.dancres.gossip.astrolabe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the aggregation logic that generates a <code>Mib</code> for a zone from scripts in the
 * child zones.  The full multi-level aggregation process is implemented by {@link AggregationProcess}
 */
public class MibAggregator {

    private static Logger _logger = LoggerFactory.getLogger(MibAggregator.class);
    private Zone _zone;

    public MibAggregator(Zone aZone) {
        _zone = aZone;
    }

    public void run() {
        /*
         * Get all child zones, scan the mibs for attributes beginning with SCRIPT_NAME_PREDICATE (indicating a script).
         * Take each discovered script and apply it to the Mibs of the child zones, storing the results in the
         * Mib of _zone.
         */

        HashMap<String, Script> myScriptMap = new HashMap<>();
        Collection<Mib> myMibList = new ArrayList<>();
        Collection<Zone> myZoneList = _zone.getChildren();
        Iterator<Zone> myZones = myZoneList.iterator();

        while (myZones.hasNext()) {
            Zone myZone = myZones.next();

            _logger.debug("Examining: " + _zone + " -> " + myZone);

            Mib myZoneMib = myZone.getMib();
            myMibList.add(myZoneMib);

            Attributes myAttrMap = myZoneMib.getAttributes();
            Iterator<String> myKeys = myAttrMap.getKeys();

            while (myKeys.hasNext()) {
                String myKey = myKeys.next();
                _logger.debug("Checking: " + myZone.getName() + " Mib key: " + myKey);

                if (myKey.startsWith(Script.NAME_PREDICATE)) {
                    Script myCurrent = myScriptMap.get(myKey);
                    Script myNewScript = (Script) myAttrMap.get(myKey);

                    // Apply precedence
                    //
                    if (myCurrent == null) {
                        _logger.debug("Adding: " + myKey);
                        myScriptMap.put(myKey, (Script) myAttrMap.get(myKey));
                    } else {
                        if (myNewScript.isPreferable(myCurrent)) {
                            _logger.debug("Replacing: " + myKey);
                            myScriptMap.put(myKey, (Script) myAttrMap.get(myKey));
                        } else {
                            _logger.debug("Keeping: " + myKey);
                        }
                    }
                }
            }
        }

        long myIssued = _zone.getMib().getIssued();
        Iterator<String> myScriptNames = myScriptMap.keySet().iterator();

        while (myScriptNames.hasNext()) {
            String myScriptName = myScriptNames.next();
            Script myScript = myScriptMap.get(myScriptName);

            try {
                _logger.debug("Evaluating: " + myScriptName);

                Mib myZoneMib = _zone.getMib();
                myScript.evaluate(myMibList, myZoneMib);
            } catch (Exception anE) {
                _logger.warn("Failed to evaluate script: " + myScriptName, anE);
            }
        }

        if (_zone.getMib().getIssued() == myIssued) {
            _zone.getMib().setIssued(System.currentTimeMillis());
        }
    }
}
