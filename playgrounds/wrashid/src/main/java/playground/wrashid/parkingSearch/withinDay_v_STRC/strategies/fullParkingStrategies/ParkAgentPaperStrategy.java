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
package playground.wrashid.parkingSearch.withinDay_v_STRC.strategies.fullParkingStrategies;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;

import playground.christoph.parking.withinday.replanner.strategy.RandomParkingSearch;
import playground.wrashid.parkingSearch.withinDay_v_STRC.core.mobsim.ParkingInfrastructure_v2;
import playground.wrashid.parkingSearch.withinDay_v_STRC.strategies.FullParkingSearchStrategy;

public class ParkAgentPaperStrategy implements FullParkingSearchStrategy {

	private RandomParkingSearch randomParkingSearch;
	private ParkingInfrastructure_v2 parkingInfrastructure;
	private ScenarioImpl scenarioData;

	public ParkAgentPaperStrategy(ParkingInfrastructure_v2 parkingInfrastructure, ScenarioImpl scenarioData) {
		this.parkingInfrastructure = parkingInfrastructure;
		this.scenarioData = scenarioData;
		randomParkingSearch = new RandomParkingSearch(scenarioData.getNetwork());
	}
	
	@Override
	public void applySearchStrategy(PlanBasedWithinDayAgent agent, double time) {
		if (isStillInRangeForParking(agent)){
			randomParkingSearch.applySearchStrategy(agent, time);
		} else {
			// try get closer to destination			
			
			Id currentLinkId = agent.getCurrentLinkId();
			
			Leg leg = agent.getCurrentLeg();

			Link currentLink = scenarioData.getNetwork().getLinks().get(currentLinkId);

			int routeIndex = agent.getCurrentRouteLinkIdIndex();

			NetworkRoute route = (NetworkRoute) leg.getRoute();

			
			Id startLink = route.getStartLinkId();
			List<Id> links = new ArrayList<Id>(route.getLinkIds()); // create a copy that can be modified
			Id endLink = route.getEndLinkId();
			
			links.add(endLink);
			endLink = getLinkWhichIsClosestToDestination(currentLink).getId();
			
			// update agent's route
			route.setLinkIds(startLink, links, endLink);
		}
	}

	private Link getLinkWhichIsClosestToDestination(Link currentLink) {
		Map<Id, ? extends Link> outLinks = currentLink.getToNode().getOutLinks();

		Link bestLink=null;
		double distance=0;
		
		for (Link link:outLinks.values()){
				
		}
		
		return null;
	}

	private boolean isStillInRangeForParking(PlanBasedWithinDayAgent agent) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean acceptParking(PlanBasedWithinDayAgent agent, Id facilityId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getStrategyName() {
		// TODO Auto-generated method stub
		return null;
	}

	

}

