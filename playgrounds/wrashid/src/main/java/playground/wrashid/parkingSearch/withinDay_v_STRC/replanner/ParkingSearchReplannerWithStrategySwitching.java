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

import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.population.routes.NetworkRoute;

import playground.christoph.parking.core.mobsim.ParkingInfrastructure;
import playground.christoph.parking.withinday.replanner.ParkingSearchReplanner;
import playground.christoph.parking.withinday.replanner.strategy.ParkingSearchStrategy;
import playground.christoph.parking.withinday.utils.ParkingAgentsTracker;
import playground.christoph.parking.withinday.utils.ParkingRouter;
import playground.wrashid.parkingSearch.withinDay_v_STRC.strategies.FullParkingSearchStrategy;
import playground.wrashid.parkingSearch.withinDay_v_STRC.util.ParkingAgentsTracker_v2;

public class ParkingSearchReplannerWithStrategySwitching extends ParkingSearchReplanner {

	private LinkedList<ParkingSearchStrategy> strategies;

	public ParkingSearchReplannerWithStrategySwitching(Id id, Scenario scenario, InternalInterface internalInterface, 
			ParkingAgentsTracker parkingAgentsTracker, ParkingInfrastructure parkingInfrastructure, ParkingRouter parkingRouter, LinkedList<ParkingSearchStrategy> strategies) {
		super(id, scenario, internalInterface, parkingAgentsTracker,parkingInfrastructure,parkingRouter);
		this.strategies = strategies;

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
	public boolean doReplanning(MobsimAgent withinDayAgent) {
		
		ParkingAgentsTracker_v2 parkingAgentsTracker_v2= (ParkingAgentsTracker_v2) parkingAgentsTracker;
		
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
			
			FullParkingSearchStrategy parkingStrategyForCurrentLeg = parkingAgentsTracker_v2.getParkingStrategyManager().getParkingStrategyForCurrentLeg(withinDayAgent);
			
			parkingStrategyForCurrentLeg.applySearchStrategy(withinDayAgent, time);
			/*
			if (withinDayAgent.getId().hashCode() % 2 == 0) {
				strategies.get(0).applySearchStrategy(withinDayAgent, this.time);
			//	this.randomParkingSearch.applySearchStrategy(withinDayAgent, this.time);
			} else {
				strategies.get(1).applySearchStrategy(withinDayAgent, this.time);
				//this.nearestAvailableParkingSearch.applySearchStrategy(withinDayAgent, time);
			}
			*/
		}

		/*
		 * The agent is going to stop its trip on the current link since a parking is available
		 * and has been accepted. Now ensure that the agent's plan is still valid.
		 */
		else {
			Leg leg = this.withinDayAgentUtils.getCurrentLeg(withinDayAgent);

			int routeIndex = this.withinDayAgentUtils.getCurrentRouteLinkIdIndex(withinDayAgent);

			NetworkRoute route = (NetworkRoute) leg.getRoute();
			
			updateAgentsPlan(withinDayAgent, parkingFacilityId, route, routeIndex);
		}

		this.withinDayAgentUtils.resetCaches(withinDayAgent);
		return true;
	}

}

