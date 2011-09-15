package playground.wrashid.parkingSearch.withinday;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.ptproject.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayInitialReplanner;

public class InsertParkingActivitiesReplanner extends WithinDayInitialReplanner {

	/*
	 * use a InitialIdentifierImpl and set handleAllAgents to true
	 */
	public InsertParkingActivitiesReplanner(Id id, Scenario scenario) {
		super(id, scenario);
	}

	@Override
	public boolean doReplanning(PlanBasedWithinDayAgent withinDayAgent) {
		Plan executedPlan = withinDayAgent.getSelectedPlan();
		
		/*
		 * TODO:
		 * changes to this plan will be executed but not written to the person
		 *  
		 * - go through the plan and identify all car legs
		 * - replace them by a new chain (walk leg - parking activity - car leg - parking activity - walk leg)
		 * - select parking facility (e.g. nearest to origin and destination, assigned to location, ... 
		 * - reroute car leg
		 * - recalculate walk leg distance
		 */
		for (PlanElement planElement : executedPlan.getPlanElements()) {
			if (planElement instanceof Activity) {
				//((Activity) planElement).setType("dummy");
			}
		}
		
		return true;
	}

}
