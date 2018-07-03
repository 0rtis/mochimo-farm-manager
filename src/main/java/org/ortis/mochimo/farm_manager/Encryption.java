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

package org.ortis.mochimo.farm_manager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.ortis.mochimo.farm_manager.farm.FieldConfig;
import org.ortis.mochimo.farm_manager.farm.MiningFarmConfig;
import org.ortis.mochimo.farm_manager.farm.miner.MinerConfig;

/**
 * Utility class for AES enctryption
 * 
 * @author Ortis <br>
 *         2018 Jul 03 12:58:44 PM <br>
 */
public abstract class Encryption
{

	public static final int PBKDF2_ITERATION = 100000;
	public static final String KEY_ALGO = "AES";
	public static final String ENCRYPTION = "AES/CBC/PKCS5Padding";
	public static final int IV_LENGTH = 16;

	public static SecureRandom getSecureRandom()
	{
		return new SecureRandom();
	}

	public static byte [] salt()
	{
		final SecureRandom random = getSecureRandom();
		final byte [] salt = new byte[16];
		random.nextBytes(salt);
		return salt;
	}

	public static byte [] hashPassword(final char [] password, final byte [] salt) throws NoSuchAlgorithmException, InvalidKeySpecException
	{

		final PBEKeySpec spec = new PBEKeySpec(password, salt, PBKDF2_ITERATION, 128);
		SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
		final byte [] key = skf.generateSecret(spec).getEncoded();

		return key;
	}

	public static SecretKey parseEncryptionKey(final char [] password, final byte [] salt) throws NoSuchAlgorithmException, InvalidKeySpecException
	{

		final byte [] key = hashPassword(password, salt);
		final SecretKeySpec keySpec = new SecretKeySpec(key, KEY_ALGO);
		return keySpec;
	}

	public static Cipher getCipher() throws NoSuchAlgorithmException, NoSuchPaddingException
	{
		return javax.crypto.Cipher.getInstance(ENCRYPTION);
	}

	public static MiningFarmConfig encrypt(final MiningFarmConfig miningFarmConfig, final byte [] salt, final SecretKey key)
			throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException
	{
		final List<MinerConfig> miners = new ArrayList<>();
		for (final MinerConfig minerConfig : miningFarmConfig.getMinerConfigs())
		{
			final MinerConfig encrypted = encrypt(minerConfig, key);
			miners.add(encrypted);
		}

		final MiningFarmConfig encrypted = new MiningFarmConfig(miners, salt, key);
		return encrypted;
	}

	public static MinerConfig encrypt(final MinerConfig minerConfig, final SecretKey key)
			throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, IOException
	{

		final Cipher cipher = getCipher();
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Encoder encoder = Base64.getEncoder();
		final Map<String, String> encrypted = new LinkedHashMap<>();
		for (final FieldConfig field : minerConfig.getFields().values())
		{
			if (MinerConfig.isIdField(field.getName()) || field.isEncrytped() || field.getValue() == null)
				encrypted.put(field.getName(), field.getValue());
			else
			{
				baos.reset();
				cipher.init(Cipher.ENCRYPT_MODE, key, getSecureRandom());
				baos.write(cipher.getIV());
				baos.write(cipher.doFinal(field.getValue().getBytes()));
				final String encryptedValue = encoder.encodeToString(baos.toByteArray());
				final String encryptedName = FieldConfig.getEncryptedName(field.getName());
				encrypted.put(encryptedName, encryptedValue);
			}
		}

		final MinerConfig encryptedConfig = new MinerConfig(encrypted);
		return encryptedConfig;

	}

	public static MiningFarmConfig decrypt(final MiningFarmConfig miningFarmConfig, final SecretKey key)
			throws InvalidKeyException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException
	{
		if (miningFarmConfig.getSalt() == null)
			throw new IllegalArgumentException("Mining farm config is missing salt");

		final List<MinerConfig> decryptedMiners = new ArrayList<>();
		for (final MinerConfig minerConfig : miningFarmConfig.getMinerConfigs())
			decryptedMiners.add(decrypt(minerConfig, key));

		final MiningFarmConfig decrypted = new MiningFarmConfig(decryptedMiners, null, key);
		return decrypted;

	}

	public static MinerConfig decrypt(final MinerConfig minerConfig, final SecretKey key)
			throws InvalidKeyException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException
	{

		final Map<String, String> decrypted = new LinkedHashMap<>();
		for (final FieldConfig field : minerConfig.getFields().values())
		{
			if (!field.isEncrytped() || field.getValue() == null)
				decrypted.put(field.getName(), field.getValue());
			else
				decrypted.put(field.getName(), decrypt(field.getValue(), key));
		}

		return new MinerConfig(decrypted);

	}

	/*
		public static ConfigField decrypt(final ConfigField field, final SecretKey key)
				throws InvalidKeyException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException
		{
			final byte [] buffer = Base64.getDecoder().decode(field.getValue());
			final Cipher cipher = getCipher();
	
			final IvParameterSpec iv = new IvParameterSpec(Arrays.copyOf(buffer, IV_LENGTH));
			cipher.init(Cipher.DECRYPT_MODE, key, iv);
	
			final String decrytpedValue = new String(cipher.doFinal(buffer, IV_LENGTH, buffer.length), StandardCharsets.UTF_8);
			final ConfigField decrypted = new ConfigField(field.getName(), decrytpedValue, false);
			return decrypted;
	
		}
		*/
	public static String decrypt(final String encryptedValue, final SecretKey key)
			throws InvalidKeyException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException
	{
		final byte [] buffer = Base64.getDecoder().decode(encryptedValue);
		final Cipher cipher = getCipher();

		final IvParameterSpec iv = new IvParameterSpec(Arrays.copyOfRange(buffer, 0, IV_LENGTH));
		cipher.init(Cipher.DECRYPT_MODE, key, iv);

		final byte [] data = Arrays.copyOfRange(buffer, IV_LENGTH, buffer.length);
		final byte [] decrytped = cipher.doFinal(data);
		return new String(decrytped, StandardCharsets.UTF_8);

	}
}
