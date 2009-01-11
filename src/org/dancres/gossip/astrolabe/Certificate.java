package org.dancres.gossip.astrolabe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The base abstraction for a certificate
 *
 * @author dan
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
                myAttrs.put(myVarName, myValueBuffer.toString());
            }

            if (myLine == null)
                break;

            if (myLine.trim().length() == 0) {
                continue;
            }

            if (myLine.startsWith("&")) {
                if (myValueState) {
                    // End of previous value - and the start of another
                    myAttrs.put(myVarName, myValueBuffer.toString());
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
