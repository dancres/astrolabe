package org.dancres.gossip.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collection of utilities for performing discovery of network interfaces and addresses that are suitable
 * for use with Gossip protocols.  In particular we require support for multicast.
 */
public class NetworkUtils {
    private static Logger _logger = LoggerFactory.getLogger(NetworkUtils.class);

    private static NetworkInterface _workableInterface = null;
    private static SortedSet<NetworkInterface> _workableInterfaces = 
    	new TreeSet<NetworkInterface>(new NetworkInterfaceComparator());

    private static class NetworkInterfaceComparator implements Comparator<NetworkInterface> {

        public int compare(NetworkInterface anO, NetworkInterface anotherO) {
            NetworkInterface myA = (NetworkInterface) anO;
            NetworkInterface myB = (NetworkInterface) anotherO;

            String myAName = myA.getName();
            String myBName = myB.getName();

            return myAName.compareTo(myBName);
        }
    }

    /*
     * Iterate interfaces and look for one that's multicast capable and not a 127 or 169 based address
     */
    static {
        try {
            Enumeration<NetworkInterface> myInterfaces = NetworkInterface.getNetworkInterfaces();

            while (myInterfaces.hasMoreElements()) {
                NetworkInterface myInterface = myInterfaces.nextElement();

                if (! isMulticastCapable(myInterface))
                    continue;

                if ((! myInterface.getName().startsWith("en")) &&
                        (! myInterface.getName().startsWith("eth")))
                    continue;

                if (hasValidAddress(myInterface))
                    _workableInterfaces.add(myInterface);
            }

            for (NetworkInterface myIf : _workableInterfaces) {
                _logger.info("Candidate Interface: " + myIf);
            }

            /*
             * We would prefer to use the index number to choose but in the absence of that we choose the
             * interface with the lowest index
             */
            _workableInterface = _workableInterfaces.first();

            _logger.info("Chosen interface: " + _workableInterface);
        } catch (Exception anE) {
            throw new Error("Failed to find interface", anE);
        }
    }

    public static InetAddress getValidAddress(NetworkInterface anIn) {
        Enumeration<InetAddress> myAddrs = anIn.getInetAddresses();

        while (myAddrs.hasMoreElements()) {
            InetAddress myAddr = myAddrs.nextElement();

            // If it's not IPv4, forget it
            if (myAddr.getAddress().length != 4)
                continue;

            boolean isReachable = false;

            if (myAddr.isLoopbackAddress())
                continue;

            try {
                isReachable = myAddr.isReachable(500);
            } catch (Exception anE) {
                _logger.debug("Not reachable: " + anIn, anE);
                continue;
            }

            if (!isReachable)
                continue;

            // Found one address on this interface that makes sense
            //
            return myAddr;
        }

        return null;
    }

    private static boolean hasValidAddress(NetworkInterface anIn) {
        return (getValidAddress(anIn) != null);
    }

    private static boolean isMulticastCapable(NetworkInterface anIn) {
        try {
            InetAddress myMcast = InetAddress.getByName("224.0.1.85");

            MulticastSocket mySocket = new MulticastSocket(4159);

            mySocket.setNetworkInterface(anIn);

            mySocket.joinGroup(myMcast);

            String myMsg = "blahblah";

            DatagramPacket myPkt = new DatagramPacket(myMsg.getBytes(), myMsg.length(),
                             myMcast, 6789);

            mySocket.send(myPkt);

            mySocket.close();

            return true;
        } catch (Exception anE) {
            _logger.debug("No mcast: " + anIn, anE.getMessage());
            return false;
        }
    }

    public static boolean isLocalInterface(InetAddress anAddr) throws IOException {
        Enumeration<NetworkInterface> myInterfaces;
        
        myInterfaces = NetworkInterface.getNetworkInterfaces();

        while (myInterfaces.hasMoreElements()) {
        	NetworkInterface myInterface = myInterfaces.nextElement();

        	Enumeration<InetAddress> myAddrs = myInterface.getInetAddresses();
        	while (myAddrs.hasMoreElements()) {
        		InetAddress myAddr = myAddrs.nextElement();
        		if (anAddr.getHostAddress().equals(myAddr.getHostAddress()))
        			return true;
        	}
        }			
		
		return false;		
    }
    
    public static boolean isWorkableSubnet(InetAddress anAddr) throws IOException {
    	byte[] myAddr = anAddr.getAddress();
    	byte[] mySelectedAddr = getWorkableAddress().getAddress();
    	
    	for (int i = 0; i < 3; i++) {
    		if (myAddr[i] != mySelectedAddr[i])
    			return false;
    	}
    	
    	return true;
    }
    
    public static NetworkInterface getWorkableInterface() throws IOException {
    	return _workableInterface;
    }
    
    public static InetAddress getWorkableAddress() throws IOException {
        return getValidAddress(_workableInterface);
    }

    public static InetAddress getBroadcastAddress() throws IOException {
        byte[] myAddr = getValidAddress(_workableInterface).getAddress();

        myAddr[3] = (byte) 255;

        return InetAddress.getByAddress(myAddr);
    }

    public static int getAddressSize() {
        return 4;
    }
}
