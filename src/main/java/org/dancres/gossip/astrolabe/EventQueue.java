package org.dancres.gossip.astrolabe;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.dancres.gossip.io.Exportable;

/**
 * Tracks all the interesting happenings in a Zone
 */
public class EventQueue implements Exportable {
    private ArrayList<Event> _events = new ArrayList<>();

    public void add(Event anEvent) {
        synchronized(this) {
            _events.add(anEvent);
        }
    }

    public void cull() {
        long myExpiry = System.currentTimeMillis() - (30 * 1000);

        synchronized(this) {
            Iterator<Event> myEvents = _events.iterator();

            while (myEvents.hasNext()) {
                Event myEvent = myEvents.next();

                if (myEvent.hasExpired(myExpiry))
                    myEvents.remove();;
            }
        }
    }

    public void export(Writer aWriter) throws IOException {
        Gson myGson = new GsonBuilder().serializeNulls().setDateFormat(DateFormat.LONG).
                setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).setPrettyPrinting().setVersion(1.0).create();

        synchronized(this) {
            myGson.toJson(_events, aWriter);
        }
    }

    public int getSize() {
        synchronized(this) {
            return _events.size();
        }
    }

    public List<Event> getEvents() {
        synchronized(this) {
            ArrayList<Event> myDupe = new ArrayList<>(_events);

            return myDupe;
        }
    }

    public void clear() {
        synchronized(this) {
            _events.clear();
        }
    }
}
