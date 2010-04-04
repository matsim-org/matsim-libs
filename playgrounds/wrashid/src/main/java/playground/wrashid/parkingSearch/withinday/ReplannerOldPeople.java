package playground.wrashid.parkingSearch.withinday;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;

import playground.christoph.withinday.mobsim.WithinDayPersonAgent;
import playground.christoph.withinday.replanning.WithinDayDuringActivityReplanner;
import playground.christoph.withinday.utils.EditRoutes;

public class ReplannerOldPeople extends WithinDayDuringActivityReplanner {

	public ReplannerOldPeople(Id id) {
		super(id);
	}

	@Override
	public WithinDayDuringActivityReplanner clone() {
		return this;
	}

	@Override
	/**
	 * return value (in future it might be true, when successful call)
	 */
	public boolean doReplanning(DriverAgent driverAgent) {
		
		// If we don't have a valid Replanner.
		// (only extra security)
		if (this.planAlgorithm == null) return false;
		
		// If we don't have a valid WithinDayPersonAgent
		// (only extra security)
		if (driverAgent == null) return false;
		
		WithinDayPersonAgent withinDayPersonAgent = null;
		if (!(driverAgent instanceof WithinDayPersonAgent)) return false;
		else
		{
			withinDayPersonAgent = (WithinDayPersonAgent) driverAgent;
		}
		
		PersonImpl person = (PersonImpl)withinDayPersonAgent.getPerson();
		PlanImpl selectedPlan = (PlanImpl)person.getSelectedPlan(); 
		
		// If we don't have a selected plan
		// (only extra security)
		if (selectedPlan == null) return false;
		
		Activity currentActivity = withinDayPersonAgent.getCurrentActivity();
		
		// If we don't have a current Activity
		// (only extra security)
		if (currentActivity == null) return false;
		
		// modify plan (the agent wants to perform an additional work activity at link 22 before going home
		// therefore the agent needs to create new route and re-routing for the rest of the plan.
		Leg homeLeg = selectedPlan.getNextLeg(currentActivity);
		Activity homeAct = selectedPlan.getNextActivity(homeLeg);
		
		ActivityImpl newWorkAct = new ActivityImpl("w", new ScenarioImpl().createId("22"));
		newWorkAct.setDuration(3600);
		
		LegImpl legToNewWork = new LegImpl(TransportMode.car);
		
		selectedPlan.insertLegAct(selectedPlan.getActLegIndex(currentActivity) + 1, legToNewWork, newWorkAct);
		
		// replan the new Legs
		new EditRoutes().replanFutureLegRoute(selectedPlan, legToNewWork, planAlgorithm);
		new EditRoutes().replanFutureLegRoute(selectedPlan, homeLeg, planAlgorithm);
				
		return true;
	}

}
