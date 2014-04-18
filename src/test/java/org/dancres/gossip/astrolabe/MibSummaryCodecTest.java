package org.dancres.gossip.astrolabe;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MibSummaryCodecTest {
	private Set<MibSummary> _emptySummary;
	private Set<MibSummary> _populatedSummary;
	
	@Before public void init() {
		_emptySummary = new HashSet<>();
		_populatedSummary = new HashSet<>();
		
		_populatedSummary.add(new MibSummary("/dancres/dredd/1", "/dancres/dredd", 12345));
		_populatedSummary.add(new MibSummary("/dancres/dredd/2", "/dancres/dredd", 67890));		
	}
	
	@Test public void writeEmptySummary() throws Exception {
		MibSummaryCodec myCodec = new MibSummaryCodec();
		Writer myWriter = new OutputStreamWriter(new ByteArrayOutputStream());

		myCodec.putSummary(_emptySummary, myWriter);
		myWriter.close();
	}

	@Test public void writePopulatedSummary() throws Exception {
		MibSummaryCodec myCodec = new MibSummaryCodec();
		Writer myWriter = new OutputStreamWriter(new ByteArrayOutputStream());

		myCodec.putSummary(_populatedSummary, myWriter);
		myWriter.close();
	}
	
	@Test public void readEmptySummary() throws Exception {
		ByteArrayOutputStream myBuffer = new ByteArrayOutputStream();
		MibSummaryCodec myCodec = new MibSummaryCodec();
		Writer myWriter = new OutputStreamWriter(myBuffer);

		myCodec.putSummary(_emptySummary, myWriter);
		myWriter.close();
		
		ByteArrayInputStream myInput = new ByteArrayInputStream(myBuffer.toByteArray());
		Reader myReader = new InputStreamReader(myInput);
		
		Set<MibSummary> mySummary = myCodec.getSummary(myReader);
		Assert.assertTrue(mySummary.size() == 0);
	}

	@Test public void readPopulatedSummary() throws Exception {
		ByteArrayOutputStream myBuffer = new ByteArrayOutputStream();
		MibSummaryCodec myCodec = new MibSummaryCodec();
		Writer myWriter = new OutputStreamWriter(myBuffer);

		myCodec.putSummary(_populatedSummary, myWriter);
		myWriter.close();
		
		ByteArrayInputStream myInput = new ByteArrayInputStream(myBuffer.toByteArray());
		Reader myReader = new InputStreamReader(myInput);
		
		Set<MibSummary> mySummary = myCodec.getSummary(myReader);
		Assert.assertTrue(mySummary.size() == _populatedSummary.size());
	}
}
