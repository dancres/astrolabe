package org.dancres.gossip.astrolabe;

import java.util.Map;
import java.util.Set;

import org.dancres.gossip.io.Exportable;


/**
 * Holds attributes for a {@link Zone}.  Standard attributes are:
 * <ul>
 * <li>Issued: The version of the Mib, updated by the representative that generated the Mib</li>
 * <li>Representative: The astrolabe id of the representative that created the Mib</li>
 * <li>NMembers: The number of nodes under the Zone of this Mib</li>
 * <li>Contacts: The addresses of those agents responsible for gossiping this Zone to others</li>
 * <li>Servers: The addresses of agents that a client may contact about a Zone</li>
 * </ul>
 * 
 * One may also store scripts as attributes.  In such a case, the name of the attribute must start with <code>&</code>
 * and the value should be a {@link Script} object.
 */
public interface Mib extends Exportable {

    /**
     * Use this method to gain access to the Mib's attributes for purposes of live modification.  Use
     * <code>exportAttributes</code> to obtain a copy of the Mib's attributes suitable for transfer between VM's.
     * @return the live attributes as a Map of String keys and Object values.
     */
    public Map getAttributes();

    /**
     * @return an immutable collection of the contacts
     */
    public Set getContacts();

    public long getIssued();

    public long getNMembers();

    public String getRepresentative();

    /**
     * @return an immutable collection of the servers
     */
    public Set getServers();

    /**
     * Tracks the last time an update for this Mib from it's representative was received.
     * This is used by <code>Zone</code> to decide which Mib to return in cases where the Zone is not a member of
     * the self chain.
     */
    public long getTouched();

    public void setContacts(Set aContacts);

    public void setIssued(long anIssued);

    public void setNMembers(long aNumMembers);

    public void setServers(Set aContacts);

    /**
     * Tracks the last time an update for this Mib from it's representative was received.
     * This is used by <code>Zone</code> to decide which Mib to return in cases where the Zone is not a member of
     * the self chain.
     *
     * @param aTime the time at which we received the update for this Mib
     */
    public void setTouched(long aTime);

    /**
     * @deprecated
     * @param anOwner
     */
    public void setZone(Zone anOwner);
}
