package org.dancres.gossip.net;

import java.util.HashMap;
import java.util.Map;

public class ServletUtils {
	public static Map<String, String> getQueryMap(String aQuery) {
		String[] myParams = aQuery.split("&");
		Map<String, String> myMap = new HashMap<>();

        for (String myParam : myParams) {
			myMap.put(myParam.split("=")[0], myParam.split("=")[1]);
		}

		return myMap;
	}	
}
