/**
 * 
 */
package playground.yu.scoring;

import org.matsim.core.events.handler.EventHandler;

/**
 * Interface for all of my Events2Scores
 * 
 * @author yu
 * 
 */
public interface Events2ScoreI extends EventHandler {

	void finish();

	@Override
	void reset(int iteration);

}
