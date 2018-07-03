/*******************************************************************************
 * Copyright (C) 2018 Ortis (cao.ortis.org@gmail.com)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package org.ortis.mochimo.farm_manager.log;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class ListneableHandler extends Handler
{

	private final List<LogListener> listeners = new ArrayList<>();

	public ListneableHandler()
	{

	}

	@Override
	public void publish(final LogRecord record)
	{
		if(record.getLevel().intValue() >= getLevel().intValue())
		
		synchronized (this.listeners)
		{
			for (final LogListener listner : listeners)
				listner.onLog(record);
		}
	}

	@Override
	public void flush()
	{

	}

	@Override
	public void close() throws SecurityException
	{

	}

	public boolean addListener(final LogListener listener)
	{
		synchronized (this.listeners)
		{
			if (this.listeners.contains(listener))
				return false;
			return this.listeners.add(listener);
		}
	}

	public boolean removeListener(final LogListener listener)
	{
		synchronized (this.listeners)
		{
			return this.listeners.remove(listener);

		}
	}
}
