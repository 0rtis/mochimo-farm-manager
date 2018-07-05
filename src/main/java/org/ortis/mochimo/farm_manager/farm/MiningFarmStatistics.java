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

public class MiningFarmStatistics
{
	private final Double totalHPS;
	private final Integer totalSolved;
	private final Double solvingRate;
	private final List<MinerStatistics> miners;

	public MiningFarmStatistics(final MiningFarm farm)
	{
		this.miners = new ArrayList<>();

		double totalHPS = 0;
		int totalSolved = 0;
		int solving = 0;
		for (final Miner miner : farm.getMiners())
		{
			final MinerStatistics stat = miner.getStatistics();

			this.miners.add(stat);

			if (stat.isDefault())
				continue;// dont aggregate if default statistics

			for (final Map.Entry<String, String> entry : stat.getStatistics().entrySet())
			{
				if (entry.getKey().contains("aiku/second"))
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
			this.solvingRate = 0d;
		} else
		{
			this.totalHPS = totalHPS;
			this.totalSolved = totalSolved;
			this.solvingRate = 100d * solving / this.miners.size();
		}

	}

	public Double getTotalHPS()
	{
		return totalHPS;
	}

	public Double getSolvingRate()
	{
		return solvingRate;
	}

	public Integer getTotalSolved()
	{
		return totalSolved;
	}

	public List<MinerStatistics> getMiners()
	{
		return miners;
	}
}
