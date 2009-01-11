package org.dancres.gossip.astrolabe;

/**
 * The basic information required by the MIB sync'ing aspect of the gossip process.  Note that MibSummary is not
 * directly an <code>Exportable</code> as we only ever send collections of these instances which requires some
 * additional marshalling beyond what would make sense within this class.  The actual (un)marshalling is done in
 * {@link MibSummaryCodec}
 */
public class MibSummary {
	private String _id;
	private String _rep;
	private long _issued;
	
	public MibSummary(String anId, String aRep, long anIssued) {
		_id = anId;
		_rep = aRep;
		_issued = anIssued;
	}
	
	public String getId() {
		return _id;
	}
	
	public String getRep() {
		return _rep;
	}
	
	public long getIssued() {
		return _issued;
	}
	
	public boolean equals(Object anObject) {
		if (anObject instanceof MibSummary) {
			MibSummary mySummary = (MibSummary) anObject;
			return ((_id.equals(mySummary._id) && (_rep.equals(mySummary._rep) && (_issued == mySummary._issued))));			
		}
		
		return false;
	}
	
	public int hashCode() {
		return _id.hashCode();
	}
	
	public String toString() {
		return "MibSummary: " + _id + " from " + _rep + " at: " + _issued;
	}
}
