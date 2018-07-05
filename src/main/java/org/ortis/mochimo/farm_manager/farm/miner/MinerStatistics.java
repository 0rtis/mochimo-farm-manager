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

package org.ortis.mochimo.farm_manager.farm.miner;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Mining statistics of a {@link Miner}
 * 
 * @author Ortis <br>
 *         2018 Jul 02 11:03:52 PM <br>
 */
public class MinerStatistics
{
	private final LocalDateTime time;
	private final String minerId;
	private Float cpu = null;
	private List<String> processes;
	private Boolean gomochi;
	private Boolean listen;
	private Boolean solving;

	private Map<String, String> statistics;

	public MinerStatistics(final String minerId, final LocalDateTime time)
	{
		this.minerId = minerId;
		this.time = time;
		this.processes = new ArrayList<>();
		this.statistics = new HashMap<>();
	}

	public String getMinerId()
	{
		return minerId;
	}

	public LocalDateTime getTime()
	{
		return time;
	}

	public Map<String, String> getStatistics()
	{
		return statistics;
	}

	public void setStatistics(final String key, final String value)
	{
		this.statistics.put(key, value);
	}

	public Float getCpu()
	{
		return cpu;
	}

	public void setCpu(final float cpu)
	{
		this.cpu = cpu;
	}

	public List<String> getProcesses()
	{
		return processes;
	}

	public void addProcess(final String process)
	{
		this.processes.add(process);
	}

	public Boolean isGomochi()
	{
		return gomochi;
	}

	public void setGomochi(final boolean gomochi)
	{
		this.gomochi = gomochi;
	}

	public Boolean isListen()
	{
		return listen;
	}

	public void setListen(final boolean listen)
	{
		this.listen = listen;
	}

	public Boolean isSolving()
	{
		return solving;
	}

	public void setSolving(final boolean solving)
	{
		this.solving = solving;
	}

	public boolean isDefault()
	{
		return this.cpu == null;
	}

	@Override
	public String toString()
	{
		return "time=" + this.time + ", cpu=" + this.cpu + "%, processes=" + this.processes;
	}

	public static MinerStatistics getEmptyStatistics(final String minerId, final Supplier<LocalDateTime> clock)
	{
		return new MinerStatistics(minerId, clock.get());
	}

}
