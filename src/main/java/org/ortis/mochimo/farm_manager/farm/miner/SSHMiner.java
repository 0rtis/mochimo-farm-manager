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
package org.ortis.mochimo.farm_manager.farm.miner;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * An SSH based implementation of {@link Miner}.
 * 
 * @author Ortis <br>
 *         2018 Jul 01 11:58:45 PM <br>
 */
public class SSHMiner implements Miner
{

	private final String id;
	private final String startCommand;
	private final String stopCommand;
	private final String logCommand;
	private final SSHConnector connector;
	private final Supplier<LocalDateTime> clock;
	private final Logger log;

	private MinerStatistics statistics;
	private final Object statisticsLock = new Object();

	public SSHMiner(final String id, final String startCommand, final String stopCommand, final String logCommand, final SSHConnector connector, final Supplier<LocalDateTime> clock, final Logger log)
	{
		this.id = id;
		this.startCommand = startCommand;
		this.stopCommand = stopCommand;
		this.logCommand = logCommand;
		this.connector = connector;
		this.clock = clock;
		this.log = log;

		clearStatistics();
	}

	private <D extends Collection<String>> D parsePids(final D destination) throws Exception
	{
		final List<String> stdout = this.connector.execute("ps faux | grep mochi");

		for (final String line : stdout)
		{
			if (line.contains("grep"))
				continue;
			destination.add(line.split(" +")[1]);
		}

		return destination;
	}

	public synchronized boolean start() throws Exception
	{
		final List<String> pids = new ArrayList<>();
		if (parsePids(pids).size() > 0)
			return true;

		if (this.startCommand == null)
		{
			this.log.warning("Start command not set");
			return false;
		}

		this.log.info("Starting");
		this.connector.execute(this.startCommand);
		Thread.sleep(3000); // let processes spawn
		pids.clear();
		final boolean success = parsePids(pids).size() > 0;
		return success;
	}

	public synchronized boolean stop() throws Exception
	{
		final List<String> pids = parsePids(new ArrayList<>());

		if (pids.isEmpty())
			return true;

		if (this.stopCommand == null)
		{
			this.log.info("Stopping with Kill Command (Deal 3 damage. If you have a Beast, deal 5 damage instead.)");
			// get mochimo pids and kill them all !

			final StringBuilder sb = new StringBuilder("date");
			for (final String pid : pids)
				sb.append(" && kill ").append(pid);
			this.connector.execute(sb.toString());
		} else
		{
			this.log.info("Stopping");
			this.connector.execute(this.stopCommand);
		}

		Thread.sleep(3000); // let processes vanish
		pids.clear();
		final boolean success = parsePids(pids).isEmpty();
		return success;
	}

	public synchronized boolean isRunning() throws Exception
	{
		return parsePids(new ArrayList<>()).size() > 0;
	}

	public synchronized void updateStatistics() throws Exception
	{
		this.log.fine("Updating statistics");

		final MinerStatistics minerStatistics = new MinerStatistics(this.id, this.clock.get());
		List<String> stdout = this.connector.execute("top -bcn1");

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

		for (final String line : stdout)
		{
			if (line.contains("grep"))
				continue;

			if (line.contains("gomochi"))
				minerStatistics.addProcess("gomochi");
			else
			{
				final String [] buffer = line.split("mochimo ");
				if (buffer.length > 1)
					minerStatistics.addProcess(buffer[1].trim());
			}
		}

		minerStatistics.getProcesses().sort(null);

		for (final String process : minerStatistics.getProcesses())
		{
			if (process.contains("gomochi"))
				minerStatistics.setGomochi(true);
			else if (process.contains("listen"))
				minerStatistics.setListen(true);
			else if (process.contains("solving"))
				minerStatistics.setSolving(true);
		}

		if (this.logCommand != null)
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
					minerStatistics.setStatistics("Block", "0x" + line.split(": 0x")[1].split(" +")[0]);

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

}
