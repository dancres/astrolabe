package org.dancres.gossip.astrolabe;

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
}
