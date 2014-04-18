package org.dancres.gossip.discovery;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apple.dnssd.BrowseListener;
import com.apple.dnssd.DNSSD;
import com.apple.dnssd.DNSSDException;
import com.apple.dnssd.DNSSDRegistration;
import com.apple.dnssd.DNSSDService;
import com.apple.dnssd.RegisterListener;
import com.apple.dnssd.ResolveListener;
import com.apple.dnssd.TXTRecord;

public class BonjourRegistrar implements Registrar, BrowseListener, ResolveListener {
    private Logger _logger;
	private DiscoveryListener _listener;
	private DNSSDService _sample;
    
    BonjourRegistrar() {
        _logger = LoggerFactory.getLogger(getClass());
    }
    
	public void register(String aType, NetworkInterface anInterface, int aPort, 
			Properties anAttrs) throws IOException {
		
		try {
			TXTRecord myAttrs = null;
			
			if (anAttrs != null) {
				myAttrs = new TXTRecord();				
				Enumeration myList = anAttrs.keys();
				
				while (myList.hasMoreElements()) {
					String myKey = (String) myList.nextElement();
					String myProp = (String) anAttrs.get(myKey);
					
					myAttrs.set(myKey, myProp);
				}
			}
			
			int myIndex = DNSSD.getIfIndexForName(anInterface.getName());
			DNSSD.register(0, myIndex, null, aType, null, null, aPort, myAttrs, new RegisterImpl());
		} catch (DNSSDException anE) {
			_logger.error("Register failed", anE);
			
			throw new IOException("Register failed");
		}
	}
	
	private class RegisterImpl implements RegisterListener {
		public void serviceRegistered(DNSSDRegistration aReg, int aFlags,
				String aServiceName, String aRegType, String aDomain) {
			_logger.info("Registered: " + aServiceName + ", " + aRegType + ", " + aDomain);
		}

		public void operationFailed(DNSSDService aaService, int anErrorCode) {
			_logger.info("Registration failed: " + anErrorCode);
		}
	}

	public void serviceFound(DNSSDService aService, int aFlags, int anIntIndex, String aServiceName,
			String aRegType, String aDomain) {
		try {
			DNSSD.resolve(0, anIntIndex, aServiceName, aRegType, aDomain, this);
		} catch (DNSSDException anE) {
			_logger.error("Resolve failed", anE);
		}
	}

	public void serviceLost(DNSSDService aService, int aFlags, int anIntIndex, String aServiceName,
			String aRegType, String aDomain) {
	}

	public void operationFailed(DNSSDService aService, int anErrorCode) {
		_logger.info("Registration failed: " + anErrorCode);
		aService.stop();
	}

	public void serviceResolved(DNSSDService aService, int aFlags, int anIntIndex,
			String aFullName, String aHostName, int aPort, TXTRecord aDescription) {
		DiscoveryListener aListener;
		
		synchronized(this) {
			aListener = _listener;
		}
		
		if (aListener != null) {
			Properties myProperties = new Properties();

			for (int i = 0; i < aDescription.size(); i++) {
                String myKey = aDescription.getKey(i);
                String myValue = aDescription.getValueAsString(i);

                if ((myKey != null) && (myValue != null))
				    myProperties.put(aDescription.getKey(i), aDescription.getValueAsString(i));
			}
			
			aListener.found(new HostDetails(aHostName, aPort, myProperties));
		} else
			_logger.warn("No listener after resolve completed");
	}

	public void sample(String aType, DiscoveryListener aListener)
			throws IOException {

		synchronized(this) {
			_listener = aListener;

			try {
				_sample = DNSSD.browse(aType, this);
			} catch (DNSSDException anE) {
				_logger.error("Browse failed", anE);

				throw new IOException("Browse failed");
			}		
		}
	}
	
	public void endSample() {
		synchronized (this) {
			_sample.stop();
			_listener = null;
		}
	}
}
