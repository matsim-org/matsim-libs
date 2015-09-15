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
package playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies;

import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;

import playground.wrashid.lib.obj.TwoHashMapsConcatenated;
import playground.wrashid.parkingChoice.trb2011.ParkingHerbieControler;
import playground.wrashid.parkingSearch.ppSim.jdepSim.AgentWithParking;
import playground.wrashid.parkingSearch.ppSim.jdepSim.Message;
import playground.wrashid.parkingSearch.ppSim.jdepSim.routing.EditRoute;
import playground.wrashid.parkingSearch.ppSim.jdepSim.routing.threads.RerouteTask;
import playground.wrashid.parkingSearch.ppSim.jdepSim.routing.threads.RerouteTaskAddLastPartToRoute;
import playground.wrashid.parkingSearch.ppSim.jdepSim.routing.threads.RerouteTaskWholeRoute;
import playground.wrashid.parkingSearch.ppSim.jdepSim.routing.threads.RerouteThreadPool;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.axhausenPolak1989.AxPo1989_Strategy3;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.manager.EvaluationContainer;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.manager.StrategyEvaluation;
import playground.wrashid.parkingSearch.ppSim.jdepSim.zurich.ZHScenarioGlobal;

// this can be thought as street parking search, but with consideration for tolled
public class AvoidRoutingThroughTolledArea extends RandomParkingSearch {

	private static final Logger log = Logger.getLogger(AvoidRoutingThroughTolledArea.class);

	protected HashSet<Id> changedRoute;
	// person, legIndex
	public static TwoHashMapsConcatenated<Id, Integer, LinkNetworkRouteImpl> routes;

	public AvoidRoutingThroughTolledArea(double maxDistance, Network network, String name) {
		super(maxDistance, network, name);
		this.parkingType = "streetParking";
		setSearchBeta(-1.0);
	}

	public static void initRoutes() {
		routes = new TwoHashMapsConcatenated<Id, Integer, LinkNetworkRouteImpl>();

		log.info("starting preparing routes");
		RerouteThreadPool rtPool = new RerouteThreadPool(ZHScenarioGlobal.numberOfRoutingThreadsAtBeginning, Message.ttMatrix,
				ZHScenarioGlobal.scenario.getNetwork());

		for (Person p : ZHScenarioGlobal.scenario.getPopulation().getPersons().values()) {
			Plan selectedPlan = p.getSelectedPlan();
			List<PlanElement> planElements = selectedPlan.getPlanElements();

			int i = 0;
			while (i < planElements.size()) {
				if (planElements.get(i) instanceof LegImpl && i >= 3) {
					Activity prevAct = (Activity) planElements.get(i - 3);
					Leg leg = (Leg) planElements.get(i);
					if (leg.getMode().equalsIgnoreCase(TransportMode.car)) {
						LinkNetworkRouteImpl route = (LinkNetworkRouteImpl) leg.getRoute();
						
						if (p.getId().toString().equalsIgnoreCase("504")) {
							DebugLib.emptyFunctionForSettingBreakPoint();
						}
						
						if (startAndEndLinkNotInTolledArea(route)) {
							if (anyRouteLinkInTolledArea(route)) {
								rtPool.addTask(new RerouteTaskWholeRoute(prevAct.getEndTime(), route.getStartLinkId(), route
										.getEndLinkId(), i, p.getId()));
							}
						}
					}
				}
				i++;
			}
		}

		rtPool.start();

		for (RerouteTask rt : rtPool.rerouteTasks) {
			RerouteTaskWholeRoute rtwr = (RerouteTaskWholeRoute) rt;
			LinkNetworkRouteImpl route = rtwr.route;

			if (route == null) {
				DebugLib.emptyFunctionForSettingBreakPoint();
			}
			
			if (rtwr.personId.toString().equalsIgnoreCase("2580")) {
				DebugLib.emptyFunctionForSettingBreakPoint();
			}

			routes.put(rtwr.personId, rtwr.legIndex, route);
		}

		log.info("end preparing routes");
	}

	public void resetForNewIteration() {
		super.resetForNewIteration();
		changedRoute = new HashSet<Id>();
	}

	@Override
	public void handleAgentLeg(AgentWithParking aem) {
		if (ZHScenarioGlobal.loadDoubleParam("radiusTolledArea") > 0) {
			Id personId = aem.getPerson().getId();
			if (!changedRoute.contains(personId)) {
				changedRoute.add(personId);
				Leg leg = (LegImpl) aem.getPerson().getSelectedPlan().getPlanElements().get(aem.getPlanElementIndex());
				LinkNetworkRouteImpl route = (LinkNetworkRouteImpl) leg.getRoute();

				if (startAndEndLinkNotInTolledArea(route)) {
					if (anyRouteLinkInTolledArea(route)) {
						// EditRoute.globalEditRoute.getRoute(aem.getMessageArrivalTime(),
						// route.getStartLinkId(),
						// route.getEndLinkId());

						LinkNetworkRouteImpl newRoute = routes.get(aem.getPerson().getId(), aem.getPlanElementIndex());
						TwoHashMapsConcatenated<Id, Integer, LinkNetworkRouteImpl> routes2 = routes;
						if (newRoute == null) {
							DebugLib.emptyFunctionForSettingBreakPoint();
						}

						leg.setRoute(newRoute);
					}
				}
			}
		}
		super.handleAgentLeg(aem);
	}

	private static boolean anyRouteLinkInTolledArea(LinkNetworkRouteImpl route) {
		Coord coordinatesLindenhofZH = ParkingHerbieControler.getCoordinatesLindenhofZH();
		for (Id linkId : route.getLinkIds()) {
			Link link = ZHScenarioGlobal.scenario.getNetwork().getLinks().get(linkId);
			if (GeneralLib.getDistance(link.getCoord(), coordinatesLindenhofZH) < ZHScenarioGlobal
					.loadDoubleParam("radiusTolledArea")) {
				return true;
			}
		}
		return false;
	}

	private static boolean startAndEndLinkNotInTolledArea(LinkNetworkRouteImpl route) {
		Coord coordinatesLindenhofZH = ParkingHerbieControler.getCoordinatesLindenhofZH();
		Link startLink = ZHScenarioGlobal.scenario.getNetwork().getLinks().get(route.getStartLinkId());
		Link endLink = ZHScenarioGlobal.scenario.getNetwork().getLinks().get(route.getEndLinkId());
		return GeneralLib.getDistance(startLink.getCoord(), coordinatesLindenhofZH) > ZHScenarioGlobal
				.loadDoubleParam("radiusTolledArea")
				&& GeneralLib.getDistance(endLink.getCoord(), coordinatesLindenhofZH) > ZHScenarioGlobal
						.loadDoubleParam("radiusTolledArea");
	}

	@Override
	public void handleParkingDepartureActivity(AgentWithParking aem) {
		super.handleParkingDepartureActivity(aem);
		changedRoute.remove(aem.getPerson().getId());
	}

}
