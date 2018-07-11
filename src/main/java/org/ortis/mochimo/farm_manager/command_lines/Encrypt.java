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

import java.io.Console;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import javax.crypto.SecretKey;

import org.ortis.mochimo.farm_manager.Encryption;
import org.ortis.mochimo.farm_manager.Version;
import org.ortis.mochimo.farm_manager.farm.MiningFarmConfig;
import org.ortis.mochimo.farm_manager.utils.Utils;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Encrypt a configuration file
 * 
 * @author Ortis <br>
 *         2018 Jul 03 9:27:34 PM <br>
 */
@Command(description = "Encrypt configuration file", name = "encrypt", mixinStandardHelpOptions = true, version = Version.VERSION, showDefaultValues = true)
public class Encrypt implements Callable<Void>
{

	@Option(names = { "-pw", "-pwd", "--password" }, description = "Encryption password")
	private String password;

	@Parameters(index = "0", paramLabel = "configuration_file", description = "System path to mining farm configuration file")
	private String farmConfigFile;

	@Parameters(index = "1", paramLabel = "encrytped_configuration_file", description = "System path to encrypted mining farm configuration file")
	private String encryptedFarmConfigFile;

	@Override
	public Void call() throws Exception
	{

		final Logger log = CommandLines.getLog();

		try
		{
			final Path configPath = Paths.get(this.farmConfigFile);
			if (!Files.exists(configPath))
			{
				log.severe("Config file '" + configPath + "' not found");
				return null;
			}

			log.info("Parsing config file '" + configPath + "'");

			final String json = new String(Files.readAllBytes(configPath));

			MiningFarmConfig miningFarmConfig = MiningFarmConfig.parse(json, null);

			final char [] cpassword;
			if (this.password == null)
			{
				final Console console = System.console();
				if (console == null)
				{
					log.severe("Couldn't get Console instance. Use -pw to specified password");
					return null;
				}
				cpassword = console.readPassword("Encryption password:");
			} else
				cpassword = this.password.toCharArray();

			final byte [] salt = Encryption.salt();
			final SecretKey key = Encryption.parseEncryptionKey(cpassword, salt);

			log.info("Encrytping");
			final MiningFarmConfig encrypted = Encryption.encrypt(miningFarmConfig, salt, key);
			final String serial = MiningFarmConfig.serialize(encrypted);
			log.info("Writing output");
			final Path outputPath = Paths.get(this.encryptedFarmConfigFile);
			Files.write(outputPath, serial.getBytes(StandardCharsets.UTF_8));
			log.info("Done");
			log.info("####### DO NOT STORE SENSITIVE DATA IN PLAIN TEXT ! DELETE " + this.farmConfigFile + " ASAP #######");

		} catch (final Exception e)
		{
			log.severe(Utils.formatException(e));
		}

		return null;
	}

}
