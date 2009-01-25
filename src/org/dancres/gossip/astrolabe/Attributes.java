package org.dancres.gossip.astrolabe;

import java.util.Iterator;

public interface Attributes {
    public boolean containsKey(String aValue);
    public Object get(String aKey);
    public void put(String aKey, Object anObject);
    public Iterator<String> getKeys();
}
