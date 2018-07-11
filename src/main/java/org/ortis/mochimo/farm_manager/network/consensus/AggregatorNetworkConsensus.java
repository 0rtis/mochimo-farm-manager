
package org.ortis.mochimo.farm_manager.network.consensus;

import java.util.List;

/**
 * An implementation of {@link NetworkConsensus} that aggregates multiple {@link NetworkConsensus}
 * 
 * @author Ortis <br>
 *         2018 Jul 08 10:30:01 PM <br>
 */
public class AggregatorNetworkConsensus implements NetworkConsensus
{

	private final List<NetworkConsensus> consensuses;

	private Double height;
	private Double difficulty;

	private final String toString;

	public AggregatorNetworkConsensus(final List<NetworkConsensus> consensuses)
	{
		this.consensuses = consensuses;

		final StringBuilder sb = new StringBuilder();
		this.consensuses.forEach(c -> sb.append("[").append(c.toString()).append("], "));

		if (sb.length() > 2)
			sb.delete(sb.length() - 2, sb.length());

		this.toString = sb.toString();
	}

	@Override
	public synchronized void update() throws Exception
	{

		Double height = null;
		int hi = 0;
		Double difficulty = null;
		int di = 0;

		for (final NetworkConsensus consensus : this.consensuses)
		{
			consensus.update();

			if (consensus.getHeight() != null)
			{
				if (height == null)
					height = consensus.getHeight();
				else
					height += consensus.getHeight();
				hi++;
			}

			if (consensus.getDifficulty() != null)
			{
				if (difficulty == null)
					difficulty = consensus.getDifficulty();
				else
					difficulty += consensus.getDifficulty();
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
