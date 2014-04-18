package org.dancres.gossip.peersampling;

import java.io.OutputStreamWriter;

import org.dancres.gossip.discovery.HostDetails;

public class CheckJson {
	public static void main(String anArgs[]) throws Exception {
		View myView = new View(16);
		
		myView.add(new HostDetails("rogue.local", 1234), 1);
		myView.add(new HostDetails("rogue2.local", 1235), 2);
		
		myView.export(new OutputStreamWriter(System.out));
	}
}
