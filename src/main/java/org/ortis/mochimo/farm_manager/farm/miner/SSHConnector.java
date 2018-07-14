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

package org.ortis.mochimo.farm_manager.farm.miner;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * SSH capable class for remote command execution
 * 
 * @author Ortis <br>
 *         2018 Jul 01 11:57:56 PM <br>
 */
public class SSHConnector
{
	private final String id;
	private final String host;
	private final int port;
	private final String user;
	private final String password;
	private final String privateKey;
	private final Logger log;

	private final JSch jSch;

	public SSHConnector(final String id, final String host, final int port, final String user, final String password, final String privateKey, final Logger log) throws JSchException
	{
		this.id = id;
		this.host = host;
		this.port = port;
		this.user = user;
		this.password = password;
		this.privateKey = privateKey;
		this.log = log;

		this.jSch = new JSch();

	}

	public Session initSession() throws JSchException, Exception
	{// cannot resuse the same Session. It generates Packet corrupt exception
		final Session session = this.jSch.getSession(this.user, this.host, this.port);
		java.util.Properties config = new java.util.Properties();
		config.put("StrictHostKeyChecking", "no");
		session.setConfig(config);

		if (this.privateKey != null)
			this.jSch.addIdentity(this.privateKey);

		if (this.password != null)
			session.setPassword(this.password);

		this.log.finer("Connecting to " + this.id);
		session.setTimeout(30000);
		session.connect(30000);
		if (session.isConnected())
			this.log.finer("Connected to " + this.id);
		else
			throw new Exception("Connection timeout");

		return session;
	}

	public synchronized List<String> execute(final String command) throws Exception
	{
		final Session session = initSession();
		final Channel channel = session.openChannel("exec");

		final String fullCommand = "bash --login -c echo '' && " + command;
		((ChannelExec) channel).setCommand(fullCommand);
		// ((ChannelExec) channel).setPty(true); //prevent some commamd to run properly

		final ByteArrayOutputStream errBaos = new ByteArrayOutputStream();
		((ChannelExec) channel).setErrStream(errBaos);

		channel.setInputStream(null);
		InputStream in = channel.getInputStream();

		this.log.finer("Executing command " + fullCommand);
		channel.connect();

		final InputStreamReader inputReader = new InputStreamReader(in, StandardCharsets.UTF_8);
		final BufferedReader bufferedReader = new BufferedReader(inputReader);
		String line = null;

		final List<String> stdout = new ArrayList<>();
		bufferedReader.readLine();// skip echo ''
		while ((line = bufferedReader.readLine()) != null)
			stdout.add(line);

		// final int exitCode = channel.getExitStatus();
		channel.disconnect();
		session.disconnect();

		bufferedReader.close();
		inputReader.close();

		if (errBaos.size() > 0)
		{
			final String stderr = new String(errBaos.toByteArray());

			if (stderr.contains("ttyname failed"))// harmless error
				this.log.fine("Silent STDERR  -> " + stderr);
			else
				throw new Exception("Error from STDERR while running command " + fullCommand + " -> " + stderr);
		}

		/*
		if (channel.getExitStatus() != 0)
		{
			final StringBuilder sb = new StringBuilder();
			for (final String l : stdout)
				sb.append(l + "\n");
		
			throw new Exception(sb.toString());
		}
		*/

		return stdout;
	}
}
