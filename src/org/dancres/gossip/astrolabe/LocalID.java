package org.dancres.gossip.astrolabe;

/**
 * Contains the Astrolabe id for this machine.
 */
public class LocalID {
	private static String _localId;
	
	public static void set(String anId) {
		_localId = anId;
	}

	public static String get() {
		return _localId;
	}
}
