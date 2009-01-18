package org.dancres.gossip.astrolabe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Culler implements Runnable {
	private static Logger _logger = LoggerFactory.getLogger(Culler.class);

    private long _cullPeriod;

    public Culler(long aCullPeriod) {
        _cullPeriod = aCullPeriod;
    }

    public void run() {
        while (true) {
            try {
                Thread.sleep(_cullPeriod);

                Zones.getRoot().cull(_cullPeriod);
            } catch (InterruptedException anIE) {
				_logger.warn("Got an interruption, ignoring", anIE);
            }
        }
    }
}
