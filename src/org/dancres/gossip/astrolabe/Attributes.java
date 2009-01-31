package org.dancres.gossip.astrolabe;

import java.util.Iterator;

/**
 * Astrolabe programmatical representation of a map (don't want all the cruft in <code>java.util.Map</code>)
 */
public interface Attributes {
    public boolean containsKey(String aValue);
    public Object get(String aKey);
    public void put(String aKey, Object anObject);
    public Iterator<String> getKeys();
}
