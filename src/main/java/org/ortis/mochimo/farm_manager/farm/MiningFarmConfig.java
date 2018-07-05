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
package org.ortis.mochimo.farm_manager.farm;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

import org.ortis.mochimo.farm_manager.beans.MiningFarmConfigBean;
import org.ortis.mochimo.farm_manager.farm.miner.MinerConfig;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

public class MiningFarmConfig
{

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

	private static final Type MAP_STRING_STRING_TYPE = new TypeToken<Map<String, String>>()
	{
	}.getType();

	public static final Type BYTE_ARRAY_TYPE = new TypeToken<byte []>()
	{
	}.getType();

	private transient final SecretKey key;
	private final byte [] salt;
	private final List<MinerConfig> miners;

	private List<MinerConfig> roMiners;

	public MiningFarmConfig(final List<MinerConfig> miners, final byte [] salt, final SecretKey key)
	{
		this.key = key;
		this.salt = salt;
		this.miners = new ArrayList<>(miners);
		this.roMiners = Collections.unmodifiableList(this.miners);
	}

	public MinerConfig getMinerConfig(final String id)
	{
		for (final MinerConfig mc : this.roMiners)
			if (mc.getId().equals(id))
				return mc;

		return null;
	}

	public List<MinerConfig> getMinerConfigs()
	{
		return this.roMiners;
	}

	public byte [] getSalt()
	{
		return this.salt;
	}

	public SecretKey getKey()
	{
		return this.key;
	}

	public static MiningFarmConfig parse(final String config, final SecretKey key) throws IOException
	{
		final JsonParser parser = new JsonParser();
		JsonObject jo = parser.parse(config).getAsJsonObject();
		final byte [] salt = jo.has("salt") ? (byte []) GSON.fromJson(jo.get("salt").toString(), BYTE_ARRAY_TYPE) : null;

		JsonArray array = jo.get("miners").getAsJsonArray();

		final List<MinerConfig> minerConfigs = new ArrayList<>();
		for (int i = 0; i < array.size(); i++)
		{
			jo = array.get(i).getAsJsonObject();
			final Map<String, String> map = new LinkedHashMap<>();
			map.putAll(GSON.fromJson(jo.toString(), MAP_STRING_STRING_TYPE));
			final MinerConfig minerConfig = new MinerConfig(map);
			minerConfigs.add(minerConfig);
		}

		final MiningFarmConfig miningFarmConfig = new MiningFarmConfig(minerConfigs, salt, key);
		return miningFarmConfig;
	}

	public static String serialize(final MiningFarmConfig config)
	{
		final MiningFarmConfigBean bean = new MiningFarmConfigBean(config);
		final String json = GSON.toJson(bean);
		return json;
	}

	@Override
	public String toString()
	{
		return (this.salt == null ? "" : "(salted) ") + this.miners.toString();
	}

}
