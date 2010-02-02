package playground.christoph.withinday;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.ptproject.qsim.DriverAgent;
import org.matsim.ptproject.qsim.QSim;

import playground.christoph.events.ExtendedAgentReplanEventImpl;
import playground.christoph.network.util.SubNetworkTools;
import playground.christoph.withinday.mobsim.WithinDayPersonAgent;
import playground.christoph.withinday.replanning.WithinDayDuringActivityReplanner;

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
		// therfore the agent needs to create new route and rerouting for the rest of the plan.
		Leg homeLeg=selectedPlan.getNextLeg(currentActivity);
		Activity homeAct=selectedPlan.getNextActivity(homeLeg);
		
		ActivityImpl newWorkAct=new ActivityImpl("w",new IdImpl("22"));
		newWorkAct.setDuration(3600);
		
		LegImpl legToNewWork=new LegImpl(TransportMode.car);
		
		selectedPlan.insertLegAct(selectedPlan.getPlanElements().indexOf(currentActivity)+1, legToNewWork, newWorkAct);
		
		
		// Create a new plan for only that part, which is changed.
		PlanImpl newPlan = new PlanImpl(person);
		person.addPlan(newPlan);
		person.setSelectedPlan(newPlan);
		
		// create new plan
		newPlan.addActivity(currentActivity);
		newPlan.addLeg(legToNewWork);
		newPlan.addActivity(newWorkAct);
		newPlan.addLeg(homeLeg);
		newPlan.addActivity(homeAct);
						
		// do routing for this plan.
		planAlgorithm.run(newPlan);
		
		// reactivate previously selected, replanned plan
		person.setSelectedPlan(selectedPlan);
		
		// remove previously added new Plan (not needed anymore)
		person.removePlan(newPlan);
		
		return true;
	}

}
