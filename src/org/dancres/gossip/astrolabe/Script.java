package org.dancres.gossip.astrolabe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import bsh.Interpreter;

/**
 * <p>Contains a beanshell script and executes it against the specified Mibs.</p>
 * 
 * <p><b>Note:</b> that a script that is "active" (added to a Mib) should not be added to any other Mib.  Instead
 * add a <code>dup()</code>.</p>
 * 
 * @todo Add support for appropriate fields - perhaps by placing them in the bsh script and extracting them via
 * interp.get.  Would mean separating out the interpreter init so it could be called early to get the variables setup
 * which we'd then access via methods on the Script instance.
 */
public class Script {
	private String _script;
	private transient Interpreter _interp;
	private transient AggregationFunction _func;
	private String _name;
	
	/**
	 * @param aName the name of the script, must begin with "&"
	 * @param aScript the script text itself
	 */
	public Script(String aName, Collection<String> aScript) {
		if (! aName.startsWith("&"))
			throw new IllegalArgumentException("Script names must begin with &");
		
		_script = concat(aScript);
		_name = aName;
	}
	
	public Script(String aName, String aScript) {
		_name = aName;
		_script = aScript;
	}
	
	public Script() {
	}
	
	public String getName() {
		return _name;
	}
	
	public void evaluate(Collection<Mib> aSetOfMibs, Mib aTarget) throws Exception {
		if (_interp == null) {
			_interp = new Interpreter();
			
			_interp.eval("import org.dancres.gossip.astrolabe.*;");
			_interp.eval(_script);
			
			_func =  (AggregationFunction) _interp.eval("return (AggregationFunction) this");
		}
		
		_func.aggregate(this, aSetOfMibs, aTarget);
	}
	
	public String toString() {
		return _script;
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
		return new Script(_name, _script);
	}
	
	public static Script create(String aName, InputStream aStream) throws IOException {
		BufferedReader myReader = new BufferedReader(new InputStreamReader(aStream));
		ArrayList<String> myStrings = new ArrayList<String>();
		
		String myLine = null;
		while ((myLine = myReader.readLine()) != null) {
			myLine = myLine.trim();
			myStrings.add(myLine);
		}
		
		return new Script(aName, myStrings);
	}
}
