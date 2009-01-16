package org.dancres.gossip.astrolabe;

import java.net.InetAddress;
import java.util.HashSet;

import org.dancres.gossip.discovery.DiscoveryListener;
import org.dancres.gossip.discovery.HostDetails;
import org.dancres.gossip.net.NetworkUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles events from the <code>org.dancres.gossip.discovery</code> layer.  It's job is to take those events
 * and insert appropriate records into the zone structure.
 */
public class NodeListener implements DiscoveryListener {
	static final String ADVERT_ID_FIELD = "id";
	private static Logger _logger = LoggerFactory.getLogger(NodeListener.class);

	private Zone _root;
	private int _port;

	NodeListener(Zone aRoot, int aPort) {
		_root = aRoot;
		_port = aPort;
	}

	public void found(HostDetails aHostDetails) {
		_logger.info("Discovered: " + aHostDetails);

		// Don't add ourselves
		//
		String myHost = aHostDetails.getHostName();

		try {
            InetAddress myAddr = InetAddress.getByName(myHost);

            if ((NetworkUtils.isLocalInterface(myAddr)) && (_port == aHostDetails.getPort())) {
                _logger.warn("Dumping our own address from view");
                return;
            }
            String myHostsId = aHostDetails.getProperties().getProperty(ADVERT_ID_FIELD);
            Zone myHostsZone = Zones.getRoot().find(myHostsId);

            /*
             * If the node died and came back we may get an announcement again.  We want to ignore that and wait
             * for it to gossip us an up-to-date MIB
             */
            if (myHostsZone == null) {
                myHostsZone = new Zone(myHostsId);
                Mib myHostsMib = new Mib(myHostsId);
                myHostsZone.add(myHostsMib);

                HashSet myDetails = new HashSet();

                // Make sure any hostname is replaced with an address that is appropriate for our chosen network interface
                //
                InetAddress[] myAddrs = InetAddress.getAllByName(myHost);
                for (int i = 0; i < myAddrs.length; i++) {
                    if (NetworkUtils.isWorkableSubnet(myAddrs[i]))
                        myAddr = myAddrs[i];
                }

                _logger.info("Adding new host: " + myAddr.getHostAddress() + ":" + aHostDetails.getPort());
                myDetails.add(new HostDetails(myAddr.getHostAddress(), aHostDetails.getPort()));

                myHostsMib.setIssued(0);  // Make sure we replace this immediately with updates
                myHostsMib.setContacts(myDetails);
                myHostsMib.setServers(myDetails);
                myHostsMib.setNMembers(1);

                Zones.getRoot().add(myHostsZone);
            }
        } catch (Exception anE) {
            _logger.warn("Couldn't check host details (ignoring): " + aHostDetails, anE);
            return;
		}
	}		
}

