package playground.balac.iduceddemand.strategies;

import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.population.algorithms.PlanAlgorithm;

public class ChooseActivityToRemove implements PlanAlgorithm {

	private final Random rng;
	private final StageActivityTypes stageActivityTypes;
	private Scenario scenario;
	
	public ChooseActivityToRemove(Random localInstance, StageActivityTypes stageActivityTypes, Scenario scenario) {
		this.rng = localInstance;
		this.stageActivityTypes = stageActivityTypes;
		this.scenario = scenario;	
	}

	@Override
	public void run(Plan plan) {
		
		List<Activity> t = TripStructureUtils.getActivities(plan, this.stageActivityTypes);

		if (t.size() == 1)
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
			plan.getPlanElements().remove(actIndex + 1);

		}
		else if (!previous && !next) {
				plan.getPlanElements().remove(actIndex);
				plan.getPlanElements().remove(actIndex + 1);
			
		}
		else {
			
			//TODO: change the mode of all legs to the mode of the rpevious one
			
		}
		
			
		
		
		
	}

}
