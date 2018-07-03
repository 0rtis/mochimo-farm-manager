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
package org.ortis.mochimo.farm_manager.utils;

/**
 * Utilisty class
 * 
 * @author Ortis <br>
 *         2018 Jul 02 8:25:28 AM <br>
 */
public class Utils
{

	/**
	 * Format the exception message
	 * 
	 * @param t
	 * @return
	 */
	public static String formatException(final Throwable t)
	{
		if (t == null)
			return null;

		final Throwable cause = t.getCause();
		final String msg = cause == null ? null : formatException(cause);
		return formatException(t.getClass(), msg, t.toString(), t.getStackTrace());

	}

	private static String formatException(final Class<?> exceptionClass, final String cause, final String msg, final StackTraceElement [] exceptionStack)
	{
		final StringBuilder builder = new StringBuilder();

		if (msg != null)
			builder.append(msg);

		if (exceptionStack != null)
		{
			builder.append(System.lineSeparator());
			for (int i = 0; i < exceptionStack.length; i++)
			{
				final String stackElement = exceptionStack[i].toString();

				builder.append(stackElement + System.lineSeparator());
			}
		}

		if (cause != null)
			builder.append("Caused by " + cause);

		return builder.toString();
	}

}
