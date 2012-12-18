package playground.kai.usecases.mentalmodule;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.replanning.ReplanningContext;

public class MyTimeMutator implements PlanStrategyModule {

	@Override
	public void finishReplanning() {
		// TODO Auto-generated method stub

	}

	@Override
	public void handlePlan(Plan plan) {
		for ( PlanElement pe : plan.getPlanElements() ) {
			if ( pe instanceof Activity ) {
				double endTime = ((Activity)pe).getEndTime() ;
				double newEndTime ;
				if ( Math.random() < 0.5 ) {
					newEndTime = endTime + Math.random() * 3600. ;
				} else {
					newEndTime = endTime - Math.random() * 3600. ;
				}
				((Activity)pe).setEndTime(newEndTime) ;
			}
		}
	}

	@Override
	public void prepareReplanning(ReplanningContext replanningContext) {
		// TODO Auto-generated method stub

	}

}
