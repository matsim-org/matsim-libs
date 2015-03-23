/* *********************************************************************** *
 * project: org.matsim.*
 * SwitchToWalk2DLegReplanner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.christoph.icem2012;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.ActivityEndRescheduler;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.facilities.ActivityFacility;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplanner;

import playground.christoph.evacuation.analysis.CoordAnalyzer;

/*
 * Walk legs inside the affected area are converted to walk2d legs.
 */
public class SwitchToWalk2DLegReplanner extends WithinDayDuringLegReplanner {

	private final CoordAnalyzer coordAnalyzer;

	/*package*/ SwitchToWalk2DLegReplanner(Id id, Scenario scenario, ActivityEndRescheduler internalInterface,
			CoordAnalyzer coordAnalyzer) {
		super(id, scenario, internalInterface);
		this.coordAnalyzer = coordAnalyzer;
	}

	@Override
	public boolean doReplanning(MobsimAgent withinDayAgent) {

		// If we don't have a valid PersonAgent
		if (withinDayAgent == null) return false;

		Plan executedPlan = this.withinDayAgentUtils.getModifiablePlan(withinDayAgent);

		// If we don't have an executed plan
		if (executedPlan == null) return false;

		int currentLegIndex = this.withinDayAgentUtils.getCurrentPlanElementIndex(withinDayAgent);

		// for walk2d legs: switch mode to walk for routing
		Leg currentLeg = this.withinDayAgentUtils.getModifiableCurrentLeg(withinDayAgent);
		boolean isWalk = currentLeg.getMode().equals(TransportMode.walk);
		
		// if it is a walk leg and the current link is affected, switch to walk 2d
		if (isWalk) {
			Id currentLinkId = withinDayAgent.getCurrentLinkId();
			Link currentLink = scenario.getNetwork().getLinks().get(currentLinkId);
			boolean isAffected = this.coordAnalyzer.isLinkAffected(currentLink);
			
			/*
			 * Some rescue links cross the affected area. We ignore them since they are at the and
			 * of the agents route.
			 */
			boolean isRescueLink = currentLinkId.toString().contains("rescue");
			
			if (!isRescueLink && isAffected) {
				Plan plan = this.withinDayAgentUtils.getModifiablePlan(withinDayAgent);
				NetworkRoute currentRoute = (NetworkRoute) currentLeg.getRoute();
				
				NetworkRoute subRoute = currentRoute.getSubRoute(currentRoute.getStartLinkId(), currentLinkId); 
				currentLeg.setRoute(subRoute);
				
				Coord coord = currentLink.getToNode().getCoord();	// links toNode coordinate
				XORShiftRandom xor = new XORShiftRandom(plan.getPerson().getId().hashCode());
				double x = coord.getX() + xor.nextDouble() - 1.0;	// +/- 0.5m
				double y = coord.getY() + xor.nextDouble() - 1.0;	// +/- 0.5m
				ActivityImpl switchActivity = (ActivityImpl) scenario.getPopulation().getFactory().createActivityFromLinkId("switchWalkMode", currentLinkId);
				switchActivity.setMaximumDuration(0.0);
				switchActivity.setCoord(scenario.createCoord(x, y));
				switchActivity.setEndTime(time);
				Id<ActivityFacility> facilityId = Id.create("switchWalkModeFacility" + currentLinkId.toString(), ActivityFacility.class);
				switchActivity.setFacilityId(facilityId);

				ActivityImpl nextActivity = (ActivityImpl) plan.getPlanElements().get(currentLegIndex + 1);
				Id nextFacilityId = nextActivity.getFacilityId();
				
				/*
				 * If the next activity is performed at the rescue facility we have to
				 * relocate it since it cannot be reached by walk2d. Therefore, we let
				 * the leg end at the network's last non-rescue link.
				 */
				Id<Link> endLinkId = currentRoute.getEndLinkId();
				if (nextFacilityId.equals(Id.create("rescueFacility", ActivityFacility.class))) {
					endLinkId = currentRoute.getLinkIds().get(currentRoute.getLinkIds().size() - 2);
					nextActivity.setFacilityId(Id.create("rescueFacility" + endLinkId.toString(), ActivityFacility.class));
					nextActivity.setLinkId(endLinkId);					
				}
				
				Leg walk2DLeg = scenario.getPopulation().getFactory().createLeg("walk2d");
				walk2DLeg.setDepartureTime(time);
				NetworkRoute subRoute2 = currentRoute.getSubRoute(withinDayAgent.getCurrentLinkId(), endLinkId);
				walk2DLeg.setRoute(subRoute2);
				
				plan.getPlanElements().add(currentLegIndex + 1, switchActivity);
				plan.getPlanElements().add(currentLegIndex + 2, walk2DLeg);
				
				// Finally reset the cached Values of the PersonAgent - they may have changed!
				this.withinDayAgentUtils.resetCaches(withinDayAgent);
			}
		}

		return true;
	}

}