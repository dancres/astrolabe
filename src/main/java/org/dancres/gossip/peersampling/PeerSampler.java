package org.dancres.gossip.peersampling;

import org.dancres.gossip.core.Peer;
import org.dancres.gossip.discovery.HostDetails;

public interface PeerSampler {
	public Peer getPeer();
	public void seed(HostDetails aDetails) throws Exception;
}
