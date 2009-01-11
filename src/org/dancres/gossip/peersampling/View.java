package org.dancres.gossip.peersampling;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.dancres.gossip.core.Peer;
import org.dancres.gossip.discovery.HostDetails;
import org.dancres.gossip.io.Exportable;

public class View implements Exportable {
	private static 	Random _random = new Random();
	private static Comparator<PeerImpl> _peerSorter = new PeerSorter();
	private static JsonFactory _jsonFactory = new JsonFactory();
	
	private HashSet<PeerImpl> _peers = new HashSet<PeerImpl>();
	private int _size;
	
	private static class PeerSorter implements Comparator<PeerImpl> {
		public int compare(PeerImpl anObject, PeerImpl anotherObject) {			
			return (anObject.getHops() - anotherObject.getHops());
		}
	}

	private static class PeerImpl implements Peer {
		private String _hostName;
		private int _port;
		private int _hops;
		
		PeerImpl(String aHostName, int aPort, int aHopCount) {
			_hostName = aHostName;
			_port = aPort;
			_hops = aHopCount;
		}
		
		public int hashCode() {
			return _hostName.hashCode();
		}
		
		public boolean equals(Object anObject) {
			if (anObject instanceof PeerImpl) {
				PeerImpl myOther = (PeerImpl) anObject;
				
				if (myOther._hostName.equals(_hostName)) {
					return (myOther._port == _port);
				}
			}
			
			return false;
		}

		public int getHops() {
			return _hops;
		}

		public String getHostName() {
			return _hostName;
		}

		public int getPort() {
			return _port;
		}

		public void incHops() {
			++_hops;
		}
		
		public String toString() {
			return _hostName + ":" + _port + " (" + _hops + ")";
		}
	}

	public View(int aSize) {
		_size = aSize;
	}

	private View(int aSize, HashSet<PeerImpl> aPeers) {
		_size = aSize;
		_peers = new HashSet<PeerImpl>(aPeers);
	}
	
	public View(Reader aReader) throws IOException {
		JsonParser myParser = _jsonFactory.createJsonParser(aReader);
		while (myParser.nextToken() != null) {
			String myHost = myParser.getText();
			
			if (myParser.nextToken() == null)
				throw new IOException("Bad peer spec - no port");
			
			int myPort = myParser.getIntValue();
			
			if (myParser.nextToken() == null)
				throw new IOException("Bad peer spec - no hops");
			
			int myHops = myParser.getIntValue();
			
			_peers.add(new PeerImpl(myHost, myPort, myHops));
		}		
		myParser.close();
	}

	/**
	 * @return The peer to use or null if there is no peer available
	 */
	public Peer getPeer() {
		PeerImpl[] myPeers;
		
		synchronized(_peers) {
			myPeers = new PeerImpl[_peers.size()];
			myPeers = (PeerImpl[]) _peers.toArray(myPeers);			
		}
		
		if (myPeers.length == 0)
			return null;
		
		Arrays.sort(myPeers, _peerSorter);
		
		return myPeers[_random.nextInt(myPeers.length)];
	}

	public void add(HostDetails aHostDetails, int aHopCount) {
		PeerImpl myPeer = new PeerImpl(aHostDetails.getHostName(), aHostDetails.getPort(), aHopCount);
		HashSet<PeerImpl> mySet = new HashSet<PeerImpl>();
		mySet.add(myPeer);
		View myView = new View(1, mySet);
		merge(myView);
	}
	
	public void discard(HostDetails aHostDetails) {
		PeerImpl myTarget = new PeerImpl(aHostDetails.getHostName(), aHostDetails.getPort(), 0);
		System.out.println("Removing: " + myTarget);
		
		synchronized(_peers) {
			_peers.remove(myTarget);
		}
	}
	
	public View dup() {
		synchronized(_peers) {
			return new View(_size, _peers);
		}
	}
	
	public int size() {
		synchronized(_peers) {
			return _peers.size();
		}
	}
	
	public void merge(View aView) {
		synchronized(_peers) {
			// No simple way to populate a map from a set and no easy way to get a member back out of a set, sigh
			//		
			HashMap<PeerImpl, PeerImpl> myMap = new HashMap<PeerImpl, PeerImpl>();
			for (PeerImpl myPeer: _peers) {
				myMap.put(myPeer, myPeer);
			}
			
			for (PeerImpl myPeer : aView._peers) {
				PeerImpl myExisting = myMap.get(myPeer);
				if (myExisting == null) {
					// Peer doesn't exist
					//
					_peers.add(myPeer);
				} else {
					// Insert the one with minimum hopcount
					//
					if (myExisting.getHops() > myPeer.getHops()) {
						_peers.remove(myExisting);
						_peers.add(myPeer);
					}
				}
			}
			
			// Merge is done, union is currently in _peers
			//
			if (_peers.size() > _size) {
				PeerImpl[] myPeers = new PeerImpl[_peers.size()];
				myPeers = (PeerImpl[]) _peers.toArray(myPeers);
				Arrays.sort(myPeers, _peerSorter);
				
				_peers.clear();
				ArrayList<PeerImpl> myCandidates = new ArrayList<PeerImpl>(Arrays.asList(myPeers));

				while (_peers.size() < _size) {
					_peers.add(myCandidates.remove(_random.nextInt(myCandidates.size())));					
				}
			}
		}
	}

	public void increaseHopCount() {
		synchronized(_peers) {
			for (PeerImpl myPeer : _peers) {
				myPeer.incHops();
			}
		}
	}

	public void export(Writer aWriter) throws IOException {
		JsonGenerator myGenerator = _jsonFactory.createJsonGenerator(aWriter);
		myGenerator.useDefaultPrettyPrinter();
		
		synchronized(_peers) {
			for (PeerImpl myPeer : _peers) {
				myGenerator.writeString(myPeer.getHostName());
				myGenerator.writeNumber(myPeer.getPort());
				myGenerator.writeNumber(myPeer.getHops());
			}			
		}
		myGenerator.close();
	}		
}
