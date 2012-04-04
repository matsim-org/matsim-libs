package playground.andreas.P2.replanning.modules;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.MatsimRandom;

import playground.andreas.P2.pbox.Cooperative;
import playground.andreas.P2.plan.PPlan;
import playground.andreas.P2.replanning.PPlanStrategy;
import playground.andreas.P2.replanning.PStrategy;

/**
 * 
 * Sets a new endTime between the old endTime and midnight.
 * 
 * @author aneumann
 *
 */
public class MaxRandomEndTimeAllocator extends PStrategy implements PPlanStrategy{
	
	private final static Logger log = Logger.getLogger(MaxRandomEndTimeAllocator.class);	
	public static final String STRATEGY_NAME = "MaxRandomEndTimeAllocator";
	
	public MaxRandomEndTimeAllocator(ArrayList<String> parameter) {
		super(parameter);
		if(parameter.size() != 0){
			log.error("There are no parameters allowed for that module");
		}
	}
	
	@Override
	public PPlan run(Cooperative cooperative) {
		if (cooperative.getBestPlan().getNVehicles() <= 1) {
			return null;
		}
		
		// enough vehicles to test, change endTime
		PPlan newPlan = new PPlan(new IdImpl(cooperative.getCurrentIteration()));
		newPlan.setStopsToBeServed(cooperative.getBestPlan().getStopsToBeServed());
		newPlan.setStartTime(cooperative.getBestPlan().getStartTime());
		
		// get a valid new end time
		double newEndTime = cooperative.getBestPlan().getEndTime() + (24.0 * 3600.0 - cooperative.getBestPlan().getEndTime()) * MatsimRandom.getRandom().nextDouble();
		newPlan.setEndTime(newEndTime);
		
		newPlan.setLine(cooperative.getRouteProvider().createTransitLine(cooperative.getId(), newPlan.getStartTime(), newPlan.getEndTime(), 1, newPlan.getStopsToBeServed(), new IdImpl(cooperative.getCurrentIteration())));
		
		cooperative.getBestPlan().setNVehicles(cooperative.getBestPlan().getNVehicles() - 1);
		
		return newPlan;
	}

	@Override
	public String getName() {
		return MaxRandomEndTimeAllocator.STRATEGY_NAME;
	}

}
