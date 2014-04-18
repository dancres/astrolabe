package org.dancres.gossip.net;

import java.util.HashMap;
import java.util.Map;

public class ServletUtils {
	public static Map<String, String> getQueryMap(String aQuery) {
		String[] myParams = aQuery.split("&");
		Map<String, String> myMap = new HashMap<String, String>();

		for (int i = 0; i < myParams.length; i++)  {
			String myName = myParams[i].split("=")[0];
			String myValue = myParams[i].split("=")[1];
			myMap.put(myName, myValue);
		}

		return myMap;
	}	
}
