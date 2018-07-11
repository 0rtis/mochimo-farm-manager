
package org.ortis.mochimo.farm_manager.farm;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import org.ortis.mochimo.farm_manager.farm.miner.Miner;
import org.ortis.mochimo.farm_manager.farm.tasks.PolicyCheckTask;
import org.ortis.mochimo.farm_manager.farm.tasks.TaskBoard;
import org.ortis.mochimo.farm_manager.farm.tasks.TaskWorker;
import org.ortis.mochimo.farm_manager.utils.Utils;

/**
 * Monitor the {@link Miner} states and take action if needed (ex: restart stopped {@link Miner}, restart lagging {@link Miner})
 * 
 * @author Ortis <br>
 *         2018 Jul 09 8:39:58 PM <br>
 */
public class WatchDog implements Runnable
{
	private final MiningFarm farm;
	private final Duration heartbeat;
	private final Logger log;

	private final TaskBoard taskBoard;
	private final ExecutorService pool;

	public WatchDog(final MiningFarm farm, final Duration heartbeat, final int parallelism, final Logger log)
	{
		this.farm = farm;
		this.heartbeat = heartbeat;
		this.log = log;

		if (parallelism < 1)
			throw new IllegalArgumentException("Parallelism cannot be less than 1");
		
		this.taskBoard = new TaskBoard(Duration.ofMillis(1000), log);
		this.pool = Executors.newFixedThreadPool(parallelism);
		for (int i = 0; i < parallelism; i++)
		{
			final TaskWorker su = new TaskWorker(this.taskBoard, log);
			this.pool.submit(su);
		}
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

				this.log.fine("Checking miners status");
				for (final Miner miner : this.farm.getMiners())
				{

					if (this.taskBoard.contains(miner))
					{
						this.log.finest("Policy check task of miner " + miner + " is already pending. Skipping");
						continue;
					}

					this.log.fine("Requesting policy check for miner " + miner);
					this.taskBoard.add(new PolicyCheckTask(miner, this.farm));

				}

				final long elapsed = System.currentTimeMillis() - start;

				final long sleep = this.heartbeat.toMillis() - elapsed;
				if (sleep > 0)
					Thread.sleep(sleep);
				else
					this.log.warning("Check took longer than " + this.heartbeat);

			}

		} catch (final InterruptedException e)
		{
			Thread.currentThread().interrupt();

		} catch (final Exception e)
		{
			this.log.severe(Utils.formatException(e));
		} finally
		{
			this.pool.shutdownNow();
			this.log.info("Stopped");
			
		}
	}

}
