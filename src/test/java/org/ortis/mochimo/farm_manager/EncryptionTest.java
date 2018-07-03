
package org.ortis.mochimo.farm_manager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ortis.mochimo.farm_manager.Encryption;
import org.ortis.mochimo.farm_manager.farm.FieldConfig;
import org.ortis.mochimo.farm_manager.farm.MiningFarmConfig;
import org.ortis.mochimo.farm_manager.farm.miner.MinerConfig;

public class EncryptionTest
{

	private static Random random;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
		random = TestUtils.getRandom();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception
	{

	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception
	{

	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception
	{

	}

	@Test
	public void test() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException,
			InvalidAlgorithmParameterException
	{

		final InputStream is = EncryptionTest.class.getResourceAsStream("/mining_farm_config.json");
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int b;
		while ((b = is.read()) > -1)
			baos.write(b);

		final String jsonConfig = new String(baos.toByteArray(), StandardCharsets.UTF_8);
		final MiningFarmConfig config = MiningFarmConfig.parse(jsonConfig, null);
		Assert.assertNull(config.getSalt());
		final String password = TestUtils.randomString(random, 8);
		final byte [] salt = Encryption.salt();
		final SecretKey key = Encryption.parseEncryptionKey(password.toCharArray(), salt);
		final MiningFarmConfig encryptedConfig = Encryption.encrypt(config, salt, key);
		Assert.assertNotNull(encryptedConfig.getSalt());

		final MiningFarmConfig decryptedConfig = Encryption.decrypt(encryptedConfig, key);
		Assert.assertNull(decryptedConfig.getSalt());

		for (final MinerConfig original : config.getMinerConfigs())
		{

			final MinerConfig decryptedMinerConfig = decryptedConfig.getMinerConfig(original.getId());
			Assert.assertNotNull(decryptedMinerConfig);
			for (final FieldConfig field : original.getFields().values())
			{
				final FieldConfig decryptedField = decryptedMinerConfig.get(field.getName());
				Assert.assertNotNull(decryptedField);
				Assert.assertEquals(field.getName(), decryptedField.getName());
				Assert.assertEquals(field.getValue(), decryptedField.getValue());
				Assert.assertEquals(field.isEncrytped(), decryptedField.isEncrytped());
			}

		}

	}

}
