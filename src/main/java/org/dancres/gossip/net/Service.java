package org.dancres.gossip.net;

import java.io.IOException;

import javax.servlet.http.HttpServlet;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.dancres.gossip.discovery.HostDetails;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles http-related aspects - clients create an instance for a specified URL root
 * and register <code>Servlets</code> to support various requests.
 */
public class Service {

    private static Logger _logger = LoggerFactory.getLogger(Service.class);
    private HttpClient _client;
    private Server _server;
    private ServletHandler _handler;
    private int _port;
    private String _root;

    public Service(String aRoot) throws Exception {
        if (!aRoot.startsWith("/")) {
            throw new IllegalArgumentException("Root must start with /");
        }
        if (aRoot.endsWith("/")) {
            throw new IllegalArgumentException("Root must not end with /");
        }

        _root = aRoot;

        HttpClientBuilder myBuilder = HttpClientBuilder.create();
        myBuilder.setConnectionManager(new PoolingHttpClientConnectionManager());

        _client = myBuilder.build();

        _server = new Server();
        SocketConnector myConnector = new SocketConnector();
        _server.setConnectors(new Connector[]{myConnector});

        _handler = new ServletHandler();
        _server.setHandler(_handler);

        _server.start();

        _logger.info("Connected on: " + myConnector.getHost() + ":" + myConnector.getLocalPort());

        _port = myConnector.getLocalPort();
    }

    public HttpClient getClient() {
        return _client;
    }

    public int getPort() {
        return _port;
    }

    public void add(HttpServlet aServlet, String aRoot) {
        if (aRoot.startsWith("/")) {
            throw new IllegalArgumentException("Root must not start with /");
        }
        if (aRoot.endsWith("/")) {
            throw new IllegalArgumentException("Root must not end with /");
        }

        _handler.addServletWithMapping(new ServletHolder(aServlet), _root + "/" + aRoot);
    }

    public String getRoot(String aRoot) {
        return _root + "/" + aRoot;
    }

    public HostDetails getContactDetails() throws IOException {
        return new HostDetails(NetworkUtils.getWorkableAddress().getHostName(), getPort());
    }
}
