/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.wrashid.parkingSearch.withinDay_v_STRC.replanner;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.core.population.routes.NetworkRoute;

import playground.christoph.parking.core.mobsim.ParkingInfrastructure;
import playground.christoph.parking.withinday.replanner.ParkingSearchReplanner;
import playground.christoph.parking.withinday.replanner.strategy.NearestAvailableParkingSearch;
import playground.christoph.parking.withinday.replanner.strategy.RandomParkingSearch;
import playground.christoph.parking.withinday.utils.ParkingAgentsTracker;
import playground.christoph.parking.withinday.utils.ParkingRouter;

public class ParkingSearchReplannerWithStrategySwitching extends ParkingSearchReplanner {

	public ParkingSearchReplannerWithStrategySwitching(Id id, Scenario scenario, InternalInterface internalInterface, 
			ParkingAgentsTracker parkingAgentsTracker, ParkingInfrastructure parkingInfrastructure, ParkingRouter parkingRouter) {
		super(id, scenario, internalInterface, parkingAgentsTracker,parkingInfrastructure,parkingRouter);

		// TODO: also get as input plans and try to assign plans to each car leg).
		// instead of assigning a fixed category to each type of strategy,
		
		// TODO: in each iteration, 90% of agents should use strategy with highest score.
		// 10% of agents should use strategy, which has not been exectued for the longest time.
		// each strategy should be tried for k=5 times (attention: all agents switch at same time).
		// 
		
		
//		this.randomParkingSearch = new RandomParkingSearch(scenario.getNetwork());
//		this.nearestAvailableParkingSearch = new NearestAvailableParkingSearch(scenario.getNetwork(), parkingRouter, parkingInfrastructure);
	}
	
	@Override
	public boolean doReplanning(PlanBasedWithinDayAgent withinDayAgent) {
		
		/*
		 * If a parking has been selected, set it as destination and adapt the
		 * current leg, the next activity and the next leg. Otherwise just add
		 * another link to the current leg.
		 */
		Id parkingFacilityId = parkingAgentsTracker.getSelectedParking(withinDayAgent.getId());

		// if no parking facility has been set, the agent has to continue its search
		if (parkingFacilityId == null) {			
//			this.randomParkingSearch.applySearchStrategy(withinDayAgent, this.time);
//			this.nearestAvailableParkingSearch.applySearchStrategy(withinDayAgent, time);
			
			if (withinDayAgent.getId().hashCode() % 2 == 0) {
				this.randomParkingSearch.applySearchStrategy(withinDayAgent, this.time);
			} else {
				this.nearestAvailableParkingSearch.applySearchStrategy(withinDayAgent, time);
			}
		}

		/*
		 * The agent is going to stop its trip on the current link since a parking is available
		 * and has been accepted. Now ensure that the agent's plan is still valid.
		 */
		else {
			Leg leg = withinDayAgent.getCurrentLeg();

			int routeIndex = withinDayAgent.getCurrentRouteLinkIdIndex();

			NetworkRoute route = (NetworkRoute) leg.getRoute();
			
			updateAgentsPlan(withinDayAgent, parkingFacilityId, route, routeIndex);
		}

		withinDayAgent.resetCaches();
		return true;
	}

}

