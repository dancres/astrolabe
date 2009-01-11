package org.dancres.gossip.core;

public interface Peer {

	public String getHostName();

	public int getPort();
	
	public int getHops();
}
