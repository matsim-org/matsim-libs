/* *********************************************************************** *
 * project: org.matsim.*
 * RandomSearchReplanner.java
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

package playground.wrashid.parkingSearch.withinday;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.ptproject.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplanner;
import org.matsim.withinday.utils.EditRoutes;

/**
 * 
 * @author cdobler
 */
public class RandomSearchReplanner extends WithinDayDuringLegReplanner {

	private final Random random;
	private final EditRoutes editRoutes;
	private final ParkingAgentsTracker parkingAgentsTracker;
	
	/*package*/ RandomSearchReplanner(Id id, Scenario scenario, ParkingAgentsTracker parkingAgentsTracker) {
		super(id, scenario);
		this.parkingAgentsTracker = parkingAgentsTracker;
		
		this.editRoutes = new EditRoutes();
		this.random = MatsimRandom.getLocalInstance();
	}

	@Override
	public boolean doReplanning(PlanBasedWithinDayAgent withinDayAgent) {
		
		Plan plan = withinDayAgent.getSelectedPlan();
		
		Leg leg = withinDayAgent.getCurrentLeg();
			
		ActivityImpl activity = (ActivityImpl) withinDayAgent.getNextPlanElement();
		Id linkId = withinDayAgent.getCurrentLinkId();
		Link link = scenario.getNetwork().getLinks().get(linkId);
		
		int routeIndex = withinDayAgent.getCurrentRouteLinkIdIndex();
		
		NetworkRoute route = (NetworkRoute) leg.getRoute();
		Id startLink = route.getStartLinkId();
		List<Id> links = new ArrayList<Id>(route.getLinkIds());	// create a copy that can be modified
		Id endLink = route.getEndLinkId();
		
		/*
		 * If a parking has been selected, set it as destination and adapt
		 * the current leg, the next activity and the next leg.
		 * Otherwise just add another link to the current leg. 
		 */
		Id parkingFacilityId = parkingAgentsTracker.getSelectedParking(withinDayAgent.getId());
		
		if (parkingFacilityId == null) {

			// check whether the car is at the routes end		
			// start link
			if (routeIndex == 0) {
				
				// if the route ends at the same link
				if (startLink.equals(endLink)) {					
					Link l = randomNextLink(link);
					links.add(l.getId());
				}
				else {
					// nothing to do here since more links available in the route
				}
			}
			// end link
			else if(routeIndex == links.size() + 1) {
				links.add(endLink);
				endLink = randomNextLink(link).getId();
			}
			// link in between
			else {
				// nothing to do here since more links available in the route
			}
			
			// update agent's route
			route.setLinkIds(startLink, links, endLink);
		} 
		
		// park vehicle at the current link
		else {
			// get parking facility
			Id facilityId = this.parkingAgentsTracker.getSelectedParking(withinDayAgent.getId());
			
			boolean updateNextLeg = false;
			
			// if the linkId has changed since the parking activity has been planned
			if (linkId != activity.getLinkId()) {
				/*
				 * move the parking activity after this leg
				 */
				ActivityFacility facility = ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().get(facilityId);
				activity.setCoord(facility.getCoord());
				activity.setLinkId(linkId);
				activity.setFacilityId(facilityId);
				
				updateNextLeg = true;
				
				/*
				 * if the next parking activity is not the agent's last 
				 * parking activity, we have to update the next parking
				 * activity too, since the car will not be at the facility
				 * initially scheduled.
				 */
				for (int i = withinDayAgent.getCurrentPlanElementIndex() + 2; i < plan.getPlanElements().size(); i++) {
					PlanElement planElement = plan.getPlanElements().get(i); 
					if (planElement instanceof ActivityImpl) {
						ActivityImpl a = (ActivityImpl) planElement;
						if (a.getType().equals("parking")) {
							a.setCoord(facility.getCoord());
							a.setLinkId(linkId);
							a.setFacilityId(facilityId);
							
							// update walk leg to parking activity
							editRoutes.replanFutureLegRoute(withinDayAgent.getSelectedPlan(), i - 1, routeAlgo);
							
							// update car leg from parking activity
							editRoutes.replanFutureLegRoute(withinDayAgent.getSelectedPlan(), i + 1, routeAlgo);
						}
					}
				}
			}
			
			// if the agent is at not the end of its route, the route has to be adapted
			if (linkId != route.getEndLinkId()) {
				// set the current link as the route's end link
				endLink = linkId;
				
				// update agent's route
				route.setLinkIds(startLink, links, endLink);
				
				// update agent's route
				editRoutes.replanCurrentLegRoute(plan, withinDayAgent.getCurrentPlanElementIndex(), 
						withinDayAgent.getCurrentRouteLinkIdIndex(), routeAlgo, this.time);
				
				updateNextLeg = true;
			}
			
			// new Route for next Leg
			if (updateNextLeg) {
				editRoutes.replanFutureLegRoute(withinDayAgent.getSelectedPlan(), 
						withinDayAgent.getCurrentPlanElementIndex() + 2, routeAlgo);
			}
		}
			
		withinDayAgent.resetCaches();
		return true;
	}

	private Link randomNextLink(Link link) {
		List<Link> links = new ArrayList<Link>(link.getToNode().getOutLinks().values());
		
		int i = random.nextInt(links.size());
		return links.get(i);
	}
}
