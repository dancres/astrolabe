package org.dancres.gossip.astrolabe;

import org.junit.*;
import org.junit.Assert.*;

public class CullTest {

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

        long myTimeInTheFuture = System.currentTimeMillis() + (5 * 60 * 1000);

        // Add does a setTouched, thus we must override it after the add() to get what we want
        //
        Mib myMib = _root.newMib("/dancresl/rhubarb");
        _root.add(myMib);
        myMib.setTouched(myTimeInTheFuture);

        myMib = _right.newMib("/dancresr/custard");
        _right.add(myMib);
        myMib.setTouched(System.currentTimeMillis() + (8 * 1000));

        myMib = _left.newMib("/dancresl/rhubarb");
        _left.add(myMib);
        myMib.setTouched(myTimeInTheFuture);

        myMib = _rightChild.newMib("/dancresr/deadcustard");
        _rightChild.add(myMib);

        // Mib should die on the first cull in the test
        //
        myMib.setTouched(1);

        myMib = _rightChild.newMib("/dancresr/soontobedeadcustard");
        _rightChild.add(myMib);

        // Mib should die on the second cull in the test
        //
        myMib.setTouched(System.currentTimeMillis() + (2 * 1000));
    }

    @Test public void testcull() throws Exception {
        // Right child should have two mibs to start with
        //
        Assert.assertTrue(_rightChild.getMibs().size() == 2);

        _root.cull(8000);

        // Right child should be one mib down
        //
        Assert.assertTrue(_rightChild.getMibs().size() == 1);

        try {
            Thread.sleep(10 * 1000);
        } catch (InterruptedException anIE) {
            throw new Exception("Got interrupted, shouldn't happen");
        }

        _root.cull(8000);

        // Right child should have one mib
        //
        Assert.assertTrue(_rightChild.getMibs().size() == 1);

        // Right should be dead
        Assert.assertTrue(_rightChild.isDead(System.currentTimeMillis() - 8000));

        // Right child should not appear in tree
        //
        Zone myChild = _right.find("/dancresr/child");
        Assert.assertNull(myChild);

        // Right should still appear even though it has a dead mib
        //
        myChild = _root.find("/dancresr");
        Assert.assertNotNull(myChild);
        Assert.assertTrue(_right.getMibs().size() == 1);

        try {
            Thread.sleep(10 * 1000);
        } catch (InterruptedException anIE) {
            throw new Exception("Got interrupted, shouldn't happen");
        }

        _root.cull(8000);

        /*
         * Right zone should now be dead, wasn't previously because it had children.
         */
        myChild = _root.find("/dancresr");
        Assert.assertNull(myChild);
    }
}
