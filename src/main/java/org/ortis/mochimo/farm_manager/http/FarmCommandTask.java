
package org.ortis.mochimo.farm_manager.http;

import java.util.logging.Logger;

import org.ortis.mochimo.farm_manager.farm.MiningFarm;
import org.ortis.mochimo.farm_manager.farm.miner.Miner;
import org.ortis.mochimo.farm_manager.utils.Utils;

public class FarmCommandTask implements Runnable
{

	public enum Command
	{
		Start, Stop, Restart
	}

	private final Command command;
	private final MiningFarm farm;
	private final Logger log;

	private Thread thread;

	public FarmCommandTask(final MiningFarm farm, final Command command, final Logger log)
	{
		this.farm = farm;
		this.command = command;
		this.log = log;
	}

	@Override
	public void run()
	{
		for (final Miner miner : this.farm.getMiners())
		{
			try
			{
				switch (this.command)
				{
					case Start:

						this.log.fine("Executing "+this.command+" on " + miner);
						miner.start();
						break;

					case Stop:

						this.log.fine("Executing "+this.command+" on " + miner);
						miner.stop();
						break;

					case Restart:

						this.log.fine("Executing "+this.command+" on " + miner);
						miner.restart();
						break;

					default:
						throw new Exception("Unhandled command " + this.command);

				}
			} catch (final Exception e)
			{
				this.log.severe("Error while processing miner " + miner + " - " + Utils.formatException(e));
			}

		}
		this.log.info("Command " + this.command + " completed");
	}

	public void start()
	{
		if (this.thread != null)
			throw new IllegalStateException("Thread already exists");

		this.thread = new Thread(this);
		thread.setName(this.getClass().getSimpleName());
		thread.start();
	}

	
	public Command getCommand()
	{
		return command;
	}
}
