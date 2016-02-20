/* *********************************************************************** *
 * project: org.matsim.*
 * TransitWithMultiModalAccessRoutingModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package eu.eunoiaproject.bikesharing.framework.router;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.CompositeStageActivityTypes;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.Facility;
import org.matsim.pt.PtConstants;
import org.matsim.pt.router.MultiNodeDijkstra;
import org.matsim.pt.router.MultiNodeDijkstra.InitialNode;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkNode;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
import org.matsim.pt.router.TransitTravelDisutility;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * A {@link RoutingModule} that allows agents to choose between bike sharing
 * and walk to go to their departure stops.
 *
 * @author thibautd
 */
public class TransitMultiModalAccessRoutingModule implements RoutingModule {

	private final MultiNodeDijkstra dijkstra;
	private final TransitTravelDisutility travelDisutility;
	private final TravelTime travelTime;

	private final Collection<InitialNodeRouter> routers;

	private final RoutingData data;

	private final Random random = MatsimRandom.getLocalInstance();
	private final double initialNodeProportion; 

	private static enum Direction {access, egress;}

	public static final String DEPARTURE_ACTIVITY_TYPE = "multimodalTransitDeparture";

	/**
	 * @param initialNodeProportion the proportion of "initial nodes" to pass to the routing algorithm.
	 * This allows some randomness in the choice of the initial nodes.
	 */
	public TransitMultiModalAccessRoutingModule(
			final double initialNodeProportion,
			final RoutingData data,
			final InitialNodeRouter... routers) {
		this( initialNodeProportion , data , Arrays.asList( routers ) );
	}

	public TransitMultiModalAccessRoutingModule(
			final double initialNodeProportion,
			final RoutingData data,
			final Collection<InitialNodeRouter> routers) {
		if ( initialNodeProportion <= 0 || initialNodeProportion > 1 ) throw new IllegalArgumentException( ""+initialNodeProportion );
		this.initialNodeProportion = initialNodeProportion;
		this.data = data;
		TransitRouterNetworkTravelTimeAndDisutility transitRouterNetworkTravelTimeAndDisutility = new TransitRouterNetworkTravelTimeAndDisutility(data.config, data.preparedTransitSchedule);
		this.travelTime = transitRouterNetworkTravelTimeAndDisutility;
		this.travelDisutility = transitRouterNetworkTravelTimeAndDisutility;
		this.dijkstra = new MultiNodeDijkstra(data.transitNetwork, this.travelDisutility, this.travelTime);
		this.routers = routers;
	}

	@Override
	public List<? extends PlanElement> calcRoute(
			final Facility fromFacility,
			final Facility toFacility,
			final double departureTime,
			final Person person) {
		// find possible start stops
		final PriorityInitialNodeMap fromNodes = new PriorityInitialNodeMap();
		final double tripLength = CoordUtils.calcEuclideanDistance( fromFacility.getCoord() , toFacility.getCoord() );

		for ( InitialNodeRouter router : routers ) {
			locateWrappedNearestTransitNodes(
					Direction.access,
					router,
					fromNodes,
					person,
					fromFacility,
					departureTime,
					tripLength);
		}

		prune( fromNodes );

		// find possible end stops
		final PriorityInitialNodeMap toNodes = new PriorityInitialNodeMap();

		for ( InitialNodeRouter router : routers ) {
			locateWrappedNearestTransitNodes(
					Direction.egress,
					router,
					toNodes,
					person,
					toFacility,
					departureTime,
					tripLength);
		}

		prune( toNodes );

		// find routes between start and end stops
		final Path p = this.dijkstra.calcLeastCostPath(
				fromNodes.getMap(),
				toNodes.getMap(),
				person);


		List<? extends PlanElement> bestDirectWay = null;
		// if no route found, thake the best of the "direct" ways
		// otherwise, take the best of the pt and the best direct way
		double bestCost = p == null ?
				Double.POSITIVE_INFINITY :
				p.travelCost +
					fromNodes.map.get( p.nodes.get( 0 ) ).initialCost +
					toNodes.map.get( p.nodes.get( p.nodes.size() - 1 ) ).initialCost;

		for ( InitialNodeRouter r : routers  ) {
			final InitialNodeWithSubTrip curr =
				r.calcRoute(
						null, // not associated with any node
						fromFacility,
						toFacility,
						departureTime,
						person );
			if ( curr.initialCost <= bestCost ) {
				bestDirectWay = curr.getSubtrip();
				bestCost = curr.initialCost;
			}
		}

		if ( bestDirectWay != null ) {
			final List<PlanElement> withdep = new ArrayList< >( bestDirectWay.size() + 1 );
			withdep.add( createDeparture( fromFacility ) );
			withdep.addAll( bestDirectWay );
			bestDirectWay = withdep;
		}
		else if ( p == null ) {
			throw new RuntimeException( "no path nor direct way!? Should not happen without a bug..." );
		}

		return bestDirectWay != null ? bestDirectWay :
			convertPathToTrip(
					departureTime,
					fromFacility,
					p,
					fromNodes.map.get( p.nodes.get( 0 ) ),
					toNodes.map.get( p.nodes.get( p.nodes.size() - 1 ) ),
					person ) ;
	}

	private void prune(final PriorityInitialNodeMap initialNodes) {
		final Map<Node, InitialNode> map = initialNodes.getMap();

		final int toKeep = (int) Math.max( 1 , initialNodeProportion * map.size() );
		final List<Node> nodes = new ArrayList<Node>( map.keySet() );
		Collections.shuffle( nodes , random );

		for ( Node n : nodes.subList( toKeep , nodes.size() ) ) map.remove( n );

		assert map.size() == toKeep : map.size() != toKeep;
	}

	private List<? extends PlanElement> convertPathToTrip(
			final double departureTime,
			final Facility fromFacility,
			final Path p,
			final InitialNodeWithSubTrip fromInitialNode,
			final InitialNodeWithSubTrip toInitialNode,
			final Person person) {
		final List<PlanElement> trip = new ArrayList<PlanElement>();

		trip.add( createDeparture( fromFacility ) );
		trip.addAll( fromInitialNode.getSubtrip() );
		// there is no Pt interaction in there...
		trip.addAll(
				fillWithActivities(
					convertPathToLegList(
						departureTime,
						fromInitialNode.getNode().getStop().getStopFacility(),
						toInitialNode.getNode().getStop().getStopFacility(),
						p,
						person ) ) );
		trip.addAll( toInitialNode.getSubtrip() );

		return trip;
	}

	private static PlanElement createDeparture(final Facility fromFacility) {
		final ActivityImpl dep = 
			new ActivityImpl(
					DEPARTURE_ACTIVITY_TYPE,
					fromFacility.getCoord(),
					fromFacility.getLinkId() );
		dep.setMaximumDuration( 0d );
		return dep;
	}

	private final List<PlanElement> fillWithActivities(
			final List<Leg> baseTrip ) {
		final List<PlanElement> trip = new ArrayList<PlanElement>();

		Coord nextCoord = null;
		Id nextLinkId = null;
		for (Leg leg : baseTrip) {
			if (leg.getRoute() instanceof ExperimentalTransitRoute) {
				final ExperimentalTransitRoute tRoute = (ExperimentalTransitRoute) leg.getRoute();
				tRoute.setTravelTime(leg.getTravelTime());
				tRoute.setDistance(
						RouteUtils.calcDistance(
							tRoute,
							data.scenario.getTransitSchedule(),
							data.scenario.getNetwork()));
				final ActivityImpl act =
					new ActivityImpl(
							PtConstants.TRANSIT_ACTIVITY_TYPE, 
							data.scenario.getTransitSchedule().getFacilities().get(tRoute.getAccessStopId()).getCoord(), 
							tRoute.getStartLinkId());
				act.setMaximumDuration(0.0);
				trip.add(act);
				nextCoord = data.scenario.getTransitSchedule().getFacilities().get(tRoute.getEgressStopId()).getCoord();
			}
			else { // walk legs don't have a coord, use the coord from the last egress point
				final ActivityImpl act =
					new ActivityImpl(
							PtConstants.TRANSIT_ACTIVITY_TYPE,
							nextCoord, 
							leg.getRoute().getStartLinkId());
				act.setMaximumDuration(0.0);
				trip.add(act);	
			}
			nextLinkId = leg.getRoute().getEndLinkId();
			trip.add(leg);
		}

		// put an interaction before the egress
		final ActivityImpl act =
			new ActivityImpl(
					PtConstants.TRANSIT_ACTIVITY_TYPE,
					nextCoord, 
					nextLinkId );
		act.setMaximumDuration(0.0);
		trip.add(act);
		return trip;
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		final CompositeStageActivityTypes stages =
			new CompositeStageActivityTypes(
					new StageActivityTypesImpl(
						DEPARTURE_ACTIVITY_TYPE,
						PtConstants.TRANSIT_ACTIVITY_TYPE ) );
		for ( InitialNodeRouter r : routers ) stages.addActivityTypes( r.getStageActivities() );
		return stages;
	}

	private void locateWrappedNearestTransitNodes(
			final Direction direction,
			final InitialNodeRouter router,
			final PriorityInitialNodeMap wrappedNearestNodes,
			final Person person,
			final Facility facility,
			final double departureTime,
			final double tripLength){
		Collection<TransitRouterNetworkNode> nearestNodes =
				data.transitNetwork.getNearestNodes(
						facility.getCoord(),
						Math.min(
							router.getSearchRadius(),
							// do not access a pt stop more than one third of the way
							// TODO: make configurable, as anything lower than one
							// is an approximation...
							tripLength / 3d ) );

		if (nearestNodes.size() < 2) {
			// also enlarge search area if only one stop found, maybe a second one is near the border of the search area
			TransitRouterNetworkNode nearestNode = data.transitNetwork.getNearestNode(facility.getCoord());
			double distance =
					CoordUtils.calcEuclideanDistance(
							facility.getCoord(),
							nearestNode.stop.getStopFacility().getCoord());
			nearestNodes =
					data.transitNetwork.getNearestNodes(
							facility.getCoord(),
							distance + data.config.getExtensionRadius());
		}

		for (TransitRouterNetworkNode node : nearestNodes) {
			for ( int i=0; i < router.getDesiredNumberOfCalls(); i++ ) {
				switch ( direction ) {
				case access:
					wrappedNearestNodes.put(
							node,
							router.calcRoute(
								node,
								facility,
								node.getStop().getStopFacility(),
								departureTime,
								person) );
					break;
				case egress:
					wrappedNearestNodes.put(
							node,
							router.calcRoute(
								node,
								node.getStop().getStopFacility(),
								facility,
								departureTime,
								person) );
					break;
				default: throw new RuntimeException( direction+"?" );
				}
			}
		}
	}

	private double getWalkTime(
			final Person person,
			final Coord coord,
			final Coord toCoord) {
		return travelDisutility.getTravelTime(person, coord, toCoord);
	}

	private List<Leg> convertPathToLegList(
			final double departureTime,
			final Facility fromFacility,
			final Facility toFacility,
			final Path p,
			final Person person ) {
		// yy there could be a better name for this method.  kai, apr'10

		// now convert the path back into a series of legs with correct routes
		double time = departureTime;
		List<Leg> legs = new ArrayList<Leg>();

		TransitLine line = null;
		TransitRoute route = null;
		TransitStopFacility accessStop = null;
		TransitRouteStop transitRouteStart = null;
		TransitRouterNetworkLink prevLink = null;
		int transitLegCnt = 0;
		for (Link link : p.links) {
			TransitRouterNetworkLink l = (TransitRouterNetworkLink) link;
			if (l.getLine() == null) {
				TransitStopFacility egressStop = l.fromNode.stop.getStopFacility();
				// it must be one of the "transfer" links. finish the pt leg, if there was one before...
				if (route != null) {
					final Leg leg = new LegImpl(TransportMode.pt);
					ExperimentalTransitRoute ptRoute = new ExperimentalTransitRoute(accessStop, line, route, egressStop);
					leg.setRoute(ptRoute);
					double arrivalOffset =
						(((TransitRouterNetworkLink) link).getFromNode().stop.getArrivalOffset() != Time.UNDEFINED_TIME) ?
							((TransitRouterNetworkLink) link).fromNode.stop.getArrivalOffset() :
							((TransitRouterNetworkLink) link).fromNode.stop.getDepartureOffset();
					double arrivalTime = data.preparedTransitSchedule.getNextDepartureTime(route, transitRouteStart, time) + (arrivalOffset - transitRouteStart.getDepartureOffset());
					leg.setTravelTime(arrivalTime - time);
					time = arrivalTime;
					legs.add(leg);
					transitLegCnt++;
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
								final Leg leg = new LegImpl(TransportMode.transit_walk);
								double walkTime = getWalkTime(person, accessStop.getCoord(), egressStop.getCoord());
								Route walkRoute = new GenericRouteImpl(accessStop.getLinkId(), egressStop.getLinkId());
								leg.setRoute(walkRoute);
								leg.setTravelTime(walkTime);
								time += walkTime;
								legs.add(leg);
							}
							// if accessStop == null, it must be the first walk-leg: handled by "subtrips"
							else {
								final TransitStopFacility depStop = ((TransitRouterNetworkNode) p.nodes.get( 0 )).getStop().getStopFacility();
								if ( !depStop.getLinkId().equals( egressStop.getLinkId() ) ) {
									// this is possible when several stops are for instance
									// on the opposite direction links
									final Leg leg = new LegImpl( TransportMode.transit_walk );
									final double walkTime =
										getWalkTime(
												person,
												depStop.getCoord(),
												egressStop.getCoord());
									final Route walkRoute =
										new GenericRouteImpl(
												depStop.getLinkId(),
												egressStop.getLinkId());
									leg.setRoute( walkRoute );
									walkRoute.setTravelTime( walkTime );
									leg.setTravelTime( walkTime );
									time += walkTime;
									legs.add(leg);
								}
							}
						}
					}
					line = l.getLine();
					route = l.getRoute();
					accessStop = egressStop;
				}
			}
			prevLink = l;
		}
		if (route != null) {
			// the last part of the path was with a transit route, so add the pt-leg and final walk-leg
			final Leg leg = new LegImpl(TransportMode.pt);
			TransitStopFacility egressStop = prevLink.toNode.stop.getStopFacility();
			ExperimentalTransitRoute ptRoute = new ExperimentalTransitRoute(accessStop, line, route, egressStop);
			leg.setRoute(ptRoute);
			double arrivalOffset = ((prevLink).toNode.stop.getArrivalOffset() != Time.UNDEFINED_TIME) ?
					(prevLink).toNode.stop.getArrivalOffset()
					: (prevLink).toNode.stop.getDepartureOffset();
			double arrivalTime = data.preparedTransitSchedule.getNextDepartureTime(route, transitRouteStart, time) + (arrivalOffset - transitRouteStart.getDepartureOffset());
			leg.setTravelTime(arrivalTime - time);

			legs.add(leg);
			transitLegCnt++;
			accessStop = egressStop;
		}

		if (transitLegCnt == 0) {
			// it seems, the agent only walked
			legs.clear();
			Leg leg = new LegImpl(TransportMode.transit_walk);
			// XXX problematic, but consistent with how it is computed in routing...
			double walkTime = getWalkTime(person, fromFacility.getCoord(), toFacility.getCoord());

			final Route walkRoute =
				new GenericRouteImpl(
						fromFacility.getLinkId(),
						toFacility.getLinkId());
			walkRoute.setTravelTime( walkTime );

			leg.setRoute( walkRoute );
			leg.setTravelTime(walkTime);

			legs.add(leg);
		}
		return legs;
	}

	public TransitRouterNetwork getTransitRouterNetwork() {
		return data.transitNetwork;
	}

	protected TransitRouterNetwork getTransitNetwork() {
		return data.transitNetwork;
	}

	protected MultiNodeDijkstra getDijkstra() {
		return dijkstra;
	}

	protected TransitRouterConfig getConfig() {
		return data.config;
	}

	private static class PriorityInitialNodeMap {
		private final Map<Node, InitialNodeWithSubTrip> map = new LinkedHashMap<Node, InitialNodeWithSubTrip>();

		public boolean put(
				final Node key,
				final InitialNodeWithSubTrip value) {
			final InitialNodeWithSubTrip old = map.get( key );

			// as we consider both walk and bike sharing as possible access mode,
			// a same node might be considered twice. Only keep the lowest cost.
			if ( old == null || old.initialCost > value.initialCost ) {
				map.put(key, value);
				return true;
			}

			return false;
		}

		public Map<Node, InitialNode> getMap() {
			return new LinkedHashMap<Node, InitialNode>( map );
		}
	}

	public static class RoutingData {
		private final Scenario scenario;
		private final TransitRouterNetwork transitNetwork;
		private final PreparedTransitSchedule preparedTransitSchedule;
		private final TransitRouterConfig config;

		public RoutingData(
				final Scenario scenario ) {
			this.scenario = scenario;
			this.preparedTransitSchedule = new PreparedTransitSchedule( scenario.getTransitSchedule() );
			this.config = new TransitRouterConfig( scenario.getConfig() );
			this.transitNetwork = TransitRouterNetwork.createFromSchedule(scenario.getTransitSchedule(), config.getBeelineWalkConnectionDistance());
		}

		public RoutingData(
				final Scenario scenario,
				final TransitRouterNetwork routingNetwork) {
			this.scenario = scenario;
			this.preparedTransitSchedule = new PreparedTransitSchedule( scenario.getTransitSchedule() );
			this.config = new TransitRouterConfig( scenario.getConfig() );
			this.transitNetwork = routingNetwork;
		}
	}
}


