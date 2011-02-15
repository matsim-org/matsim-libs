package playground.christoph.withinday;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.ptproject.qsim.agents.WithinDayAgent;

import playground.christoph.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplanner;
import playground.christoph.withinday.utils.EditRoutes;
import playground.christoph.withinday.utils.ReplacePlanElements;

public class ReplannerYoungPeople extends WithinDayDuringLegReplanner {

	/*package*/ ReplannerYoungPeople(Id id, Scenario scenario) {
		super(id, scenario);
	}

	@Override
	public boolean doReplanning(WithinDayAgent withinDayAgent) {
		
		// If we don't have a valid Replanner.
		if (this.routeAlgo == null) return false;

		// If we don't have a valid personAgent
		if (withinDayAgent == null) return false;

		PlanImpl executedPlan = (PlanImpl)withinDayAgent.getExecutedPlan();

		// If we don't have an executed plan
		if (executedPlan == null) return false;

		Leg currentLeg = withinDayAgent.getCurrentLeg();
		int currentLegIndex = withinDayAgent.getCurrentPlanElementIndex();
		Activity nextActivity = executedPlan.getNextActivity(currentLeg);

		// If it is not a car Leg we don't replan it.
		if (!currentLeg.getMode().equals(TransportMode.car)) return false;
		
		ActivityImpl newWorkAct = new ActivityImpl("w", this.scenario.createId("22"));
		newWorkAct.setDuration(3600);

		// Replace Activity
		new ReplacePlanElements().replaceActivity(executedPlan, nextActivity, newWorkAct);
		
		/*
		 *  Replan Routes
		 */
		int currentPlanElementIndex =  withinDayAgent.getCurrentPlanElementIndex();
		
		// new Route for current Leg
		new EditRoutes().replanCurrentLegRoute(executedPlan, currentLegIndex, currentPlanElementIndex, routeAlgo, scenario.getNetwork(), time);
		
		// new Route for next Leg
		Leg homeLeg = executedPlan.getNextLeg(newWorkAct);
		int homeLegIndex = executedPlan.getPlanElements().indexOf(homeLeg);
		new EditRoutes().replanFutureLegRoute(executedPlan, homeLegIndex, routeAlgo);
		
		// finally reset the cached Values of the PersonAgent - they may have changed!
		withinDayAgent.resetCaches();
		
		return true;
	}

}
