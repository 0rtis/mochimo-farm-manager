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

package org.ortis.mochimo.farm_manager.beans;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.ortis.mochimo.farm_manager.farm.miner.MinerStatistics;

/**
 * Bean of {@link MinerStatistics}
 * 
 * @author Ortis <br>
 *         2018 Jul 02 11:04:20 PM <br>
 */
public class MinerStatisticsBean
{

	public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	public final String minerId;
	public final String datetime;
	public final Float cpu;
	public final List<String> processes;
	public final Boolean gomochi;
	public final Boolean listen;
	public final Boolean solving;
	public final Double hps;
	public final String block;
	public final Integer solved;
	public final Integer difficulty;

	public MinerStatisticsBean(final MinerStatistics statistics)
	{
		this.minerId = statistics.getMinerId();
		this.datetime = DATE_TIME_FORMATTER.format(statistics.getTime());
		this.cpu = statistics.getCpu();
		this.processes = new ArrayList<>(statistics.getProcesses());

		this.gomochi = statistics.isGomochi();
		this.listen = statistics.isListen();
		this.solving = statistics.isSolving();

		Double hps = null;
		String block = null;
		Integer solved = null;
		Integer difficulty = null;

		for (final Map.Entry<String, String> entry : statistics.getStatistics().entrySet())
		{
			final String keyUpper = entry.getKey().toUpperCase(Locale.ENGLISH);
			if (keyUpper.contains("HAIKU/SECOND"))
				hps = Double.parseDouble(entry.getValue());
			else if (keyUpper.contains("SOLVED"))
				solved = Integer.parseInt(entry.getValue());
			else if (keyUpper.contains("DIFFICULTY"))
				difficulty = Integer.parseInt(entry.getValue());
			else if (keyUpper.contains("BLOCK"))
				block = entry.getValue();

		}

		if (statistics.getStatistics().isEmpty())
		{
			this.hps = null;
			this.block = null;
			this.solved = null;
			this.difficulty = null;
		} else
		{
			this.hps = hps;
			this.block = block;
			this.solved = solved;
			this.difficulty = difficulty;
		}
	}

}
