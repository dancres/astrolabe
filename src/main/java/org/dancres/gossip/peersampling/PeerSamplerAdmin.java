package org.dancres.gossip.peersampling;

import org.dancres.gossip.discovery.HostDetails;

public interface PeerSamplerAdmin extends PeerSampler {
	public void add(HostDetails aHostDetails, int aHopCount);
}
