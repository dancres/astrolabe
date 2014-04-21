package org.dancres.gossip.astrolabe;

import java.util.Collection;

import org.junit.*;

public class ZoneTest {
	private Zone _Root;
	private Zone _First;
	private Zone _Second;
	private Zone _Third;
	private Zone _Fourth;
	
	@Before public void init() {
		LocalID.set("/rhubarb/custardcreme");
		
		_Root = new Zone();
		_First = new Zone("/rhubarb");
		_Second = new Zone("/rhubarb/custardcreme");
		_Third = new Zone("/custard");
		_Fourth = new Zone("/custard/birdies");				

		_Root.add(_First);
		_Root.add(_Second);
		_Root.add(_Third);
		_Root.add(_Fourth);		
	}
	
	@Test public void workingAdd() {
		Collection<Zone> myChildren = _Root.getChildren();
		Assert.assertTrue(myChildren.size() == 2);
		Assert.assertTrue(myChildren.contains(_First));
		Assert.assertTrue(myChildren.contains(_Third));
		
		myChildren = _First.getChildren();
		Assert.assertTrue(myChildren.contains(_Second));
		
		myChildren = _Third.getChildren();
		Assert.assertTrue(myChildren.contains(_Fourth));
		
		// dumpTree(_Root, "");		
	}

    @Test public void rootFindRoot() {
        Zone myZone = _Root.find("");
        Assert.assertTrue(myZone.getId().equals(_Root.getId()));
    }

	@Test public void rootFind() {
		Zone myZone = _Root.find(_Second.getId());
		Assert.assertTrue(myZone.getId().equals(_Second.getId()));
	}
	
	@Test public void noParentForRoot() {
		Assert.assertTrue((_Root.getParent() == null));
	}
	
	@Test public void fourthParent() {
		Zone myParent = _Fourth.getParent();
		Assert.assertTrue(myParent.equals(_Third));
	}
	
	@Test public void thirdParent() {
		Zone myParent = _Third.getParent();
		Assert.assertTrue(myParent.equals(_Root));
	}
	
	@Test public void relFind() {
		Zone myZone = _First.find(_Second.getId());
		Assert.assertTrue(myZone.getId().equals(_Second.getId()));		
	}
	
	@Test public void noNodeToFind() {
		Zone myZone = _Root.find("/rhubarb/doesntexist");
		Assert.assertTrue((myZone == null));
	}
	
	@Test public void getChild() {
		Zone myZone = _Third.get("birdies");
		Assert.assertTrue((myZone.getId().equals(_Fourth.getId())));
	}
	
	@Test public void noPathToFindNode() {
		boolean passed = false;
		
		try {
			_Second.find(_Fourth.getId());
		} catch (IllegalArgumentException anE) {
			passed = true;
		}
		
		Assert.assertTrue(passed);				
	}
	
	@Test public void notRooted() {
		boolean passed = false;
		
		try {
			new Zone("custard/rhubarb/zone");
		} catch (IllegalArgumentException anE) {
			passed = true;
		}
		
		Assert.assertTrue(passed);				
	}
	
	@Test public void notNamed() {
		boolean passed = true;
		
		try {
			new Zone("custard/rhubarb/");
		} catch (IllegalArgumentException anE) {
			passed = true;
		}
		
		Assert.assertTrue(passed);					
	}

	@Test public void badAdd() {		
		boolean passed = false;
		
		try {
			_Fourth.add(new Zone("/custard/rhubarb/zone"));
		} catch (IllegalArgumentException anE) {
			passed = true;
		}
		
		Assert.assertTrue(passed);		
	}
	
	@Test public void relAdd() {
		_Fourth.add(new Zone("/custard/birdies/tweet"));
	}
	
	private void dumpTree(Zone aZone, String anIndent) {
		String myName = aZone.getName();
		
		System.out.println(anIndent + myName);

        for (Zone myZone : aZone.getChildren()) {
			dumpTree(myZone, anIndent + "  ");
		}
	}
}
