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
 * 
 * @todo Apply logic to figure out the latest version of script and run that rather than whichever version we saw
 * last during scanning for scripts.
 */
public class MibAggregator {
	private static Logger _logger = LoggerFactory.getLogger(MibAggregator.class);
	
	private Zone _zone;
	
	public MibAggregator(Zone aZone) {
		_zone = aZone;
	}
	
	public void run() {
		/*
		 * Get all child zones, scan the mibs for attributes beginning with "&" (indicating a script).
		 * Take each discovered script and apply it to the Mibs of the child zones, storing the results in the
		 * Mib of _zone.
		 */
		
		HashMap<String, Script> myScriptMap = new HashMap<String, Script>();
		Collection<Mib> myMibList = new ArrayList<Mib>();		
		Collection<Zone> myZoneList = _zone.getChildren();
		Iterator<Zone> myZones = myZoneList.iterator();
		
		while (myZones.hasNext()) {
			Zone myZone = myZones.next();
			
			_logger.debug("Examining: " + _zone + " -> " + myZone);
			
			Mib myZoneMib = myZone.getMib();
			myMibList.add(myZoneMib);
			
			Map<String, Object> myAttrMap = myZoneMib.getAttributes();
			Iterator<String> myKeys = myAttrMap.keySet().iterator();
			
			while (myKeys.hasNext()) {
				String myKey = myKeys.next();
				_logger.debug("Checking: " + myZone.getName() + " Mib key: " + myKey);
				
				if (myKey.startsWith("&")) {
					myScriptMap.put(myKey, (Script) myAttrMap.get(myKey));
					_logger.debug("Keeping: " + myKey);
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

                /*
                 * We don't want scripts directly updating the Mib attributes of the zone.  This is because we have to control/limit
                 * attribute propogation.  Thus we clone the Mib, pass it in for updating and merge the results back to the clone's
                 * donor.
                 */
                Mib myZoneMib = _zone.getMib();
                Mib myTempMib = myZoneMib.dup();
				myScript.evaluate(myMibList, myTempMib);
                Iterator<String> myAttrKeys = myTempMib.getAttributes().keySet().iterator();

                while (myAttrKeys.hasNext()) {
                    String myKey = myAttrKeys.next();
                    Object myValue = myTempMib.getAttributes().get(myKey);

                    if (canPropogate(_zone, myKey, myValue)) {
                        myZoneMib.getAttributes().put(myKey, myValue);
                    }
                }
			} catch (Exception anE) {
				_logger.warn("Failed to evaluate script: " + myScriptName, anE);
			}
		}
		
		if (_zone.getMib().getIssued() == myIssued)
			_zone.getMib().setIssued(System.currentTimeMillis());
	}

    private boolean canPropogate(Zone aZone, String aKey, Object aValue) {
        // Only propogate a script if it's origin zone matches this Zone or a parent of this Zone
        //
        if (aValue instanceof Script) {
            Script myMergeScript = (Script) aValue;
            String myOrigin = myMergeScript.getAttribute(Script.CERT_ORIGIN);
            Zone myCurrent = aZone;

            do {
                if (myOrigin.equals(myCurrent.getId()))
                    return true;
                myCurrent = myCurrent.getParent();
            } while (myCurrent != null);

            return false;
        }

        return true;
    }
}
