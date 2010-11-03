package playground.wrashid.parkingSearch.withinday;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.ptproject.qsim.agents.WithinDayAgent;

import playground.christoph.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplanner;
import playground.christoph.withinday.utils.EditRoutes;

public class ReplannerOldPeople extends WithinDayDuringActivityReplanner {

	public ReplannerOldPeople(Id id, Scenario scenario) {
		super(id, scenario);
	}

	@Override
	public WithinDayDuringActivityReplanner clone() {
		return this;
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
				
		Person person = withinDayAgent.getPerson();
		PlanImpl selectedPlan = (PlanImpl)person.getSelectedPlan(); 
		
		// If we don't have a selected plan
		// (only extra security)
		if (selectedPlan == null) return false;
		
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
		Leg homeLeg = selectedPlan.getNextLeg(currentActivity);
		Activity homeAct = selectedPlan.getNextActivity(homeLeg);
		
		ActivityImpl newWorkAct = new ActivityImpl("w", new ScenarioImpl().createId("22"));
		newWorkAct.setDuration(3600);
		
		LegImpl legToNewWork = new LegImpl(TransportMode.car);
		
		selectedPlan.insertLegAct(selectedPlan.getActLegIndex(currentActivity) + 1, legToNewWork, newWorkAct);
		
		// replan the new Legs
		new EditRoutes().replanFutureLegRoute(selectedPlan, selectedPlan.getActLegIndex(legToNewWork), routeAlgo);
		new EditRoutes().replanFutureLegRoute(selectedPlan, selectedPlan.getActLegIndex(homeLeg), routeAlgo);
				
		return true;
	}

}
