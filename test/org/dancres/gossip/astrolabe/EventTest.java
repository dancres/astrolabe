package org.dancres.gossip.astrolabe;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import org.junit.*;
import org.junit.Assert.*;

public class EventTest {
    private Zone _root;
    private Zone _left;
    private Zone _right;
    private Zone _rightChild;

    @Before public void init() {
        LocalID.set("/dancresl/rhubarb");

        _root = new Zone();
        Mib myMib = _root.newMib("/dancresl/rhubarb");
        _root.add(myMib);
        _root.getQueue().clear();

        _left = new Zone("/dancresl");
        _right = new Zone("/dancresr");
        _rightChild = new Zone("/dancresr/child");

        _root.add(_left);
        _root.add(_right);
        _right.add(_rightChild);


        myMib = _right.newMib("/dancresr/custard");
        _right.add(myMib);

        myMib = _left.newMib("/dancresl/rhubarb");
        _left.add(myMib);

        myMib = _rightChild.newMib("/dancresr/deadcustard");
        _rightChild.add(myMib);

        myMib = _rightChild.newMib("/dancresr/soontobedeadcustard");
        _rightChild.add(myMib);
    }

    @Test public void checkQueue() throws IOException {
        Assert.assertTrue(_root.getQueue().getSize() == 2);

        _root.getMib().getAttributes().put("newKey", "aValue");

        Assert.assertTrue(_root.getQueue().getSize() == 3);

        Iterator<Event> myEvents = _root.getQueue().getEvents().iterator();

        Event myEvent = myEvents.next();

        Assert.assertTrue(myEvent.getType() == Event.ZONE_TYPE);
        Assert.assertTrue(myEvent.getOp() == Event.ZONE_ADD);
        Assert.assertTrue(myEvent.getZone().equals(_root.getId()));
        Assert.assertTrue(myEvent.getValue().equals("dancresl"));

        myEvent = myEvents.next();

        Assert.assertTrue(myEvent.getType() == Event.ZONE_TYPE);
        Assert.assertTrue(myEvent.getOp() == Event.ZONE_ADD);
        Assert.assertTrue(myEvent.getZone().equals(_root.getId()));
        Assert.assertTrue(myEvent.getValue().equals("dancresr"));

        myEvent = myEvents.next();

        Assert.assertTrue(myEvent.getType() == Event.ATTR_TYPE);
        Assert.assertTrue(myEvent.getOp() == Event.ATTR_SET);
        Assert.assertTrue(myEvent.getZone().equals(_root.getId()));
        Assert.assertTrue(myEvent.getValue().equals("newKey"));
    }
}
