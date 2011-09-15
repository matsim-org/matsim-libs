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
 * @author aneumann
 *
 */
public class IncreaseNumberOfVehicles extends PStrategy implements PPlanStrategy{
	
	private final static Logger log = Logger.getLogger(IncreaseNumberOfVehicles.class);
	
	public static final String STRATEGY_NAME = "IncreaseNumberOfVehicles";
	
	public IncreaseNumberOfVehicles(ArrayList<String> parameter) {
		super(parameter);
		if(parameter.size() != 0){
			log.error("There are no parameters allowed for that module");
		}
	}
	
	@Override
	public PPlan run(Cooperative cooperative) {
		// sufficient founds, so buy one
		PPlan plan = new PPlan(cooperative.getBestPlan().getId(), cooperative.getBestPlan().getStartStop(), cooperative.getBestPlan().getEndStop(), cooperative.getBestPlan().getStartTime(), cooperative.getBestPlan().getEndTime());
		plan.setScore(cooperative.getBestPlan().getScore());
		plan.setLine(cooperative.getRouteProvider().createTransitLine(cooperative.getId(), plan.getStartTime(), plan.getEndTime(), cooperative.getBestPlan().getNVehciles() +1, plan.getStartStop(), plan.getEndStop(), new IdImpl(cooperative.getCurrentIteration())));
		return plan;			
	}

	@Override
	public String getName() {
		return IncreaseNumberOfVehicles.STRATEGY_NAME;
	}

}
