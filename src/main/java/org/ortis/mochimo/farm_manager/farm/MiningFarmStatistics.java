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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.ortis.mochimo.farm_manager.farm.miner.Miner;
import org.ortis.mochimo.farm_manager.farm.miner.MinerStatistics;
import org.ortis.mochimo.farm_manager.network.MochimoNetwork;

public class MiningFarmStatistics
{
	private final Double totalHPS;
	private final Integer totalSolved;
	private final Integer solvingMiners;
	private final Integer runningMiners;

	private final Double networkHeightConsensus;
	private final Double networkDifficultyConsensus;
	private final Double networkBlockReward;
	private final Double networkEstimatedHPS;
	private final Long estimatedTimeToReward;
	private final String humanEstimatedTimeToReward;

	private final List<MinerStatistics> miners;

	public MiningFarmStatistics(final MiningFarm farm)
	{
		this.miners = new ArrayList<>();

		double totalHPS = 0;
		int totalSolved = 0;
		int solving = 0;
		int running = 0;

		for (final Miner miner : farm.getMiners())
		{
			final MinerStatistics stat = miner.getStatistics();

			this.miners.add(stat);

			if (stat.isDefault())
				continue;// dont aggregate if default statistics

			if (stat.isRunning())
				running++;

			for (final Map.Entry<String, String> entry : stat.getStatistics().entrySet())
			{
				if (stat.isSolving() && entry.getKey().contains("aiku/second"))
					totalHPS += Double.parseDouble(entry.getValue());
				else if (entry.getKey().contains("olved"))
					totalSolved += Integer.parseInt(entry.getValue());

			}

			if (stat.isSolving() != null && stat.isSolving())
				solving++;
		}

		if (this.miners.isEmpty())
		{
			this.totalHPS = 0d;
			this.totalSolved = 0;
			this.solvingMiners = 0;
			this.runningMiners = 0;
		} else
		{
			this.totalHPS = totalHPS;
			this.totalSolved = totalSolved;
			this.solvingMiners = solving;
			this.runningMiners = running;
		}

		this.networkHeightConsensus = farm.getNetworkConsensus().getHeight();
		this.networkDifficultyConsensus = farm.getNetworkConsensus().getDifficulty();

		this.networkBlockReward = this.networkHeightConsensus == null ? null : MochimoNetwork.miningReward(this.networkHeightConsensus.intValue());

		this.networkEstimatedHPS = this.networkDifficultyConsensus == null ? null : MochimoNetwork.totalHPS(this.networkDifficultyConsensus);
		this.estimatedTimeToReward = this.networkEstimatedHPS == null || this.totalHPS == null ? null : MochimoNetwork.timeToReward(this.totalHPS, this.networkEstimatedHPS);
		this.humanEstimatedTimeToReward = this.estimatedTimeToReward == null ? null : MochimoNetwork.humanTimeToReward(this.estimatedTimeToReward);
	}

	public Double getTotalHPS()
	{
		return totalHPS;
	}

	public Integer getSolvingMiners()
	{
		return solvingMiners;
	}

	public Integer getTotalSolved()
	{
		return totalSolved;
	}

	public List<MinerStatistics> getMiners()
	{
		return miners;
	}

	public Long getEstimatedTimeToReward()
	{
		return estimatedTimeToReward;
	}

	public String getHumanEstimatedTimeToReward()
	{
		return humanEstimatedTimeToReward;
	}

	public Double getNetworkBlockReward()
	{
		return networkBlockReward;
	}

	public Double getNetworkDifficultyConsensus()
	{
		return networkDifficultyConsensus;
	}

	public Double getNetworkEstimatedHPS()
	{
		return networkEstimatedHPS;
	}

	public Double getNetworkHeightConsensus()
	{
		return networkHeightConsensus;
	}

	public Integer getRunningMiners()
	{
		return runningMiners;
	}

}
