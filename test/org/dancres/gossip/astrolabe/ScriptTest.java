package org.dancres.gossip.astrolabe;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.*;
import org.junit.Assert.*;

public class ScriptTest {
    private StringBuffer _script;

    @Before public void init() {
        _script = new StringBuffer();
        _script.append("&name\n");
        _script.append("Promiscuous\n");
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
    }

    @Test public void createScript() throws IOException {
        ByteArrayInputStream myBuffer = new ByteArrayInputStream(_script.toString().getBytes());
        Script myScript = Script.create(myBuffer);

        Assert.assertTrue(myScript.canCopy() == true);
        Assert.assertTrue(myScript.getName().equals("&Promiscuous"));
        Assert.assertTrue(myScript.getAttribute(Script.SCRIPT_CODE).length() != 0);
        Assert.assertTrue(myScript.getAttribute(Script.SCRIPT_ORIGIN).length() == 0);
    }
}
