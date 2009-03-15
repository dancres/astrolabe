package org.dancres.gossip.astrolabe;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.*;
import org.junit.Assert.*;

public class ScriptTest {
    private StringBuffer _script;
    private StringBuffer _differentScript;

    @Before public void init() {
        _script = new StringBuffer();
        _script.append("&name\n");
        _script.append("Promiscuous\n");
        _script.append("&issued\n");
        _script.append("25\n");
        _script.append("&originzone\n");
        _script.append("\"\"\n");
        _script.append("&copy\n");
        _script.append("true\n");
        _script.append("&code\n");
        _script.append("import org.dancres.gossip.astrolabe.*;\n");
        _script.append("aggregate( Script anEnclosing, Collection aMibs, Mib aTarget ) {\n");
        _script.append("  if (aTarget.getAttributes().get(anEnclosing.getName()) == null)\n");
        _script.append("    aTarget.getAttributes().put(anEnclosing.getName(), anEnclosing.dup());\n");
        _script.append("}\n");

        _differentScript = new StringBuffer();
        _differentScript.append("&name\n");
        _differentScript.append("Promiscuous\n");
        _differentScript.append("&issued\n");
        _differentScript.append("26\n");
        _differentScript.append("&originzone\n");
        _differentScript.append("\"\"\n");
        _differentScript.append("&copy\n");
        _differentScript.append("true\n");
        _differentScript.append("&code\n");
        _differentScript.append("import org.dancres.gossip.astrolabe.*;\n");
        _differentScript.append("aggregate( Script anEnclosing, Collection aMibs, Mib aTarget ) {\n");
        _differentScript.append("  if (aTarget.getAttributes().get(anEnclosing.getName()) == null)\n");
        _differentScript.append("    aTarget.getAttributes().put(anEnclosing.getName(), anEnclosing.dup());\n");
        _differentScript.append("}\n");
    }

    @Test public void createScript() throws IOException {
        ByteArrayInputStream myBuffer = new ByteArrayInputStream(_script.toString().getBytes());
        Script myScript = Script.create(myBuffer);

        Assert.assertTrue(myScript.canCopy() == true);
        Assert.assertTrue(myScript.getName().equals("&Promiscuous"));
        Assert.assertTrue(myScript.getAttribute(Script.CODE).length() != 0);
        Assert.assertTrue(myScript.getAttribute(Script.ORIGIN).length() == 0);
        Assert.assertTrue(myScript.getAttribute(Script.ISSUED).equals("25"));
    }

    @Test public void testEquals() throws IOException {
        ByteArrayInputStream myBuffer = new ByteArrayInputStream(_script.toString().getBytes());
        Script myScript = Script.create(myBuffer);

        myBuffer = new ByteArrayInputStream(_differentScript.toString().getBytes());
        Script myOtherScript = Script.create(myBuffer);

        Assert.assertTrue(myScript.equals(myScript));
        Assert.assertFalse(myScript.equals(myOtherScript));
        Assert.assertTrue(myOtherScript.equals(myOtherScript));
    }
}
