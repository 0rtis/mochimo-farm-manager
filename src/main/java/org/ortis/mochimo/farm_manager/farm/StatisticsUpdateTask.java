
package org.ortis.mochimo.farm_manager.farm;

import java.util.concurrent.Callable;

import org.ortis.mochimo.farm_manager.farm.miner.Miner;

public class StatisticsUpdateTask implements Callable<Void>
{
	private final Miner miner;

	private Object owner;
	private final Object lock = new Object();

	public StatisticsUpdateTask(final Miner miner)
	{
		this.miner = miner;

	}

	public boolean acquire(final Object bidder)
	{
		synchronized (this.lock)
		{

			if (this.owner == null)
			{
				this.owner = bidder;
				return true;
			} else
				return false;

		}
	}

	@Override
	public Void call() throws Exception
	{
		this.miner.updateStatistics();

		return null;
	}

	public Miner getMiner()
	{
		return miner;
	}

	@Override
	public int hashCode()
	{
		return this.miner.hashCode();
	}

	@Override
	public boolean equals(final Object o)
	{
		if (o == this.miner)
			return true;

		if (o instanceof StatisticsUpdateTask)
		{
			final StatisticsUpdateTask task = (StatisticsUpdateTask) o;

			return task.getMiner().equals(this.miner);
		}

		return false;
	}

}
