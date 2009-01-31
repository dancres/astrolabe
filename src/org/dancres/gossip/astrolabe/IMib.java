package org.dancres.gossip.astrolabe;

import java.util.Set;
import org.dancres.gossip.io.Exportable;

public interface IMib extends Exportable {
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
