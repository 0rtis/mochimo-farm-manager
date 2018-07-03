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

package org.ortis.mochimo.farm_manager.farm;

import java.util.Locale;
import java.util.Map;

public class FieldConfig
{

	public static String ENCRYPTED_FIELD_PREFIX = "encrypted";
	public static int ENCRYPTED_FIELD_PREFIX_LENGTHG = ENCRYPTED_FIELD_PREFIX.length();

	private final String name;
	private final String value;
	private final boolean encrytped;

	public FieldConfig(final String name, final String value, final boolean encrytped)
	{

		this.name = name;
		this.value = value;
		this.encrytped = encrytped;
	}

	public String getName()
	{
		return name;
	}

	public String getValue()
	{
		return value;
	}

	public boolean isEncrytped()
	{
		return encrytped;
	}

	@Override
	public String toString()
	{
		return this.name + "->" + this.value;
	}

	public static String getEncryptedName(final String name)
	{

		return ENCRYPTED_FIELD_PREFIX + name.substring(0, 1).toUpperCase(Locale.ENGLISH) + name.substring(1);

	}

	public static String getName(final String name)
	{
		if (isEncryptedName(name))
			return name.substring(ENCRYPTED_FIELD_PREFIX_LENGTHG, ENCRYPTED_FIELD_PREFIX_LENGTHG + 1).toLowerCase(Locale.ENGLISH) + name.substring(ENCRYPTED_FIELD_PREFIX_LENGTHG + 1);

		return name;
	}

	public static boolean isEncryptedName(final String name)
	{
		return name.startsWith(ENCRYPTED_FIELD_PREFIX);

	}

	public static FieldConfig parseField(final String name, final Map<String, String> config)
	{

		if (config.containsKey(name))
			return new FieldConfig(name, config.get(name), false);

		final String encryptedName = getEncryptedName(name);

		if (config.containsKey(encryptedName))
			return new FieldConfig(name, config.get(encryptedName), true);

		return null;

	}

	public static <D extends Map<String, FieldConfig>> D parseConfig(final Map<String, String> config, final D destination)
	{

		for (final Map.Entry<String, String> entry : config.entrySet())
		{
			final String plainTextName = getName(entry.getKey());
			final FieldConfig configField = new FieldConfig(plainTextName, entry.getValue(), isEncryptedName(entry.getKey()));
			destination.put(plainTextName, configField);
		}

		return destination;
	}

}
