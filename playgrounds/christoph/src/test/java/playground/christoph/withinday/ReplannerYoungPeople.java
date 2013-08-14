package playground.christoph.withinday;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplanner;
import org.matsim.withinday.utils.EditRoutes;
import org.matsim.withinday.utils.ReplacePlanElements;

public class ReplannerYoungPeople extends WithinDayDuringLegReplanner {

	private final TripRouter tripRouter;
	private final EditRoutes editRoutes;
	
	/*package*/ ReplannerYoungPeople(Id id, Scenario scenario, InternalInterface internalInterface, TripRouter tripRouter) {
		super(id, scenario, internalInterface);
		this.tripRouter = tripRouter;
		this.editRoutes = new EditRoutes();
	}

	@Override
	public boolean doReplanning(MobsimAgent withinDayAgent) {

		// If we don't have a valid personAgent
		if (withinDayAgent == null) return false;

		PlanImpl executedPlan = (PlanImpl) ((PlanAgent) withinDayAgent).getSelectedPlan();

		// If we don't have an executed plan
		if (executedPlan == null) return false;

		Leg currentLeg = this.withinDayAgentUtils.getCurrentLeg(withinDayAgent);
		Activity nextActivity = executedPlan.getNextActivity(currentLeg);
		int currentLinkIndex = this.withinDayAgentUtils.getCurrentRouteLinkIdIndex(withinDayAgent);

		// If it is not a car Leg we don't replan it.
		if (!currentLeg.getMode().equals(TransportMode.car)) return false;
		
		Id linkId = this.scenario.createId("22");
		ActivityImpl newWorkAct = new ActivityImpl("w", linkId);
		newWorkAct.setEndTime(nextActivity.getEndTime());

		// Replace Activity
		new ReplacePlanElements().replaceActivity(executedPlan, nextActivity, newWorkAct);
		
		/*
		 *  Replan Routes
		 */		
		
		// new Route for current Leg
		this.editRoutes.relocateCurrentLegRoute(currentLeg, executedPlan.getPerson(), currentLinkIndex, linkId, 
				time, scenario.getNetwork(), tripRouter); 
		
		// new Route for next Leg
		Leg homeLeg = executedPlan.getNextLeg(newWorkAct);
		homeLeg.setDepartureTime(newWorkAct.getEndTime());
		this.editRoutes.relocateFutureLegRoute(homeLeg, linkId, homeLeg.getRoute().getEndLinkId(), 
				executedPlan.getPerson(), scenario.getNetwork(), tripRouter);
		
		// finally reset the cached Values of the PersonAgent - they may have changed!
		this.withinDayAgentUtils.resetCaches(withinDayAgent);
		
		return true;
	}

}
