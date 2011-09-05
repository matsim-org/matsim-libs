package playground.andreas.P2.replanning;

import org.matsim.api.core.v01.Id;

import playground.andreas.P2.pbox.Cooperative;
import playground.andreas.P2.plan.PPlan;
import playground.andreas.P2.plan.PRouteProvider;

public interface PPlanStrategy {

	@Deprecated
	public PPlan modifyPlan(PPlan plan, Id pLineId, PRouteProvider pRouteProvider, int iteration);
	
	public PPlan modifyBestPlan(Cooperative cooperative);
	
	public String getName();
}
