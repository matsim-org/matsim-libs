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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;

import playground.christoph.parking.core.mobsim.ParkingInfrastructure.ParkingFacility;
import playground.christoph.parking.withinday.replanner.strategy.RandomParkingSearch;
import playground.wrashid.parkingSearch.withinDay_v_STRC.core.mobsim.ParkingInfrastructure_v2;
import playground.wrashid.parkingSearch.withinDay_v_STRC.strategies.FullParkingSearchStrategy;
import playground.wrashid.parkingSearch.withinDay_v_STRC.util.ParkingAgentsTracker_v2;

public class ParkAgentPaperStrategy implements FullParkingSearchStrategy {

	private RandomParkingSearch randomParkingSearch;
	private ParkingInfrastructure_v2 parkingInfrastructure;
	private ScenarioImpl scenarioData;
	private double distance_awareNess;
	private double distance_parking;
	private double sizeOfParkingSpace = 6.5; // in [m]
	private double minParkingExpectancyForContinueDriving = 3;

	private HashMap<Id, ParkAgent> parkAgents;
	private ParkingAgentsTracker_v2 parkingAgentsTracker;
	private WithinDayAgentUtils withinDayAgentUtils;

	public ParkAgentPaperStrategy(ParkingInfrastructure_v2 parkingInfrastructure, ScenarioImpl scenarioData,
			double distance_awareNess, double distance_parking, ParkingAgentsTracker_v2 parkingAgentsTracker) {
		this.parkingInfrastructure = parkingInfrastructure;
		this.scenarioData = scenarioData;
		this.distance_awareNess = distance_awareNess;
		this.distance_parking = distance_parking;
		this.parkingAgentsTracker = parkingAgentsTracker;
		this.randomParkingSearch = new RandomParkingSearch(scenarioData.getNetwork());
		this.parkAgents = new HashMap<Id, ParkAgentPaperStrategy.ParkAgent>();
		this.withinDayAgentUtils = new WithinDayAgentUtils();
	}

	@Override
	public void applySearchStrategy(MobsimAgent agent, double time) {
		Id currentLinkId = agent.getCurrentLinkId();
		int currentPlanElementIndex = this.withinDayAgentUtils.getCurrentPlanElementIndex(agent);

		Leg leg = this.withinDayAgentUtils.getCurrentLeg(agent);

		Link currentLink = scenarioData.getNetwork().getLinks().get(currentLinkId);

	//	if (isStillInRangeForParking(agent, currentLink)) {
	//		randomParkingSearch.applySearchStrategy(agent, time);
	//	} else {
			// try get closer to destination

			int routeIndex = this.withinDayAgentUtils.getCurrentRouteLinkIdIndex(agent);

			NetworkRoute route = (NetworkRoute) leg.getRoute();

			Id startLink = route.getStartLinkId();
			List<Id> links = new ArrayList<Id>(route.getLinkIds()); // create a
																	// copy that
																	// can be
																	// modified
			Id endLink = route.getEndLinkId();

			links.add(endLink);

			ActivityImpl nextActivity = (ActivityImpl) this.withinDayAgentUtils.getSelectedPlan(agent).getPlanElements().get(currentPlanElementIndex + 3);

		//	endLink = getLinkWhichIsClosestToDestination(currentLink, nextActivity.getCoord()).getId();

			// update agent's route
			route.setLinkIds(startLink, links, endLink);
	//	}
	}

	@Override
	public boolean acceptParking(MobsimAgent agent, Id facilityId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getStrategyName() {
		// TODO Auto-generated method stub
		return "ParkAgentPaperStrategy";
	}

	public class ParkAgent {

		boolean parkingFound = false;
		private MobsimAgent agent;
		int stage = 1;
		Id lastLinkHandled = null;
		int numberOfFreeParking = 0;
		int numberOfTotalParking = 0;
		private double time;
		private double stage3StartTime = 0;
		private WithinDayAgentUtils withinDayAgentUtils = new WithinDayAgentUtils();
		
		public ParkAgent(MobsimAgent agent) {
			this.agent = agent;
		}

		public boolean isStillInRangeForParking() {
			return false;
		}

		public void updateStage1And2() {

		}

		public int getStage() {
			return stage;
		}

		public void updateOccupancyInformation() {
			if (lastLinkHandled != agent.getCurrentLinkId()) {
				lastLinkHandled = agent.getCurrentLinkId();
				List<Id> freeParkingFacilitiesOnLink = parkingInfrastructure.getFreeParkingFacilitiesOnLink(
						agent.getCurrentLinkId(), "streetParking");

				for (Id facilityId : freeParkingFacilitiesOnLink) {
					ParkingFacility parkingFacility = ((ParkingInfrastructure_v2) parkingInfrastructure)
							.getParkingFacility(facilityId);

				//	numberOfFreeParking += parkingFacility.getFreeCapacity();
				//	numberOfTotalParking += parkingFacility.getCapacity();
				}

			}
		}

		public void handleStage1() {
			if (lastLinkHandled != agent.getCurrentLinkId()) {
				lastLinkHandled = agent.getCurrentLinkId();
				if (stage == 1 && distance_parking > getDistanceToDestination()) {
					stage = 2;
				}
			}
		}

		public double getDistanceToDestination() {
			Link currentLink = scenarioData.getNetwork().getLinks().get(agent.getCurrentLinkId());
			ActivityImpl destination = (ActivityImpl) this.withinDayAgentUtils.getSelectedPlan(agent).getPlanElements().get(this.withinDayAgentUtils.getCurrentPlanElementIndex(agent) + 3);
			return GeneralLib.getDistance(currentLink.getCoord(), destination.getCoord());
		}

		private Link getNextLinkClosestToDestination() {
			Link currentLink = scenarioData.getNetwork().getLinks().get(agent.getCurrentLinkId());
			Coord destinationCoord = ((ActivityImpl) this.withinDayAgentUtils.getSelectedPlan(agent).getPlanElements().get(this.withinDayAgentUtils.getCurrentPlanElementIndex(agent) + 3)).getCoord();
			
			Map<Id, ? extends Link> outLinks = currentLink.getToNode().getOutLinks();

			Link bestLink = null;
			double shortestDistance = Double.POSITIVE_INFINITY;

			for (Link link : outLinks.values()) {
				double distanceToDestination = GeneralLib.getDistance(destinationCoord, link.getToNode().getCoord());
				if (distanceToDestination < shortestDistance) {
					shortestDistance = distanceToDestination;
					bestLink = link;
				}
			}

			return bestLink;
		}

		public void handleStage2() {
			if (lastLinkHandled != agent.getCurrentLinkId()) {
				lastLinkHandled = agent.getCurrentLinkId();

				if (isCarAtEndOfCurrentRoute()) {
					List<Id> freeParkingFacilitiesOnLink = parkingInfrastructure.getFreeParkingFacilitiesOnLink(
							agent.getCurrentLinkId(), "streetParking");
					if (freeParkingFacilitiesOnLink.size() > 0) {
						parkingAgentsTracker.setSelectedParking(agent.getId(), freeParkingFacilitiesOnLink.get(0), false);
					} else {
						randomParkingSearch.applySearchStrategy(agent, 0);
						stage = 3;
						stage3StartTime = time;
					}
				} else {
					if (shouldContinueDriving_Stage2()) {
						// TODO: handle driving => especially, if end of
					} else {

					}
				}
			}
		}

		public void updateTime(double time) {
			this.time = time;
		}

		private boolean isCarAtEndOfCurrentRoute() {
			Leg leg = this.withinDayAgentUtils.getCurrentLeg(agent);

			int routeIndex = this.withinDayAgentUtils.getCurrentRouteLinkIdIndex(agent);

			NetworkRoute route = (NetworkRoute) leg.getRoute();

			List<Id> links = new ArrayList<Id>(route.getLinkIds()); // create a
																	// copy that
																	// can be
																	// modified

			// end link
			if (routeIndex == links.size() + 1) {
				return true;
			} else {
				return false;
			}
		}

		private boolean shouldContinueDriving_Stage2() {
			double p_unoccupied = numberOfFreeParking / numberOfTotalParking;
			double f_exp = p_unoccupied * getDistanceToDestination() / sizeOfParkingSpace;
			if (f_exp > 1 + MatsimRandom.getRandom().nextDouble() * (minParkingExpectancyForContinueDriving - 1)) {
				return true;
			} else
				return false;
		}

		public void handleStage3() {
			if (lastLinkHandled != agent.getCurrentLinkId()) {
				lastLinkHandled = agent.getCurrentLinkId();
			double maxDistanceToDestination=distance_parking + 30*(time-stage3StartTime)/60.0;
			
			// if parking available accept immediatly
			//if (){
				
			//}
			
			if (getDistanceToDestination()>maxDistanceToDestination){
				Id currentLinkId = agent.getCurrentLinkId();
				
				Leg leg = this.withinDayAgentUtils.getCurrentLeg(agent);

				//Link currentLink = this.network.getLinks().get(currentLinkId);

				int routeIndex = this.withinDayAgentUtils.getCurrentRouteLinkIdIndex(agent);

				NetworkRoute route = (NetworkRoute) leg.getRoute();

				
				Id startLink = route.getStartLinkId();
				List<Id> links = new ArrayList<Id>(route.getLinkIds()); // create a copy that can be modified
				Id endLink = route.getEndLinkId();
				
				// check whether the car is at the routes start link
				if (routeIndex == 0) {
					
					// if the route ends at the same link
					if (startLink.equals(endLink)) {
						//Link l = randomNextLink(currentLink);
						//links.add(l.getId());
						
						//log.warn("Car trip ends as the same link as it started - this should not happen since " + 
						//		"such trips should be replaced by walk trips!");
					} else {
						// nothing to do here since more links available in the route
					}
				}
				// end link
				else if (routeIndex == links.size() + 1) {
					links.add(endLink);
					//endLink = randomNextLink(currentLink).getId();
				}
				// link in between
				else {
					// nothing to do here since more links available in the route
				}
				
				// update agent's route
				route.setLinkIds(startLink, links, endLink);
				
				
				
				
				
				
				
				
				//getNextLinkClosestToDestination
			}
			
			}
		}
	}

	private ParkAgent getParkAgent(MobsimAgent agent) {
		// TODO: perhaps also check, if correct leg.
		if (!parkAgents.containsKey(agent.getId())) {
			parkAgents.put(agent.getId(), new ParkAgent(agent));
		}

		ParkAgent parkAgent = parkAgents.get(agent.getId());
		return parkAgent;
	}

	public void handleAgent(MobsimAgent agent, double time) {
		ParkAgent parkAgent = getParkAgent(agent);
		parkAgent.updateTime(time);

		if (parkAgent.getStage() == 1) {
			parkAgent.updateOccupancyInformation();
			parkAgent.handleStage1();
		} else if (parkAgent.getStage() == 2) {
			parkAgent.updateOccupancyInformation();
			parkAgent.handleStage2();
		} else if (parkAgent.getStage() == 3) {
			parkAgent.handleStage3();
		} else if (parkAgent.getStage() == 4) {

		}

	}

}
