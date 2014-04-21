package org.dancres.gossip.discovery;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Properties;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import org.dancres.gossip.net.NetworkUtils;

/**
 * @todo Complete implementation
 */
public class JmDNSRegistrar implements Registrar {
	public void register(String aType, NetworkInterface anInterface, int aPort, 
			Properties anAttrs) throws IOException {
		InetAddress myAddr = NetworkUtils.getValidAddress(anInterface);
        JmDNS myAdvertiser = JmDNS.create(myAddr, flatten(myAddr));
        
        String myAttrs = "";
        
		if (anAttrs != null) {
			StringBuilder myTextProps = new StringBuilder();
			Enumeration myList = anAttrs.keys();
			
			while (myList.hasMoreElements()) {
				String myKey = (String) myList.nextElement();
				String myProp = (String) anAttrs.get(myKey);
				
				myTextProps.append(myKey + "=" + myProp + " ");				
			}
			
			myAttrs = myTextProps.toString();
		}
        
        ServiceInfo myInfo = ServiceInfo.create(aType + ".local", 
        		"paxos@" + flatten(myAddr), aPort, myAttrs);
       
        myAdvertiser.registerService(myInfo);		
	}
	
    private String flatten(InetAddress anAddress) {
    	byte[] myAddr = anAddress.getAddress();
    	long myNodeId = 0;
    	
        for (int i = 0; i < 4; i++) {
            myNodeId = myNodeId << 8;
        	myNodeId |= (int) myAddr[i] & 0xFF;
        }
    	
    	return Long.toHexString(myNodeId);
    }

	public void endSample() {
		throw new Error("Not supported");
	}

	public void sample(String type, DiscoveryListener listener)
			throws IOException {
		throw new Error("Not supported");
	}
}
