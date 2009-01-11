package org.dancres.gossip.core;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @todo When making a connection:  Issue a challenge, the winner is responsible for making the connection, the
 * loser gives up leaving the other end to connect.  If the winner is the recipient of the connection it checks to see
 * if it already has one open to the loser before attempting a connection.
 * 
 * @author dan
 *
 */
public class PrimitivesImpl implements Primitives {

	public void addNeighbour(Peer peer) {
		// TODO Auto-generated method stub
	}

	public void removeNeighbour(Peer peer) {
		// TODO Auto-generated method stub
	}
	
	public List<Peer> getNeighbours(int maxSize) {
		// TODO Auto-generated method stub
		return null;
	}

	public int random(int range) {
		// TODO Auto-generated method stub
		return 0;
	}

	public void receive(MessageReceiver receiver) {
		// TODO Auto-generated method stub

	}

	public void register(Runnable runnable, long delay, TimeUnit unit) {
		// TODO Auto-generated method stub

	}

	public void send(List<Peer> listOfPeers, byte[] message) {
		// TODO Auto-generated method stub

	}

	public void send(Peer peer, byte[] message) {
		// TODO Auto-generated method stub
	}
}
