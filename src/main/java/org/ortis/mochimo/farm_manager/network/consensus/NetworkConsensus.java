
package org.ortis.mochimo.farm_manager.network.consensus;

/**
 * Consensus of statistics of the Mochimo Network
 * 
 * @author Ortis <br>
 *         2018 Jul 08 10:27:19 PM <br>
 */
public interface NetworkConsensus
{

	void update() throws Exception;

	Double getDifficulty();

	Double getHeight();

}
