package org.dancres.gossip.discovery;

public class RegistrarFactory {
	private static Registrar _instance;
	
	public static synchronized Registrar getRegistrar() {
		if (_instance == null) {
			try {
				// Test for Bonjour/ZeroConf and use it by preference
				//
				Class.forName("com.apple.dnssd.DNSSD");

				_instance = new BonjourRegistrar();
			} catch (ClassNotFoundException aCNFE) {
				// Fallback to JmDNS
				//
				_instance = new JmDNSRegistrar();
			}
		}
		
		return _instance;
	}
}
