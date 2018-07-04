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

package org.ortis.mochimo.farm_manager.farm;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import org.ortis.mochimo.farm_manager.Encryption;
import org.ortis.mochimo.farm_manager.farm.miner.Miner;
import org.ortis.mochimo.farm_manager.farm.miner.MinerConfig;
import org.ortis.mochimo.farm_manager.farm.miner.SSHConnector;
import org.ortis.mochimo.farm_manager.farm.miner.SSHMiner;
import org.ortis.mochimo.farm_manager.http.HttpRequestHandler;
import org.ortis.mochimo.farm_manager.http.HttpServer;
import org.ortis.mochimo.farm_manager.log.LogFactory;
import org.ortis.mochimo.farm_manager.log.LogListener;
import org.ortis.mochimo.farm_manager.utils.Host;

import com.jcraft.jsch.JSchException;

/**
 * Aggregation of {@link Miner}
 * 
 * @author Ortis <br>
 *         2018 Jul 01 11:56:32 PM <br>
 */
public class MiningFarm
{
	private final Supplier<LocalDateTime> clock;
	private final Logger log;

	private Thread statisticUpdateSchedulerThread;
	private final ExecutorService statisticsPool;
	private final List<Miner> miners;
	private final List<Miner> roMiners;

	public MiningFarm(final MiningFarmConfig config, final Duration statisticsUpdateHeartbeat, final int statisticsParallelism, final Supplier<LocalDateTime> clock, final Logger log)
			throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException,
			BadPaddingException, JSchException
	{

		this.clock = clock;
		this.log = log;

		this.miners = new ArrayList<>();
		this.roMiners = Collections.unmodifiableList(this.miners);

		final SecretKey key = config.getKey();

		final List<String> ids = new ArrayList<>();
		for (final MinerConfig minerConfig : config.getMinerConfigs())
		{
			final FieldConfig cfid = minerConfig.get(MinerConfig.MINER_ID_LABEL);
			final String id = cfid.getValue();
			if (ids.contains(cfid.getValue()))
				throw new IllegalArgumentException("Duplicate miner id " + id);

			this.log.fine("Building miner " + cfid.getValue());

			final String hostString = parseField(id, minerConfig, "host", key);
			if (hostString == null)
				throw new IllegalArgumentException("Field " + id + "@host is missing");

			final Host host = new Host(hostString, 22);

			final String user = parseField(id, minerConfig, "user", key);
			if (user == null)
				throw new IllegalArgumentException("Field " + id + "@user is missing");

			final String password = parseField(id, minerConfig, "password", key);
			final String privateKey = parseField(id, minerConfig, "privateKeyPath", key);

			final String startCommand = parseField(id, minerConfig, "startCommand", key);
			final String stopCommand = parseField(id, minerConfig, "stopCommand", key);
			final String logCommand = parseField(id, minerConfig, "logCommand", key);

			final SSHConnector connector = new SSHConnector(id, host.getHostname(), host.getPort(), user, password, privateKey, LogFactory.getLogger(id + "-SSHConnector"));
			final SSHMiner miner = new SSHMiner(id, startCommand, stopCommand, logCommand, connector, this.clock, LogFactory.getLogger(id + "-Miner"));
			this.miners.add(miner);
		}

		final TaskBoard taskBoard = new TaskBoard(Duration.ofMillis(1000), LogFactory.getLogger("TaskBoard"));
		this.statisticUpdateSchedulerThread = new Thread(
				new StatisticsUpdateScheduler(this, Duration.ofMillis(Math.max(1000, statisticsUpdateHeartbeat.toMillis() / 10)), statisticsUpdateHeartbeat, taskBoard, this.clock, log));
		this.statisticUpdateSchedulerThread.setName("StatisticsUpdateScheduler");
		this.statisticsPool = Executors.newFixedThreadPool(statisticsParallelism);

		for (int i = 0; i < statisticsParallelism; i++)
		{
			final StatisticsUpdater su = new StatisticsUpdater(taskBoard, log);
			this.statisticsPool.submit(su);
		}

	}

	private String parseField(final String id, final MinerConfig minerConfig, final String field, final SecretKey key)
			throws InvalidKeyException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException
	{

		final FieldConfig cf = minerConfig.get(field);
		if (cf == null)
			return null;

		if (cf.isEncrytped())
		{
			return Encryption.decrypt(cf.getValue(), key);

		} else
		{
			this.log.warning("Field " + id + "@" + cf.getName() + " is in plain text");
			return cf.getValue();
		}

	}

	public MiningFarmStatistics statistics()
	{
		return new MiningFarmStatistics(this);
	}

	public void start()
	{
		this.log.info("Starting statistics updater");
		this.statisticUpdateSchedulerThread.start();

	}

	public void stop()
	{
		this.log.info("Stopping statistics updater");
		this.statisticUpdateSchedulerThread.interrupt();

	}

	public Miner getMiner(final String id)
	{

		for (final Miner miner : this.roMiners)
			if (miner.getId().equals(id))
				return miner;

		return null;
	}

	public List<Miner> getMiners()
	{
		return this.roMiners;
	}

	public static void main(String [] args) throws Exception
	{

		final Supplier<LocalDateTime> clock = new Supplier<LocalDateTime>()
		{

			@Override
			public LocalDateTime get()
			{
				return LocalDateTime.now();
			}
		};

		LogFactory.setLevel(Level.INFO);
		LogFactory.addLogListener(new LogListener()
		{

			@Override
			public void onLog(LogRecord record)
			{

				System.out.println(LogFactory.format(clock.get(), record));
			}
		});
		// parse config
		final Path p = Paths.get("pool-configs\\prod_mining_pool.json");
		final Path html = Paths.get("html");
		final String json = new String(Files.readAllBytes(p));
		final MiningFarmConfig mfc = MiningFarmConfig.parse(json, null);

		MiningFarm farm = new MiningFarm(mfc, Duration.ofSeconds(20), 3, clock, LogFactory.getLogger("farm"));
		farm.start();

		String host = "127.0.0.1:8888";

		final ExecutorService httpPool = Executors.newFixedThreadPool(3);

		int port = 80;
		if (host.contains(":"))
		{
			final String [] buffer = host.split(":");
			port = Integer.parseInt(buffer[buffer.length - 1]);
			host = host.substring(0, host.length() - buffer[buffer.length - 1].length() - 1);
		}

		HttpServer httpServer = new HttpServer(new InetSocketAddress(host, port), httpPool);
		httpServer.addContext("/", new HttpRequestHandler(farm, html, LogFactory.getLogger("HttpRequestHandler")));

		httpServer.start();
	}

}
