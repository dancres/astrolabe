package org.dancres.gossip.discovery;

import java.io.IOException;
import java.net.NetworkInterface;
import java.util.Properties;

public interface Registrar {
	public static final String PAXOS_TYPE = "_paxos._udp";

	public void register(String aType, NetworkInterface anAddr, int aPort, Properties anAttributes) throws IOException;
	
	/**
	 * Update the registered listener with new service instances.  Note that only one sample can be active at
	 * any one time and must be terminated with <code>endSample</code> before another can be started.
	 */
	public void sample(String aType, DiscoveryListener aListener) throws IOException;
	
	/**
	 * Cease sampling as started above
	 */
	public void endSample();
}
