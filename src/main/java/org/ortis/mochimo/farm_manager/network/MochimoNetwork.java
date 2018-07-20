
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
		final double expectedSolvePerDay = miningHPS / networkHPS * 256;

		final double daysBeforeSolve = 1 / expectedSolvePerDay;

		return (long) (daysBeforeSolve * 24 * 60 * 60 * 1000);

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

	public static double miningReward(int height)
	{
		//from C source code
		if (height == 0)
			return 4757066;

		if (height == 1)
			return 5;

		if (height > 2097152)
			return 0;

		height -= 1;
		if (height < 1048576)
			return height * .000056 + 5;
		else
			return (2097152 - height) * .000056 + 5;

	}

}
