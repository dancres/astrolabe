package org.dancres.gossip.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import com.google.gson.Gson;

/**
 * Gson works pretty well but doesn't want to parse collections or maps containing values of more than one type or of
 * a type only known at runtime.  This limitation in combination with Gson's inability to parse single Gson elements
 * means we must split the output across lines so that we can force Gson to parse a term at a time.
 *
 * @todo Consider use of serializers and deserializers to work around the issue above.
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

        for (Object myKey : aMap.keySet()) {
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

        for (byte b : myBytes) {
			System.out.print(Integer.toHexString(b) + " ");
		}
		
		System.out.println();
	}
	
	public Map readMap(String aPrimitiveType) throws IOException {
		try {
			Map myMap = (Map) Class.forName(aPrimitiveType).newInstance();

			long myMapSize = _gson.fromJson(_reader.readLine(), Long.class);

			if (myMapSize == 0)
				return myMap;

			for (long i = 0; i < myMapSize; i++) {
				// Read key-type
				//
				String myKeyType = _gson.fromJson(_reader.readLine(), String.class);

				// Read key value
				//
				Object myKey = _gson.fromJson(_reader.readLine(), Class.forName(myKeyType));

				// Read value-type
				//
				String myValueType = _gson.fromJson(_reader.readLine(), String.class);
				Class myValueClazz = Class.forName(myValueType);

				Object myValue;

				if (Collection.class.isAssignableFrom(myValueClazz)) {
					myValue = readCollection(myValueType);
				} else {
					myValue = _gson.fromJson(_reader.readLine(), myValueClazz);
				}

				myMap.put(myKey, myValue);
			}

			return myMap;
		
		} catch (ClassNotFoundException | IllegalAccessException | InstantiationException anE) {
			IOException myIOE = new IOException();
			myIOE.initCause(anE);
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
            for (Object myObject : aCollection) {
				_gson.toJson(myObject, myType, _writer);
				_writer.write("\n");
			}
		}		
	}
	
	public Collection readCollection(String aPrimitiveType) throws IOException {
		try {
			Collection myCollection = (Collection) Class.forName(aPrimitiveType).newInstance();

			long myCollectionSize = _gson.fromJson(_reader.readLine(), Long.class);

			if (myCollectionSize == 0)
				return myCollection;

			String myContentType = _gson.fromJson(_reader.readLine(), String.class);
			Class myContentClazz = Class.forName(myContentType);

			for (long i = 0; i < myCollectionSize; i++) {
				Object myValue = _gson.fromJson(_reader.readLine(), myContentClazz);
				myCollection.add(myValue);
			}

			return myCollection;
		
		} catch (ClassNotFoundException | IllegalAccessException | InstantiationException anE) {
			IOException myIOE = new IOException();
			myIOE.initCause(anE);
			throw myIOE;
		}
	}
}
