package playground.andreas.P2.replanning;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.MatsimRandom;

import playground.andreas.P2.plan.PPlan;
import playground.andreas.P2.plan.PRouteProvider;

public class RandomEndTimeAllocator implements PPlanStrategy{

	@Override
	public PPlan modifyPlan(PPlan oldPlan, Id id, PRouteProvider pRouteProvider) {
		// profitable route, change startTime
		PPlan newPlan = new PPlan(new IdImpl(pRouteProvider.getIteration()));
		newPlan.setStartStop(oldPlan.getStartStop());
		newPlan.setEndStop(oldPlan.getEndStop());
		newPlan.setStartTime(oldPlan.getStartTime());
		
		// get a valid new end time
		double newEndTime = Math.min(24 * 3600.0, oldPlan.getEndTime() + (-0.5 + MatsimRandom.getRandom().nextDouble()) * 6 * 3600);
		newEndTime = Math.max(newEndTime, oldPlan.getStartTime() + 1 * 3600);
		newPlan.setEndTime(newEndTime);
		
		newPlan.setLine(pRouteProvider.createTransitLine(id, newPlan.getStartTime(), newPlan.getEndTime(), 1, newPlan.getStartStop(), newPlan.getEndStop(), null));
		
		return newPlan;
	}

}
