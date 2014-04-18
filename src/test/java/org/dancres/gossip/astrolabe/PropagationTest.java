package org.dancres.gossip.astrolabe;

import java.io.FileInputStream;
import java.util.HashSet;
import org.dancres.gossip.discovery.HostDetails;
import org.junit.*;
import org.junit.Assert.*;

public class PropagationTest {
    private Script _first;
    private Script _second;
    private Script _third;
    private Script _fourth;

    private Mib _sysMib;
    private Mib _rootMib;

    @Before public void init() throws Exception {
        String myId = "/org/dancres/dredd1";

		LocalID.set(myId);

    	HostDetails myContactDetails = new HostDetails("rhubarb", 8192);
    	HashSet<HostDetails> myContactsSet = new HashSet<>();
    	myContactsSet.add(myContactDetails);

		Zone myRoot = new Zone();

		_rootMib = myRoot.newMib(myId);
		myRoot.add(_rootMib);

		Zone myMachineZone = new Zone(myId);

		Mib myMib = myMachineZone.newMib(myId);
		myMachineZone.add(myMib);

		Zone mySystemZone = new Zone(myId + "/" + Zone.SYSTEM);

		_sysMib = mySystemZone.newMib(myId);
		mySystemZone.add(_sysMib);

		myMachineZone.add(mySystemZone);
		myRoot.add(myMachineZone);

        FileInputStream myStream = new FileInputStream("/Users/dan/src/gossip/config/copyable.bsh");

        _first = Script.create(myStream);
        myStream.close();

        myStream = new FileInputStream("/Users/dan/src/gossip/config/copyable2.bsh");
        _second = Script.create(myStream);
        myStream.close();

        myStream = new FileInputStream("/Users/dan/src/gossip/config/copyable3.bsh");
        _third = Script.create(myStream);
        myStream.close();

        myStream = new FileInputStream("/Users/dan/src/gossip/config/copyable4.bsh");
        _fourth = Script.create(myStream);
        myStream.close();
    }

    /**
     * “Preferable” is a partial order relation between certficates in the same
     * category: x > y means that certiﬁcate x is preferable to certificate y . x > y iff
     * (1) — x is valid, and y is not, or
     * (2) — x and y are both valid, the level of x is strong, and x.id is a child zone of y.id , or
     * (3) — x and y are both valid, x is not stronger than y , and x.issued > y.issued
     */
    @Test public void precedence() {
        // Test condition 2
        //
        Assert.assertTrue(_third.isPreferable(_fourth));

        // Test condition 3
        //
        Assert.assertTrue(_second.isPreferable(_first));
    }

    @Test public void testWeakProp() {
        _rootMib.getAttributes().put(_first.getName(), _first);
        new AggregationProcess().run();

        Assert.assertTrue(_sysMib.getAttributes().containsKey(_first.getName()));

        _rootMib.getAttributes().put(_first.getName(), _second);
        new AggregationProcess().run();

        long myIssued = Long.parseLong(((Script) _sysMib.getAttributes().get(_second.getName())).getAttribute(Script.ISSUED));

        Assert.assertTrue(myIssued == 2);
    }
}
