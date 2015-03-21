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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.router.PlanRouter;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayInitialReplanner;

public class CreateEvacuationPlanReplanner extends WithinDayInitialReplanner {
	
	private Random random;
	private PlanRouter planRouter;
	
	/*package*/ CreateEvacuationPlanReplanner(Id id, Scenario scenario, InternalInterface internalInterface,
			PlanRouter planRouter) {
		super(id, scenario, internalInterface);
		this.planRouter = planRouter;
		this.random = MatsimRandom.getLocalInstance();
	}

	@Override
	public boolean doReplanning(MobsimAgent withinDayAgent) {

		// If we don't have a valid personAgent
		if (withinDayAgent == null) return false;

		Plan executedPlan = this.withinDayAgentUtils.getModifiablePlan(withinDayAgent);

		// If we don't have an executed plan
		if (executedPlan == null) return false;

		/*
		 *  Get the index of the current PlanElement
		 */
		int currentPlanElementIndex = this.withinDayAgentUtils.getCurrentPlanElementIndex(withinDayAgent);
		
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
		Activity activity = factory.createActivityFromLinkId(DemoConfig.activityType, Id.create(DemoConfig.evacuationLink, Link.class));
		
		executedPlan.addLeg(leg);
		executedPlan.addActivity(activity);
		
		// set new departure time
		currentActivity.setMaximumDuration(departureTime - currentActivity.getStartTime());
		currentActivity.setEndTime(departureTime);
		leg.setDepartureTime(departureTime);
				
		// create a route for the new leg
		this.planRouter.run(executedPlan);
		
		activity.setStartTime(departureTime + leg.getTravelTime());
		
		/*
		 * Reschedule the currently performed Activity in the Mobsim - there
		 * the activityEndsList has to be updated.
		 */
//		this.withinDayAgentUtils.calculateAndSetDepartureTime(withinDayAgent, currentActivity);
		WithinDayAgentUtils.resetCaches( withinDayAgent );
		return true;
	}
}