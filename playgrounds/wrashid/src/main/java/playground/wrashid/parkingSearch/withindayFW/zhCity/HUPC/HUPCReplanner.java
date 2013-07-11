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

package playground.wrashid.parkingSearch.withindayFW.zhCity.HUPC;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioImpl;
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

	HUPCReplanner(Id id, Scenario scenario, InternalInterface internalInterface, ParkingAgentsTracker parkingAgentsTracker,
			TripRouter tripRouter) {
		super(id, scenario, internalInterface);
		this.parkingAgentsTracker = parkingAgentsTracker;
		this.tripRouter = tripRouter;
		
		this.editRoutes = new EditRoutes();
		this.random = MatsimRandom.getLocalInstance();
	}

	@Override
	public boolean doReplanning(PlanBasedWithinDayAgent withinDayAgent) {

		
		//EditPartialRoute editPartialRoute=new EditPartialRoute(scenario, routeAlgo);
		
		Plan plan = withinDayAgent.getSelectedPlan();
		
		int currentLegIndex = withinDayAgent.getCurrentPlanElementIndex();

		if (currentLegIndex == 33) {

			DebugLib.traceAgent(plan.getPerson().getId(),10);
			
			// DebugLib.traceAgent(personId);
		}
		
		Id parkingFacilityId = parkingAgentsTracker.getSelectedParking(withinDayAgent.getId());

		int firstParkingActIndex = currentLegIndex + 1;
		int firstWalkLegIndex = currentLegIndex + 2;

		ActivityImpl firstParkingAct = (ActivityImpl) plan.getPlanElements().get(firstParkingActIndex);
		firstParkingAct.setFacilityId(parkingFacilityId);
		ActivityFacility parkingFacility = ((ScenarioImpl) scenario).getActivityFacilities().getFacilities()
				.get(parkingFacilityId);
		firstParkingAct.setLinkId(parkingFacility.getLinkId());

		
		editRoutes.replanFutureLegRoute(withinDayAgent.getSelectedPlan(), firstWalkLegIndex, tripRouter);

		Integer secondParkingActIndex = getSecondParkingActIndex(withinDayAgent);
		if (!lastParkingOfDay(secondParkingActIndex)) {
			
			
			Integer secondWalkgLegIndex = secondParkingActIndex - 1;
			Integer nextCarLegIndex = secondParkingActIndex + 1;

			ActivityImpl secondParkingAct = (ActivityImpl) plan.getPlanElements().get(secondParkingActIndex);
			secondParkingAct.setFacilityId(parkingFacilityId);
			secondParkingAct.setLinkId(parkingFacility.getLinkId());

			InsertParkingActivities.updateNextParkingActivityIfNeededDuringDay(parkingAgentsTracker.getParkingInfrastructure(),
					withinDayAgent, scenario, tripRouter);
			
			editRoutes.replanFutureLegRoute(withinDayAgent.getSelectedPlan(), secondWalkgLegIndex, tripRouter);
			
			editRoutes.replanFutureLegRoute(withinDayAgent.getSelectedPlan(), nextCarLegIndex, tripRouter);
		}
		
		

		int currentLinkIndex = withinDayAgent.getCurrentRouteLinkIdIndex();
		
		
		Route preRoute = ((LegImpl) plan.getPlanElements().get(currentLegIndex)).getRoute().clone();
		//editRoutes.replanCurrentLegRoute(withinDayAgent.getSelectedPlan(), currentLegIndex, currentLinkIndex, routeAlgo, time);
		editRoutes.replanCurrentLegRoute(plan, currentLegIndex, currentLinkIndex, tripRouter, time);
		
		
		Route postRoute = ((LegImpl) plan.getPlanElements().get(currentLegIndex)).getRoute();
		
		
		
		

		withinDayAgent.resetCaches();
		return true;
	}

	private boolean lastParkingOfDay(Integer secondParkingActIndex) {
		return secondParkingActIndex == null;
	}

	private Integer getSecondParkingActIndex(PlanBasedWithinDayAgent withinDayAgent) {
		List<PlanElement> planElements = withinDayAgent.getSelectedPlan().getPlanElements();

		for (int i = withinDayAgent.getCurrentPlanElementIndex() + 2; i < planElements.size(); i++) {
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
