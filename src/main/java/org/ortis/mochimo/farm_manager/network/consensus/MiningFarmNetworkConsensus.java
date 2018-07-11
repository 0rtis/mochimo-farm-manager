
package org.ortis.mochimo.farm_manager.network.consensus;

import java.util.ArrayList;
import java.util.List;

import org.ortis.mochimo.farm_manager.farm.MiningFarm;
import org.ortis.mochimo.farm_manager.farm.miner.Miner;
import org.ortis.mochimo.farm_manager.farm.miner.MinerStatistics;

/**
 * Am implementation of {@link NetworkConsensus} that derives consensus from the state of an underlying {@link MiningFarm}
 * 
 * @author Ortis <br>
 *         2018 Jul 08 10:29:02 PM <br>
 */
public class MiningFarmNetworkConsensus implements NetworkConsensus
{

	private final MiningFarm farm;
	private final List<String> minerIds;

	private Double height;
	private Double difficulty;

	private final String toString;

	public MiningFarmNetworkConsensus(final MiningFarm miningFarm)
	{
		final List<String> minerIds = new ArrayList<>();
		miningFarm.getMiners().forEach(m -> minerIds.add(m.getId()));

		this.farm = miningFarm;
		this.minerIds = new ArrayList<>(minerIds);
		this.toString = "Mining Farm";

	}

	public MiningFarmNetworkConsensus(final MiningFarm miningFarm, final List<String> minerIds)
	{
		this.farm = miningFarm;
		this.minerIds = new ArrayList<>(minerIds);

		final StringBuilder sb = new StringBuilder();
		this.minerIds.forEach(m -> sb.append(m.toString()).append(", "));

		if (sb.length() > 2)
			sb.delete(sb.length() - 2, sb.length());

		this.toString = sb.toString();
	}

	@Override
	public synchronized void update() throws Exception
	{

		Integer height = null;
		int hi = 0;
		Integer difficulty = null;
		int di = 0;

		for (final String minerId : this.minerIds)
		{
			final Miner miner = this.farm.getMiner(minerId);
			if (miner == null)
				continue;

			final MinerStatistics stat = miner.getStatistics();

			if (stat == null || stat.isDefault())
				continue;

			final String hex = stat.getStatistics().get("Block");
			if (hex != null)
			{
				if (height == null)
					height = Integer.parseInt(hex.substring(2), 16);
				else
					height += Integer.parseInt(hex.substring(2), 16);
				hi++;
			}

			final String diff = stat.getStatistics().get("Difficulty");
			if (diff != null)
			{
				if (difficulty == null)
					difficulty = Integer.parseInt(diff);
				else
					difficulty += Integer.parseInt(diff);
				di++;
			}
		}

		this.height = height == null ? null : ((double) height / hi);
		this.difficulty = difficulty == null ? null : ((double) difficulty / di);

	}

	@Override
	public synchronized Double getDifficulty()
	{
		return this.difficulty;
	}

	@Override
	public synchronized Double getHeight()
	{
		return this.height;
	}

	@Override
	public String toString()
	{
		return this.toString;
	}
}
