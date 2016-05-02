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

package playground.wrashid.parkingSearch.withindayFW.randomTestStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.ActivityEndRescheduler;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.facilities.ActivityFacility;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplanner;
import org.matsim.withinday.utils.EditRoutes;

import playground.wrashid.parkingSearch.withindayFW.core.InsertParkingActivities;
import playground.wrashid.parkingSearch.withindayFW.core.ParkingAgentsTracker;

/**
 * 
 * @author cdobler, wrashid
 */
public class RandomSearchReplanner extends WithinDayDuringLegReplanner {

	private final Random random;
	private final EditRoutes editRoutes;
	private final ParkingAgentsTracker parkingAgentsTracker;
	private final TripRouter tripRouter;
	
	RandomSearchReplanner(Id id, Scenario scenario, ActivityEndRescheduler internalInterface, 
			ParkingAgentsTracker parkingAgentsTracker, TripRouter tripRouter) {
		super(id, scenario, internalInterface);
		this.parkingAgentsTracker = parkingAgentsTracker;
		this.tripRouter = tripRouter;
		
		this.editRoutes = new EditRoutes();
		this.random = MatsimRandom.getLocalInstance();
	}

	@Override
	public boolean doReplanning(MobsimAgent withinDayAgent) {

		if (this.withinDayAgentUtils.getCurrentPlanElementIndex(withinDayAgent) == 3){
			DebugLib.traceAgent(withinDayAgent.getId(), 6);
		}
		
		//EditPartialRoute editPartialRoute=new EditPartialRoute(scenario, routeAlgo);
		
		Plan plan = this.withinDayAgentUtils.getModifiablePlan(withinDayAgent);

		Leg leg = this.withinDayAgentUtils.getModifiableCurrentLeg(withinDayAgent);

		int currentPlanElementIndex = this.withinDayAgentUtils.getCurrentPlanElementIndex(withinDayAgent);
		ActivityImpl activity = (ActivityImpl) this.withinDayAgentUtils.getModifiablePlan(withinDayAgent).getPlanElements().get(currentPlanElementIndex + 1);
		Id linkId = withinDayAgent.getCurrentLinkId();
		Link link = scenario.getNetwork().getLinks().get(linkId);

		int routeIndex = this.withinDayAgentUtils.getCurrentRouteLinkIdIndex(withinDayAgent);

		NetworkRoute route = (NetworkRoute) leg.getRoute();
		Id<Link> startLink = route.getStartLinkId();
		List<Id<Link>> links = new ArrayList<Id<Link>>(route.getLinkIds()); // create a copy
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

		else {
			// mit next leg ist der walk leg nach der nächsten parking activity gemeint.
			boolean updateNextLeg = false;

			// if the current linkId is different than the parking activity planned
	
			boolean parkingFacilityChanged = linkId != activity.getLinkId();
			Integer nextParkingLegIndex=null;
			if (parkingFacilityChanged) {
				/*
				 * move the parking activity after this leg
				 */
				ActivityFacility facility = ((MutableScenario) scenario).getActivityFacilities().getFacilities()
						.get(parkingFacilityId);
				activity.setCoord(facility.getCoord());
				activity.setLinkId(linkId);
				activity.setFacilityId(parkingFacilityId);

				updateNextLeg = true;
				
				for (int i = this.withinDayAgentUtils.getCurrentPlanElementIndex(withinDayAgent) + 2; i < plan.getPlanElements().size(); i++) {
					PlanElement planElement = plan.getPlanElements().get(i);
					if (planElement instanceof ActivityImpl) {
						ActivityImpl a = (ActivityImpl) planElement;
						if (a.getType().equals("parking")) {
							a.setCoord(facility.getCoord());
							a.setLinkId(linkId);
							a.setFacilityId(parkingFacilityId);

							nextParkingLegIndex=i;
							break;
						}
					}
				}
			}
			
			
				/*
				 * as the parking has changed, we must also
				 * change the next parking when departing from the actual activity.
				 * 
				 * RW new comment: wir müssen beim letzen parking das nicht
				 * machen, weil hier geht es ja nur um nicht ausgeführte Teile
				 * des Plans.
				 */
				InsertParkingActivities.updateNextParkingActivityIfNeededDuringDay(parkingAgentsTracker.getParkingInfrastructure()  , withinDayAgent , scenario, tripRouter);
							
				if (nextParkingLegIndex!=null) {
					
					// update walk leg to parking activity
					Leg previousLeg = (Leg) plan.getPlanElements().get(nextParkingLegIndex - 1);
					this.editRoutes.relocateFutureLegRoute(previousLeg, previousLeg.getRoute().getStartLinkId(), 
							linkId, plan.getPerson(), scenario.getNetwork(), tripRouter);

					// update car leg from parking activity
					Leg nextLeg = (Leg) plan.getPlanElements().get(nextParkingLegIndex + 1);
					this.editRoutes.relocateFutureLegRoute(nextLeg, linkId, previousLeg.getRoute().getEndLinkId(), 
							plan.getPerson(), scenario.getNetwork(), tripRouter);					
				}

			// as we have found a parking on our way to the planned parking, we discard the rest
			// of the route.
			if (linkId != route.getEndLinkId()) {
				// set the current link as the route's end link
				endLink = linkId;

				// update agent's route
				//route.setLinkIds(startLink, links, endLink);
				
				// update agent's route
//				editRoutes.replanCurrentLegRoute(plan, withinDayAgent.getCurrentPlanElementIndex(),
//						withinDayAgent.getCurrentRouteLinkIdIndex(), routeAlgo, this.time);
				
				throw new RuntimeException( Gbl.PROBLEM_WITH_ACCESS_EGRESS ) ;

//				this.editRoutes.relocateCurrentLegRoute(leg, plan.getPerson(), 
//						this.withinDayAgentUtils.getCurrentRouteLinkIdIndex(withinDayAgent), endLink, time, scenario.getNetwork(), tripRouter);			
//				
//				updateNextLeg = true;
			}

			// update walk leg away from arriving parking act
			Leg nextLeg = (Leg) plan.getPlanElements().get(this.withinDayAgentUtils.getCurrentPlanElementIndex(withinDayAgent) + 2);
			this.editRoutes.relocateFutureLegRoute(nextLeg, endLink, nextLeg.getRoute().getEndLinkId(), 
					plan.getPerson(), scenario.getNetwork(), tripRouter);	
		}

		this.withinDayAgentUtils.resetCaches(withinDayAgent);
		return true;
	}

	private Link randomNextLink(Link link) {
		List<Link> links = new ArrayList<Link>(link.getToNode().getOutLinks().values());

		int i = random.nextInt(links.size());
		return links.get(i);
	}
}
