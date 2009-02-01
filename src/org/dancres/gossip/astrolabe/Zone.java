package org.dancres.gossip.astrolabe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.dancres.gossip.discovery.HostDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>A Zone represents some partition within the Astrolabe tree.  There is a root, and branches, typically
 * mapped in some way to administrative zones (e.g. by network or datacentre), the leaf Zones represent individual
 * machines and these contain "virtual" zones which can be created by Agents and those that connect to Agents.  There
 * is a default virtual zone, system which contains basic machine attributes such as contact addresses, a machine count
 * (which will be one).</p>
 * 
 * <p>Each Zone contains a {@link Mib} which holds some standard attributes and any custom attributes defined.  In fact
 * due to the means by which gossip occurs and the fact that a number of nodes will compute a given Mib, a Zone may
 * track more than one Mib (one per representative.</p>
 * 
 * <p>If the zone forms part of the "self" structure (i.e. it appears in the path that is the local machine's id) then
 * the locally computed Mib takes precedent.  Otherwise, the Mib that was most recently updated takes precedent.
 * This precedence applies when exposing a set of attributes to aggregate against.  Because we cannot declare any
 * particular Mib the most up-to-date (other than at the representative that generated it) we must maintain multiple
 * Mibs per Zone.</p>
 */
public class Zone {
	private static Logger _logger = LoggerFactory.getLogger(Zone.class);

	public static final String SYSTEM = "system";

	private static String ROOT = "";
	
	private String _id;
	private String _name;
	
	private HashMap<String, Mib> _mibs = new HashMap<String, Mib>();
	private ConcurrentHashMap<String, Zone> _children = new ConcurrentHashMap<String, Zone>();
	
	private boolean _isSelf;
	private Zone _parent;

    private EventQueue _queue = new EventQueue();
	
	/**
	 * Create the a root Zone.
	 */
	public Zone() {
		_parent = null;
		_id = ROOT;
		_name = "<root>";

		// Always belongs to "self"
		//
		_isSelf = true;

        Zones.setRoot(this);
	}
		
	/**
	 * Create a child Zone.
	 * @param anId a path for the zone - must start with "/" and end with a name (not a "/")
	 */
	public Zone(String anId) throws IllegalArgumentException {
		if (! anId.startsWith("/"))
			throw new IllegalArgumentException("Zone ids must start with /: " + anId);
		if (anId.endsWith("/"))
			throw new IllegalArgumentException("Zone must end with a name not a /: " + anId);

		_id = anId;
		
		int myIndex = anId.lastIndexOf("/");
		_name = anId.substring(myIndex + 1);
		
		// If we're part of the path for the local id, we belong to "self"
		//
		_isSelf = (LocalID.get().startsWith(anId));
	}

    public EventQueue getQueue() {
        return _queue;
    }

    public Mib newMib(String aRepresentative) {
        return new MibImpl(this, aRepresentative);
    }

	public void add(Mib aMib) {
		synchronized(_mibs) {
			Mib myExisting = _mibs.get(aMib.getRepresentative());

			if ((myExisting != null) && (myExisting.getIssued() > aMib.getIssued())) {
				_logger.warn("Discarding a MIB: " + myExisting.getRepresentative() + " " + myExisting.getIssued() + 
						aMib.getIssued());
				return;
			}

			aMib.setTouched(System.currentTimeMillis());			
			_mibs.put(aMib.getRepresentative(), aMib);
		}
	}

    private void cleanMibs(long aDeadTime) {
        synchronized(_mibs) {
            /*
             * Mibs generated by this node in it's self-tree do not have their "touched" times maintained
             * as they aren't touched by the normal gossip process which only updates "touched" of Mibs from
             * other nodes.  Thus we must force the update before we do any cleaning.
             */
            if (isSelf()) {
                _mibs.get(LocalID.get()).setTouched(System.currentTimeMillis());
            }

            Iterator<Mib> myMibs = _mibs.values().iterator();

            // We must leave a Mib present (even if it's untouched) in case someone calls getMib(), we don't want
            // random NullPointerExceptions
            //
            while ((myMibs.hasNext()) && (_mibs.size() != 1)) {
                Mib myMib = myMibs.next();

                if (myMib.getTouched() <= aDeadTime) {
                    _mibs.remove(myMib.getRepresentative());
                }
            }
        }
    }

	/**
	 * Get the Mib for this zone
	 * 
	 * @return the Mib generated by this representative if the zone is part of "self", otherwise the most recently
	 * updated Mib provided by some representative.
	 */
	public Mib getMib() {
		synchronized(_mibs) {
			if (_isSelf) {
				return _mibs.get(LocalID.get());
			} else {
				Mib myRecent = null;

				Iterator<Mib> myMibs = _mibs.values().iterator();
				while (myMibs.hasNext()) {
					if (myRecent == null) {
						myRecent = myMibs.next();
					} else {
						Mib myNext = myMibs.next();
						if (myRecent.getTouched() < myNext.getTouched())
							myRecent = myNext;
					}
				}

				return myRecent;
			}
		}
	}

	/**
	 * Get a Mib for this zone produced by a specific representative
	 * 
	 * @param aRep the representative that should have produced the Mib
	 * @return the Mib or <code>null</code> if there is no Mib associated with the representative
	 */
	public Mib getMib(String aRep) {
		synchronized(_mibs) {
			return _mibs.get(aRep);
		}
	}

    /**
     * @return a list containing the the most recent Mib update from each representative encountered.
     */
	public Collection<Mib> getMibs() {
		ArrayList<Mib> myMibs = new ArrayList<Mib>();
		
		synchronized(_mibs) {
			myMibs.addAll(_mibs.values());
		}
		
		return myMibs;
	}
	
	/**
	 * @return the set of Mib summaries (id, rep and issued) relevant to this zone for gossip
	 */
	public Set<MibSummary> getMibSummaries() {
		Set<MibSummary> mySummaries = new HashSet<MibSummary>();
		Zone myCurrent = this;
		do {
			Collection<Zone> myZoneList = myCurrent.getChildren();
			Iterator<Zone> myZones = myZoneList.iterator();
			while (myZones.hasNext()) {
				Zone myZone = myZones.next();
				Iterator<Mib> myMibs = myZone.getMibs().iterator();
				while (myMibs.hasNext()) {
					Mib myMib = myMibs.next();
					mySummaries.add(new MibSummary(myZone.getId(), myMib.getRepresentative(), myMib.getIssued()));
				}
			}

			myCurrent = myCurrent.getParent();
		} while (myCurrent != null);
		
		return mySummaries;
	}
		
	/**
	 * @return A collection consisting of the appropriate Mib (as returned by <code>getMib()</code> of each child zone
	 */
	public Collection<Mib> getChildMibs() {
		Iterator<Zone> myChildren = getChildren().iterator();
		ArrayList<Mib> myMibs = new ArrayList<Mib>();
		
		while (myChildren.hasNext()) {
			myMibs.add(myChildren.next().getMib());
		}
		
		return myMibs;
	}
	
	public String getName() {
		return _name;
	}

    /**
     * Recurse the tree of zones, from this instance clearing out those that have expired (weren't updated
     * within the specified number of milliseconds).
     *
     * @param anExpiryPeriod the period (backwards from the point at which this method is called) within which an update
     * must have been received for a Mib to remain valid (and unculled).
     */
    public void cull(long anExpiryPeriod) {
        internalCull(System.currentTimeMillis() - anExpiryPeriod);
        _queue.cull();
    }

    private void internalCull(long aDeadTime) {
        /*
         * Note:  We do this in a fashion that can lead to a race condition where we decide to remove a Zone just as a
         * new Mib update arrives.  This might seem bad but if things are performing normally, there will be more updates
         * that will reintroduce the zone later.  The reason we're invalidating the zone is due to a dearth of Mib updates
         * and thus the update we'll have missed is the first in some substantial period of time, indicating we've had
         * some kind of problem which we're now recovering from.  Thus to indicate for slightly longer than is accurate
         * that a zone is dead is not a big deal.
         */
        cleanMibs(aDeadTime);

        Iterator<Zone> myZones = _children.values().iterator();

        while (myZones.hasNext()) {
            Zone myZone = myZones.next();

            if (myZone.isDead(aDeadTime)) {
                _children.remove(myZone.getName());
                _queue.add(new Event(Event.ZONE_TYPE, Event.ZONE_REMOVE, _id, myZone.getName()));
            } else
                myZone.internalCull(aDeadTime);
        }
    }

	/**
	 * @return a copy of the child zones of this zone
	 */
	public Collection<Zone> getChildren() {
		ArrayList<Zone> myList = new ArrayList<Zone>();
		myList.addAll(_children.values());

		return myList;
	}

	/**
	 * Get a specific child zone
	 */
	public Zone get(String aName) {
		return _children.get(aName);
	}
	
	/**
	 * Given the full path of a Zone find it via this Zone.
	 * 
	 * @param aPath the path of the Zone we wish to locate
	 * @return the Zone if it's found.
	 */
	public Zone find(String aPath) {
		String myRemainder;
		
		if (getId() == ROOT) {
            if (aPath.equals(ROOT))
                return this;

			myRemainder = aPath;
		} else {
			if (! aPath.startsWith(getId()))
				throw new IllegalArgumentException(getId() + "is not a parent for this zone: " + aPath);
			
			myRemainder = aPath.substring(getId().length());			
		}
		
		return relativeFind(myRemainder);
	}
	
	private Zone relativeFind(String aPath) {
		// Ignore the first slash, find the next
		//
		int myNextSep = aPath.indexOf('/', 1);
		
		if (myNextSep == -1) {
			// We've reached the end of the path, the zone is my child or doesn't exist
			//
			return _children.get(aPath.substring(1));
		} else {
			String myChildZone = aPath.substring(1, myNextSep);
			Zone myChild = _children.get(myChildZone);
			
			if (myChild == null) {
				// End of the path but didn't find the target
				//
				return null;
			} else {
				return myChild.relativeFind(aPath.substring(myNextSep));
			}
		}		
	}
	
	/**
	 * Add the named Zone in the appropriate place under this Zone.
	 * 
	 * @param aZone
	 */
	public void add(Zone aZone) {
		String myRemainder;
		
		if (getId() == ROOT) {
			myRemainder = aZone.getId();
		} else {
			if (! aZone.getId().startsWith(getId()))
				throw new IllegalArgumentException(getId() + "is not a parent for this zone: " + aZone.getId());
			
			myRemainder = aZone.getId().substring(getId().length());
		}
		
		relativeAdd(myRemainder, aZone);
	}
	
	private void relativeAdd(String aPath, Zone aZone) {
		// Ignore the first slash, find the next
		//
		int myNextSep = aPath.indexOf('/', 1);
		
		if (myNextSep == -1) {
			// We've reached the end of the path, add the zone here
			//
			String myChildZone = aPath.substring(1);
			aZone.setParent(this);
			addChild(myChildZone, aZone);
			
			return;			
		} else {
			String myChildZone = aPath.substring(1, myNextSep);			
			Zone myChild = _children.get(myChildZone);
			
			if (myChild == null) {
				myChild = new Zone(getId() + "/" + myChildZone);
				myChild.setParent(this);
				
				Mib myMib = myChild.newMib(LocalID.get());
				myChild.add(myMib);
				addChild(myChildZone, myChild);
			}
			
			myChild.relativeAdd(aPath.substring(myNextSep), aZone);
		}
	}
	
    private void addChild(String aName, Zone aChild) {
        _children.put(aName, aChild);
        _queue.add(new Event(Event.ZONE_TYPE, Event.ZONE_ADD, _id, aName));
    }

	private void setParent(Zone aZone) {
		if (_id.equals(ROOT))
			throw new IllegalArgumentException("Root Zone cannot have a root");
		
		_parent = aZone;
	}

	public Zone getParent() {
		return _parent;
	}
	
	public String getId() {
		return _id;
	}

	public boolean isSelf() {
		return _isSelf;
	}

    public boolean isChildOf(String aParentId) {
        Zone myCurrent = getParent();

        while (myCurrent != null) {
            if (myCurrent.getId().equals(aParentId))
                return true;
        }

        return false;
    }
	
	public String toString() {
		return "Zone: " + _id;
	}
	
    /**
     * A zone is dead if it has no child zones and all it's Mibs are expired
     * 
     * @param anExpiryTime the time after which a Mib must have been updated to be declared live
     * @return whether the zone is dead in respect of the specified time.
     */
    public boolean isDead(long anExpiryTime) {
        if (isSelf())
            return false;

        synchronized(_mibs) {
            Iterator<Mib> myMibs = _mibs.values().iterator();

            while (myMibs.hasNext()) {
                Mib myMib = myMibs.next();

                if (myMib.getTouched() > anExpiryTime)
                    return false;
            }
        }

        return (_children.size() == 0);
    }

	public void dumpTree(String anIndent) {
		dumpTree(this, anIndent);
	}
	
	public void dumpTree(Zone aZone, String anIndent) {
		String myName = aZone.getName();
		
		System.out.println(anIndent + myName + ": " + aZone.getMib().getIssued() + ", " + 
				aZone.getMib().getNMembers() + ", " +
				aZone.getMib().getRepresentative() + ", " + aZone.isSelf() + ", " + aZone.getMib().getTouched());
		
		Iterator<Zone> myZones = aZone.getChildren().iterator();
		while (myZones.hasNext()) {
			dumpTree(myZones.next(), anIndent + "  ");
		}
	}			
}
