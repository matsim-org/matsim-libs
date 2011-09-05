package playground.andreas.P2.replanning;

import org.matsim.api.core.v01.Id;

import playground.andreas.P2.plan.PPlan;
import playground.andreas.P2.plan.PRouteProvider;

public interface PPlanStrategy {

	public PPlan modifyPlan(PPlan plan, Id id, PRouteProvider pRouteProvider);
	
	public String getName();
}
