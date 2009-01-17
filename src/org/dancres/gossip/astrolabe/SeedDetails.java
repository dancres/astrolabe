package org.dancres.gossip.astrolabe;

import org.dancres.gossip.discovery.HostDetails;

public class SeedDetails {

    private HostDetails _contactDetails;
    private String _id;

    public SeedDetails(String anId, HostDetails aContactDetails) {
        _id = anId;
        _contactDetails = aContactDetails;
    }

    public String getId() {
        return _id;
    }

    public HostDetails getContactDetails() {
        return _contactDetails;
    }
}

