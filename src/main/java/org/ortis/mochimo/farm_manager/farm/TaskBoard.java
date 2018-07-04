
package org.ortis.mochimo.farm_manager.farm;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.ortis.mochimo.farm_manager.farm.miner.Miner;
import org.ortis.mochimo.farm_manager.utils.Utils;

/**
 * Keep track of pending {@link StatisticsUpdateTask}
 * 
 * @author Ortis <br>
 *         2018 Jul 04 7:34:27 PM <br>
 */
public class TaskBoard
{
	private final Duration sleepTime;
	private final Logger log;

	public TaskBoard(final Duration sleepTime, final Logger log)
	{
		this.sleepTime = sleepTime;
		if (this.sleepTime.isNegative() || this.sleepTime.isZero())
			throw new IllegalArgumentException("Sleep time must be greater than 0");

		this.log = log;
	}

	private final List<StatisticsUpdateTask> tasks = new LinkedList<>();

	public boolean contains(final Miner miner)
	{
		synchronized (this.tasks)
		{
			for (final StatisticsUpdateTask stu : this.tasks)
				if (miner.equals(stu.getMiner()))
					return true;
		}
		return false;
	}

	public void add(final Miner miner)
	{
		synchronized (this.tasks)
		{
			this.tasks.add(new StatisticsUpdateTask(miner));
		}

	}

	public void execute() throws InterruptedException, Exception
	{
		StatisticsUpdateTask task = null;
		synchronized (this.tasks)
		{

			for (final StatisticsUpdateTask sut : this.tasks)
				if (sut.acquire(Thread.currentThread()))
				{
					task = sut;
					break;
				}
		}

		if (task == null)
		{
			this.log.finer("No task, sleeping " + this.sleepTime);
			Thread.sleep(this.sleepTime.toMillis());
		} else
		{
			this.log.finer("Executing statistics update task " + task);
			try
			{
				task.call();
			} catch (Exception e)
			{
				this.log.severe("Error while executing statistics update task " + task + " - " + Utils.formatException(e));

			}

			remove(task);

		}

	}

	private boolean remove(final StatisticsUpdateTask task)
	{
		synchronized (this.tasks)
		{
			return this.tasks.remove(task);
		}
	}

	public int pendingSize()
	{
		synchronized (this.tasks)
		{
			return this.tasks.size();
		}
	}

}
