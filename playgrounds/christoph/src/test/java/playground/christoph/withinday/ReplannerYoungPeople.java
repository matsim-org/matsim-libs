package playground.christoph.withinday;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.mobsim.framework.PersonDriverAgent;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;

import playground.christoph.withinday.mobsim.WithinDayPersonAgent;
import playground.christoph.withinday.replanning.WithinDayDuringLegReplanner;
import playground.christoph.withinday.utils.EditRoutes;
import playground.christoph.withinday.utils.ReplacePlanElements;

public class ReplannerYoungPeople extends WithinDayDuringLegReplanner {

	@Override
	public WithinDayDuringLegReplanner clone() {
		return this;
	}

	public ReplannerYoungPeople(Id id, Scenario scenario)
	{
		super(id, scenario);
	}

	@Override
	public boolean doReplanning(PersonDriverAgent driverAgent) {
		
		// If we don't have a valid Replanner.
		if (this.planAlgorithm == null) return false;

		// If we don't have a valid WithinDayPersonAgent
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
		if (selectedPlan == null) return false;

		Leg currentLeg = driverAgent.getCurrentLeg();
		Activity nextActivity = selectedPlan.getNextActivity(currentLeg);

		// If it is not a car Leg we don't replan it.
		if (!currentLeg.getMode().equals(TransportMode.car)) return false;
		
		ActivityImpl newWorkAct = new ActivityImpl("w", this.scenario.createId("22"));
		newWorkAct.setDuration(3600);

		// Replace Activity
		new ReplacePlanElements().replaceActivity(selectedPlan, nextActivity, newWorkAct);
		
		/*
		 *  Replan Routes
		 */
		// new Route for current Leg
		new EditRoutes().replanCurrentLegRoute(selectedPlan, currentLeg, ((WithinDayPersonAgent)driverAgent).getCurrentNodeIndex(), planAlgorithm, scenario.getNetwork(), time);
		
		// new Route for next Leg
		Leg homeLeg = selectedPlan.getNextLeg(newWorkAct);
		new EditRoutes().replanFutureLegRoute(selectedPlan, homeLeg, planAlgorithm);
		
		// finally reset the cached Values of the PersonAgent - they may have changed!
		withinDayPersonAgent.resetCaches();
		
		return true;
	}

}
