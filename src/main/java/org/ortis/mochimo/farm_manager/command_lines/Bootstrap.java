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
package org.ortis.mochimo.farm_manager.command_lines;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import org.ortis.mochimo.farm_manager.Version;

import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * Entry point of Mochimo Farm Manager application. All command are subcommand of this one
 * 
 * @author Ortis <br>
 *         2018 Jul 03 9:27:20 PM <br>
 */
@Command(description = "Bootstrap", mixinStandardHelpOptions = true, version = Version.VERSION, subcommands = { Start.class, Encrypt.class, Decrypt.class, Estimate.class })
public class Bootstrap implements Callable<Void>
{

	@Override
	public Void call() throws Exception
	{
		displayMessage(CommandLines.getLog());
		return null;
	}

	public static void displayMessage(final Logger log) throws IOException
	{
		final InputStream is = Bootstrap.class.getResourceAsStream("/ascii-art/mochimo-farm-manager.txt");
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int b;
		while ((b = is.read()) > -1)
			baos.write(b);
		final String art = new String(baos.toByteArray(), Charset.forName("UTF-8"));
		final StringBuilder sb = new StringBuilder();
		sb.append(art);
		sb.append("\n\n\t");
		sb.append(Version.VERSION);
		sb.append("\n\n");
		log.info(sb.toString());
	}

	/**
	 * Entry point of JSafe application
	 * 
	 * @param args
	 */
	public static void main(String [] args)
	{

		CommandLine.call(new Bootstrap(), System.err, args);

	}

}
