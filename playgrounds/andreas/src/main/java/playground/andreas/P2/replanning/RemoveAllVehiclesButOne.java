package playground.andreas.P2.replanning;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

import playground.andreas.P2.plan.PPlan;
import playground.andreas.P2.plan.PRouteProvider;

public class RemoveAllVehiclesButOne implements PPlanStrategy{

	@Override
	public PPlan modifyPlan(PPlan oldPlan, Id id, PRouteProvider pRouteProvider) {
		// profitable route, change startTime
		PPlan newPlan = new PPlan(new IdImpl(pRouteProvider.getIteration()), oldPlan);
		newPlan.setLine(pRouteProvider.createTransitLine(id, newPlan.getStartTime(), newPlan.getEndTime(), 1, newPlan.getStartStop(), newPlan.getEndStop(), null));

		return newPlan;
	}

}
