/* *********************************************************************** *
 * project: org.matsim.*
 * ParkAndRideRoutingModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.parknride;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.core.utils.misc.RouteUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.PtConstants;
import org.matsim.pt.router.MultiNodeDijkstra;
import org.matsim.pt.router.MultiNodeDijkstra.InitialNode;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkNode;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeCost;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.thibautd.parknride.ParkAndRideRouterNetwork.ParkAndRideLink;
import playground.thibautd.router.RoutingModule;
import playground.thibautd.router.StageActivityTypes;
import playground.thibautd.router.StageActivityTypesImpl;

/**
 * A routing module for park and ride access trips. It is not meant
 * to be used in a TripRouter!
 *
 * @author thibautd
 */
public class ParkAndRideRoutingModule implements RoutingModule {
	private static final StageActivityTypes ACT_TYPES =
		new StageActivityTypesImpl(
				Arrays.asList(
					new String[]{
						PtConstants.TRANSIT_ACTIVITY_TYPE,
						ParkAndRideConstants.PARKING_ACT } ) );
	private final ParkAndRideFacilities facilities;
	private final ParkAndRideRouterNetwork routingNetwork;
	private final ParkAndRideCostAggregator timeCost;
	private final MultiNodeDijkstra leastCostPathAlgo;
	private final TransitRouterConfig transitRouterConfig;
	private final ModeRouteFactory routeFactory;
	private final PopulationFactory populationFactory;
	private final TransitRouterNetworkTravelTimeCost ttCalculator;

	public ParkAndRideRoutingModule(
			final ModeRouteFactory routeFactory,
			final PopulationFactory populationFactory,
			final Network carNetwork,
			final TransitSchedule schedule,
			final double maxBeelineWalkConnectionDistance,
			final ParkAndRideFacilities parkAndRideFacilities,
			final TransitRouterConfig transitRouterConfig,
			final PersonalizableTravelCost carCost,
			final PersonalizableTravelTime carTime,
			final TransitRouterNetworkTravelTimeCost ptTimeCost,
			final PersonalizableTravelCost pnrCost,
			final PersonalizableTravelTime pnrTime) {
		this.ttCalculator = ptTimeCost;
		this.facilities = parkAndRideFacilities;
		this.routeFactory = routeFactory;
		this.populationFactory = populationFactory;
		this.routingNetwork =
			new ParkAndRideRouterNetwork(
					carNetwork,
					schedule,
					maxBeelineWalkConnectionDistance,
					parkAndRideFacilities);
		this.timeCost =
			new ParkAndRideCostAggregator(
					carTime,
					carCost,
					ptTimeCost,
					pnrTime,
					pnrCost);
		this.leastCostPathAlgo = 
			new MultiNodeDijkstra(
					routingNetwork,
					timeCost,
					timeCost);
		this.transitRouterConfig = transitRouterConfig;
	}

	@Override
	public List<? extends PlanElement> calcRoute(
			final Facility fromFacility,
			final Facility toFacility,
			final double departureTime,
			final Person person) {
		timeCost.setPerson( person );

		// find possible start stops
		Node fromNode = this.routingNetwork.getLinks().get( fromFacility.getLinkId() ).getFromNode();
		Map<Node, InitialNode> wrappedFromNodes = new LinkedHashMap<Node, InitialNode>();
		wrappedFromNodes.put(
				fromNode,
				new InitialNode( 0 , 0 ) );

		// find possible end stops
		Coord toCoord = toFacility.getCoord();
		Collection<TransitRouterNetworkNode> toNodes =
			routingNetwork.getNearestTransitNodes(
					toCoord,
					transitRouterConfig.searchRadius);

		if (toNodes.size() < 2) {
			// also enlarge search area if only one stop found, maybe a second one is near the border of the search area
			TransitRouterNetworkNode nearestNode =
				routingNetwork.getNearestTransitNode(
						toFacility.getCoord() );
			double distance =
				CoordUtils.calcDistance(
						toCoord,
						nearestNode.stop.getStopFacility().getCoord());
			toNodes = routingNetwork.getNearestTransitNodes(
					toCoord,
					distance + transitRouterConfig.extensionRadius);
		}

		Map<Node, InitialNode> wrappedToNodes = new LinkedHashMap<Node, InitialNode>();
		for (TransitRouterNetworkNode node : toNodes) {
			double distance =
				CoordUtils.calcDistance(
						toCoord,
						node.stop.getStopFacility().getCoord());
			double initialTime = distance / transitRouterConfig.getBeelineWalkSpeed();
			double initialCost = - (initialTime * transitRouterConfig.getMarginalUtilityOfTravelTimeWalk_utl_s());
			wrappedToNodes.put(node, new InitialNode(initialCost, initialTime + departureTime));
		}

		// find routes between start and end stops
		Path p = leastCostPathAlgo.calcLeastCostPath(wrappedFromNodes, wrappedToNodes);

		if (p == null) {
			throw new RuntimeException( "no path was found! Origin node id: "+fromNode.getId()+", destination stops: "+printStops(toNodes) );
			//return null;
		}

		Coord fromCoord = fromFacility.getCoord();
		double directWalkCost =
			CoordUtils.calcDistance(fromCoord, toCoord) /
				(transitRouterConfig.getBeelineWalkSpeed() *
				 ( 0 - transitRouterConfig.getMarginalUtilityOfTravelTimeWalk_utl_s()));
		double pathCost = p.travelCost + wrappedFromNodes.get(p.nodes.get(0)).initialCost + wrappedToNodes.get(p.nodes.get(p.nodes.size() - 1)).initialCost;
		if (directWalkCost < pathCost) {
			List<Leg> legs = new ArrayList<Leg>();
			Leg leg = new LegImpl(TransportMode.transit_walk);
			double walkTime = CoordUtils.calcDistance(fromCoord, toCoord) / transitRouterConfig.getBeelineWalkSpeed();
			Route walkRoute = new GenericRouteImpl(null, null);
			leg.setRoute(walkRoute);
			leg.setTravelTime(walkTime);
			legs.add(leg);
			return legs;
		}

		return fromPathToPlanElements( departureTime, p, fromCoord, toCoord ) ;
	}

	private static String printStops(final Collection<TransitRouterNetworkNode> toNodes) {
		StringBuffer buffer = new StringBuffer();

		buffer.append( "[" );
		boolean isFirst = true;
		for (TransitRouterNetworkNode node : toNodes) {
			buffer.append( (isFirst ? "" : ", ")+node.getStop().getStopFacility() );
			isFirst = false;
		}
		buffer.append( "]" );

		return buffer.toString();
	}

	private List<? extends PlanElement> fromPathToPlanElements(
			final double departureTime,
			final Path path,
			final Coord fromCoord,
			final Coord toCoord) {
		List<PlanElement> trip = new ArrayList<PlanElement>();
		LinkIterator links = new LinkIterator( departureTime , path.links );

		trip.add( parseCarLeg( departureTime , links ) );

		trip.add( getChangeActivity( links ) );

		trip.addAll( parsePtSubTrip(
					links.current().getToNode().getCoord(),
					toCoord,
					links) );

		return trip;
	}

	private Leg parseCarLeg(
			final double departure,
			final LinkIterator links) {
		Leg leg = populationFactory.createLeg( TransportMode.car );

		List<Id> carLinks = new ArrayList<Id>();

		double dist = 0;
		while ( !(links.current() instanceof ParkAndRideRouterNetwork.ParkAndRideLink) ) {
			carLinks.add( links.current().getId() );
			dist += links.current().getLength();
			links.next();
		}

		Id from = carLinks.remove(0);
		Id to = from;
		if (carLinks.size() > 0) {
			carLinks.remove( carLinks.size() - 1 );
		}

		NetworkRoute route = (NetworkRoute) routeFactory.createRoute(
				TransportMode.car,
				from,
				to);
		route.setLinkIds(from, carLinks, to);
		route.setTravelTime( links.now() - departure );
		route.setTravelCost( links.totalCost() );
		route.setDistance( dist );
		leg.setRoute(route);
		leg.setDepartureTime( departure );
		leg.setTravelTime( links.now() - departure );

		return leg;
	}

	private Activity getChangeActivity(
			final LinkIterator links) {
		ParkAndRideLink link = (ParkAndRideLink) links.current();

		ParkAndRideFacility facility =
			facilities.getFacilities().get( link.getParkAndRideFacilityId() );
		//Activity act =
		//	populationFactory.createActivityFromLinkId(
		//			ParkAndRideConstants.PARKING_ACT,
		//			facility.getLinkId());
		ActivityImpl act = new ActivityImpl(
					ParkAndRideConstants.PARKING_ACT,
					facility.getCoord(),
					facility.getLinkId());
		act.setMaximumDuration( 0d );

		// XXX: dangerous! Not a facility from ActivityFacilities!
		act.setFacilityId( facility.getId() );

		return act;
	}

	// adapted from TransitRouterImpl.convert(...)
	private List<PlanElement> parsePtSubTrip(
			final Coord fromCoord,
			final Coord toCoord,
			final LinkIterator links) {
		double time = links.now();

		List<PlanElement> trip = new ArrayList<PlanElement>();
		Leg leg = null;

		TransitLine line = null;
		TransitRoute route = null;
		TransitStopFacility accessStop = null;
		TransitRouteStop transitRouteStart = null;
		TransitRouterNetworkLink prevLink = null;

		if ( !links.hasNext() ) {
			// it seems, the agent only walked
			trip.clear();
			leg = new LegImpl(TransportMode.transit_walk);
			double walkTime = CoordUtils.calcDistance(fromCoord, toCoord) / transitRouterConfig.getBeelineWalkSpeed();
			leg.setTravelTime(walkTime);
			trip.add(leg);
			return trip;
		}

		boolean isFirst = true;
		for (Link genericLink = links.next(); links.hasNext(); genericLink = links.next()) {
			TransitRouterNetworkLink link = (TransitRouterNetworkLink) genericLink;

			if ( isFirst ) {
				isFirst = false;
				leg = new LegImpl(TransportMode.transit_walk);
				double walkTime = CoordUtils.calcDistance(fromCoord, link.getFromNode().getCoord()) / transitRouterConfig.getBeelineWalkSpeed();
				leg.setTravelTime(walkTime);
				trip.add(leg);
				trip.add( createInteraction( link.getFromNode().getCoord() ) );
			}

			if (link.getLine() == null) {
				TransitStopFacility egressStop = link.fromNode.stop.getStopFacility();
				// it must be one of the "transfer" links. finish the pt leg, if there was one before...
				if (route != null) {
					leg = new LegImpl(TransportMode.pt);
					ExperimentalTransitRoute ptRoute =
						new ExperimentalTransitRoute(
								accessStop,
								line,
								route,
								egressStop);
					leg.setRoute(ptRoute);
					double arrivalOffset =
						(link.getFromNode().stop.getArrivalOffset() != Time.UNDEFINED_TIME) ?
						link.fromNode.stop.getArrivalOffset() :
						link.fromNode.stop.getDepartureOffset();
					double arrivalTime =
						this.ttCalculator.getNextDepartureTime(route, transitRouteStart, time) + (arrivalOffset - transitRouteStart.getDepartureOffset());
					leg.setTravelTime(arrivalTime - time);
					time = arrivalTime;
					trip.add(leg);
					accessStop = egressStop;
				}
				line = null;
				route = null;
				transitRouteStart = null;
			}
			else {
				if (link.getRoute() != route) {
					// the line changed
					TransitStopFacility egressStop = link.fromNode.stop.getStopFacility();
					if (route == null) {
						// previously, the agent was on a transfer, add the walk leg
						transitRouteStart = link.getFromNode().stop;
						if (accessStop != egressStop) {
							if (accessStop != null) {
								leg = new LegImpl(TransportMode.transit_walk);
								double walkTime = CoordUtils.calcDistance(accessStop.getCoord(), egressStop.getCoord()) / transitRouterConfig.getBeelineWalkSpeed();
								Route walkRoute = new GenericRouteImpl(accessStop.getLinkId(), egressStop.getLinkId());
								leg.setRoute(walkRoute);
								leg.setTravelTime(walkTime);
								time += walkTime;
								trip.add(leg);
							}
							else { // accessStop == null, so it must be the first walk-leg
								leg = new LegImpl(TransportMode.transit_walk);
								double walkTime = CoordUtils.calcDistance(fromCoord, egressStop.getCoord()) / transitRouterConfig.getBeelineWalkSpeed();
								leg.setTravelTime(walkTime);
								time += walkTime;
								trip.add(leg);
							}
						}
					}
					line = link.getLine();
					route = link.getRoute();
					accessStop = egressStop;
				}
			}
			prevLink = link;

			if (links.hasNext()) {
				Activity interaction = createInteraction( link.getToNode().getCoord() );
				trip.add( interaction );
			}
		}

		if (route != null) {
			// the last part of the path was with a transit route, so add the pt-leg and final walk-leg
			leg = new LegImpl(TransportMode.pt);
			TransitStopFacility egressStop = prevLink.toNode.stop.getStopFacility();
			ExperimentalTransitRoute ptRoute = new ExperimentalTransitRoute(accessStop, line, route, egressStop);
			leg.setRoute(ptRoute);
			double arrivalOffset = ((prevLink).toNode.stop.getArrivalOffset() != Time.UNDEFINED_TIME) ?
					(prevLink).toNode.stop.getArrivalOffset()
					: (prevLink).toNode.stop.getDepartureOffset();
			double arrivalTime = this.ttCalculator.getNextDepartureTime(route, transitRouteStart, time) + (arrivalOffset - transitRouteStart.getDepartureOffset());
			leg.setTravelTime(arrivalTime - time);

			trip.add(leg);
			accessStop = egressStop;
		}

		if (prevLink != null) {
			leg = new LegImpl(TransportMode.transit_walk);
			double walkTime;
			if (accessStop == null) {
				walkTime = CoordUtils.calcDistance(fromCoord, toCoord) / transitRouterConfig.getBeelineWalkSpeed();
			}
			else {
				walkTime = CoordUtils.calcDistance(accessStop.getCoord(), toCoord) / transitRouterConfig.getBeelineWalkSpeed();
			}
			leg.setTravelTime(walkTime);
			trip.add(leg);
		}

		return trip;
	}

	private final Activity createInteraction(final Coord coord) {
		Activity interact = populationFactory.createActivityFromCoord(
					PtConstants.TRANSIT_ACTIVITY_TYPE,
					coord);
		interact.setMaximumDuration( 0 );
		return interact;
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return ACT_TYPES;
	}

	private class LinkIterator {
		private final Iterator<Link> iterator;
		private Link currentElement;
		private double currentTravelTime = 0;
		private double currentTravelCost = 0;
		private double now;
		private double cost;

		public LinkIterator(
				final double departure,
				final List<Link> links) {
			iterator = links.iterator();
			currentElement = iterator.next();
			now = departure;
		}

		public boolean hasNext() {
			return iterator.hasNext();
		}

		public Link current() {
			return currentElement;
		}

		public double now() {
			return now;
		}

		public double totalCost() {
			return cost;
		}

		public double currentTravelTime() {
			return currentTravelTime;
		}

		public double currentTravelCost() {
			return currentTravelCost;
		}

		public Link next() {
			currentElement = iterator.next();
			currentTravelTime = timeCost.getLinkTravelTime( currentElement , now );
			currentTravelCost = timeCost.getLinkGeneralizedTravelCost( currentElement , now );

			now += currentTravelTime;
			cost += currentTravelCost;
			return currentElement;
		}
	}
}
