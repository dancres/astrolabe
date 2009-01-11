package org.dancres.gossip.astrolabe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.google.gson.Gson;

/**
 * A class to convert a collection of MibSummary's to/from an http-friendly wire format (currently JSON).
 */
public class MibSummaryCodec {
	/**
	 * <b>Hack:</b> Reading a line and then feeding it to Gson as it seemingly over-consumes the Reader resulting in
	 * erroneous EOF.
	 * 
	 * @todo Fix hack
	 */	
	public Set<MibSummary> getSummary(Reader aReader) throws IOException {
		Gson myGson = new Gson();
		HashSet<MibSummary> mySet = new HashSet<MibSummary>();
		
		BufferedReader myReader = new BufferedReader(aReader);
		String myInput = myReader.readLine();
		
		long myTotal = (myGson.fromJson(myInput, Long.class)).longValue();
		
		for (long i = 0; i < myTotal; i++) {
			myInput = myReader.readLine();
			String myId = myGson.fromJson(myInput, String.class);
			
			myInput = myReader.readLine();
			String myRep = myGson.fromJson(myInput, String.class);
			
			myInput = myReader.readLine();
			long myIssued = (myGson.fromJson(myInput, Long.class)).longValue();
			
			mySet.add(new MibSummary(myId, myRep, myIssued));
		}
		
		return mySet;
	}
	
	public void putSummary(Set<MibSummary> aSummary, Writer aWriter) throws IOException {
		Gson myGson = new Gson();
		
		myGson.toJson(aSummary.size(), aWriter);
		aWriter.write("\n");
		
		Iterator<MibSummary> mySummaries = aSummary.iterator();
		while (mySummaries.hasNext()) {
			MibSummary mySummary = mySummaries.next();
			
			myGson.toJson(mySummary.getId(), aWriter);
			aWriter.write("\n");
			
			myGson.toJson(mySummary.getRep(), aWriter);
			aWriter.write("\n");
			
			myGson.toJson(mySummary.getIssued(), aWriter);
			aWriter.write("\n");
		}
	}
}
