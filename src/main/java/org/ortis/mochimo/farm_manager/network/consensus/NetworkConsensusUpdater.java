
package org.ortis.mochimo.farm_manager.network.consensus;

import java.time.Duration;
import java.util.logging.Logger;

import org.ortis.mochimo.farm_manager.utils.Utils;

/**
 * A {@link Runnable} for updating {@link NetworkConsensus}
 * @author Ortis
 *<br>
 *2018 Jul 08 10:28:13 PM 
 *<br>
 */
public class NetworkConsensusUpdater implements Runnable
{

	private final NetworkConsensus networkConsensus;
	private final Duration heartbeat;
	private final Logger log;

	public NetworkConsensusUpdater(final NetworkConsensus networkConsensus, final Duration heartbeat, final Logger log)
	{
		this.networkConsensus = networkConsensus;
		this.heartbeat = heartbeat;

		if (this.heartbeat.isNegative() || this.heartbeat.isZero())
			throw new IllegalArgumentException("Hearbeat duration must be greater than 0");

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

				this.log.fine("Updating network consensus");
				this.networkConsensus.update();

				final long elapsed = System.currentTimeMillis() - start;

				final long sleep = this.heartbeat.toMillis() - elapsed;
				if (sleep > 0)
					Thread.sleep(sleep);
				else
					this.log.warning("Update took longer than " + this.heartbeat);

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
