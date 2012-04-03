package playground.andreas.P2.replanning;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.core.basic.v01.IdImpl;

import playground.andreas.P2.pbox.Cooperative;
import playground.andreas.P2.plan.PPlan;

/**
 * 
 * Increases the number of vehicles for the given cooperative's best plan, if the budget allows for that.
 * 
 * Buys as much vehicles as possible.
 * 
 * @author aneumann
 *
 */
public class AggressiveIncreaseNumberOfVehicles extends PStrategy implements PPlanStrategy{
	
	private final static Logger log = Logger.getLogger(AggressiveIncreaseNumberOfVehicles.class);
	
	public static final String STRATEGY_NAME = "AggressiveIncreaseNumberOfVehicles";
	
	public AggressiveIncreaseNumberOfVehicles(ArrayList<String> parameter) {
		super(parameter);
		if(parameter.size() != 0){
			log.error("There are no parameters allowed for that module");
		}
	}
	
	@Override
	public PPlan run(Cooperative cooperative) {
		
		int vehicleBought = 0;
		
		while (cooperative.getBudget() > cooperative.getCostPerVehicleBuy()) {
			// budget ok, buy one
			cooperative.setBudget(cooperative.getBudget() - cooperative.getCostPerVehicleBuy());
			vehicleBought++;
		}					
		
		if (vehicleBought == 0) {
			return null;
		}
			
		// vehicles were bought - create plan
		PPlan plan = new PPlan(cooperative.getBestPlan().getId(), cooperative.getBestPlan().getStopsToBeServed(), cooperative.getBestPlan().getStartTime(), cooperative.getBestPlan().getEndTime());
		plan.setScore(cooperative.getBestPlan().getScore());
		plan.setLine(cooperative.getRouteProvider().createTransitLine(cooperative.getId(), plan.getStartTime(), plan.getEndTime(), vehicleBought, plan.getStopsToBeServed(), new IdImpl(cooperative.getCurrentIteration())));
		return plan;
	}

	@Override
	public String getName() {
		return AggressiveIncreaseNumberOfVehicles.STRATEGY_NAME;
	}
}