package org.dancres.gossip.astrolabe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import bsh.Interpreter;
import java.io.Reader;

/**
 * <p>Contains a beanshell script and executes it against the specified Mibs.</p>
 * 
 * <p><b>Note:</b> that a script that is "active" (added to a Mib) should not be added to any other Mib.  Instead
 * add a <code>dup()</code>.</p>
 *
 * <p>A script is a kind of certificate - at minimum it must contain two attributes &name and &code representing it's name
 * and the associated code.</p>
 *
 * @todo Remove SCRIPT predicate from public static final's
 */
public class Script {
    public static final String SCRIPT_NAME_PREDICATE = "&";

    public static final String SCRIPT_NAME = Certificate.ATTRIBUTE_PREDICATE + "name";
    public static final String SCRIPT_CODE = Certificate.ATTRIBUTE_PREDICATE + "code";
    public static final String SCRIPT_ORIGIN = Certificate.ATTRIBUTE_PREDICATE + "originzone";
    public static final String SCRIPT_COPY = Certificate.ATTRIBUTE_PREDICATE + "copy";
    public static final String SCRIPT_LEVEL = Certificate.ATTRIBUTE_PREDICATE + "level";
    public static final String SCRIPT_ISSUED = Certificate.ATTRIBUTE_PREDICATE + "issued";

    public static final String STRONG = "strong";
    public static final String WEAK = "weak";

	private transient Interpreter _interp;
	private transient AggregationFunction _func;
    private Certificate _cert;
	
	public Script(Certificate aCert) {
        _cert = aCert;
	}
	
	public Script() {
	}
	
	public String getName() {
		return SCRIPT_NAME_PREDICATE + _cert.getValue(SCRIPT_NAME);
	}
	
	public void evaluate(Collection<Mib> aSetOfMibs, Mib aTarget) throws Exception {
		if (_interp == null) {
			_interp = new Interpreter();
			
			_interp.eval("import org.dancres.gossip.astrolabe.*;");
			_interp.eval(_cert.getValue(SCRIPT_CODE));
			
			_func =  (AggregationFunction) _interp.eval("return (AggregationFunction) this");
		}
		
		_func.aggregate(this, aSetOfMibs, aTarget);
	}
	
	public String toString() {
		return _cert.toString();
	}

    public String getAttribute(String aField) {
        return _cert.getValue(aField);
    }

    public boolean canCopy() {
        String myCopyFlag = _cert.getValue(SCRIPT_COPY);

        if ((myCopyFlag != null) && (Boolean.parseBoolean(myCopyFlag))) {
            return true;
        }

        return false;
    }
	
	private String concat(Collection<String> aStrings) {
		StringBuffer myConcatenation = new StringBuffer();
		
		Iterator<String> myStrings = aStrings.iterator();
		while (myStrings.hasNext()) {
			myConcatenation.append(myStrings.next());
			myConcatenation.append(" ");
		}
		
		return myConcatenation.toString();		
	}
	
	/**
	 * @return a duplicated script without any associated runtime state of it's sibling.  Such a duplicate is
	 * suitable for adding to another Mib.
	 */
	public Script dup() {
		return new Script(_cert);
	}

    /**
     * “Preferable” is a partial order relation between certficates in the same
     * category: x > y means that certiﬁcate x is preferable to certificate y . x > y iff
     * — x is valid, and y is not, or
     * — x and y are both valid, the level of x is strong, and x.id is a child zone of y.id , or
     * — x and y are both valid, x is not stronger than y , and x.issued > y.issued
     */
    public boolean isPreferable(Script anotherScript) {
        if (getAttribute(Script.SCRIPT_LEVEL).equals(Script.STRONG)) {
            String myScriptZone = getAttribute(Script.SCRIPT_ORIGIN);
            String myOtherScriptZone = anotherScript.getAttribute(Script.SCRIPT_ORIGIN);

            // Must deduce parent/child relationship via path as actual Zone may not be present at this point
            //
            if (myScriptZone.startsWith(myOtherScriptZone)) {
                return true;
            }
        } else {
            boolean myFirstIsStrong = getAttribute(Script.SCRIPT_LEVEL).equals(Script.STRONG);
            boolean myOtherIsWeak = (anotherScript.getAttribute(Script.SCRIPT_LEVEL).equals(Script.WEAK));
            long myFirstIssued = Long.parseLong(getAttribute(Script.SCRIPT_ISSUED));
            long myOtherIssued = Long.parseLong(anotherScript.getAttribute(Script.SCRIPT_ISSUED));

            if ((!(myFirstIsStrong && myOtherIsWeak)) && (myFirstIssued > myOtherIssued)) {
                return true;
            }
        }

        return false;
    }

	public static Script create(InputStream aStream) throws IOException {
		Reader myReader = new InputStreamReader(aStream);
        Certificate myCert = Certificate.create(myReader);

        return new Script(myCert);
	}
}
