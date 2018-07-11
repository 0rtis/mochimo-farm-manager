
package org.ortis.mochimo.farm_manager.farm.tasks;

import org.ortis.mochimo.farm_manager.farm.miner.Miner;

public class StatisticsUpdateTask implements MinerTask
{
	private final Miner miner;

	private Object owner;
	private final Object lock = new Object();

	public StatisticsUpdateTask(final Miner miner)
	{
		this.miner = miner;

	}

	@Override
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

	@Override
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
		if (o == this)
			return true;

		if (o instanceof StatisticsUpdateTask)
		{
			final StatisticsUpdateTask task = (StatisticsUpdateTask) o;

			return task.getMiner().equals(this.miner);
		}

		return false;
	}

	@Override
	public String toString()
	{
		return this.miner.getId() + "@" + this.getClass().getSimpleName();
	}
}
