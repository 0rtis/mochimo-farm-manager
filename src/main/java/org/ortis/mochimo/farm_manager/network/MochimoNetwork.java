
package org.ortis.mochimo.farm_manager.network;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for the Mochimo Network
 * 
 * @author Ortis <br>
 *         2018 Jul 08 10:25:08 PM <br>
 */
public class MochimoNetwork
{

	public final static Duration TARGET_BLOCK_TIME = Duration.ofMillis(337500);

	/**
	 * Compute the estimated HPS of the global Mochimo Network
	 * 
	 * @param difficulty:
	 *            the difficulty of the Mochimo Network
	 * @return
	 */
	public static double totalHPS(final double difficulty)
	{
		return totalHPS(difficulty, TARGET_BLOCK_TIME.toMillis());
	}

	/**
	 * Compute the estimated HPS of the global Mochimo Network
	 * 
	 * @param difficulty:
	 *            the difficulty of the Mochimo Network
	 * @param blockTime:
	 *            time between blocks in milli seconds
	 * @return
	 */
	public static double totalHPS(final double difficulty, final long blockTime)
	{
		return Math.pow(2, difficulty) / (blockTime / 1000);
	}

	/**
	 * Compute the estimated mining time before reward in milli seconds
	 * 
	 * @param miningHPS:
	 *            the HPS value of the miner
	 * @param networkHPS:
	 *            the HPS value of the Mochimo Network
	 * @return time to reward in milli seconds
	 */
	public static long timeToReward(final double miningHPS, final double networkHPS)
	{
		final double expectedRewardPerDay = miningHPS / networkHPS * 256;

		final double daysBeforeReward = 1 / expectedRewardPerDay;

		return (long) (daysBeforeReward * 24 * 60 * 60 * 1000);

	}

	/**
	 * Convert the Estimated Time to Reward to human readable {@link String}
	 * 
	 * @param timeToReward:
	 *            the Estimated Time to Reward in milli seconds
	 * @return time to reward in milli seconds
	 */
	public static String humanTimeToReward(final long timeToReward)
	{
		final long seconds = timeToReward / 1000;
		long days = TimeUnit.SECONDS.toDays(seconds);
		long hours = TimeUnit.SECONDS.toHours(seconds) - (days * 24);
		long minutes = TimeUnit.SECONDS.toMinutes(seconds) - (TimeUnit.SECONDS.toHours(seconds) * 60);
		long secondss = TimeUnit.SECONDS.toSeconds(seconds) - (TimeUnit.SECONDS.toMinutes(seconds) * 60);

		return days + " days, " + hours + " Hrs, " + minutes + " Minutes, " + secondss + " Seconds";

	}

	public static double miningReward(final int height)
	{
		double reward = 0;
		if (height <= 373760)
		{
			reward = 5;
			for (int i = 0; i < height; i++)
				reward += .000199637;
		} else if (height <= 1586265)
		{
			reward = 79.616325120;
			for (int i = 373760; i < height; i++)
				reward -= .000061539;
		}

		return reward;
	}

}
