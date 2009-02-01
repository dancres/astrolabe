package org.dancres.gossip.astrolabe;

import org.junit.*;
import org.junit.Assert.*;

public class MibTest {
    private Zone _root;
    private Zone _left;
    private Zone _right;
    private Zone _rightChild;

    @Before public void init() {
        LocalID.set("/dancresl/rhubarb");

        _root = new Zone();
        _left = new Zone("/dancresl");
        _right = new Zone("/dancresr");
        _rightChild = new Zone("/dancresr/child");

        _root.add(_left);
        _root.add(_right);
        _right.add(_rightChild);

        Mib myMib = _root.newMib("/dancresl/rhubarb");
        _root.add(myMib);

        myMib = _right.newMib("/dancresr/custard");
        _right.add(myMib);

        myMib = _left.newMib("/dancresl/rhubarb");
        _left.add(myMib);

        myMib = _rightChild.newMib("/dancresr/deadcustard");
        _rightChild.add(myMib);

        myMib = _rightChild.newMib("/dancresr/soontobedeadcustard");
        _rightChild.add(myMib);
    }

    @Test public void checkZoneWontSet() {
        boolean didFail = false;

        try {
            _rightChild.getMib().getAttributes().put("zone", "shouldntwork");
        } catch (IllegalArgumentException anIE) {
            didFail = true;
        }

        Assert.assertTrue(didFail);
    }

    @Test public void checkRepWontSet() {
        boolean didFail = false;

        try {
            _rightChild.getMib().getAttributes().put("representative", "shouldntwork");
        } catch (IllegalArgumentException anIE) {
            didFail = true;
        }

        Assert.assertTrue(didFail);
    }
}
