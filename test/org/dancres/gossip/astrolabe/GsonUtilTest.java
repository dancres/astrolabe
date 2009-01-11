package org.dancres.gossip.astrolabe;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.dancres.gossip.discovery.HostDetails;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;

public class GsonUtilTest {
	private List _emptyList;
	private List _populatedList;
	private Map _emptyMap;
	private Map _populatedMap;
	
	@Before public void init() {
		_emptyList = new ArrayList();

		_populatedList = new LinkedList();
		
		HostDetails myDetails = new HostDetails("rhubarb", 12345);
		_populatedList.add(myDetails);
		myDetails = new HostDetails("custard", 12345);
		_populatedList.add(myDetails);
		
		_emptyMap = new HashMap();
		
		_populatedMap = new TreeMap();
		_populatedMap.put("integer", new Integer(5));
		_populatedMap.put("collection", _populatedList);
	}
	
	@Test public void testEmptyMapWrite() throws Exception {
		Gson myGson = new Gson();
		
		Writer myWriter = new OutputStreamWriter(new ByteArrayOutputStream());
		
		GsonUtils myUtils = new GsonUtils(myGson, myWriter);
		myUtils.writeMap(_emptyMap);		
	}
	
	@Test public void testPopulatedMapWrite() throws Exception {
		Gson myGson = new Gson();
		Writer myWriter = new OutputStreamWriter(new ByteArrayOutputStream());
		
		GsonUtils myUtils = new GsonUtils(myGson, myWriter);
		myUtils.writeMap(_populatedMap);		
	}

	@Test public void testEmptyCollectionWrite() throws Exception {
		Gson myGson = new Gson();
		Writer myWriter = new OutputStreamWriter(new ByteArrayOutputStream());
		
		GsonUtils myUtils = new GsonUtils(myGson, myWriter);
		myUtils.writeCollection(_emptyList);		
	}

	@Test public void testPopulatedCollectionWrite() throws Exception {
		Gson myGson = new Gson();
		Writer myWriter = new OutputStreamWriter(new ByteArrayOutputStream());
		
		GsonUtils myUtils = new GsonUtils(myGson, myWriter);
		myUtils.writeCollection(_populatedList);		
	}

	@Test public void testEmptyMapRead() throws Exception {
		Gson myGson = new Gson();
		ByteArrayOutputStream myBuffer = new ByteArrayOutputStream();
		Writer myWriter = new OutputStreamWriter(myBuffer);
		
		GsonUtils myUtils = new GsonUtils(myGson, myWriter);
		myUtils.writeMap(_emptyMap);
		myWriter.close();
		
		ByteArrayInputStream myInput = new ByteArrayInputStream(myBuffer.toByteArray());
		BufferedReader myReader = new BufferedReader(new InputStreamReader(myInput));
		
		myUtils = new GsonUtils(myGson, myReader);
		String myLine = myReader.readLine();
		String myType = myGson.fromJson(myLine, String.class);
		
		Map myResult = myUtils.readMap(myType);
		Assert.assertTrue(myResult.size() == 0);
	}
	
	@Test public void testPopulatedMapRead() throws Exception {
		Gson myGson = new Gson();
		ByteArrayOutputStream myBuffer = new ByteArrayOutputStream();
		Writer myWriter = new OutputStreamWriter(myBuffer);
		
		GsonUtils myUtils = new GsonUtils(myGson, myWriter);
		myUtils.writeMap(_populatedMap);
		myWriter.close();
		
		ByteArrayInputStream myInput = new ByteArrayInputStream(myBuffer.toByteArray());
		BufferedReader myReader = new BufferedReader(new InputStreamReader(myInput));
		
		myUtils = new GsonUtils(myGson, myReader);
		String myLine = myReader.readLine();
		String myType = myGson.fromJson(myLine, String.class);
		
		Map myResult = myUtils.readMap(myType);
		Assert.assertTrue(myResult.size() == _populatedMap.size());
	}	

	@Test public void testEmptyCollectionRead() throws Exception {
		Gson myGson = new Gson();
		ByteArrayOutputStream myBuffer = new ByteArrayOutputStream();
		Writer myWriter = new OutputStreamWriter(myBuffer);
		
		GsonUtils myUtils = new GsonUtils(myGson, myWriter);
		myUtils.writeCollection(_emptyList);
		myWriter.close();
		
		ByteArrayInputStream myInput = new ByteArrayInputStream(myBuffer.toByteArray());
		BufferedReader myReader = new BufferedReader(new InputStreamReader(myInput));
		
		myUtils = new GsonUtils(myGson, myReader);
		String myLine = myReader.readLine();
		String myType = myGson.fromJson(myLine, String.class);
		
		Collection myResult = myUtils.readCollection(myType);
		Assert.assertTrue(myResult.size() == 0);
	}
	
	@Test public void testPopulatedCollectionRead() throws Exception {
		Gson myGson = new Gson();
		ByteArrayOutputStream myBuffer = new ByteArrayOutputStream();
		Writer myWriter = new OutputStreamWriter(myBuffer);
		
		GsonUtils myUtils = new GsonUtils(myGson, myWriter);
		myUtils.writeCollection(_populatedList);
		myWriter.close();
		
		ByteArrayInputStream myInput = new ByteArrayInputStream(myBuffer.toByteArray());
		BufferedReader myReader = new BufferedReader(new InputStreamReader(myInput));
		
		myUtils = new GsonUtils(myGson, myReader);
		String myLine = myReader.readLine();
		String myType = myGson.fromJson(myLine, String.class);
		
		Collection myResult = myUtils.readCollection(myType);
		Assert.assertTrue(myResult.size() == _populatedList.size());
	}	
}
