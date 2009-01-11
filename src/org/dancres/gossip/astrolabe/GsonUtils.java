package org.dancres.gossip.astrolabe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import com.google.gson.Gson;

/**
 * Gson works pretty well but doesn't want to parse collections or maps containing values of more than one type or of
 * a type only known at runtime.  This class provides some utility methods to handle these edge cases.
 * 
 * @todo Write classes to represent type/value pairs etc so Gson can properly handle it and we generate properly valid
 * json.  Collection first, then Map.
 */
public class GsonUtils {
	private Gson _gson;
	private Writer _writer;
	private BufferedReader _reader;
	
	public GsonUtils(Gson aGson, Writer aWriter) {
		_gson = aGson;
		_writer = aWriter;
	}
	
	public GsonUtils(Gson aGson, BufferedReader aReader) {
		_gson = aGson;
		_reader = aReader;
	}
	
	public void writeMap(Map aMap) throws IOException {
		// Write the type of collection
		//
		String myConcreteClass = aMap.getClass().getName();
		_gson.toJson(myConcreteClass, _writer);
		_writer.write("\n");
		
		// Write the number of items in the map
		//
		_gson.toJson(aMap.size(), _writer);
		_writer.write("\n");
		
		Iterator myKeys = aMap.keySet().iterator();
		while (myKeys.hasNext()) {
			Object myKey = myKeys.next();
			String myKeyType = myKey.getClass().getName();

			// Write the key's type
			//
			_gson.toJson(myKeyType, _writer);
			_writer.write("\n");
			
			// Write the key value - assuming it's a simple type
			//
			_gson.toJson(myKey, _writer);
			_writer.write("\n");
			
			Object myValue = aMap.get(myKey);
			if (Collection.class.isInstance(myValue)) {
				writeCollection((Collection) myValue);
			} else {
				String myValueType = myValue.getClass().getName();

				// Write the value's type
				//
				_gson.toJson(myValueType, _writer);
				_writer.write("\n");

				// Write the value - assuming it's a simple type not a map
				//
				_gson.toJson(myValue, _writer);
				_writer.write("\n");
			}
		}
	}
	
	private void dumpBytes(String aString) {
		byte[] myBytes = aString.getBytes();
		
		for (int i = 0; i < myBytes.length; i++) {
			System.out.print(Integer.toHexString(myBytes[i]) + " ");
		}
		
		System.out.println();
	}
	
	/**
	 * <b>Hack:</b> Reading a line and then feeding it to Gson as it seemingly over-consumes the Reader resulting in
	 * erroneous EOF.
	 * 
	 * @todo Fix hack
	 */
	public Map readMap(String aPrimitiveType) throws IOException {
		try {
			Map myMap = (Map) Class.forName(aPrimitiveType).newInstance();

			String myInput = _reader.readLine();
			long myMapSize = _gson.fromJson(myInput, Long.class);

			if (myMapSize == 0)
				return myMap;

			for (long i = 0; i < myMapSize; i++) {
				// Read key-type
				//
				myInput = _reader.readLine();
				String myKeyType = _gson.fromJson(myInput, String.class);

				// Read key value
				//
				myInput = _reader.readLine();
				Object myKey = _gson.fromJson(myInput, Class.forName(myKeyType));

				// Read value-type
				//
				myInput = _reader.readLine();
				String myValueType = _gson.fromJson(myInput, String.class);
				Class myValueClazz = Class.forName(myValueType);

				Object myValue;

				if (Collection.class.isAssignableFrom(myValueClazz)) {
					myValue = readCollection(myValueType);
				} else {
					myInput = _reader.readLine();
					myValue = _gson.fromJson(myInput, myValueClazz);
				}

				myMap.put(myKey, myValue);
			}

			return myMap;
		
		} catch (ClassNotFoundException aCNFE) {
			IOException myIOE = new IOException();
			myIOE.initCause(aCNFE);
			throw myIOE;
		} catch (IllegalAccessException aIAE) {
			IOException myIOE = new IOException();
			myIOE.initCause(aIAE);
			throw myIOE;			
		} catch (InstantiationException aIE) {
			IOException myIOE = new IOException();
			myIOE.initCause(aIE);
			throw myIOE;			
		}
	}
	
	public void writeCollection(Collection aCollection) throws IOException {
		// Write the type of collection
		//
		String myConcreteClass = aCollection.getClass().getName();
		_gson.toJson(myConcreteClass, _writer);
		_writer.write("\n");
		
		// Write the number of items in the collection
		//
		_gson.toJson(aCollection.size(), _writer);
		_writer.write("\n");
		
		// Write the type of the contents - we assume it's only one type so the first item will do
		//
		Class myType = null;
		
		if (aCollection.size() > 0) {
			myType = aCollection.iterator().next().getClass();
			_gson.toJson(myType.getName(), _writer);
			_writer.write("\n");
		}
		
		// Now write each member of the collection
		if (myType != null) {
			Iterator myObjects = aCollection.iterator();
			while (myObjects.hasNext()) {
				Object myObject = myObjects.next();
				_gson.toJson(myObject, myType, _writer);
				_writer.write("\n");
			}
		}		
	}
	
	/**
	 * <b>Hack:</b> Reading a line and then feeding it to Gson as it seemingly over-consumes the Reader resulting in
	 * erroneous EOF.
	 * 
	 * @todo Fix hack
	 */
	public Collection readCollection(String aPrimitiveType) throws IOException {
		try {
			Collection myCollection = (Collection) Class.forName(aPrimitiveType).newInstance();

			String myInput = _reader.readLine();
			long myCollectionSize = _gson.fromJson(myInput, Long.class);

			if (myCollectionSize == 0)
				return myCollection;

			myInput = _reader.readLine();
			String myContentType = _gson.fromJson(myInput, String.class);
			Class myContentClazz = Class.forName(myContentType);

			for (long i = 0; i < myCollectionSize; i++) {
				myInput = _reader.readLine();
				Object myValue = _gson.fromJson(myInput, myContentClazz);
				myCollection.add(myValue);
			}

			return myCollection;
		
		} catch (ClassNotFoundException aCNFE) {
			IOException myIOE = new IOException();
			myIOE.initCause(aCNFE);
			throw myIOE;
		} catch (IllegalAccessException aIAE) {
			IOException myIOE = new IOException();
			myIOE.initCause(aIAE);
			throw myIOE;
		} catch (InstantiationException aIE) {
			IOException myIOE = new IOException();
			myIOE.initCause(aIE);
			throw myIOE;			
		}
	}
}
