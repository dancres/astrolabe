package org.dancres.gossip.astrolabe;

import com.google.gson.Gson;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import org.junit.*;
import org.junit.Assert.*;

public class CertificateTest {

    private StringBuffer _validScript;
    private StringBuffer _invalidScript;

    @Before
    public void init() {
        _validScript = new StringBuffer();
        _validScript.append("&var1\n");
        _validScript.append("a single line value\n");
        _validScript.append("&var2\n");
        _validScript.append("a multi-line\n");
        _validScript.append("script to see if we cope\n");

        _invalidScript = new StringBuffer();
        _invalidScript.append("var1\n");
        _invalidScript.append("a single line value\n");
        _invalidScript.append("&var2\n");
        _invalidScript.append("a multi-line\n");
        _invalidScript.append("script to see if we cope\n");
    }

    @Test public void checkRead() throws IOException {
        ByteArrayInputStream myBuffer = new ByteArrayInputStream(_validScript.toString().getBytes());
        Certificate myCert = Certificate.create(new InputStreamReader(myBuffer));

        Assert.assertTrue(myCert.getValues().size() == 2);
        Assert.assertNotNull(myCert.getValue("&var1"));
        Assert.assertNotNull(myCert.getValue("&var2"));
        Assert.assertFalse(myCert.getValue("&var2").contains("\n"));
    }

    @Test public void parseFailure() throws IOException {
        ByteArrayInputStream myBuffer = new ByteArrayInputStream(_invalidScript.toString().getBytes());
        boolean passed = false;

        try {
            Certificate myCert = Certificate.create(new InputStreamReader(myBuffer));
        } catch (IOException anIOE) {
            passed = true;
        }

        Assert.assertTrue(passed);
    }

    @Test public void willGson() throws IOException {
        Gson myGson = new Gson();
        ByteArrayInputStream myBuffer = new ByteArrayInputStream(_validScript.toString().getBytes());
        Certificate myCert = Certificate.create(new InputStreamReader(myBuffer));

        String myResult = myGson.toJson(myCert);
        myCert = myGson.fromJson(myResult, Certificate.class);
    }
}
