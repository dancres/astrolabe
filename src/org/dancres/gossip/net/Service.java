package org.dancres.gossip.net;

import java.io.IOException;

import javax.servlet.http.HttpServlet;

import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.dancres.gossip.discovery.HostDetails;
import org.dancres.gossip.peersampling.GossipServlet;
import org.dancres.gossip.peersampling.RemotePeerSampler;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Service {
    private static Logger _logger = LoggerFactory.getLogger(Service.class);
	
	private HttpClient _client;
	private SocketConnector _connector;
	private Server _server;
	private ServletHandler _handler;
	private int _port;
	private String _root;
	
	public Service(String aRoot) throws Exception {
		if (!aRoot.startsWith("/"))
			throw new IllegalArgumentException("Root must start with /");
		if (aRoot.endsWith("/"))
			throw new IllegalArgumentException("Root must not end with /");
		
		_root = aRoot;
		
		HttpParams params = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(params, 20 * 1000);
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);

		// Create and initialize scheme registry 
		SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));

		// Create an HttpClient with the ThreadSafeClientConnManager.
		// This connection manager must be used if more than one thread will
		// be using the HttpClient.
		ClientConnectionManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);

		_client = new DefaultHttpClient(cm, params);	
		
        _server = new Server();
        SocketConnector myConnector=new SocketConnector();
        _server.setConnectors(new Connector[]{myConnector});
        
        _handler=new ServletHandler();
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
		if (aRoot.startsWith("/"))
			throw new IllegalArgumentException("Root must not start with /");
		if (aRoot.endsWith("/"))
			throw new IllegalArgumentException("Root must not end with /");
		
        _handler.addServletWithMapping(new ServletHolder(aServlet), _root + "/" + aRoot);        		
	}
	
	public String getRoot(String aRoot) {
		return _root + "/" + aRoot;
	}
	
	public HostDetails getContactDetails() throws IOException {
		return new HostDetails(NetworkUtils.getWorkableAddress().getHostName(), getPort());		
	}
}
