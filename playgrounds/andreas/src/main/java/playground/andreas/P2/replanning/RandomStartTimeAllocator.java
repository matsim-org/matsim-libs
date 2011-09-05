package playground.andreas.P2.replanning;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.MatsimRandom;

import playground.andreas.P2.pbox.Cooperative;
import playground.andreas.P2.plan.PPlan;

/**
 * 
 * Changes the start time of operation randomly within a certain range, leaving a minimal operating time.
 * 
 * @author aneumann
 *
 */
public class RandomStartTimeAllocator extends PStrategy implements PPlanStrategy{
	
	private final static Logger log = Logger.getLogger(RandomStartTimeAllocator.class);
	public static final String STRATEGY_NAME = "RandomStartTimeAllocator";
	
	private final int mutationRange;
	private final int minimalOperatingTime;

	public RandomStartTimeAllocator(ArrayList<String> parameter) {
		super(parameter);
		if(parameter.size() != 2){
			log.error("Missing parameter: 1 - Mutation range in seconds, 2 - Minimal operating time in seconds");
		}
		this.mutationRange = Integer.parseInt(parameter.get(0));
		this.minimalOperatingTime = Integer.parseInt(parameter.get(1));
	}

	@Override
	public PPlan run(Cooperative cooperative) {
		// profitable route, change startTime
		PPlan newPlan = new PPlan(new IdImpl(cooperative.getCurrentIteration()));
		newPlan.setStartStop(cooperative.getBestPlan().getStartStop());
		newPlan.setEndStop(cooperative.getBestPlan().getEndStop());
		
		// get a valid new start time
		double newStartTime = Math.max(0.0, cooperative.getBestPlan().getStartTime() + (-0.5 + MatsimRandom.getRandom().nextDouble()) * this.mutationRange);
		newStartTime = Math.min(newStartTime, cooperative.getBestPlan().getEndTime() - this.minimalOperatingTime);
		newPlan.setStartTime(newStartTime);
		
		newPlan.setEndTime(cooperative.getBestPlan().getEndTime());
		newPlan.setLine(cooperative.getRouteProvider().createTransitLine(cooperative.getId(), newPlan.getStartTime(), newPlan.getEndTime(), 1, newPlan.getStartStop(), newPlan.getEndStop(), new IdImpl(cooperative.getCurrentIteration())));

		return newPlan;
	}
	
	@Override
	public String getName() {
		return RandomStartTimeAllocator.STRATEGY_NAME;
	}

}
