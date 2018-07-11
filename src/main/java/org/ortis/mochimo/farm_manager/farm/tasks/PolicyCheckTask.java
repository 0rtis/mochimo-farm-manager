
package org.ortis.mochimo.farm_manager.farm.tasks;

import org.ortis.mochimo.farm_manager.farm.MiningFarm;
import org.ortis.mochimo.farm_manager.farm.miner.Miner;

public class PolicyCheckTask implements MinerTask
{
	private final Miner miner;
	private final MiningFarm farm;
	private Object owner;
	private final Object lock = new Object();

	public PolicyCheckTask(final Miner miner, final MiningFarm farm)
	{
		this.miner = miner;
		this.farm = farm;
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
		this.miner.checkPolicies(this.farm.getNetworkConsensus());
		return null;
	}

	@Override
	public Miner getMiner()
	{
		return this.miner;
	}

	@Override
	public String toString()
	{
		return this.miner.getId() + "@" + this.getClass().getSimpleName();
	}
}
