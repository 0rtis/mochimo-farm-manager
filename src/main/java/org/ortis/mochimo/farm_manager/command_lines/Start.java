/*******************************************************************************
 * Copyright (C) 2018 Ortis (cao.ortis.org@gmail.com)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without
 * limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT
 * SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 ******************************************************************************/

package org.ortis.mochimo.farm_manager.command_lines;

import java.io.Console;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.crypto.SecretKey;

import org.ortis.mochimo.farm_manager.Encryption;
import org.ortis.mochimo.farm_manager.Version;
import org.ortis.mochimo.farm_manager.farm.MiningFarm;
import org.ortis.mochimo.farm_manager.farm.MiningFarmConfig;
import org.ortis.mochimo.farm_manager.http.HttpRequestHandler;
import org.ortis.mochimo.farm_manager.http.HttpServer;
import org.ortis.mochimo.farm_manager.log.LogFactory;
import org.ortis.mochimo.farm_manager.log.LogListener;
import org.ortis.mochimo.farm_manager.utils.Host;
import org.ortis.mochimo.farm_manager.utils.Utils;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Start {@link MiningFarm} and {@link HttpServer}
 * 
 * @author Ortis <br>
 *         2018 Jul 03 9:27:34 PM <br>
 */
@Command(description = "Start the manager", name = "start", mixinStandardHelpOptions = true, version = Version.VERSION, showDefaultValues = true)
public class Start implements Callable<Void>
{

	@Option(names = { "-pw", "-pwd", "--password" }, description = "Encryption password")
	private String password = null;

	@Option(names = { "-b", "--bind" }, description = "Host to bind")
	private String hostBind = "127.0.0.1:8888";

	@Option(names = { "-sp", "--statistics-parallelism" }, paramLabel = "statistics_parallelism", description = "Number of statistics computing thread")
	private int statParallelism = 6;

	@Option(names = { "-sh", "--statistics-heartbeat" }, paramLabel = "statistics_heartbeat", description = "Delay between statistic computation in seconds")
	private int statHeartbeat = 60;

	@Option(names = { "-hp", "--http-parallelism" }, paramLabel = "http_parallelism", description = "Number of http handler thread")
	private int httpParallelism = 5;

	@Option(names = { "-ll", "--log-level" }, paramLabel = "log_level", description = "Log level (SEVERE, WARNING, INFO, FINE, FINER, FINEST)")
	private String logLevel = "INFO";

	@Parameters(index = "0", paramLabel = "configuration_file", description = "System path to mining farm configuration file")
	private String farmConfigFile;

	@Parameters(index = "1", paramLabel = "html_directory", description = "System path to html directory")
	private String htmlDirectory;

	@Override
	public Void call() throws Exception
	{

		final Logger log = CommandLines.getLog();

		try
		{
			final Supplier<LocalDateTime> clock = new Supplier<LocalDateTime>()
			{

				@Override
				public LocalDateTime get()
				{
					return LocalDateTime.now();
				}
			};

			switch (this.logLevel.toUpperCase(Locale.ENGLISH))
			{
				case "ERROR":
					LogFactory.setLevel(Level.SEVERE);
					break;
				case "WARNING":
					LogFactory.setLevel(Level.WARNING);
					break;
				case "INFO":
					LogFactory.setLevel(Level.INFO);
					break;
				case "FINE":
					LogFactory.setLevel(Level.FINE);
					break;
				case "FINER":
					LogFactory.setLevel(Level.FINER);
					break;
				case "FINEST":
					LogFactory.setLevel(Level.FINEST);
					break;

				default:
					break;
			}

			LogFactory.addLogListener(new LogListener()
			{

				@Override
				public void onLog(LogRecord record)
				{

					System.out.println(LogFactory.format(clock.get(), record));
				}
			});

			// parse config

			final Path configPath = Paths.get(this.farmConfigFile);
			if (!Files.exists(configPath))
			{
				log.severe("Config file '" + configPath + "' not found");
				return null;
			}

			log.info("Parsing config file '" + configPath + "'");

			final String json = new String(Files.readAllBytes(configPath));

			MiningFarmConfig miningFarmConfig = MiningFarmConfig.parse(json, null);

			if (miningFarmConfig.getSalt() != null)
			{
				final char [] cpassword;
				if (this.password == null)
				{
					final Console console = System.console();
					if (console == null)
					{
						log.severe("Couldn't get Console instance. Use -pw to specified password");
						return null;
					}
					cpassword = console.readPassword("Encryption password:");
				} else
					cpassword = this.password.toCharArray();

				final SecretKey key = Encryption.parseEncryptionKey(cpassword, miningFarmConfig.getSalt());
				miningFarmConfig = MiningFarmConfig.parse(json, key);
			}

			if (this.statHeartbeat <= 0)
			{
				log.severe("statistics-heartbeat must be greater than 0");
				return null;
			}

			if (this.statParallelism <= 0)
			{
				log.severe("statistics-parallelism must be greater than 0");
				return null;
			}

			final MiningFarm miningFarm = new MiningFarm(miningFarmConfig, Duration.ofSeconds(this.statHeartbeat), this.statParallelism, clock, LogFactory.getLogger("farm"));

			// http server
			if (this.httpParallelism <= 0)
			{
				log.severe("http-parallelism must be greater than 0");
				return null;
			}

			final ExecutorService httpPool = Executors.newFixedThreadPool(this.httpParallelism);
			final Path htmlPath = Paths.get(this.htmlDirectory);
			if (!Files.exists(htmlPath))
			{
				log.severe("HTML directory '" + htmlPath + "' not found");
				return null;
			}

			if (!Files.isDirectory(htmlPath))
			{
				log.severe("HTML path '" + htmlPath + "' is not a directory");
				return null;
			}

			final Host host = new Host(this.hostBind, 8888);

			HttpServer httpServer = new HttpServer(new InetSocketAddress(host.getHostname(), host.getPort()), httpPool);
			httpServer.addContext("/", new HttpRequestHandler(miningFarm, htmlPath, LogFactory.getLogger("HttpRequestHandler")));

			// start farm & httpd
			log.info("Starting farm");
			miningFarm.start();
			log.info("Starting http server");
			httpServer.start();

		} catch (final Exception e)
		{
			log.severe(Utils.formatException(e));
		}

		return null;
	}

}
