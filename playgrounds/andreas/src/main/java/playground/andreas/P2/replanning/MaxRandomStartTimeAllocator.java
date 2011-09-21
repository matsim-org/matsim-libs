package playground.andreas.P2.replanning;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.MatsimRandom;

import playground.andreas.P2.pbox.Cooperative;
import playground.andreas.P2.plan.PPlan;

/**
 * 
 * Changes the start time of operation randomly from midnight to startTime.
 * 
 * @author aneumann
 *
 */
public class MaxRandomStartTimeAllocator extends PStrategy implements PPlanStrategy{
	
	private final static Logger log = Logger.getLogger(MaxRandomStartTimeAllocator.class);
	public static final String STRATEGY_NAME = "MaxRandomStartTimeAllocator";
	
	public MaxRandomStartTimeAllocator(ArrayList<String> parameter) {
		super(parameter);
		if(parameter.size() != 0){
			log.error("There are no parameters allowed for that module");
		}
	}

	@Override
	public PPlan run(Cooperative cooperative) {
		// profitable route, change startTime
		PPlan newPlan = new PPlan(new IdImpl(cooperative.getCurrentIteration()));
		newPlan.setStartStop(cooperative.getBestPlan().getStartStop());
		newPlan.setEndStop(cooperative.getBestPlan().getEndStop());
		
		// get a valid new start time
		double newStartTime = cooperative.getBestPlan().getStartTime() * MatsimRandom.getRandom().nextDouble();
		newPlan.setStartTime(newStartTime);
		
		newPlan.setEndTime(cooperative.getBestPlan().getEndTime());
		newPlan.setLine(cooperative.getRouteProvider().createTransitLine(cooperative.getId(), newPlan.getStartTime(), newPlan.getEndTime(), 1, newPlan.getStartStop(), newPlan.getEndStop(), new IdImpl(cooperative.getCurrentIteration())));

		return newPlan;
	}
	
	@Override
	public String getName() {
		return MaxRandomStartTimeAllocator.STRATEGY_NAME;
	}

}
