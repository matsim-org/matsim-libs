package playground.christoph.withinday;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.ptproject.qsim.agents.WithinDayAgent;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplanner;
import org.matsim.withinday.utils.EditRoutes;

public class ReplannerOldPeople extends WithinDayDuringActivityReplanner {

	/*package*/ ReplannerOldPeople(Id id, Scenario scenario) {
		super(id, scenario);
	}

	@Override
	/**
	 * return value (in future it might be true, when successful call)
	 */
	public boolean doReplanning(WithinDayAgent withinDayAgent) {
		
		// If we don't have a valid Replanner.
		// (only extra security)
		if (this.routeAlgo == null) return false;
		
		// If we don't have a valid personAgent (only extra security)
		if (withinDayAgent == null) return false;
			
		PlanImpl executedPlan = (PlanImpl)withinDayAgent.getExecutedPlan();

		// If we don't have an executed plan
		if (executedPlan == null) return false;
		
		Activity currentActivity;
		/*
		 *  Get the current PlanElement and check if it is an Activity
		 */
		PlanElement currentPlanElement = withinDayAgent.getCurrentPlanElement();
		if (currentPlanElement instanceof Activity) {
			currentActivity = (Activity) currentPlanElement;
		} else return false;
		
		// modify plan (the agent wants to perform an additional work activity at link 22 before going home
		// therefore the agent needs to create new route and re-routing for the rest of the plan.
		Leg homeLeg = executedPlan.getNextLeg(currentActivity);
		Activity homeAct = executedPlan.getNextActivity(homeLeg);
		int homeLegIndex = executedPlan.getActLegIndex(homeLeg);
		
		ActivityImpl newWorkAct = new ActivityImpl("w", this.scenario.createId("22"));
		newWorkAct.setMaximumDuration(3600);
		
		LegImpl legToNewWork = new LegImpl(TransportMode.car);
		
		int legToNewWorkIndex = executedPlan.getActLegIndex(currentActivity) + 1;
		executedPlan.insertLegAct(legToNewWorkIndex, legToNewWork, newWorkAct);
		
		// replan the new Legs
		new EditRoutes().replanFutureLegRoute(executedPlan, legToNewWorkIndex, routeAlgo);
		new EditRoutes().replanFutureLegRoute(executedPlan, homeLegIndex, routeAlgo);
				
		return true;
	}

}
