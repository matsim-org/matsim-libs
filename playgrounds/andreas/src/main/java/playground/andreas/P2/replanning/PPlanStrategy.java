package playground.andreas.P2.replanning;

import playground.andreas.P2.pbox.Cooperative;
import playground.andreas.P2.plan.PPlan;

public interface PPlanStrategy {

	/**
	 * 
	 * @param cooperative
	 * @return the plan found or null if there is no better plan 
	 */
	public PPlan run(Cooperative cooperative);
	
	public String getName();
}
