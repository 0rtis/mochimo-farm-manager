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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Factory class for {@link Logger}
 * @author Ortis
 *<br>
 *2018 Jul 03 9:29:16 PM 
 *<br>
 */
public abstract class LogFactory
{
	public final static DateTimeFormatter LOG_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
	private final static Map<String, Logger> CACHE = new HashMap<>();

	private final static ListneableHandler HANDLER = new ListneableHandler();

	public static Logger getLogger(final String name)
	{

		Logger log;
		synchronized (CACHE)
		{
			log = CACHE.get(name);
			if (log != null)
				return log;

			log = Logger.getLogger(name);
			CACHE.put(name, log);
			log.setUseParentHandlers(false);

			log.setLevel(Level.ALL);

			log.addHandler(HANDLER);
			return log;
		}

	}

	public static void setLevel(final Level level)
	{
		HANDLER.setLevel(level);
	}

	public static boolean addLogListener(final LogListener listener)
	{
		return HANDLER.addListener(listener);

	}

	public static boolean removeLogListener(final LogListener listener)
	{

		return HANDLER.removeListener(listener);

	}

	public static String format(final LocalDateTime now, final LogRecord record)
	{
		final String log = "[" + record.getLevel().getName() + "] " + LOG_FORMATTER.format(now) + " - " + record.getLoggerName() + "|" + Thread.currentThread().getName() + "|"
				+ record.getSourceClassName() + "." + record.getSourceMethodName() + ":  " + record.getMessage();
		return log;
	}

}
