package org.dancres.gossip.core;

import java.util.List;
import java.util.concurrent.TimeUnit;

public interface Primitives {
	public int random(int aRange);
	
	public List<Peer> getNeighbours(int aMaxSize);
	public void addNeighbour(Peer aPeer);
	public void removeNeighbour(Peer aPeer);
	
	public void send(List<Peer> aListOfPeers, byte[] aMessage);
	public void send(Peer aPeer, byte[] aMessage);
	
	/**
	 * Request messages be routed to the specified receiver.
	 */
	public void receive(MessageReceiver aReceiver);
	
	/**
	 * Callback after the specified amount of time
	 */
	public void register(Runnable aRunnable, long aDelay, TimeUnit aUnit);
}
