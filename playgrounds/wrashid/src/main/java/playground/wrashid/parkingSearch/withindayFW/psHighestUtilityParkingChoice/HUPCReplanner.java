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

package playground.wrashid.parkingSearch.withindayFW.psHighestUtilityParkingChoice;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.ActivityEndRescheduler;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
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
public class HUPCReplanner extends WithinDayDuringLegReplanner {

	private final Random random;
	private final EditRoutes editRoutes;
	private final ParkingAgentsTracker parkingAgentsTracker;
	private final TripRouter tripRouter;

	HUPCReplanner(Id id, Scenario scenario, ActivityEndRescheduler internalInterface, ParkingAgentsTracker parkingAgentsTracker,
			TripRouter tripRouter) {
		super(id, scenario, internalInterface);
		this.parkingAgentsTracker = parkingAgentsTracker;
		this.tripRouter = tripRouter;

		this.editRoutes = new EditRoutes();
		this.random = MatsimRandom.getLocalInstance();
	}

	@Override
	public boolean doReplanning(MobsimAgent withinDayAgent) {

		
		//EditPartialRoute editPartialRoute=new EditPartialRoute(scenario, routeAlgo);
		
		Plan plan = this.withinDayAgentUtils.getModifiablePlan(withinDayAgent);
		
		int currentLegIndex = this.withinDayAgentUtils.getCurrentPlanElementIndex(withinDayAgent);

		if (currentLegIndex == 33) {

			DebugLib.traceAgent(plan.getPerson().getId(),10);
			
			// DebugLib.traceAgent(personId);
		}
		
		Id parkingFacilityId = parkingAgentsTracker.getSelectedParking(withinDayAgent.getId());

		int firstParkingActIndex = currentLegIndex + 1;
		int firstWalkLegIndex = currentLegIndex + 2;

		ActivityImpl firstParkingAct = (ActivityImpl) plan.getPlanElements().get(firstParkingActIndex);
		firstParkingAct.setFacilityId(parkingFacilityId);
		ActivityFacility parkingFacility = ((MutableScenario) scenario).getActivityFacilities().getFacilities()
				.get(parkingFacilityId);
		firstParkingAct.setLinkId(parkingFacility.getLinkId());

		Leg firstWalkLeg = (Leg) plan.getPlanElements().get(firstParkingActIndex);
		this.editRoutes.relocateFutureLegRoute(firstWalkLeg, parkingFacility.getLinkId(), firstWalkLeg.getRoute().getEndLinkId(),
				plan.getPerson(), scenario.getNetwork(), tripRouter);

		Integer secondParkingActIndex = getSecondParkingActIndex(withinDayAgent);
		if (!lastParkingOfDay(secondParkingActIndex)) {
			
			Integer secondWalkgLegIndex = secondParkingActIndex - 1;
			Integer nextCarLegIndex = secondParkingActIndex + 1;

			ActivityImpl secondParkingAct = (ActivityImpl) plan.getPlanElements().get(secondParkingActIndex);
			secondParkingAct.setFacilityId(parkingFacilityId);
			secondParkingAct.setLinkId(parkingFacility.getLinkId());

			InsertParkingActivities.updateNextParkingActivityIfNeededDuringDay(parkingAgentsTracker.getParkingInfrastructure(),
					withinDayAgent, scenario, tripRouter);
			
			Leg secondWalkLeg = (Leg) plan.getPlanElements().get(secondWalkgLegIndex);
			this.editRoutes.relocateFutureLegRoute(secondWalkLeg, secondWalkLeg.getRoute().getStartLinkId(), parkingFacility.getLinkId(),
					plan.getPerson(), scenario.getNetwork(), tripRouter);
			
			Leg nextCarLeg = (Leg) plan.getPlanElements().get(nextCarLegIndex);
			this.editRoutes.relocateFutureLegRoute(nextCarLeg, parkingFacility.getLinkId(), nextCarLeg.getRoute().getEndLinkId(),
					plan.getPerson(), scenario.getNetwork(), tripRouter);
		}
		int currentLinkIndex = this.withinDayAgentUtils.getCurrentRouteLinkIdIndex(withinDayAgent);
		
		Route preRoute = ((LegImpl) plan.getPlanElements().get(currentLegIndex)).getRoute().clone();
		
		throw new RuntimeException( Gbl.PROBLEM_WITH_ACCESS_EGRESS ) ;
		
//		this.editRoutes.relocateCurrentLegRoute(this.withinDayAgentUtils.getModifiableCurrentLeg(withinDayAgent), plan.getPerson(), currentLinkIndex, 
//				parkingFacility.getLinkId(), time, scenario.getNetwork(), tripRouter);
//		
//		Route postRoute = ((LegImpl) plan.getPlanElements().get(currentLegIndex)).getRoute();
//
//		this.withinDayAgentUtils.resetCaches(withinDayAgent);
//		return true;
	}

	private boolean lastParkingOfDay(Integer secondParkingActIndex) {
		return secondParkingActIndex == null;
	}

	private Integer getSecondParkingActIndex(MobsimAgent withinDayAgent) {
		List<PlanElement> planElements = this.withinDayAgentUtils.getModifiablePlan(withinDayAgent).getPlanElements();

		for (int i = this.withinDayAgentUtils.getCurrentPlanElementIndex(withinDayAgent) + 2; i < planElements.size(); i++) {
			if (planElements.get(i) instanceof Activity) {
				Activity act = (Activity) planElements.get(i);
				if (act.getType().equalsIgnoreCase("parking")) {
					return i;
				}
			}
		}

		return null;
	}

	private Link randomNextLink(Link link) {
		List<Link> links = new ArrayList<Link>(link.getToNode().getOutLinks().values());

		int i = random.nextInt(links.size());
		return links.get(i);
	}
}
