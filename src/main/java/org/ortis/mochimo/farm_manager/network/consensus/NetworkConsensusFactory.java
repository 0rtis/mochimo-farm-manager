
package org.ortis.mochimo.farm_manager.network.consensus;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.ortis.mochimo.farm_manager.farm.MiningFarm;

/**
 * Factory of {@link NetworkConsensus}
 * 
 * @author Ortis <br>
 *         2018 Jul 08 10:28:39 PM <br>
 */
public abstract class NetworkConsensusFactory
{

	public static NetworkConsensus get(List<String> consensuses, final MiningFarm farm)
	{

		final List<NetworkConsensus> networkConsensuses = new ArrayList<>();

		for (final String consensus : consensuses)
			networkConsensuses.add(build(consensus, farm));

		return new AggregatorNetworkConsensus(networkConsensuses);

	}

	private static NetworkConsensus build(String consensus, final MiningFarm farm)
	{

		final String upperConsensus = consensus.trim().toUpperCase(Locale.ENGLISH);

		if (upperConsensus.equals("BX.MOCHIMO.ORG"))
			return new BXMochimoConsensus();
		else if (upperConsensus.equals("FARM"))
			return new MiningFarmNetworkConsensus(farm);
		else if (upperConsensus.startsWith("MINER"))
		{

			final String [] buffer = consensus.split(" +");

			final List<String> minerIds = new ArrayList<>();
			for (int i = 1; i < buffer.length; i++)
				minerIds.add(buffer[i]);
			return new MiningFarmNetworkConsensus(farm, minerIds);
		}

		throw new IllegalArgumentException("Unhandled consensus '" + consensus + "'");

	}

	public static NetworkConsensus getDefaultNetworkConsensus()
	{

		return new BXMochimoConsensus();
	}

}
