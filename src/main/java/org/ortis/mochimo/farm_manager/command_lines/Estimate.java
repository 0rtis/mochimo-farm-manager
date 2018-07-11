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

package org.ortis.mochimo.farm_manager.command_lines;

import java.text.DecimalFormat;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import org.ortis.mochimo.farm_manager.Version;
import org.ortis.mochimo.farm_manager.farm.MiningFarm;
import org.ortis.mochimo.farm_manager.http.HttpServer;
import org.ortis.mochimo.farm_manager.network.MochimoNetwork;
import org.ortis.mochimo.farm_manager.utils.Utils;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Start {@link MiningFarm} and {@link HttpServer}
 * 
 * @author Ortis <br>
 *         2018 Jul 03 9:27:34 PM <br>
 */
@Command(description = "Estimate mining statistics", name = "estimate", mixinStandardHelpOptions = true, version = Version.VERSION, showDefaultValues = true)
public class Estimate implements Callable<Void>
{
	private final static DecimalFormat HPS_FORMAT = new DecimalFormat("###,###.00");

	@Option(names = { "-d", "--diff" }, description = "Difficulty")
	private Double difficulty = null;

	@Option(names = { "-H", "--height" }, description = "Height")
	private Double height = null;

	@Option(names = { "-hps", "--hps" }, description = "Mining HPS")
	private Double hps = null;

	@Override
	public Void call() throws Exception
	{

		final Logger log = CommandLines.getLog();

		try
		{

			if (this.difficulty == null && this.height == null)
			{
				log.info("Difficulty and/or Height must be set");
				return null;
			}

			if (this.height != null)
			{
				log.info("Height:\t" + this.height);
				final double reward = MochimoNetwork.miningReward(this.height.intValue());
				log.info("Block Reward:\t" + reward + " MCM");
			}

			if (this.difficulty != null)
			{
				log.info("Difficulty:\t" + this.difficulty);

				final double networkHPS = MochimoNetwork.totalHPS(this.difficulty);
				log.info("Estimated Network HPS:\t" + HPS_FORMAT.format(networkHPS));

				if (this.hps != null)
				{
					log.info("Mining HPS:\t" + HPS_FORMAT.format(this.hps));
					final double contribution = 100 * this.hps / networkHPS;
					log.info("Contribution:\t" + contribution + " %");
					final long etr = MochimoNetwork.timeToReward(this.hps, networkHPS);
					final String humanETR = MochimoNetwork.humanTimeToReward(etr);
					log.info("Estimated Time to Reward:\t" + humanETR);
				}
			}

		} catch (final Exception e)
		{
			log.severe(Utils.formatException(e));
		}

		return null;
	}
}
