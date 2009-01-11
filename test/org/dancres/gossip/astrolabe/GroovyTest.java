package org.dancres.gossip.astrolabe;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

public class GroovyTest {
	@Test public void test() {
		Zone _Root;
		Zone _First;
		Zone _Second;
		Zone _Third;
		Zone _Fourth;
  
		LocalID.set("/rhubarb/custardcreme");
		
		_Root = new Zone();
		_First = new Zone("/rhubarb");
		_Second = new Zone("/rhubarb/custardcreme");
		_Third = new Zone("/custard");
		_Fourth = new Zone("/custard/birdies");				

		_Root.add(_First);
		_Root.add(_Second);
		_Root.add(_Third);
		_Root.add(_Fourth);
		
		Binding binding = new Binding();
		binding.setVariable("foo", new Integer(2));
		binding.setVariable("rhubarb", new Integer(34));				
		binding.setVariable("root", _Root);
		
		GroovyShell shell = new GroovyShell(binding);

		/*
		 * Make sure basic Groovy integration works
		 */
		Object value = shell.evaluate("println 'Hello World!'; x = 123; rhubarb = 92; return foo * 10");		
		Assert.assertTrue(value.equals(new Integer(20)));
		Assert.assertTrue(binding.getVariable("x").equals(new Integer(123)));
		Assert.assertTrue(binding.getVariable("rhubarb").equals(new Integer(92)));
		
		/*
		 * If this doesn't work we can't use Groovy for Astrolabe scripting....
		 */
		value = shell.evaluate("for (zone in root.getChildren()) { println zone.getId() }; return true");
		Assert.assertTrue(value.equals(true));
	}
}
