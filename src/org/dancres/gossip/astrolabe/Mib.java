package org.dancres.gossip.astrolabe;

import java.util.Set;
import org.dancres.gossip.io.Exportable;

public interface Mib extends Exportable {
	public static final String ISSUED_ATTR = "issued";
	public static final String REPRESENTATIVE_ATTR = "representative";
    public static final String ZONE_ATTR = "zone";
	public static final String NMEMBERS_ATTR = "nmembers";
	public static final String CONTACTS_ATTR = "contacts";
	public static final String SERVERS_ATTR = "servers";

    public MibImpl dup();

	public void setIssued(long anIssued);

	public long getIssued();

	public String getRepresentative();

	public void setContacts(Set aContacts);

	public Set getContacts();

	public void setServers(Set aContacts);

	public Set getServers();

	public void setNMembers(long aNumMembers);

	public long getNMembers();

	public void setTouched(long aTime);

	public long getTouched();

	public Attributes getAttributes();
}
