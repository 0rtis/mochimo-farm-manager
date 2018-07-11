
package org.ortis.mochimo.farm_manager.network.consensus;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * A {@link NetworkConsensus} implementation based on https://bx.mochimo.org/
 * 
 * @author Ortis <br>
 *         2018 Jul 07 2:58:20 PM <br>
 */
public class BXMochimoConsensus implements NetworkConsensus
{

	private Integer blockHeight;
	private String blockHex;
	private Integer difficulty;

	public synchronized void update() throws IOException
	{

		final Document doc = Jsoup.connect("https://bx.mochimo.org/").userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6").get();

		Element table = doc.select("table").get(0);

		Elements rows = table.select("tr");

		this.blockHeight = null;
		this.blockHex = null;

		if (rows.size() < 2)
			return;

		Element row = rows.get(1);
		Elements cols = row.select("td");
		if (cols.size() < 1)
			return;

		this.blockHeight = Integer.parseInt(cols.get(0).text().trim());
		this.blockHex = "0x" + Integer.toHexString(this.blockHeight);
		this.difficulty = Integer.parseInt(cols.get(4).text().trim());

	}

	@Override
	public synchronized Double getDifficulty()
	{
		return this.difficulty == null ? null : this.difficulty.doubleValue();
	}

	@Override
	public synchronized Double getHeight()
	{
		return this.blockHeight == null ? null : this.blockHeight.doubleValue();
	}

	public synchronized String getBlockHex()
	{
		return this.blockHex;
	}
	
	@Override
	public String toString()
	{
		return "bx.mochimo.org";
	}
}
