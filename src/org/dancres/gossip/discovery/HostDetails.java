package org.dancres.gossip.discovery;

import java.util.Properties;

public class HostDetails {
	private String _host;
	private int _port;
	private transient Properties _properties;
	
	public HostDetails() {		
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
