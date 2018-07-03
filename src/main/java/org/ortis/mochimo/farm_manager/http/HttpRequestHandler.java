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

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import org.ortis.mochimo.farm_manager.beans.ErrorBean;
import org.ortis.mochimo.farm_manager.beans.MinerStatisticsBean;
import org.ortis.mochimo.farm_manager.beans.MiningFarmStatisticsBean;
import org.ortis.mochimo.farm_manager.farm.MiningFarm;
import org.ortis.mochimo.farm_manager.farm.MiningFarmStatistics;
import org.ortis.mochimo.farm_manager.farm.miner.Miner;
import org.ortis.mochimo.farm_manager.farm.miner.MinerStatistics;
import org.ortis.mochimo.farm_manager.utils.Utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * {@link HttpRequestHandler} for API and web interface
 * 
 * @author Ortis <br>
 *         2018 Jul 01 11:57:23 PM <br>
 */
@SuppressWarnings("restriction")
public class HttpRequestHandler implements HttpHandler
{
	private final static Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final MiningFarm miningFarm;
	private final Path htmlRepository;
	private final Logger log;

	public HttpRequestHandler(final MiningFarm miningFarm, final Path htmlRepository, final Logger log)
	{
		this.miningFarm = miningFarm;

		this.htmlRepository = htmlRepository;
		if (this.htmlRepository != null && !Files.isDirectory(this.htmlRepository))
			throw new IllegalArgumentException("html repository must be a directory");

		this.log = log;
	}

	@Override
	public void handle(final HttpExchange httpExchange)
	{

		final String path = httpExchange.getRequestURI().getPath();
		final String upperPath = path.toUpperCase(Locale.ENGLISH);
		final String query = httpExchange.getRequestURI().getQuery();

		int responseCode = 500;
		final Map<String, String> responseHeaders = new LinkedHashMap<>();
		byte [] response = new byte[0];

		try
		{
			processRequest:
			{
				if (httpExchange.getRequestMethod().equals("GET"))
				{

					if (path.length() <= 1)
					{
						responseCode = 301;
						responseHeaders.put("Location", "/dashboard.html?refresh=10000");
					} else if (upperPath.endsWith(".HTML"))
					{
						final String fileName = path.substring(1);
						final Path htmlFile = this.htmlRepository.resolve(fileName);
						if (Files.isReadable(this.htmlRepository))
						{
							this.log.info("Serving HTML page " + htmlFile);
							responseCode = 200;
							responseHeaders.put("Content-type", "text/html");
							response = Files.readAllBytes(htmlFile);
						} else
						{
							this.log.info("HTML file " + fileName + " not found");
							responseCode = 404;
						}

					} else if (upperPath.equals("/STATUS"))
					{
						responseCode = 200;
						responseHeaders.put("Content-type", "application/json");
						final MiningFarmStatistics statistics = this.miningFarm.statistics();
						final MiningFarmStatisticsBean bean = new MiningFarmStatisticsBean(statistics);
						response = GSON.toJson(bean).getBytes();

					} else if (upperPath.equals("/MINER"))
					{

						final Map<String, String> params = HttpServer.parseQuery(query, new LinkedHashMap<>());
						final String minerId = params.get("id");
						if (minerId == null)
						{
							responseCode = 400;
							responseHeaders.put("Content-type", "application/json");
							final ErrorBean bean = new ErrorBean("id must be specified");
							response = GSON.toJson(bean).getBytes();
							break processRequest;
						}

						final Miner miner = this.miningFarm.getMiner(minerId);

						if (miner == null)
						{
							responseCode = 404;
							responseHeaders.put("Content-type", "application/json");
							final ErrorBean bean = new ErrorBean("Miner not found");
							response = GSON.toJson(bean).getBytes();
							break processRequest;
						}

						responseCode = 200;
						responseHeaders.put("Content-type", "application/json");
						final MinerStatistics statistics = miner.getStatistics();
						final MinerStatisticsBean bean = new MinerStatisticsBean(statistics);
						response = GSON.toJson(bean).getBytes();

					} else if (upperPath.equals("/COMMAND"))
					{
						final Map<String, String> params = HttpServer.parseQuery(query, new LinkedHashMap<>());
						final String minerId = params.get("id");
						if (minerId == null)
						{
							responseCode = 400;
							responseHeaders.put("Content-type", "application/json");
							final ErrorBean bean = new ErrorBean("id must be specified");
							response = GSON.toJson(bean).getBytes();
							break processRequest;
						}

						final Miner miner = this.miningFarm.getMiner(minerId);

						if (miner == null)
						{
							responseCode = 404;
							responseHeaders.put("Content-type", "application/json");
							final ErrorBean bean = new ErrorBean("Miner not found");
							response = GSON.toJson(bean).getBytes();
							break processRequest;
						}

						final String command = params.get("cmd");
						if (command == null)
						{
							responseCode = 400;
							responseHeaders.put("Content-type", "application/json");
							final ErrorBean bean = new ErrorBean("cmd must be specified");
							response = GSON.toJson(bean).getBytes();
							break processRequest;
						}

						final String commandUpper = command.toUpperCase(Locale.ENGLISH);
						switch (commandUpper)
						{
							case "START":

								responseHeaders.put("Content-type", "application/json");
								if (miner.start())
									responseCode = 200;
								else
								{
									responseCode = 500;
									final ErrorBean bean = new ErrorBean("Starting failed");
									response = GSON.toJson(bean).getBytes();
								}
								miner.clearStatistics();
								break;

							case "STOP":

								responseHeaders.put("Content-type", "application/json");
								if (miner.stop())
									responseCode = 200;
								else
								{
									responseCode = 500;
									final ErrorBean bean = new ErrorBean("Stopping failed");
									response = GSON.toJson(bean).getBytes();
								}
								miner.clearStatistics();
								break;

							default:
								responseCode = 400;
								responseHeaders.put("Content-type", "application/json");
								final ErrorBean bean = new ErrorBean("Command not found");
								response = GSON.toJson(bean).getBytes();

								break;
						}

					} else
					{
						responseCode = 404;
					}
				} else
				{
					responseCode = 405;
					response = "Method not supported".getBytes();
				}

			}
		} catch (final Exception e)
		{
			final String msg = Utils.formatException(e);
			this.log.severe("Error while processing request - " + msg);
			responseCode = 500;
			response = msg.getBytes();
		}

		try
		{
			responseHeaders.forEach((k, v) -> httpExchange.getResponseHeaders().add(k, v));
			httpExchange.sendResponseHeaders(responseCode, response.length);
			final OutputStream os = httpExchange.getResponseBody();
			os.write(response);
			os.flush();
			os.close();
		} catch (Exception e)
		{
			this.log.severe(Utils.formatException(e));
		}
	}

}
