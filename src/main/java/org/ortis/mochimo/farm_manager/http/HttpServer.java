/*******************************************************************************
 * Copyright (C) 2018 Ortis (cao.ortis.org@gmail.com)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package org.ortis.mochimo.farm_manager.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * A simple HTTP server
 * 
 * @author Ortis <br>
 *         2018 Jul 03 9:28:50 PM <br>
 */
@SuppressWarnings("restriction")
public class HttpServer
{
	public final static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
	public final static Charset UTF8 = Charset.forName("UTF-8");
	public final static Gson GSON = new Gson();

	private final com.sun.net.httpserver.HttpServer server;

	public HttpServer(final InetSocketAddress inetSocketAddress, final ExecutorService pool) throws IOException
	{
		this.server = com.sun.net.httpserver.HttpServer.create(inetSocketAddress, 0);
		this.server.setExecutor(pool);

	}

	public void addContext(final String path, final HttpHandler httpHandler)
	{
		this.server.createContext(path, httpHandler);

	}

	public static String getHeader(final String name, final HttpExchange exchange)
	{
		final List<String> values = exchange.getRequestHeaders().get(name);
		if (values == null || values.isEmpty())
			return null;

		return values.get(0);

	}

	public static <D extends Map<String, String>> D parseQuery(final String query, final D destination)
	{

		final String [] buffer = query.split("&");
		for (final String b : buffer)
		{
			final String [] bb = b.split("=");
			destination.put(bb[0], bb[1]);
		}

		return destination;
	}

	public void start()
	{
		this.server.start();
	}

	public void stop()
	{
		this.server.stop(10);
	}

}
