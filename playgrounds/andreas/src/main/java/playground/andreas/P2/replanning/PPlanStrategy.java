package playground.andreas.P2.replanning;

import playground.andreas.P2.pbox.Cooperative;
import playground.andreas.P2.plan.PPlan;

public interface PPlanStrategy {

	public PPlan run(Cooperative cooperative);
	
	public String getName();
}
