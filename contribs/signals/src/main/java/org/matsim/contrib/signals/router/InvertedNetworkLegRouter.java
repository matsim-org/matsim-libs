/* *********************************************************************** *
 * project: org.matsim.*
 * InvertedNetworkLegRouter
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
package org.matsim.contrib.signals.router;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.signals.SignalsTurnInfoBuilder;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.network.algorithms.NetworkExpandNode.TurnInfo;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.old.LegRouter;
import org.matsim.core.router.util.*;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.lanes.data.v20.Lane;
import org.matsim.lanes.data.v20.LaneDefinitions20;
import org.matsim.lanes.data.v20.LanesToLinkAssignment20;

import java.util.*;

/**
 * This leg router takes travel times needed for turning moves into account. This is done by a routing on an inverted
 * network, i.e. the links of the street networks are converted to nodes and for each turning move a link is inserted.
 * 
 * This LegRouter can only be used if the enableLinkToLinkRouting parameter in the controler config module is set and
 * AStarLandmarks routing is not enabled.
 * 
 * @author dgrether
 * 
 */
public class InvertedNetworkLegRouter implements LegRouter {

	private LeastCostPathCalculator leastCostPathCalculator = null;

	private Network invertedNetwork = null;

	private ModeRouteFactory routeFactory = null;

	private Network network = null;

	public InvertedNetworkLegRouter(Scenario sc,
			LeastCostPathCalculatorFactory leastCostPathCalcFactory,
			TravelDisutilityFactory travelCostCalculatorFactory, LinkToLinkTravelTime travelTimes) {
		PlanCalcScoreConfigGroup cnScoringGroup = sc.getConfig().planCalcScore();
		this.routeFactory = ((PopulationFactoryImpl) sc.getPopulation().getFactory())
				.getModeRouteFactory();
		this.network = sc.getNetwork();

		Map<Id<Link>, List<TurnInfo>> allowedInLinkTurnInfoMap = this.createAllowedTurnInfos(sc);
		
		NetworkInverter networkInverter = new NetworkInverter(network, allowedInLinkTurnInfoMap);
		this.invertedNetwork = networkInverter.getInvertedNetwork();

		TravelTimesInvertedNetProxy travelTimesProxy = new TravelTimesInvertedNetProxy(network, travelTimes);
		TravelDisutility travelCost = travelCostCalculatorFactory.createTravelDisutility(
				travelTimesProxy, cnScoringGroup);

		this.leastCostPathCalculator = leastCostPathCalcFactory.createPathCalculator(
				this.invertedNetwork, travelCost, travelTimesProxy);
	}

	public static RoutingModule createInvertedNetworkRouter( String mode, PopulationFactory popFact,  Scenario sc,
			LeastCostPathCalculatorFactory leastCostPathCalcFactory, TravelDisutilityFactory travelCostCalculatorFactory, LinkToLinkTravelTime travelTimes  ) {
		LegRouter toWrap = new InvertedNetworkLegRouter(sc, leastCostPathCalcFactory, travelCostCalculatorFactory, travelTimes) ;
		return new LegRouterWrapper( mode, popFact, toWrap ) ;
	}

	static Map<Id<Link>, List<TurnInfo>> createTurnInfos(LaneDefinitions20 laneDefs) {
		Map<Id<Link>, List<TurnInfo>> inLinkIdTurnInfoMap = new HashMap<>();
		Set<Id<Link>> toLinkIds = new HashSet<>();
		for (LanesToLinkAssignment20 l2l : laneDefs.getLanesToLinkAssignments().values()) {
			toLinkIds.clear();
			for (Lane lane : l2l.getLanes().values()) {
				if (lane.getToLinkIds() != null
						&& (lane.getToLaneIds() == null || lane.getToLaneIds().isEmpty())) { // make sure that it is a lane at the
																																									// end of a link
					toLinkIds.addAll(lane.getToLinkIds());
				}
			}
			if (!toLinkIds.isEmpty()) {
				List<TurnInfo> turnInfoList = new ArrayList<TurnInfo>();
				for (Id<Link> toLinkId : toLinkIds) {
					turnInfoList.add(new TurnInfo(l2l.getLinkId(), toLinkId));
				}
				inLinkIdTurnInfoMap.put(l2l.getLinkId(), turnInfoList);
			}
		}

		return inLinkIdTurnInfoMap;
	}

	private Map<Id<Link>, List<TurnInfo>> createAllowedTurnInfos(Scenario sc){
		Map<Id<Link>, List<TurnInfo>> allowedInLinkTurnInfoMap = new HashMap<>();

		NetworkTurnInfoBuilder netTurnInfoBuilder = new NetworkTurnInfoBuilder();
		netTurnInfoBuilder.createAndAddTurnInfo(TransportMode.car, allowedInLinkTurnInfoMap, this.network);

		if (sc.getConfig().scenario().isUseLanes()) {
			LaneDefinitions20 ld = (LaneDefinitions20) sc.getScenarioElement(LaneDefinitions20.ELEMENT_NAME);
			Map<Id<Link>, List<TurnInfo>> lanesTurnInfoMap = createTurnInfos(ld);
			netTurnInfoBuilder.mergeTurnInfoMaps(allowedInLinkTurnInfoMap, lanesTurnInfoMap);
		}
		if (sc.getConfig().scenario().isUseSignalSystems()) {
			SignalSystemsData ssd = ((SignalsData) sc.getScenarioElement(SignalsData.ELEMENT_NAME)).getSignalSystemsData();
			Map<Id<Link>, List<TurnInfo>> signalsTurnInfoMap = new SignalsTurnInfoBuilder().createSignalsTurnInfos(ssd);
			netTurnInfoBuilder.mergeTurnInfoMaps(allowedInLinkTurnInfoMap, signalsTurnInfoMap);
		}
		return allowedInLinkTurnInfoMap;
	}

	@Override
	public double routeLeg(Person person, Leg leg, Activity fromAct, Activity toAct,
			double departureTime) {
		double travelTime = 0.0;
		NetworkRoute route = null;
		Id<Link> fromLinkId = fromAct.getLinkId();
		Id<Link> toLinkId = toAct.getLinkId();
		if (fromLinkId == null)
			throw new RuntimeException("fromLink Id missing in Activity.");
		if (toLinkId == null)
			throw new RuntimeException("toLink Id missing in Activity.");

		if (fromLinkId.equals(toLinkId)) { // no route has to be calculated
			route = (NetworkRoute) this.routeFactory.createRoute(TransportMode.car, fromLinkId, toLinkId);
			route.setDistance(0.0);
		}
		else {
			Node fromINode = this.invertedNetwork.getNodes().get(Id.create(fromLinkId, Node.class));
			Node toINode = this.invertedNetwork.getNodes().get(Id.create(toLinkId, Node.class));
			Path path = null;
			path = this.leastCostPathCalculator.calcLeastCostPath(fromINode, toINode, departureTime, person, null);
			if (path == null)
				throw new RuntimeException("No route found on inverted network from link " + fromLinkId
						+ " to link " + toLinkId + ".");
			route = this.invertPath2NetworkRoute(path, fromLinkId, toLinkId);
			travelTime = path.travelTime;
		}

		leg.setDepartureTime(departureTime);
		leg.setTravelTime(travelTime);
		((LegImpl) leg).setArrivalTime(departureTime + travelTime);
		leg.setRoute(route);
		return travelTime;
	}

	private NetworkRoute invertPath2NetworkRoute(Path path, Id<Link> fromLinkId, Id<Link> toLinkId) {
		NetworkRoute route = (NetworkRoute) this.routeFactory.createRoute(TransportMode.car,
				fromLinkId, toLinkId);
		List<Node> nodes = path.nodes;
		// remove first and last as their ids are, due to inversion, from and to link id of the route
		nodes.remove(0);
		nodes.remove(nodes.size() - 1);
		List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
		for (Node n : nodes) {
			linkIds.add(Id.create(n.getId().toString(), Link.class));
		}
		route.setLinkIds(fromLinkId, linkIds, toLinkId);
		route.setTravelTime((int) path.travelTime);
		route.setTravelCost(path.travelCost);
		route.setDistance(RouteUtils.calcDistance(route, this.network));
		return route;
	}

}
