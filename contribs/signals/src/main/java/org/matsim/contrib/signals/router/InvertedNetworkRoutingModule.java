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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.network.algorithms.NetworkExpandNode.TurnInfo;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.RouteFactoryImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.FacilityWrapperActivity;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.LinkToLinkTravelTime;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;

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
class InvertedNetworkRoutingModule implements RoutingModule {

	private final String mode;
	private final PopulationFactory populationFactory;
	private LeastCostPathCalculator leastCostPathCalculator = null;

	private Network invertedNetwork = null;

	private RouteFactoryImpl routeFactory = null;

	private Network network = null;

	InvertedNetworkRoutingModule(
			final String mode,
			final PopulationFactory populationFactory,
			Scenario sc,
			LeastCostPathCalculatorFactory leastCostPathCalcFactory,
			TravelDisutilityFactory travelCostCalculatorFactory, LinkToLinkTravelTime l2ltravelTimes) {
		this.mode = mode;
		this.populationFactory = populationFactory;
		PlanCalcScoreConfigGroup cnScoringGroup = sc.getConfig().planCalcScore();
		this.routeFactory = ((PopulationFactoryImpl) sc.getPopulation().getFactory())
				.getRouteFactory();
		this.network = sc.getNetwork();

		Map<Id<Link>, List<TurnInfo>> allowedInLinkTurnInfoMap = Utils.createAllowedTurnInfos(sc);
		
		this.invertedNetwork = new NetworkInverter(network, allowedInLinkTurnInfoMap).getInvertedNetwork();

		// convert l2ltravelTimes into something that can be used by the inverted network router:
		TravelTimesInvertedNetProxy travelTimesProxy = new TravelTimesInvertedNetProxy(network, l2ltravelTimes);
		// (method that takes a getLinkTravelTime( link , ...) with a link from the inverted network, converts it into links on the 
		// original network, and looks up the link2link tttime in the l2ltravelTimes data structure)
		
		TravelDisutility travelCost = travelCostCalculatorFactory.createTravelDisutility(
				travelTimesProxy);

		this.leastCostPathCalculator = leastCostPathCalcFactory.createPathCalculator(
				this.invertedNetwork, travelCost, travelTimesProxy);
	}

	public static RoutingModule createInvertedNetworkRouter( String mode, PopulationFactory popFact,  Scenario sc,
			LeastCostPathCalculatorFactory leastCostPathCalcFactory, TravelDisutilityFactory travelCostCalculatorFactory, LinkToLinkTravelTime travelTimes  ) {
		return new InvertedNetworkRoutingModule( mode, popFact, sc, leastCostPathCalcFactory, travelCostCalculatorFactory, travelTimes) ;
	}

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
			route = this.routeFactory.createRoute(NetworkRoute.class, fromLinkId, toLinkId);
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
	
	/**
	 * This looks like it is taking whatever comes out of the inverted network router and converts it to a normal route which
	 * can be given to the plan. 
	 */
	private NetworkRoute invertPath2NetworkRoute(Path path, Id<Link> fromLinkId, Id<Link> toLinkId) {
		NetworkRoute route = this.routeFactory.createRoute(NetworkRoute.class,
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
		route.setDistance(RouteUtils.calcDistanceExcludingStartEndLink(route, this.network));
		return route;
	}

	@Override
	public List<? extends PlanElement> calcRoute(
			final Facility fromFacility,
			final Facility toFacility,
			final double departureTime,
			final Person person) {
		Leg newLeg = populationFactory.createLeg( mode );
		newLeg.setDepartureTime( departureTime );

		double travTime = routeLeg(
				person,
				newLeg,
				new FacilityWrapperActivity( fromFacility ),
				new FacilityWrapperActivity( toFacility ),
				departureTime);

		// otherwise, information may be lost
		newLeg.setTravelTime( travTime );

		return Arrays.asList( newLeg );
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return EmptyStageActivityTypes.INSTANCE;
	}

}
