package playground.andreas.P2.replanning;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.MatsimRandom;

import playground.andreas.P2.plan.PPlan;
import playground.andreas.P2.plan.PRouteProvider;

/**
 * 
 * Changes the end time of operation randomly within a certain range, leaving a minimal operating time.
 * 
 * @author aneumann
 *
 */
public class RandomEndTimeAllocator extends PStrategy implements PPlanStrategy{
	
	private final static Logger log = Logger.getLogger(RandomEndTimeAllocator.class);
	
	public static final String STRATEGY_NAME = "RandomEndTimeAllocator";
	
	private final int mutationRange;
	private final int minimalOperatingTime;

	public RandomEndTimeAllocator(ArrayList<String> parameter) {
		super(parameter);
		if(parameter.size() != 2){
			log.error("Missing parameter: 1 - Mutation range in seconds, 2 - Minimal operating time in seconds");
		}
		this.mutationRange = Integer.parseInt(parameter.get(0));
		this.minimalOperatingTime = Integer.parseInt(parameter.get(1));
	}

	@Override
	public PPlan modifyPlan(PPlan oldPlan, Id id, PRouteProvider pRouteProvider) {
		// profitable route, change startTime
		PPlan newPlan = new PPlan(new IdImpl(pRouteProvider.getIteration()));
		newPlan.setStartStop(oldPlan.getStartStop());
		newPlan.setEndStop(oldPlan.getEndStop());
		newPlan.setStartTime(oldPlan.getStartTime());
		
		// get a valid new end time
		double newEndTime = Math.min(24 * 3600.0, oldPlan.getEndTime() + (-0.5 + MatsimRandom.getRandom().nextDouble()) * this.mutationRange);
		newEndTime = Math.max(newEndTime, oldPlan.getStartTime() + this.minimalOperatingTime);
		newPlan.setEndTime(newEndTime);
		
		newPlan.setLine(pRouteProvider.createTransitLine(id, newPlan.getStartTime(), newPlan.getEndTime(), 1, newPlan.getStartStop(), newPlan.getEndStop(), null));
		
		return newPlan;
	}

	@Override
	public String getName() {
		return RandomEndTimeAllocator.STRATEGY_NAME;
	}

}
