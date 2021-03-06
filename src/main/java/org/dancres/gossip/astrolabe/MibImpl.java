package org.dancres.gossip.astrolabe;

import org.dancres.gossip.io.GsonUtils;
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

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class MibImpl implements Mib {
    private static Logger _logger = LoggerFactory.getLogger(MibImpl.class);

	private ConcurrentHashMap<String, Object> _attributes;

	private long _touched;
	
	/**
     * Use <code>{@link Zone}.newMib()</code> to construct a Mib rather than this method as it reduces the risk of having
     * mismatched zone id's across Zone and associated Mibs.
     *
     * @param aZone is the zone to which this Mib is attached
	 * @param aRepresentative is the issuer (<code>LocalID.get()</code>) of the Mib.
	 */
	public MibImpl(Zone aZone, String aRepresentative) {
		_attributes = new ConcurrentHashMap<>();

        setZone(aZone.getId());
		setRepresentative(aRepresentative);
		_attributes.put(ISSUED_ATTR, 0);
		_attributes.put(NMEMBERS_ATTR, 0);
	    _attributes.put(CONTACTS_ATTR, new HashSet<HostDetails>());
		_attributes.put(SERVERS_ATTR, new HashSet<HostDetails>());
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
        return new MibImpl(_touched, new ConcurrentHashMap<>(_attributes));
    }

    private void setZone(String anId) {
        _attributes.put(ZONE_ATTR, anId);
    }

    public String getZoneId() {
        return (String) _attributes.get(ZONE_ATTR);
    }

	private void setRepresentative(String aRep) {
		_attributes.put(REPRESENTATIVE_ATTR, aRep);
	}

	public String getRepresentative() {
		return (String) _attributes.get(REPRESENTATIVE_ATTR);
	}

	public void setIssued(long anIssued) { getAttributes().put(ISSUED_ATTR, Long.toString(anIssued));
	}

	public long getIssued() { return (new Long(_attributes.get(ISSUED_ATTR).toString()));
	}

	public void setContacts(Set<HostDetails> aContacts) {
		getAttributes().put(CONTACTS_ATTR, aContacts);
	}
	
	/**
	 * @return an immutable collection of the contacts
	 */
	public Set<HostDetails> getContacts() { return Collections.unmodifiableSet((Set<HostDetails>) _attributes.get(CONTACTS_ATTR));
	}

	public void setServers(Set<HostDetails> aContacts) {
		getAttributes().put(SERVERS_ATTR, aContacts);
	}
	
	/**
	 * @return an immutable collection of the servers
	 */
	public Set<HostDetails> getServers() { return Collections.unmodifiableSet((Set<HostDetails>) _attributes.get(SERVERS_ATTR));
	}

	public void setNMembers(long aNumMembers) { getAttributes().put(NMEMBERS_ATTR, aNumMembers);
	}
	
	public long getNMembers() {
		return ((Long) _attributes.get(NMEMBERS_ATTR));
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
     *
	 * @return the live attributes
	 */
	public Attributes getAttributes() {
		return new AttributeFilter(_attributes);
	}
	
	public void export(Writer aWriter) throws IOException {
		Map myAttrs = new HashMap<>(_attributes);
		
		Gson myGson = new Gson();
		GsonUtils myUtils = new GsonUtils(myGson, aWriter);
		
		myUtils.writeMap(myAttrs);
	}

    private class AttributeFilter implements Attributes {
        private ConcurrentHashMap<String, Object> _attributes;

        AttributeFilter(ConcurrentHashMap<String, Object> anAttributes) {
            _attributes = anAttributes;
        }

        public Object get(String aKey) {
            return _attributes.get(aKey);
        }

        /**
         * @todo Add detection of past object and equals() tests to generate events only on genuine change
         */
        public void put(String aKey, Object anObject) {
            if ((aKey.equals(ZONE_ATTR)) || (aKey.equals(REPRESENTATIVE_ATTR)))
                throw new IllegalArgumentException("Cannot change the zone id/rep");

            // We compare the new value with the old to determine if something genuinely changed
            //
            Object myOld;

            // Only propogate a script if it's origin zone matches this Zone or a parent of this Zone
            //
            if (anObject instanceof Script) {
                Script myMergeScript = (Script) anObject;
                String myOrigin = myMergeScript.getAttribute(Script.ORIGIN);
                Zone myCurrent = Zones.getRoot().find(getZoneId());

                do {
                    if (myOrigin.equals(myCurrent.getId())) {
                        myOld = _attributes.put(aKey, anObject);
                        break;
                    }

                    myCurrent = myCurrent.getParent();
                } while (myCurrent != null);

                return;
            } else {
                myOld = _attributes.put(aKey, anObject);
            }

            // In normal processing the zone should always be found but during initialisation, things can get messy and
            // we may not be able to generate the event
            //
            Zone myZone = Zones.getRoot().find(getZoneId());
            if (myZone != null) {

                // If there was no old object or the old and new objects are different, generate an event
                //
                if ((myOld == null) || (! myOld.equals(anObject))) {
                    myZone.getQueue().add(new Event(Event.ATTR_TYPE, Event.ATTR_SET, getZoneId(), aKey));
                }
            } else
                _logger.debug("Couldn't send an event for: " + getZoneId(), new Exception());
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
