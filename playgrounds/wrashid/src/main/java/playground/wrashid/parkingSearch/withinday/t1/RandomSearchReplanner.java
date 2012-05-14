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

package playground.wrashid.parkingSearch.withinday.t1;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
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
	
	RandomSearchReplanner(Id id, Scenario scenario, InternalInterface internalInterface, 
			ParkingAgentsTracker parkingAgentsTracker) {
		super(id, scenario, internalInterface);
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

		// das zählen vom route index wird beim start link begonnen, d.h. letzter
		// index ist links.size()+1
		int routeIndex = withinDayAgent.getCurrentRouteLinkIdIndex();

		NetworkRoute route = (NetworkRoute) leg.getRoute();
		Id startLink = route.getStartLinkId();
		// actung: links enthält start und end link nicht!!!!!
		List<Id> links = new ArrayList<Id>(route.getLinkIds()); // create a copy
																// that can be
																// modified
		Id endLink = route.getEndLinkId();

		/*
		 * If a parking has been selected, set it as destination and adapt the
		 * current leg, the next activity and the next leg. Otherwise just add
		 * another link to the current leg.
		 * 
		 * RW comment: Der autofahrer fährt bis zum original planned parking, falls er nicht vorh
		 * -er einen parkplatz findet. 
		 */
		Id parkingFacilityId = parkingAgentsTracker.getSelectedParking(withinDayAgent.getId());

		if (parkingFacilityId == null) {

			// verlängere route, nur wenn am original planned parking angekommen.
			
			// check whether the car is at the routes
			// start link
			if (routeIndex == 0) {

				// if the route ends at the same link
				if (startLink.equals(endLink)) {
					Link l = randomNextLink(link);
					links.add(l.getId());
				} else {
					// nothing to do here since more links available in the
					// route
				}
			}
			// end link
			else if (routeIndex == links.size() + 1) {
				// rw question: is the sequence right, bzw. was geschieht oben?
				links.add(endLink);
				// in diesem moment is endLink==link.getId()
				endLink = randomNextLink(link).getId();
			}
			// link in between
			else {
				// nothing to do here since more links available in the route
			}

			// update agent's route
			route.setLinkIds(startLink, links, endLink);
		}

		else {
			// mit next leg ist der walk leg nach der nächsten parking activity gemeint.
			boolean updateNextLeg = false;

			// if the current linkId is different than the link id of the parking activity planned
	
			// rw: TODO: den fall noch abdecken, wo alte und neue parking facility auf gleichem link liegt.
			
			if (linkId != activity.getLinkId()) {
				/*
				 * move the parking activity after this leg
				 */
				
				// if old parking and new parking are on different links, update parking facility id
				ActivityFacility facility = ((ScenarioImpl) scenario).getActivityFacilities().getFacilities()
						.get(parkingFacilityId);
				activity.setCoord(facility.getCoord());
				activity.setLinkId(linkId);
				activity.setFacilityId(parkingFacilityId);

				updateNextLeg = true;

				/*
				 * as the parking has changed, we must also
				 * change the next parking when departing from the actual activity.
				 * 
				 * RW new comment: wir müssen beim letzen parking das nicht
				 * machen, weil hier geht es ja nur um nicht ausgeführte Teile
				 * des Plans.
				 * 
				 */
				for (int i = withinDayAgent.getCurrentPlanElementIndex() + 2; i < plan.getPlanElements().size(); i++) {
					PlanElement planElement = plan.getPlanElements().get(i);
					if (planElement instanceof ActivityImpl) {
						ActivityImpl a = (ActivityImpl) planElement;
						if (a.getType().equals("parking")) {
							a.setCoord(facility.getCoord());
							a.setLinkId(linkId);
							a.setFacilityId(parkingFacilityId);

							
							//es wird die abfahrts parking activity gesucht und davon sowohl der vorhergehende walk leg
							// und der anschliessende car leg repariert.
							editRoutes.replanFutureLegRoute(withinDayAgent.getSelectedPlan(), i - 1, routeAlgo);

							// update car leg from parking activity
							editRoutes.replanFutureLegRoute(withinDayAgent.getSelectedPlan(), i + 1, routeAlgo);
							break;
						}
					}
				}
			}

			// repair car route to parking (discard unused rest)
			
			// as we have found a parking on our way to the planned parking, we discard the rest
			// of the route.
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

			
			// repair walk from old parking to "work" activity
			
			// adapt next walk leg.
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
