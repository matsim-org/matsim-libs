package playground.balac.induceddemand.strategies;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;

import java.util.List;
import java.util.Random;

public class ChooseRandomActivitiesToSwap implements PlanAlgorithm {

	private final Random rng;
	private final StageActivityTypes stageActivityTypes;
	private Scenario scenario;

	public ChooseRandomActivitiesToSwap(Scenario scenario, 
			Random localInstance, StageActivityTypes stageActivityTypes) {

		this.rng = localInstance;
		this.stageActivityTypes = stageActivityTypes;
		this.scenario = scenario;

	}

	@Override
	public void run(Plan plan) {

		if (!Boolean.parseBoolean(this.scenario.getConfig().getModule("ActivityStrategies").getValue("useSwapActivitiesStrategy"))) 
			return;
		List<Activity> t = TripStructureUtils.getActivities(plan, this.stageActivityTypes);
		int countActivities = t.size();	
		if (countActivities > 3) {
			
			int index1 = 1 + this.rng.nextInt(countActivities - 2);			
			
			int index2 = 1 + this.rng.nextInt(countActivities - 2);
			
			while (index1 == index2)
				index2 = 1 + this.rng.nextInt(countActivities - 2);
			
			swap(plan, t.get(index1), t.get(index2));
		}
	}
	
	private void swap(Plan plan, Activity activity1, Activity activity2) {
		
		int index1 = plan.getPlanElements().indexOf(activity1);
		
		int index2 = plan.getPlanElements().indexOf(activity2);
		
		plan.getPlanElements().set( index1 , activity2 );
		plan.getPlanElements().set( index2 , activity1 );
		
	}

}
