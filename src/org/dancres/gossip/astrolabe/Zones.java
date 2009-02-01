package org.dancres.gossip.astrolabe;

import java.util.HashSet;
import org.dancres.gossip.discovery.HostDetails;

/**
 * Maintains a reference to the root zone for this Astrolabe agent.
 */
public class Zones {
	private static Zone _root;
	
	static void setRoot(Zone aZone) {
		_root = aZone;
	}
	
	public static Zone getRoot() {
		return _root;
	}

    public static void addHost(SeedDetails aDetails) {
        Zone myHostsZone = Zones.getRoot().find(aDetails.getId());

        /*
         * If the node died and came back we may get an announcement again.  We want to ignore that and wait
         * for it to gossip us an up-to-date MIB
         */
        if (myHostsZone == null) {
            myHostsZone = new Zone(aDetails.getId());
            Mib myHostsMib = myHostsZone.newMib(aDetails.getId());
            myHostsZone.add(myHostsMib);

            HashSet myDetails = new HashSet();
            myDetails.add(aDetails.getContactDetails());

            myHostsMib.setIssued(0);  // Make sure we replace this immediately with updates
            myHostsMib.setContacts(myDetails);
            myHostsMib.setServers(myDetails);
            myHostsMib.setNMembers(1);

            Zones.getRoot().add(myHostsZone);
        }
    }
}
