package org.dancres.gossip.astrolabe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.dancres.gossip.discovery.HostDetails;
import org.dancres.gossip.net.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the full set of details required to represent an Astrolabe host as a {@link Zone}.  These are
 * the minimum set, all other information for a <code>Zone</code> can be found via gossip.
 */
public class SeedDetails {
    private static Logger _logger = LoggerFactory.getLogger(SeedDetails.class);

    /**
     * Generate full seed details given just a contact address.  Uses the targets IdServlet to recovery
     * the additional information required to create a <code>SeedDetails</code> instance.
     *
     * @param aService to be used for making http requests
     * @param aContact is the endpoint of the host to generate <code>SeedDetails</code> for
     * @return a populated <code>SeedDetails<code>
     * @throws java.io.IOException if the discovery process fails
     */
    public static SeedDetails discover(Service aService, HostDetails aContact) throws IOException {
        HttpGet myGetMethod = null;
        URL myURL = null;
        HttpResponse myResponse = null;

        try {
            // Hit the id endpoint
            //
            myURL = new URL("http://" + aContact.getHostName() + ":" + aContact.getPort() + aService.getRoot(IdServlet.PATH));

            _logger.info("Connecting for gossip with: " + myURL);

            myGetMethod = new HttpGet(myURL.toString());
            myResponse = aService.getClient().execute(myGetMethod);
            if (myResponse.getStatusLine().getStatusCode() != 200) {
                myGetMethod.abort();
                throw new IOException("Failed to post Mib summary: " + myResponse.getStatusLine());
            }

            /*
             *  Need to process response which will be another set of mib summaries.
             */
            HttpEntity entity = myResponse.getEntity();

            if (entity != null) {
                BufferedReader myReader = new BufferedReader(new InputStreamReader(entity.getContent()));
                String myHostId = myReader.readLine();
                myReader.close();

                return new SeedDetails(myHostId, aContact);
            } else {
                _logger.warn("Didn't get a response back - might be bad");
                IOException myIOE = new IOException("Failed to get ID: no response" + myURL);
                throw myIOE;
            }
        } catch (Exception anE) {
            _logger.warn("Failed to get ID: " + myURL, anE);
            if (myResponse != null) {
                myGetMethod.abort();
            }

            IOException myIOE = new IOException("Failed to get ID: " + myURL);
            myIOE.initCause(anE);
            throw myIOE;
        }
    }

    private HostDetails _contactDetails;
    private String _id;

    public SeedDetails(String anId, HostDetails aContactDetails) {
        _id = anId;
        _contactDetails = aContactDetails;
    }

    public String getId() {
        return _id;
    }

    public HostDetails getContactDetails() {
        return _contactDetails;
    }
}

