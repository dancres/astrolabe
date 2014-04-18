package org.dancres.gossip.astrolabe;

import java.net.InetAddress;

import org.dancres.gossip.discovery.DiscoveryListener;
import org.dancres.gossip.discovery.HostDetails;
import org.dancres.gossip.net.NetworkUtils;
import org.dancres.gossip.net.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles events from the <code>org.dancres.gossip.discovery</code> layer.  It's job is to take those events
 * and insert appropriate records into the zone structure.
 */
public class NodeListener implements DiscoveryListener {
	static final String ADVERT_ID_FIELD = "id";
	private static Logger _logger = LoggerFactory.getLogger(NodeListener.class);

	private int _port;
    private Service _service;

	NodeListener(Service aService, int aPort) {
        _service = aService;
		_port = aPort;
	}

	public void found(HostDetails aHostDetails) {
		_logger.info("Discovered: " + aHostDetails);

		// Don't add ourselves
		//
		String myHost = aHostDetails.getHostName();
        String myHostsId = aHostDetails.getProperties().getProperty(ADVERT_ID_FIELD);

		try {
            InetAddress myAddr = InetAddress.getByName(myHost);

            if ((NetworkUtils.isLocalInterface(myAddr)) && (_port == aHostDetails.getPort())) {
                _logger.warn("Dumping our own address from view");
                return;
            }

            // Make sure any hostname is replaced with an address that is appropriate for our chosen network interface
            //
            InetAddress[] myAddrs = InetAddress.getAllByName(myHost);
            for (int i = 0; i < myAddrs.length; i++) {
                if (NetworkUtils.isWorkableSubnet(myAddrs[i])) {
                    myAddr = myAddrs[i];
                }
            }

            _logger.info("Adding new host: " + myAddr.getHostAddress() + ":" + aHostDetails.getPort());

            if (myHostsId == null)
                Zones.addHost(SeedDetails.discover(_service, new HostDetails(myAddr.getHostAddress(), aHostDetails.getPort())));
            Zones.addHost(new SeedDetails(myHostsId, new HostDetails(myAddr.getHostAddress(), aHostDetails.getPort())));
        } catch (Exception anE) {
            _logger.warn("Couldn't check host details (ignoring): " + aHostDetails, anE);
            return;
		}
	}		
}

