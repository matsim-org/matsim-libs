/* *********************************************************************** *
 * project: org.matsim.*
 * CreateEvacuationPlanReplanner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.christoph.oldenburg;

import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.ptproject.qsim.agents.ExperimentalBasicWithindayAgent;
import org.matsim.ptproject.qsim.agents.PersonDriverAgentImpl;
import org.matsim.ptproject.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayInitialReplanner;

public class CreateEvacuationPlanReplanner extends WithinDayInitialReplanner {
	
	private Random random;
	
	/*package*/ CreateEvacuationPlanReplanner(Id id, Scenario scenario) {
		super(id, scenario);
		this.random = MatsimRandom.getLocalInstance();
	}

	@Override
	public boolean doReplanning(PlanBasedWithinDayAgent withinDayAgent) {
		
		// If we don't have a valid Replanner.
		if (this.routeAlgo == null) return false;

		// If we don't have a valid personAgent
		if (withinDayAgent == null) return false;

		Plan executedPlan = withinDayAgent.getSelectedPlan();

		// If we don't have an executed plan
		if (executedPlan == null) return false;

		/*
		 *  Get the index of the current PlanElement
		 */
		int currentPlanElementIndex = withinDayAgent.getCurrentPlanElementIndex();
		
		// if it is not the first activity which is being at home
		if (currentPlanElementIndex > 0) return false;
		
		Activity currentActivity = (Activity) executedPlan.getPlanElements().get(0);
		
		/*
		 * calculate departure time
		 */
		double departureTimeDelta = -DemoConfig.lambda * Math.log(this.random.nextDouble()) * DemoConfig.multiplier; 
		double departureTime = DemoConfig.evacuationTime + DemoConfig.evacuationDeltaTime + departureTimeDelta;
				
		PopulationFactory factory = scenario.getPopulation().getFactory();
		Leg leg = factory.createLeg(TransportMode.car);
		Activity activity = factory.createActivityFromLinkId(DemoConfig.activityType, scenario.createId(DemoConfig.evacuationLink));
		
		executedPlan.addLeg(leg);
		executedPlan.addActivity(activity);
		
		// get old departure time
		double oldDepartureTime = withinDayAgent.getActivityEndTime();
		
		// set new departure time
		currentActivity.setMaximumDuration(departureTime - currentActivity.getStartTime());
		currentActivity.setEndTime(departureTime);
		leg.setDepartureTime(departureTime);
				
		// create a route for the new leg
		this.routeAlgo.run(executedPlan);
		
		activity.setStartTime(departureTime + leg.getTravelTime());
		
		/*
		 * Reschedule the currently performed Activity in the Mobsim - there
		 * the activityEndsList has to be updated.
		 */
		if (withinDayAgent instanceof PersonDriverAgentImpl) {	
			((ExperimentalBasicWithindayAgent) withinDayAgent).calculateDepartureTime(currentActivity);
			double newDepartureTime = withinDayAgent.getActivityEndTime();
			
			((PersonDriverAgentImpl) withinDayAgent).getMobsim().rescheduleActivityEnd(withinDayAgent, oldDepartureTime, newDepartureTime);
			
			this.agentCounter.incLiving();
			
			return true;
		}
		else {
			return false;
		}
	}

}
