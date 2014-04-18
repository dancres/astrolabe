package org.dancres.gossip.astrolabe;

import java.util.Iterator;

/**
 * Astrolabe programmatical representation of a map (don't want all the cruft in <code>java.util.Map</code>)
 * for a {@link Mib}s attributes.
 */
public interface Attributes {
    public boolean containsKey(String aValue);
    public Object get(String aKey);

    /**
     * Always generates an event for the attribute update
     *
     * @param aKey is the name of the attribute
     * @param anObject is the value to associate with the name
     *
     * @throws IllegalArgumentException if an attempt is made to set the zone id or representative of a Mib
     */
    public void put(String aKey, Object anObject);

    public Iterator<String> getKeys();
}
