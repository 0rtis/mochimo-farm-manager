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

package org.ortis.mochimo.farm_manager.farm.miner;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.logging.Logger;

import org.ortis.mochimo.farm_manager.network.MochimoNetwork;
import org.ortis.mochimo.farm_manager.network.consensus.NetworkConsensus;

/**
 * An SSH based implementation of {@link Miner}.
 * 
 * @author Ortis <br>
 *         2018 Jul 01 11:58:45 PM <br>
 */
public class SSHMiner implements Miner
{
	private final static long SWITCH_SLEEP = 3000;
	private final static int SWITCH_LOOP = 3;

	private final String id;
	private final String startCommand;
	private final String stopCommand;
	private final String logCommand;
	private final SSHConnector connector;
	private final List<String> policies;
	private final Supplier<LocalDateTime> clock;
	private final Logger log;

	private MinerStatistics statistics;
	private final Object statisticsLock = new Object();

	private LocalDateTime startTime;
	private LocalDateTime stopTime;
	private final Object timeLock = new Object();

	public SSHMiner(final String id, final String startCommand, final String stopCommand, final String logCommand, final List<String> restartPolicies, final SSHConnector connector,
			final Supplier<LocalDateTime> clock, final Logger log)
	{
		this.id = id;
		this.startCommand = startCommand;
		this.stopCommand = stopCommand;
		this.logCommand = logCommand;

		checkPoliciesFormat(restartPolicies);
		this.policies = Collections.unmodifiableList(new ArrayList<>(restartPolicies));

		this.connector = connector;
		this.clock = clock;
		this.log = log;

		clearStatistics();
	}

	private <D extends Collection<String>> D parseMainPids(final D destination) throws Exception
	{

		final List<String> stdout = this.connector.execute("pidof mochimo ; echo 'delimiter' ; ps faux | grep 'gomochi '");

		boolean delimiter = false;
		for (final String line : stdout)
		{
			if (line.startsWith("delimiter"))
			{
				delimiter = true;
				continue;
			}

			if (delimiter)
			{
				if (line.contains("grep"))
					continue;

				destination.add(line.split(" +")[1]);

			} else
			{
				final String [] buffer = line.split(" +");
				for (final String pid : buffer)
					destination.add(pid);
			}
		}

		return destination;
	}

	private boolean waitSwitch(final boolean start) throws InterruptedException, Exception
	{

		for (int i = 0; i < SWITCH_LOOP; i++)
		{
			if (i > 0)
				this.log.info("Waiting for miner to " + (start ? "start" : "stop"));

			Thread.sleep(SWITCH_SLEEP);

			final boolean running = isRunning();

			if (start && running)
				return true;

			if (!start && !running)
				return true;

		}

		return false;
	}

	@Override
	public synchronized boolean start() throws Exception
	{

		if (isRunning())
			return true;

		if (this.startCommand == null)
		{
			this.log.warning("Start command not set");
			return false;
		}

		this.log.info("Starting");
		this.log.fine("Start command -> " + this.startCommand);
		final List<String> stdout = this.connector.execute(this.startCommand);
		final StringBuilder sbout = new StringBuilder("Start command STDOUT -> ");
		stdout.forEach(l -> sbout.append("\n").append(l));
		this.log.fine(sbout.toString());

		// let processes spawn
		final boolean success = waitSwitch(true);

		if (success)
			synchronized (this.timeLock)
			{
				this.startTime = this.clock.get();
				this.stopTime = null;
			}
		clearStatistics();
		return success;
	}

	@Override
	public synchronized boolean stop() throws Exception
	{
		final List<String> pids = parseMainPids(new ArrayList<>());

		if (pids.isEmpty())
			return true;

		if (this.stopCommand == null)
		{
			this.log.info("Stopping with Kill Command (Deal 3 damage. If you have a Beast, deal 5 damage instead.)");

			// get mochimo pids and kill them all !
			final StringBuilder sb = new StringBuilder("date");
			for (final String pid : pids)
				sb.append(" ; kill ").append(pid); // separate kill: sometime we will get something that is not mochimo and the kill will failed because we dont have the right.

			this.log.fine("Stop command -> " + sb);
			final List<String> stdout = this.connector.execute(sb.toString());
			final StringBuilder sbout = new StringBuilder("Stop command STDOUT -> ");
			stdout.forEach(l -> sbout.append("\n").append(l));
			this.log.fine(sbout.toString());
		} else
		{
			this.log.info("Stopping");
			this.log.fine("Stop command -> " + this.stopCommand);
			final List<String> stdout = this.connector.execute(this.stopCommand);
			final StringBuilder sbout = new StringBuilder("Stop command STDOUT -> \n");
			stdout.forEach(l -> sbout.append("\n").append(l));
			this.log.fine(sbout.toString());
		}

		// let processes vanish
		final boolean success = waitSwitch(false);

		if (success)
			synchronized (this.timeLock)
			{
				this.stopTime = this.clock.get();
				this.startTime = null;
			}

		clearStatistics();

		return success;
	}

	@Override
	public synchronized boolean restart() throws Exception
	{
		// stop if running
		if (isRunning())
			if (!stop())
				return false;// could not be stopped

		return start();

	}

	public synchronized boolean isRunning() throws Exception
	{
		return !parseMainPids(new ArrayList<>()).isEmpty();
	}

	public synchronized void updateStatistics() throws Exception
	{
		this.log.fine("Updating statistics");

		final MinerStatistics minerStatistics = new MinerStatistics(this.id, this.clock.get());
		List<String> stdout = this.connector.execute("top -bcn2");// tic 2 time. First one are not accurate

		for (final String line : stdout)
		{
			if (line.contains("Cpu(s): "))
			{
				final String [] buffer = line.substring(8).split(",");
				float load = 0;
				for (final String b : buffer)
				{
					if (b.contains("id"))
						continue;

					final String [] bb = b.trim().split("%|( )");
					load += Float.parseFloat(bb[0]);
				}
				minerStatistics.setCpu(load);
			}

		}

		stdout = this.connector.execute("ps faux | grep mochi");

		for (String line : stdout)
		{
			String [] buffer = line.split(" +");

			// remove username in case it is 'mochimo' or 'gomochi'
			final StringBuilder sb = new StringBuilder();
			for (int i = 1; i < buffer.length; i++)
				sb.append(buffer[i]).append(" ");

			line = sb.toString();

			if (line.contains("grep"))
				continue;

			if (line.contains("gomochi"))
				minerStatistics.addProcess("gomochi");
			else
			{
				buffer = line.split("mochimo ");
				if (buffer.length > 1)
					minerStatistics.addProcess(buffer[1].trim());
			}
		}

		minerStatistics.getProcesses().sort(null);

		minerStatistics.setGomochi(false);
		minerStatistics.setListen(false);
		minerStatistics.setSolving(false);
		minerStatistics.setSyncing(false);

		for (final String process : minerStatistics.getProcesses())
		{
			if (process.contains("gomochi"))
				minerStatistics.setGomochi(true);
			else if (process.contains("listen"))
				minerStatistics.setListen(true);
			else if (process.contains("solving"))
				minerStatistics.setSolving(true);
			else if (process.contains("update"))
				minerStatistics.setSyncing(true);
			else if (process.contains("getblock"))
				minerStatistics.setSyncing(true);
			else if (process.contains("coreip"))
				minerStatistics.setSyncing(true);
			else if (process.contains("quorum"))
				minerStatistics.setSyncing(true);

		}

		if (!minerStatistics.getProcesses().isEmpty() && this.logCommand != null)
		{
			stdout = this.connector.execute(this.logCommand);
			for (String line : stdout)
			{
				line = line.replaceAll(" +", " ");
				if (line.contains("Haiku/second:"))
				{
					final String [] buffer = line.split(" ");
					for (int i = 0; i < buffer.length; i += 2)
						minerStatistics.setStatistics(buffer[i].substring(0, buffer[i].length() - 1), buffer[i + 1]);

				} else if (line.contains(": 0x"))
				{
					final String hex = line.split(": 0x")[1].split(" +")[0];
					minerStatistics.setStatistics("Block", "0x" + hex);
					minerStatistics.setStatistics("Height", Integer.toString(Integer.parseInt(hex, 16)));
				}

			}
		}

		synchronized (this.statisticsLock)
		{
			this.statistics = minerStatistics;

		}
	}

	@Override
	public void clearStatistics()
	{
		synchronized (this.statisticsLock)
		{
			this.statistics = MinerStatistics.getEmptyStatistics(this.id, this.clock);
		}
	}

	public MinerStatistics getStatistics()
	{
		synchronized (this.statisticsLock)
		{
			return this.statistics;
		}
	}

	@Override
	public void checkPolicies(final NetworkConsensus networkConsensus) throws Exception
	{
		final Duration uptime;
		final Duration downtime;
		if (isRunning())
		{
			synchronized (this.timeLock)
			{

				if (this.startTime == null)
				{
					if (this.stopTime != null)// dont log on farm start
						this.log.info("External start detected");

					this.startTime = this.clock.get();
					uptime = Duration.ZERO;
				} else
				{
					uptime = Duration.between(this.startTime, this.clock.get());
					this.log.fine("Uptime = " + uptime);
				}

				this.stopTime = null;
			}
			downtime = null;
		} else
		{
			synchronized (this.timeLock)
			{

				if (this.stopTime == null)
				{
					if (this.startTime != null)// dont log on farm start
						this.log.info("External stop detected");
					this.stopTime = this.clock.get();
					downtime = Duration.ZERO;
				} else
				{
					downtime = Duration.between(this.stopTime, this.clock.get());
					this.log.fine("Downtime = " + downtime);
				}

				this.startTime = null;
			}

			uptime = null;
		}

		final MinerStatistics stat = getStatistics();
		final Double networkHeight = networkConsensus == null ? null : networkConsensus.getHeight();

		String restartTrigger = null;
		for (final String policy : getPolicies())
		{
			if (restartTrigger != null)
				break;

			final String upperPolicy = policy.trim().toUpperCase(Locale.ENGLISH);
			if (upperPolicy.startsWith("MAXDOWNTIME"))
			{
				if (downtime == null)
					continue;

				final String [] buffer = upperPolicy.split(" +");

				Duration delay = Duration.ZERO;
				delay = Duration.parse("PT" + buffer[1]);

				if (downtime.compareTo(delay) > 0)
					restartTrigger = "Max Downtime " + delay + " (current downtime = " + downtime + ")";

			} else if (upperPolicy.startsWith("MAXUPTIME"))
			{
				if (uptime == null)
					continue;

				final String [] buffer = upperPolicy.split(" +");

				Duration delay = Duration.ZERO;
				delay = Duration.parse("PT" + buffer[1]);

				if (uptime.compareTo(delay) > 0)
					restartTrigger = "Max Uptime " + delay + "(current uptime = " + uptime + ")";

			} else if (upperPolicy.startsWith("MAXLAG"))
			{

				if (uptime == null /*miner is running*/ || networkHeight == null || stat == null || stat.isDefault() || stat.isSyncing() || !stat.getStatistics().containsKey("Height")
						|| stat.getAge(this.clock.get()).compareTo(MochimoNetwork.TARGET_BLOCK_TIME) > 0)
					continue;

				final Integer minerHeight = Integer.parseInt(stat.getStatistics().get("Height"));

				final double lag = networkHeight - minerHeight;

				final String [] buffer = upperPolicy.split(" +");

				final double delay = Double.parseDouble(buffer[1]);
				if (delay < 1)
					throw new IllegalArgumentException("Lag policy cannot be less than 1");

				if (lag >= delay)
					restartTrigger = "Lag " + delay + " (current lag = " + lag + ")";
			}

		}

		if (restartTrigger != null)
		{
			this.log.info("Policy " + restartTrigger + " triggered. Initiating restart.");

			if (restart())
				this.log.info("Restart sucessfull");
			else
				this.log.warning("Restart Failed");

			clearStatistics();
		}

	}

	@Override
	public List<String> getPolicies()
	{
		return this.policies;
	}

	public String getId()
	{
		return this.id;
	}

	@Override
	public int hashCode()
	{
		return this.id.hashCode();
	}

	@Override
	public boolean equals(final Object o)
	{
		if (this == o)
			return true;

		if (o instanceof SSHMiner)
		{
			final SSHMiner miner = (SSHMiner) o;
			return this.id.equals(miner.id);
		}
		return false;
	}

	@Override
	public String toString()
	{
		return this.id;
	}

	public static void checkPoliciesFormat(final List<String> policies)
	{
		for (final String policy : policies)
			checkPolicyFormat(policy);
	}

	public static void checkPolicyFormat(final String policy)
	{
		try
		{
			final String upperPolicy = policy.trim().toUpperCase(Locale.ENGLISH);
			if (upperPolicy.startsWith("MAXDOWNTIME"))
			{

				final String [] buffer = upperPolicy.split(" +");

				Duration d = null;

				if (buffer.length < 1)
					throw new IllegalArgumentException("Could not parse value of Max Downtime policy");

				d = Duration.parse("PT" + buffer[1]);

				if (d.isNegative())
					throw new IllegalArgumentException("Max Downtime policy value cannot be less than 0");

			} else if (upperPolicy.startsWith("MAXUPTIME"))
			{

				final String [] buffer = upperPolicy.split(" +");

				Duration d = null;

				if (buffer.length < 1)
					throw new IllegalArgumentException("Could not parse value of Max Uptime policy");

				d = Duration.parse("PT" + buffer[1]);

				if (d.isNegative())
					throw new IllegalArgumentException("Max Uptime policy value cannot be less than 0");

			} else if (upperPolicy.startsWith("MAXLAG"))
			{

				final String [] buffer = upperPolicy.split(" +");

				final double delay = Double.parseDouble(buffer[1]);

				if (delay < 1)
					throw new IllegalArgumentException("Max Lag policy cannot be less than 1");

			} else
				throw new IllegalArgumentException("Failed to parse policy '" + policy + "'");

		} catch (final Exception e)
		{
			throw new RuntimeException("Check of policy '" + policy + "' failed", e);
		}
	}
}
