package playground.andreas.P2.replanning;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.core.basic.v01.IdImpl;

import playground.andreas.P2.pbox.Cooperative;
import playground.andreas.P2.plan.PPlan;

/**
 * 
 * Creates a completely new plan.
 * 
 * @author aneumann
 *
 */
public class CreateNewPlan extends PStrategy implements PPlanStrategy{
	
	private final static Logger log = Logger.getLogger(CreateNewPlan.class);
	public static final String STRATEGY_NAME = "CreateNewPlan";

	public CreateNewPlan(ArrayList<String> parameter) {
		super(parameter);
		if(parameter.size() != 0){
			log.error("Too many parameter. Will ignore: " + parameter);
		}
	}
	
	@Override
	public PPlan run(Cooperative cooperative) {
		PPlan newPlan;		
		
		do {
			newPlan = new PPlan(new IdImpl(cooperative.getCurrentIteration()), cooperative.getRouteProvider().getRandomTransitStop(), cooperative.getRouteProvider().getRandomTransitStop(), 0.0, 24.0 * 3600); 
			while(newPlan.getStartStop() == newPlan.getEndStop()){
				newPlan.setEndStop(cooperative.getRouteProvider().getRandomTransitStop());
			}
			newPlan.setLine(cooperative.getRouteProvider().createTransitLine(cooperative.getId(), newPlan.getStartTime(), newPlan.getEndTime(), 1, newPlan.getStartStop(), newPlan.getEndStop(), new IdImpl(cooperative.getCurrentIteration())));
		} while (cooperative.getFranchise().planRejected(newPlan));		

		return newPlan;
	}

	@Override
	public String getName() {
		return CreateNewPlan.STRATEGY_NAME;
	}

}