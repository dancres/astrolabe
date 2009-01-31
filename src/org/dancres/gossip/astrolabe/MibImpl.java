package org.dancres.gossip.astrolabe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.dancres.gossip.discovery.HostDetails;
import org.dancres.gossip.io.Exportable;

import com.google.gson.Gson;

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
public class MibImpl implements IMib {
	private static final String ISSUED_ATTR = "issued";
	private static final String REPRESENTATIVE_ATTR = "representative";
	private static final String NMEMBERS_ATTR = "nmembers";
	private static final String CONTACTS_ATTR = "contacts";
	private static final String SERVERS_ATTR = "servers";
	
	private ConcurrentHashMap _attributes;

	private long _touched;
	
	/**
	 * @param aRepresentative is the issuer (<code>LocalID.get()</code>) of the Mib.
	 */
	public MibImpl(String aRepresentative) {
		_attributes = new ConcurrentHashMap();
		
		setIssued(0);
		setRepresentative(aRepresentative);
		setNMembers(0);		
		setContacts(new HashSet<HostDetails>());
		setServers(new HashSet<HostDetails>());
	}
	
	public MibImpl(Reader aReader) throws IOException {
		Gson myGson = new Gson();
		BufferedReader myReader = new BufferedReader(aReader);
		GsonUtils myUtils = new GsonUtils(myGson, myReader);
		String myMapType = myGson.fromJson(myReader.readLine(), String.class);
		Map myAttrs = myUtils.readMap(myMapType);
		
		_attributes = new ConcurrentHashMap(myAttrs);
	}

    private MibImpl(long aTouched, ConcurrentHashMap anAttributes) {
        _touched = aTouched;
        _attributes = anAttributes;
    }

    public MibImpl dup() {
        return new MibImpl(_touched, new ConcurrentHashMap(_attributes));
    }

	public void setIssued(long anIssued) {
		_attributes.put(ISSUED_ATTR, new Long(anIssued));
	}

	public long getIssued() {
		return ((Long) _attributes.get(ISSUED_ATTR)).longValue();
	}
	
	private void setRepresentative(String aRep) {
		_attributes.put(REPRESENTATIVE_ATTR, aRep);
	}

	public String getRepresentative() {
		return (String) _attributes.get(REPRESENTATIVE_ATTR);
	}
	
	public void setContacts(Set aContacts) {
		_attributes.put(CONTACTS_ATTR, aContacts);				
	}
	
	/**
	 * @return an immutable collection of the contacts
	 */
	public Set getContacts() {
		return Collections.unmodifiableSet((Set) _attributes.get(CONTACTS_ATTR));
	}

	public void setServers(Set aContacts) {
		_attributes.put(SERVERS_ATTR, aContacts);				
	}
	
	/**
	 * @return an immutable collection of the servers
	 */
	public Set getServers() {
		return Collections.unmodifiableSet((Set) _attributes.get(SERVERS_ATTR));
	}

	public void setNMembers(long aNumMembers) {
		_attributes.put(NMEMBERS_ATTR, new Long(aNumMembers));
	}
	
	public long getNMembers() {
		return ((Long) _attributes.get(NMEMBERS_ATTR)).longValue();
	}
	
	/**
	 * Tracks the last time an update for this Mib from it's representative was received.  
	 * This is used by <code>Zone</code> to decide which Mib to return in cases where the Zone is not a member of
	 * the self chain.
	 * 
	 * @param aTime the time at which we received the update for this Mib
	 */
	public void setTouched(long aTime) {
		_touched = aTime;
	}
	
	/**
	 * Tracks the last time an update for this Mib from it's representative was received.  
	 * This is used by <code>Zone</code> to decide which Mib to return in cases where the Zone is not a member of
	 * the self chain.
	 */
	public long getTouched() {
		return _touched;
	}
	
	/**
	 * Use this method to gain access to the Mib's attributes for purposes of live modification.
	 * @return the live attributes
	 */
	public Attributes getAttributes() {
		return new AttributeFilter(_attributes);
	}
	
	public void export(Writer aWriter) throws IOException {
		Map myAttrs = new HashMap(_attributes);
		
		Gson myGson = new Gson();
		GsonUtils myUtils = new GsonUtils(myGson, aWriter);
		
		myUtils.writeMap(myAttrs);
	}

    private static class AttributeFilter implements Attributes {
        private ConcurrentHashMap _attributes;

        AttributeFilter(ConcurrentHashMap anAttributes) {
            _attributes = anAttributes;
        }

        public Object get(String aKey) {
            return _attributes.get(aKey);
        }

        public void put(String aKey, Object anObject) {
            _attributes.put(aKey, anObject);
        }

        public Iterator<String> getKeys() {
            return _attributes.keySet().iterator();
        }

        public boolean containsKey(String aValue) {
            return _attributes.containsKey(aValue);
        }

        public String toString() {
            return _attributes.toString();
        }
    }
}
