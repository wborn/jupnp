package org.jupnp.transport.impl.osgi;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.jupnp.transport.spi.ServletContainerAdapter;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a servlet container adapter for an OSGi http service.
 * 
 * @author Kai Kreuzer
 *
 */
public class HttpServiceServletContainerAdapter implements
		ServletContainerAdapter {

	private final static Logger logger = LoggerFactory.getLogger(HttpServiceServletContainerAdapter.class);
	
	protected HttpService httpService;
	
	public HttpServiceServletContainerAdapter(HttpService httpService) {
		this.httpService = httpService;
	}
	
	@Override
	public void setExecutorService(ExecutorService executorService) {
	}

	@Override
	public int addConnector(String host, int port) throws IOException {
		return 0;
	}

	@Override
	public void registerServlet(String contextPath, Servlet servlet) {
		Dictionary params = new Properties();
		try {
			httpService.registerServlet(contextPath, servlet, params, httpService.createDefaultHttpContext());
		} catch (ServletException e) {
			logger.error("Failed to register UPnP servlet!", e);
		} catch (NamespaceException e) {
			logger.error("Failed to register UPnP servlet!", e);
		}
	}

	@Override
	public void startIfNotRunning() {
	}

	@Override
	public void stopIfRunning() {
	}

}
