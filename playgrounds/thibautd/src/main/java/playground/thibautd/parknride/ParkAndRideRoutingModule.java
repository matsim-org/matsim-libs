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
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.PersonalizableTravelDisutility;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.PtConstants;
import org.matsim.pt.router.MultiNodeDijkstra;
import org.matsim.pt.router.MultiNodeDijkstra.InitialNode;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkNode;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
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
	private final TransitRouterNetworkTravelTimeAndDisutility ttCalculator;
	private final TransitSchedule transitSchedule;

	public ParkAndRideRoutingModule(
			final ModeRouteFactory routeFactory,
			final PopulationFactory populationFactory,
			final Network carNetwork,
			final TransitSchedule schedule,
			final double maxBeelineWalkConnectionDistance,
			final double pnrConnectionDistance,
			final ParkAndRideFacilities parkAndRideFacilities,
			final TransitRouterConfig transitRouterConfig,
			final PersonalizableTravelDisutility carCost,
			final PersonalizableTravelTime carTime,
			final TransitRouterNetworkTravelTimeAndDisutility ptTimeCost,
			final PersonalizableTravelDisutility pnrCost,
			final PersonalizableTravelTime pnrTime) {
		this.transitSchedule = schedule;
		this.ttCalculator = ptTimeCost;
		this.facilities = parkAndRideFacilities;
		this.routeFactory = routeFactory;
		this.populationFactory = populationFactory;
		this.routingNetwork =
			new ParkAndRideRouterNetwork(
					carNetwork,
					schedule,
					maxBeelineWalkConnectionDistance,
					pnrConnectionDistance,
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
		try {
			timeCost.setPerson( person );

			// start is unique: it is a car linkstart is unique: it is a car link
			Node fromNode = this.routingNetwork.getLinks().get( fromFacility.getLinkId() ).getToNode();
			Map<Node, InitialNode> wrappedFromNodes = new LinkedHashMap<Node, InitialNode>();
			wrappedFromNodes.put(
					fromNode,
					new InitialNode( 0 , departureTime ) );

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
				// throw new RuntimeException( "no path was found! Origin node id: "+fromNode.getId()+", destination stops: "+printStops(toNodes) );
				return null;
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

			return fromPathToPlanElements( departureTime, p, fromFacility, toFacility ) ;
		}
		catch (Exception e) {
			throw new RuntimeException( "problem in park and ride routing from "+asString( fromFacility )
					+" to "+asString( toFacility )+" at time "+departureTime+" for person "+person.getId(), e );
		}
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
			final Facility fromFacility,
			final Facility toFacility) {
		try {
			List<PlanElement> trip = new ArrayList<PlanElement>();
			LinkIterator links = new LinkIterator( departureTime , path.links );

			Leg carLeg = parseCarLeg( departureTime , links , fromFacility );

			Facility currentFromFacility = fromFacility;
			if ( carLeg != null) {
				trip.add( carLeg );
				trip.add( getChangeActivity( links ) );

				ParkAndRideLink link = (ParkAndRideLink) links.current();
				currentFromFacility = facilities.getFacilities().get( link.getParkAndRideFacilityId() );
			}

			trip.addAll(
					parsePtSubTrip(
						currentFromFacility,
						toFacility,
						links) );

			return trip;
		}
		catch (Exception e) {
			throw new RuntimeException( "error while parsing path for dep="+departureTime
					+", path="+asString( path )+", from="+fromFacility+", to="+toFacility, e);
		}
	}

	private static String asString(final Path path) {
		return "[[Nodes="+path.nodes+"][Links="+path.links+"][travelTime="+path.travelTime+"][travelCost="+path.travelCost+"]]";
	}

	private static String asString(final Facility facility) {
		return "["+facility.getClass().getSimpleName()+
			", x="+facility.getCoord().getX()+
			", y="+facility.getCoord().getY()+
			", linkId="+facility.getLinkId()+"]";
	}

	private Leg parseCarLeg(
			final double departure,
			final LinkIterator links,
			final Facility fromFacility) {
		Leg leg = populationFactory.createLeg( TransportMode.car );

		List<Id> carLinks = new ArrayList<Id>();

		double dist = 0;
		while ( !(links.current() instanceof ParkAndRideRouterNetwork.ParkAndRideLink) ) {
			carLinks.add( links.current().getId() );
			dist += links.current().getLength();
			links.next();
		}

		if (carLinks.size() == 0) {
			// the trip starts at the PT station: no car leg
			return null;
		}

		Id from = fromFacility.getLinkId();
		Id to = from;
		if (carLinks.size() > 0) {
			to = carLinks.remove( carLinks.size() - 1 );
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

	private List<PlanElement> parsePtSubTrip(
			final Facility fromFacility,
			final Facility toFacility,
			final LinkIterator links) {
		List< Leg > baseTrip = parsePtSubTripLegs( fromFacility , toFacility , links );
		List<PlanElement> trip = new ArrayList<PlanElement>();

		// the following was executed in PlansCalcTransitRoute at plan insertion.
		Leg firstLeg = baseTrip.get(0);
		Id fromLinkId = fromFacility.getLinkId();
		Id toLinkId = null;
		if (baseTrip.size() > 1) { // at least one pt leg available
			toLinkId = (baseTrip.get(1).getRoute()).getStartLinkId();
		} else {
			toLinkId = toFacility.getLinkId();
		}

		//XXX: use ModeRouteFactory instead?
		Route route = new GenericRouteImpl(fromLinkId, toLinkId);
		route.setTravelTime( firstLeg.getTravelTime() );
		firstLeg.setRoute( route );

		Leg lastLeg = baseTrip.get(baseTrip.size() - 1);
		toLinkId = toFacility.getLinkId();
		if (baseTrip.size() > 1) { // at least one pt leg available
			fromLinkId = (baseTrip.get(baseTrip.size() - 2).getRoute()).getEndLinkId();
		}

		//XXX: use ModeRouteFactory instead?
		route = new GenericRouteImpl(fromLinkId, toLinkId);
		route.setTravelTime( lastLeg.getTravelTime() );
		lastLeg.setRoute( route );

		boolean isFirstLeg = true;
		Coord nextCoord = null;
		for (Leg leg2 : baseTrip) {
			if (isFirstLeg) {
				trip.add( leg2 );
				isFirstLeg = false;
			}
			else {
				if (leg2.getRoute() instanceof ExperimentalTransitRoute) {
					ExperimentalTransitRoute tRoute = (ExperimentalTransitRoute) leg2.getRoute();
					ActivityImpl act =
						new ActivityImpl(
								PtConstants.TRANSIT_ACTIVITY_TYPE, 
								transitSchedule.getFacilities().get(tRoute.getAccessStopId()).getCoord(), 
								tRoute.getStartLinkId());
					act.setMaximumDuration(0.0);
					trip.add(act);
					nextCoord = transitSchedule.getFacilities().get(tRoute.getEgressStopId()).getCoord();
				}
				else { // walk legs don't have a coord, use the coord from the last egress point
					ActivityImpl act =
						new ActivityImpl(
								PtConstants.TRANSIT_ACTIVITY_TYPE,
								nextCoord, 
								leg2.getRoute().getStartLinkId());
					act.setMaximumDuration(0.0);
					trip.add(act);
				}

				trip.add( leg2 );
			}
		}
		return trip;
	}

	// adapted from TransitRouterImpl.convert(...)
	// returns a list of tuples leg/arrival coord of the leg
	private List<Leg> parsePtSubTripLegs(
			final Facility fromFacility,
			final Facility toFacility,
			final LinkIterator links) {
		double time = links.now();
		List<Leg> legs = new ArrayList<Leg>();
		Leg leg = null;

		if (!links.hasNext()) {
			// it seems, the agent only walked
			legs.clear();
			leg = createFullWalk( fromFacility , toFacility );
			legs.add( leg );
			return legs;
		}

		TransitLine line = null;
		TransitRoute route = null;
		TransitStopFacility accessStop = null;
		TransitRouteStop transitRouteStart = null;
		TransitRouterNetworkLink prevLink = null;

		int transitLegCount = 0;
		for (Link link = links.next(); links.hasNext(); link = links.next()) {
			TransitRouterNetworkLink l = (TransitRouterNetworkLink) link;

			if (l.getLine() == null) {
				TransitStopFacility egressStop = l.fromNode.stop.getStopFacility();

				// it must be one of the "transfer" links. finish the pt leg, if there was one before...
				if (route != null) {
					leg = new LegImpl(TransportMode.pt);
					ExperimentalTransitRoute ptRoute = new ExperimentalTransitRoute(accessStop, line, route, egressStop);
					leg.setRoute(ptRoute);
					double arrivalOffset =
						(((TransitRouterNetworkLink) link).getFromNode().stop.getArrivalOffset() != Time.UNDEFINED_TIME) ?
						((TransitRouterNetworkLink) link).fromNode.stop.getArrivalOffset() :
						((TransitRouterNetworkLink) link).fromNode.stop.getDepartureOffset();
					double arrivalTime = this.ttCalculator.getNextDepartureTime(route, transitRouteStart, time) + (arrivalOffset - transitRouteStart.getDepartureOffset());
					leg.setTravelTime(arrivalTime - time);
					time = arrivalTime;
					legs.add( leg );
					transitLegCount++;
					accessStop = egressStop;
				}
				line = null;
				route = null;
				transitRouteStart = null;
			}
			else {
				if (l.getRoute() != route) {
					// the line changed
					TransitStopFacility egressStop = l.fromNode.stop.getStopFacility();

					if (route == null) {
						// previously, the agent was on a transfer, add the walk leg
						transitRouteStart = ((TransitRouterNetworkLink) link).getFromNode().stop;
						if (accessStop != egressStop) {
							if (accessStop != null) {
								leg = new LegImpl(TransportMode.transit_walk);
								double walkTime =
									CoordUtils.calcDistance(
											accessStop.getCoord(),
											egressStop.getCoord()) /
									transitRouterConfig.getBeelineWalkSpeed();
								setWalkRoute( leg , walkTime , accessStop.getLinkId() , egressStop.getLinkId() );
								time += walkTime;
								legs.add( leg );
							}
							else { // accessStop == null, so it must be the first walk-leg
								leg = new LegImpl(TransportMode.transit_walk);
								double walkTime =
									CoordUtils.calcDistance(
											fromFacility.getCoord(),
											egressStop.getCoord()) /
									transitRouterConfig.getBeelineWalkSpeed();
								setWalkRoute( leg , walkTime , fromFacility.getLinkId() , egressStop.getLinkId() );
								time += walkTime;
								legs.add( leg );
							}
						}
					}

					line = l.getLine();;
					route = l.getRoute();
					accessStop = egressStop;
				}
			}
			prevLink = l;
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

			legs.add( leg );
			transitLegCount++;
			accessStop = egressStop;
		}

		if (prevLink != null) {
			leg = new LegImpl(TransportMode.transit_walk);
			if (accessStop == null) {
				double walkTime =
					CoordUtils.calcDistance(
							fromFacility.getCoord(),
							toFacility.getCoord()) /
					transitRouterConfig.getBeelineWalkSpeed();
				setWalkRoute( leg , walkTime , fromFacility.getLinkId() , toFacility.getLinkId() );
			}
			else {
				double walkTime =
					CoordUtils.calcDistance(
							accessStop.getCoord(),
							toFacility.getCoord()) /
					transitRouterConfig.getBeelineWalkSpeed();
				setWalkRoute( leg , walkTime , accessStop.getLinkId() , toFacility.getLinkId() );
			}
			legs.add( leg );
		}

		if ( transitLegCount == 0 ) {
			// it seems, the agent only walked
			legs.clear();
			leg = createFullWalk( fromFacility , toFacility );
			legs.add( leg );
		}

		return legs;
	}

	private Leg createFullWalk(
			final Facility fromFacility,
			final Facility toFacility) {
		Leg leg = new LegImpl(TransportMode.transit_walk);
		double walkTime =
			CoordUtils.calcDistance(
					fromFacility.getCoord(),
					toFacility.getCoord()) /
			transitRouterConfig.getBeelineWalkSpeed();
		setWalkRoute( leg , walkTime , fromFacility.getLinkId() , toFacility.getLinkId() );
		return leg;
	}

	private static void setWalkRoute(
			final Leg leg,
			final double travelTime,
			final Id fromLink,
			final Id toLink) {
		leg.setTravelTime(travelTime);
		leg.setRoute( new GenericRouteImpl( fromLink , toLink ) );
		leg.getRoute().setTravelTime( travelTime );
	}

	//private final Activity createInteraction(final Coord coord) {
	//	Activity interact = populationFactory.createActivityFromCoord(
	//				PtConstants.TRANSIT_ACTIVITY_TYPE,
	//				coord);
	//	interact.setMaximumDuration( 0 );
	//	return interact;
	//}

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
			currentTravelCost = timeCost.getLinkTravelDisutility( currentElement , now );

			now += currentTravelTime;
			cost += currentTravelCost;
			return currentElement;
		}
	}
}
