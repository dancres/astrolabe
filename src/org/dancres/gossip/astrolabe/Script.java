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
 */
public class Script {
    public static final String CERT_NAME = "&name";
    public static final String CERT_SCRIPT = "&code";

	private transient Interpreter _interp;
	private transient AggregationFunction _func;
    private Certificate _cert;
	
	public Script(Certificate aCert) {
        _cert = aCert;
	}
	
	public Script() {
	}
	
	public String getName() {
		return "&" + _cert.getValue(CERT_NAME);
	}
	
	public void evaluate(Collection<Mib> aSetOfMibs, Mib aTarget) throws Exception {
		if (_interp == null) {
			_interp = new Interpreter();
			
			_interp.eval("import org.dancres.gossip.astrolabe.*;");
			_interp.eval(_cert.getValue(CERT_SCRIPT));
			
			_func =  (AggregationFunction) _interp.eval("return (AggregationFunction) this");
		}
		
		_func.aggregate(this, aSetOfMibs, aTarget);
	}
	
	public String toString() {
		return _cert.toString();
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
	
	public static Script create(InputStream aStream) throws IOException {
		Reader myReader = new InputStreamReader(aStream);
        Certificate myCert = Certificate.create(myReader);

        return new Script(myCert);
	}
}
