package playground.andreas.P2.replanning;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.MatsimRandom;

import playground.andreas.P2.plan.PPlan;
import playground.andreas.P2.plan.PRouteProvider;

public class RandomStartTimeAllocator implements PPlanStrategy{

	@Override
	public PPlan modifyPlan(PPlan oldPlan, Id id, PRouteProvider pRouteProvider) {
		// profitable route, change startTime
		PPlan newPlan = new PPlan(new IdImpl(pRouteProvider.getIteration()));
		newPlan.setStartStop(oldPlan.getStartStop());
		newPlan.setEndStop(oldPlan.getEndStop());
		
		// get a valid new start time
		double newStartTime = Math.max(0.0, oldPlan.getStartTime() + (-0.5 + MatsimRandom.getRandom().nextDouble()) * 6 * 3600);
		newStartTime = Math.min(newStartTime, oldPlan.getEndTime() - 1 * 3600);
		newPlan.setStartTime(newStartTime);
		
		newPlan.setEndTime(oldPlan.getEndTime());
		newPlan.setLine(pRouteProvider.createTransitLine(id, newPlan.getStartTime(), newPlan.getEndTime(), 1, newPlan.getStartStop(), newPlan.getEndStop(), null));

		return newPlan;
	}

}
