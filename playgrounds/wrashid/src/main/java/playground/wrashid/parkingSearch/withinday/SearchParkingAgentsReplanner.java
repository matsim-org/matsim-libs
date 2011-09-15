package playground.wrashid.parkingSearch.withinday;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.ptproject.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplanner;

public class SearchParkingAgentsReplanner extends WithinDayDuringLegReplanner {

	/*
	 * Add object which is a LinkEnterEventHandler that decides whether
	 * an agents wants to (or can) park on the link it has just entered.
	 * Or use e.g. a ConcurrentHashMap that can be used by all Replanners
	 * at the same time.
	 */
	
	public SearchParkingAgentsReplanner(Id id, Scenario scenario) {
		super(id, scenario);
	}

	@Override
	public boolean doReplanning(PlanBasedWithinDayAgent withinDayAgent) {
		
//		withinDayAgent.getNextPlanElement(); -> next activity -> parking activity
		
		/*
		 * search for new location of next parking facility
		 */
		
		/*
		 * Move parking activity
		 * cast to ActivityImpl to set LinkId and FacilityId
		 */
		
		/*
		 * reroute current route
		 */
//		withinDayAgent.getCurrentLeg();
//		new EditRoutes().replanCurrentLegRoute(plan, legPlanElementIndex, currentLinkIndex, planAlgorithm, time);

		/*
		 * recalculate walk travel time
		 */
		
		/*
		 * Reset caches after replanning
		 */
//		agent.resetCaches()

		return false;
	}

}
