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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Supplier;
import java.util.logging.Logger;

import org.ortis.mochimo.farm_manager.farm.miner.Miner;
import org.ortis.mochimo.farm_manager.farm.miner.MinerStatistics;
import org.ortis.mochimo.farm_manager.utils.Utils;

/**
 * Schedule update of {@link MinerStatistics}
 * 
 * @author Ortis <br>
 *         2018 Jul 02 12:49:10 AM <br>
 */
public class StatisticsUpdateScheduler implements Runnable
{

	private final MiningFarm farm;
	private final Duration checkHeartbeat;
	private final Duration updateHeartbeat;
	private final Queue<Miner> queue;
	private final Supplier<LocalDateTime> clock;
	private Logger log;

	/**
	 * 
	 * @param farm:
	 *            {@link MiningFarm} to monitor
	 * @param checkHeartbeat:
	 *            minimum time between checks
	 * @param updateHeartbeat:
	 *            minimum time between {@link MinerStatistics} update
	 * @param queue:
	 *            pending update queue
	 * @param log
	 */
	public StatisticsUpdateScheduler(final MiningFarm farm, final Duration checkHeartbeat, final Duration updateHeartbeat, final BlockingQueue<Miner> queue, final Supplier<LocalDateTime> clock,
			final Logger log)
	{
		this.farm = farm;
		this.checkHeartbeat = checkHeartbeat;
		if (this.checkHeartbeat.isNegative() || this.checkHeartbeat.isZero())
			throw new IllegalArgumentException("Check heartbeat duration must be positive");

		this.updateHeartbeat = updateHeartbeat;
		if (this.updateHeartbeat.isNegative() || this.updateHeartbeat.isZero())
			throw new IllegalArgumentException("Update heartbeat duration must be positive");

		this.queue = queue;
		this.clock = clock;
		this.log = log;

	}

	@Override
	public void run()
	{
		this.log.info("Started");
		try
		{
			while (!Thread.interrupted())
			{

				final long start = System.currentTimeMillis();
				for (final Miner miner : farm.getMiners())
				{

					if (this.queue.contains(miner))
					{
						this.log.finest("Statistics update of miner " + miner + " is already pending. Skipping");
						continue;
					}

					if (miner.getStatistics().isDefault() || Duration.between(miner.getStatistics().getTime(), this.clock.get()).compareTo(this.updateHeartbeat) > 0)
					{
						this.log.fine("Requesting update for miner " + miner);
						this.queue.add(miner);
					}

				}

				this.log.fine("Pending statistics update -> " + this.queue.size());

				final long elapsed = System.currentTimeMillis() - start;

				final long sleep = this.checkHeartbeat.toMillis() - elapsed;
				if (sleep <= 0)
					this.log.warning("Check run took longer than " + this.checkHeartbeat);
				else
					Thread.sleep(sleep);

			}

		} catch (final InterruptedException e)
		{
			Thread.currentThread().interrupt();

		} catch (final Exception e)
		{
			this.log.severe(Utils.formatException(e));
		} finally
		{
			this.log.info("Stopped");
		}
	}

}
