
package org.ortis.mochimo.farm_manager.farm.tasks;

import java.util.concurrent.Callable;

import org.ortis.mochimo.farm_manager.farm.miner.Miner;

public interface MinerTask extends Callable<Void>
{
	boolean acquire(final Object bidder);

	Miner getMiner();
}
