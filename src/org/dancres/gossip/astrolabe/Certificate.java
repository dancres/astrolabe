package org.dancres.gossip.astrolabe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * <p>The base abstraction for a certificate.</p>
 *
 * <p>Certificates are a collection of text lines.  An entry in a certificate consists of an attribute name (starting with &)
 * and a value.  An attribute name appears on a line alone, followed by the value (which may span multiple lines).</p>
 *
 * <p>An example certificate would be:</p>
 * <pre>
    &name
    default
    &code
    import org.dancres.gossip.astrolabe.*;

    aggregate( Script anEnclosing, Collection aMibs, Mib aTarget ) {
      HashSet servers = new HashSet();
      HashSet contacts = new HashSet();
      long depth = 0;
      long memberCount = 0;

      for (m : aMibs) {
        memberCount = memberCount + m.getNMembers();

        mibDepth = m.getAttributes().get("depth");
        if (mibDepth != null) {
    	  if (mibDepth > depth)
    	    depth = mibDepth;
    	}

    	merge(servers, m.getServers());
    	merge(contacts, m.getContacts());
      }

      aTarget.setNMembers(memberCount);
      aTarget.getAttributes().put("depth", depth + 1);
      aTarget.setContacts(contacts);
      aTarget.setServers(servers);

      if (aTarget.getAttributes().get(anEnclosing.getName()) == null)
        aTarget.getAttributes().put(anEnclosing.getName(), anEnclosing.dup());
    }

    merge(HashSet aResult, Set aSource) {
    	for (e : aSource) {
    		if (aResult.size() == 3)
    			return;
    		else
    			aResult.add(e);
    	}
    }

 * </pre>
 * 
 */
public class Certificate {
    private HashMap<String, String> _attributes = new HashMap<String, String>();

    public Certificate(HashMap<String, String> anAttrs) {
        _attributes = anAttrs;
    }

    public Certificate() {
    }


    public String getValue(String aVarName) {
        return _attributes.get(aVarName);
    }

    public Map<String, String> getValues() {
        return _attributes;
    }

    public static Certificate create(Reader aReader) throws IOException {
        HashMap<String, String> myAttrs = new HashMap<String, String>();
        BufferedReader myReader = new BufferedReader(aReader);
        StringBuffer myValueBuffer = null;
        String myVarName = null;
        int myLineNo = -1;
        boolean myValueState = false;

        while (true) {
            String myLine = myReader.readLine();
            myLineNo++;

            if ((myLine == null) && (myValueState)) {
                // We have a variable to store
                myAttrs.put(myVarName, replaceEmpty(myValueBuffer.toString()));
            }

            if (myLine == null)
                break;

            myLine = myLine.trim();
            if (myLine.length() == 0) {
                continue;
            }

            if (myLine.startsWith("&")) {
                if (myValueState) {
                    // End of previous value - and the start of another
                    myAttrs.put(myVarName, replaceEmpty(myValueBuffer.toString()));
                    myVarName = myLine.trim();
                    myValueBuffer = new StringBuffer();
                } else {
                    myVarName = myLine.trim();
                    myValueBuffer = new StringBuffer();
                    myValueState = true;
                }
                
                continue;
            }

            if (myValueState) {
                if (myValueBuffer.length() == 0)
                    myValueBuffer.append(myLine);
                else
                    myValueBuffer.append(" " + myLine);
            } else {
                if (myLine.startsWith("&")) {
                    myVarName = myLine.trim();
                    myValueBuffer = new StringBuffer();
                    myValueState = true;
                } else {
                    throw new IOException("Parse error, was expecting a varname at: " + myLineNo + " " + myLine);
                }
            }
        }

        return new Certificate(myAttrs);
    }

    /**
     * Convert matched double-quotes into an empty string
     */
    private static String replaceEmpty(String aString) {
        if (aString.equals("\"\""))
            return "";
        else
            return aString;
    }

    public String toString() {
        StringBuffer myBuffer = new StringBuffer("Certificate -> ");

        Iterator<String> myKeys = _attributes.keySet().iterator();
        while (myKeys.hasNext()) {
            String myKey = myKeys.next();
            myBuffer.append(myKey + ": " + _attributes.get(myKey) + ", ");
        }

        return myBuffer.toString();
    }
}
