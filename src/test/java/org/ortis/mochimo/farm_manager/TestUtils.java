
package org.ortis.mochimo.farm_manager;

import java.io.File;
import java.util.Random;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TestUtils
{

	private static final char [] CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123446789".toCharArray();

	private static Logger log;

	private final static Random RANDOM = new Random();

	static
	{
		log = Logger.getLogger("UnitTest");
		log.setUseParentHandlers(false);
		log.setLevel(Level.ALL);
		ConsoleHandler handler = new ConsoleHandler();
		handler.setLevel(Level.ALL);
		log.addHandler(handler);

	}

	public static Logger getLog()
	{
		return log;
	}

	public static String randomString(final Random random, final int length)
	{
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length; i++)
			sb.append(CHARS[random.nextInt(CHARS.length)]);

		return sb.toString();

	}

	public static Random getRandom()
	{
		return RANDOM;
	}
	
	public static void delete(final File file) throws Exception
	{
		if (file == null)
			return;

		if (file.isDirectory())
			for (final File f : file.listFiles())
				delete(f);

		if (file.isDirectory())
			log.fine("Deleting folder " + file);
		else
			log.fine("Deleting file " + file);

		if (!file.delete())
			throw new Exception("Could not delete file " + file.getAbsolutePath());

	}

}
