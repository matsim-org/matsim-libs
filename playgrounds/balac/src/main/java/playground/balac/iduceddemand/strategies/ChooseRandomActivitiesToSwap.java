package playground.balac.iduceddemand.strategies;

import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.population.algorithms.PlanAlgorithm;

public class ChooseRandomActivitiesToSwap implements PlanAlgorithm {

	private final Random rng;
	private final StageActivityTypes stageActivityTypes;

	public ChooseRandomActivitiesToSwap(Random localInstance,
			StageActivityTypes stageActivityTypes) {

		this.rng = localInstance;
		this.stageActivityTypes = stageActivityTypes;

	}

	@Override
	public void run(Plan plan) {

		List<Activity> t = TripStructureUtils.getActivities(plan, this.stageActivityTypes);
		int countActivities = t.size();	
		if (countActivities > 3) {

			if (countActivities == 0) {
				return;
			}
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
