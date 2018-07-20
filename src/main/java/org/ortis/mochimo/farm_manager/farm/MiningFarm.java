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
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
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
import org.ortis.mochimo.farm_manager.log.LogFactory;
import org.ortis.mochimo.farm_manager.network.consensus.NetworkConsensus;
import org.ortis.mochimo.farm_manager.network.consensus.NetworkConsensusFactory;
import org.ortis.mochimo.farm_manager.network.consensus.NetworkConsensusUpdater;
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
	private Thread watchDogThread;

	private final List<Miner> miners;
	private final List<Miner> roMiners;

	private final NetworkConsensus networkConsensus;
	private Thread networkConsensusUpdateThread;

	public MiningFarm(final MiningFarmConfig config, final Duration statisticsUpdateHeartbeat, final int statisticsParallelism, final Duration watchDogHeartbeat, final int watchDogParallelism,
			final Duration networkConsensusUpdateHeartbeat, final Supplier<LocalDateTime> clock, final Logger log) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException,
			InvalidKeyException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, JSchException
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
			final String id = cfid.getValue().trim();
			if (ids.contains(id))
				throw new IllegalArgumentException("Duplicate miner id " + id);

			ids.add(id);

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
			final List<String> policies = parseArrayField(id, minerConfig, "policy", key);

			final SSHConnector connector = new SSHConnector(id, host.getHostname(), host.getPort(), user, password, privateKey, LogFactory.getLogger(id + "-SSHConnector"));
			final SSHMiner miner = new SSHMiner(id, startCommand, stopCommand, logCommand, policies, connector, this.clock, LogFactory.getLogger(id + "-Miner"));
			this.miners.add(miner);
		}

		// set watchdog
		final WatchDog watchDog = new WatchDog(this, watchDogHeartbeat, watchDogParallelism, this.log);
		this.watchDogThread = new Thread(watchDog);
		this.watchDogThread.setName("WatchDog");

		// set consensus
		if (config.getNetworkConsensuses().isEmpty())
		{
			this.networkConsensus = NetworkConsensusFactory.getDefaultNetworkConsensus();
			this.log.info("Using default network consensus " + this.networkConsensus);
		} else
		{
			this.networkConsensus = NetworkConsensusFactory.get(config.getNetworkConsensuses(), this);
			this.log.info("Using network consensus " + this.networkConsensus);
		}

		this.networkConsensusUpdateThread = new Thread(new NetworkConsensusUpdater(this.networkConsensus, networkConsensusUpdateHeartbeat, log));
		this.networkConsensusUpdateThread.setName("NetworkConsensusUpdater");

		this.statisticUpdateSchedulerThread = new Thread(
				new StatisticsUpdateScheduler(this, Duration.ofMillis(Math.max(1000, statisticsUpdateHeartbeat.toMillis() / 10)), statisticsUpdateHeartbeat, statisticsParallelism, this.clock, log));
		this.statisticUpdateSchedulerThread.setName("StatisticsUpdateScheduler");

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

	private List<String> parseArrayField(final String id, final MinerConfig minerConfig, final String field, final SecretKey key)
			throws InvalidKeyException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException
	{

		final List<String> array = new ArrayList<>();

		final FieldConfig cf = minerConfig.get(field);
		if (cf == null)
			return array;

		final String serial;
		if (cf.isEncrytped())
		{
			serial = Encryption.decrypt(cf.getValue(), key);

		} else
		{
			this.log.warning("Field " + id + "@" + cf.getName() + " is in plain text");
			serial = cf.getValue();
		}

		final String [] entries = serial.trim().split(",|;");
		for (final String entry : entries)
			array.add(entry.trim());

		return array;

	}

	public MiningFarmStatistics statistics()
	{
		return new MiningFarmStatistics(this);
	}

	public void start()
	{
		this.log.info("Starting statistics updater");
		this.statisticUpdateSchedulerThread.start();
		this.watchDogThread.start();
		this.networkConsensusUpdateThread.start();
	}

	public void stop()
	{
		this.log.info("Stopping statistics updater");
		this.statisticUpdateSchedulerThread.interrupt();
		this.watchDogThread.interrupt();
		this.networkConsensusUpdateThread.interrupt();
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

	public NetworkConsensus getNetworkConsensus()
	{
		return this.networkConsensus;
	}

}
