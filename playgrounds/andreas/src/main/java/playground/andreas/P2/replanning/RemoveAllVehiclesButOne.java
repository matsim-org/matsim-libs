package playground.andreas.P2.replanning;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

import playground.andreas.P2.plan.PPlan;
import playground.andreas.P2.plan.PRouteProvider;

/**
 * 
 * Clones a given plan, but resets the number of vehicles to one.
 * 
 * @author aneumann
 *
 */
public class RemoveAllVehiclesButOne extends PStrategy implements PPlanStrategy{
	
	private final static Logger log = Logger.getLogger(RemoveAllVehiclesButOne.class);
	public static final String STRATEGY_NAME = "RemoveAllVehiclesButOne";

	public RemoveAllVehiclesButOne(ArrayList<String> parameter) {
		super(parameter);
		if(parameter.size() != 0){
			log.error("Too many parameter. Will ignore: " + parameter);
		}
	}

	@Override
	public PPlan modifyPlan(PPlan oldPlan, Id id, PRouteProvider pRouteProvider) {
		// profitable route, change startTime
		PPlan newPlan = new PPlan(new IdImpl(pRouteProvider.getIteration()), oldPlan);
		newPlan.setLine(pRouteProvider.createTransitLine(id, newPlan.getStartTime(), newPlan.getEndTime(), 1, newPlan.getStartStop(), newPlan.getEndStop(), null));

		return newPlan;
	}

	@Override
	public String getName() {
		return RemoveAllVehiclesButOne.STRATEGY_NAME;
	}

}