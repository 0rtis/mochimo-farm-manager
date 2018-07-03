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
package org.ortis.mochimo.farm_manager.farm;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.ortis.mochimo.farm_manager.farm.miner.Miner;
import org.ortis.mochimo.farm_manager.farm.miner.MinerStatistics;
import org.ortis.mochimo.farm_manager.utils.Utils;

/**
 * Update {@link MinerStatistics}
 * 
 * @author Ortis <br>
 *         2018 Jul 01 11:59:20 PM <br>
 */
public class StatisticsUpdater implements Runnable
{

	private final BlockingQueue<Miner> queue;
	private final Logger log;

	public StatisticsUpdater(final BlockingQueue<Miner> queue, final Logger log)
	{
		this.queue = queue;
		this.log = log;
	}

	public void run()
	{

		this.log.info("Started");
		try
		{
			while (!Thread.interrupted())
			{

				final Miner miner = this.queue.poll(1000, TimeUnit.MILLISECONDS);

				if (miner == null)
					continue;

				this.log.fine("Updating statistics for miner" + miner);

				try
				{
					miner.updateStatistics();
				} catch (final Exception e)
				{
					this.log.severe("Error while updating miner " + miner + " -> " + Utils.formatException(e));
				}

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
