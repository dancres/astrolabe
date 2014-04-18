package org.dancres.gossip.discovery;

import java.util.Properties;

public class HostDetails {
	private String _host;
	private int _port;
	private transient Properties _properties;
	
	public HostDetails() {		
	}
	
    /**
     * 
     * @param aSpec a host port specification separated by a ":"
     */
    public static HostDetails parse(String aSpec) throws IllegalArgumentException {
        int mySep = aSpec.indexOf(":");

        if (mySep == -1)
            throw new IllegalArgumentException("Missing : in spec: " + aSpec);

        String myHost = aSpec.substring(0, mySep);
        int myPort = Integer.parseInt(aSpec.substring(mySep + 1));

        return new HostDetails(myHost, myPort);
    }

	public HostDetails(String aHostName, int aPort) {
		this(aHostName, aPort, new Properties());
	}
	
	public HostDetails(String aHostName, int aPort, Properties aProperties) {
		_host = aHostName;
		_port = aPort;
		_properties = aProperties;
	}
	
	public String toString() {
		return _host + ":" + _port;
	}
	
	public String getHostName() {
		return _host;
	}
	
	public Properties getProperties() {
		return _properties;
	}
	
	public int getPort() {
		return _port;
	}
	
	public boolean equals(Object anObject) {
		if (anObject instanceof HostDetails) {
			HostDetails myOther = (HostDetails) anObject;
			
			return ((myOther.getHostName().equals(_host)) && (myOther.getPort() == _port));
		}
		
		return false;
	}
	
	public int hashCode() {
		return _host.hashCode();
	}
}
