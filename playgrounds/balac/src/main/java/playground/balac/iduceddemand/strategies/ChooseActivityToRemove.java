package playground.balac.iduceddemand.strategies;

import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;

public class ChooseActivityToRemove implements PlanAlgorithm {

	private final Random rng;
	private final StageActivityTypes stageActivityTypes;
	
	public ChooseActivityToRemove(Random localInstance, StageActivityTypes stageActivityTypes) {
		this.rng = localInstance;
		this.stageActivityTypes = stageActivityTypes;
	}

	@Override
	public void run(Plan plan) {
		
		List<Activity> t = TripStructureUtils.getActivities(plan, this.stageActivityTypes);

		if (t.size() == 1 || t.size() == 2)
			return;
		
		int index = rng.nextInt(t.size() - 2) + 1;
		
		int actIndex = plan.getPlanElements().indexOf(t.get(index));
		
		boolean previous = false;
		boolean next = false;
		
		if (((Leg) plan.getPlanElements().get(actIndex - 1)).getMode().equals("car") ||
				((Leg) plan.getPlanElements().get(actIndex - 1)).getMode().equals("bike"))
			previous = true;
		if (((Leg) plan.getPlanElements().get(actIndex + 1)).getMode().equals("car") ||
				((Leg) plan.getPlanElements().get(actIndex + 1)).getMode().equals("bike"))
			next = true;
		
		if  (((Leg) plan.getPlanElements().get(actIndex - 1)).getMode()
				.equals(((Leg) plan.getPlanElements().get(actIndex - 1)).getMode())) {
			
			plan.getPlanElements().remove(actIndex);
			plan.getPlanElements().remove(actIndex);

		}
		else if (!previous && !next) {
				plan.getPlanElements().remove(actIndex);
				plan.getPlanElements().remove(actIndex);
			
		}
		else {
			
			String previousLegMode = ( (Leg) plan.getPlanElements().get(actIndex - 1) ).getMode();
			
			plan.getPlanElements().remove(actIndex);
			plan.getPlanElements().remove(actIndex);
			
			for(PlanElement pe : plan.getPlanElements()) {
				
				if (pe instanceof Leg)
					((Leg) pe).setMode(previousLegMode);
			}			
			
		}		
	}
}
