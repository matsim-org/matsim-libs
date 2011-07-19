package playground.mzilske.teach;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;

public class ChangeActivityTimes implements PlanStrategyModule, ActivityEndEventHandler {

	@Override
	public void prepareReplanning() {
		System.out.println("prepare.");
	}

	@Override
	public void handlePlan(Plan plan) {
		Activity act = (Activity) plan.getPlanElements().get(0);
		act.setEndTime(60*60*4);
	}

	@Override
	public void finishReplanning() {
		System.out.println("Finish.");
	}

	@Override
	public void reset(int iteration) {
		System.out.println("Reset.");
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		// TODO Auto-generated method stub
		
	}
	


}
