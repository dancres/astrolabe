package org.dancres.gossip.astrolabe;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.Writer;
import org.dancres.gossip.io.Exportable;

/**
 * Events come in two forms:
 *
 * <ol>
 * <li>Zone type with operations of add or remove.  First value is the enclosing zone, the second is the child</li>
 * <li>Attribute type with operation set.  First value is the enclosing zone, second is the key</li>
 * </ol>
 */
public class Event implements Exportable {
    public static final int ZONE_TYPE = 1;
    public static final int ATTR_TYPE = 2;

    public static final int ZONE_ADD = 1;
    public static final int ZONE_REMOVE = 2;
    public static final int ATTR_SET = 3;

    private int _type;
    private int _op;
    private String _zone;
    private String _value;
    private long _timestamp;

    public Event() {
    }

    public Event(int aType, int anOp, String aZone, String aValue) {
        _type = aType;
        _op = anOp;
        _zone = aZone;
        _value = aValue;
        _timestamp = System.currentTimeMillis();
    }

    public void export(Writer aWriter) throws IOException {
        Gson myGson = new Gson();
        myGson.toJson(this, aWriter);
    }

    public int getType() {
        return _type;
    }

    public int getOp() {
        return _op;
    }

    public String getZone() {
        return _zone;
    }

    public String getValue() {
        return _value;
    }

    public long getTimestamp() {
        return _timestamp;
    }

    public String toString() {
        return "Evt: " + _type + ": " + _op + " " + _zone + ", " + _value + " @ " + _timestamp;
    }

    boolean hasExpired(long myExpiry) {
        return (myExpiry > _timestamp);
    }
}
