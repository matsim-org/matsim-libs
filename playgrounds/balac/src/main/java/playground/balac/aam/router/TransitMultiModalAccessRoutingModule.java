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
package playground.balac.aam.router;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
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
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
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

/**
 * A {@link RoutingModule} that allows agents to choose between bike sharing
 * and walk to go to their departure stops.
 *
 * @author thibautd
 */
public class TransitMultiModalAccessRoutingModule implements RoutingModule {

	private final TransitRouterNetwork transitNetwork;

	private final Scenario scenario;
	private final MultiNodeDijkstra dijkstra;
	private final TransitRouterConfig config;
	private final TransitTravelDisutility travelDisutility;
	private final TravelTime travelTime;

	private final Collection<InitialNodeRouter> routers;

	private final PreparedTransitSchedule preparedTransitSchedule; 

	private static enum Direction {access, egress;}

	public TransitMultiModalAccessRoutingModule(
			final Scenario scenario,
			final InitialNodeRouter... routers) {
		this.scenario = scenario;
		this.config = new TransitRouterConfig( scenario.getConfig() );
		this.preparedTransitSchedule = new PreparedTransitSchedule( scenario.getTransitSchedule() );
		TransitRouterNetworkTravelTimeAndDisutility transitRouterNetworkTravelTimeAndDisutility = new TransitRouterNetworkTravelTimeAndDisutility(config, preparedTransitSchedule);
		this.travelTime = transitRouterNetworkTravelTimeAndDisutility;
		this.travelDisutility = transitRouterNetworkTravelTimeAndDisutility;
		this.transitNetwork = TransitRouterNetwork.createFromSchedule(scenario.getTransitSchedule(), config.getBeelineWalkConnectionDistance());
		this.dijkstra = new MultiNodeDijkstra(this.transitNetwork, this.travelDisutility, this.travelTime);
		this.routers = Arrays.asList( routers );
	}

	@Override
	public List<? extends PlanElement> calcRoute(
			final Facility fromFacility,
			final Facility toFacility,
			final double departureTime,
			final Person person) {
		// find possible start stops
		final PriorityInitialNodeMap fromNodes = new PriorityInitialNodeMap();

		for ( InitialNodeRouter router : routers ) {
			locateWrappedNearestTransitNodes(
					Direction.access,
					router,
					fromNodes,
					person,
					fromFacility,
					departureTime);
		}

		// find possible end stops
		final PriorityInitialNodeMap toNodes = new PriorityInitialNodeMap();

		for ( InitialNodeRouter router : routers ) {
			locateWrappedNearestTransitNodes(
					Direction.egress,
					router,
					toNodes,
					person,
					toFacility,
					departureTime);
		}

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
						fromFacility,
						toFacility,
						departureTime,
						person );
			if ( curr.initialCost <= bestCost ) {
				bestDirectWay = curr.subtrip;
				bestCost = curr.initialCost;
			}
		}

		assert bestDirectWay != null || p != null;
		return bestDirectWay != null ? bestDirectWay :
			convertPathToTrip(
					departureTime,
					p,
					fromNodes.map.get( p.nodes.get( 0 ) ),
					toNodes.map.get( p.nodes.get( p.nodes.size() - 1 ) ),
					person ) ;
	}

	private List<? extends PlanElement> convertPathToTrip(
			final double departureTime,
			final Path p,
			final InitialNodeWithSubTrip fromInitialNode,
			final InitialNodeWithSubTrip toInitialNode,
			final Person person) {
		final List<PlanElement> trip = new ArrayList<PlanElement>();

		trip.addAll( fromInitialNode.subtrip );
		// there is no Pt interaction in there...
		trip.addAll(
				fillWithActivities(
					convertPathToLegList(
						departureTime,
						p,
						person ) ) );
		trip.addAll( toInitialNode.subtrip );

		return trip;
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
							scenario.getTransitSchedule(),
							scenario.getNetwork()));
				final ActivityImpl act =
					new ActivityImpl(
							PtConstants.TRANSIT_ACTIVITY_TYPE, 
							scenario.getTransitSchedule().getFacilities().get(tRoute.getAccessStopId()).getCoord(), 
							tRoute.getStartLinkId());
				act.setMaximumDuration(0.0);
				trip.add(act);
				nextCoord = scenario.getTransitSchedule().getFacilities().get(tRoute.getEgressStopId()).getCoord();
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
			final double departureTime){
		Collection<TransitRouterNetworkNode> nearestNodes =
				this.transitNetwork.getNearestNodes(
						facility.getCoord(),
						router.getSearchRadius());

		if (nearestNodes.size() < 2) {
			// also enlarge search area if only one stop found, maybe a second one is near the border of the search area
			TransitRouterNetworkNode nearestNode = this.transitNetwork.getNearestNode(facility.getCoord());
			double distance =
					CoordUtils.calcEuclideanDistance(
							facility.getCoord(),
							nearestNode.stop.getStopFacility().getCoord());
			nearestNodes =
					this.transitNetwork.getNearestNodes(
							facility.getCoord(),
							distance + this.config.getExtensionRadius());
		}

		for (TransitRouterNetworkNode node : nearestNodes) {
			for ( int i=0; i < router.getDesiredNumberOfCalls(); i++ ) {
				switch ( direction ) {
				case access:
					wrappedNearestNodes.put(
							node,
							router.calcRoute(
								facility,
								new NodeFacility( node ),
								departureTime,
								person) );
					break;
				case egress:
					wrappedNearestNodes.put(
							node,
							router.calcRoute(
								new NodeFacility( node ),
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
		//int transitLegCnt = 0;
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
					double arrivalTime = this.preparedTransitSchedule.getNextDepartureTime(route, transitRouteStart, time) + (arrivalOffset - transitRouteStart.getDepartureOffset());
					leg.setTravelTime(arrivalTime - time);
					time = arrivalTime;
					legs.add(leg);
					//transitLegCnt++;
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
			double arrivalTime = this.preparedTransitSchedule.getNextDepartureTime(route, transitRouteStart, time) + (arrivalOffset - transitRouteStart.getDepartureOffset());
			leg.setTravelTime(arrivalTime - time);

			legs.add(leg);
			//transitLegCnt++;
			accessStop = egressStop;
		}

		//if (transitLegCnt == 0) {
		//	// it seems, the agent only walked
		//	legs.clear();
		//	leg = new LegImpl(TransportMode.transit_walk);
		//	double walkTime = getWalkTime(person, fromCoord, toCoord);
		//	leg.setTravelTime(walkTime);
		//	legs.add(leg);
		//}
		return legs;
	}

	public TransitRouterNetwork getTransitRouterNetwork() {
		return this.transitNetwork;
	}

	protected TransitRouterNetwork getTransitNetwork() {
		return transitNetwork;
	}

	protected MultiNodeDijkstra getDijkstra() {
		return dijkstra;
	}

	protected TransitRouterConfig getConfig() {
		return config;
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

	public static class InitialNodeWithSubTrip extends InitialNode {
		public final List<? extends PlanElement> subtrip;

		public InitialNodeWithSubTrip(
				final double initialCost,
				final double initialTime,
				final List<? extends PlanElement> subtrip) {
			super(initialCost, initialTime);
			this.subtrip = subtrip;
		}
	}

	public static class InitialNodeRouter {
		private final RoutingModule delegate;
		private final double searchRadius;

		private final int desiredNumberOfCalls;
		private final CharyparNagelScoringParameters scoringParams;

		public InitialNodeRouter(
				final RoutingModule delegate,
				final double searchRadius,
				final int desiredNumberOfCalls,
				final CharyparNagelScoringParameters scoringParams) {
			this.delegate = delegate;
			this.searchRadius = searchRadius;
			this.desiredNumberOfCalls = desiredNumberOfCalls;
			this.scoringParams = scoringParams;
		}

		public InitialNodeWithSubTrip calcRoute(
				final Facility from,
				final Facility to,
				final double dep,
				final Person pers) {
			final List<? extends PlanElement> trip = delegate.calcRoute( from , to , dep , pers );
			final double duration = calcDuration( trip );
			final double cost = calcCost( trip );
			return new InitialNodeWithSubTrip( cost , dep + duration , trip );
		}


		protected double calcDuration(final List<? extends PlanElement> trip) {
			double tt = 0;

			for ( PlanElement pe : trip ) {
				if ( pe instanceof Leg ) {
					final double curr = ((Leg) pe).getTravelTime();
					if ( curr == Time.UNDEFINED_TIME ) throw new RuntimeException( pe+" has not travel time" );
					tt += curr;
				}

				if ( pe instanceof Activity ) {
					final double dur = ((Activity) pe).getMaximumDuration();
					if ( dur != Time.UNDEFINED_TIME ) {
						tt += dur;
					}
				}

			}

			return tt;
		}

		protected double calcCost(final List<? extends PlanElement> trip) {
			double cost = 0;

			for ( PlanElement pe : trip ) {
				if ( pe instanceof Leg ) {
					final Leg leg = (Leg) pe;
					final double time = leg.getTravelTime();
					if ( time == Time.UNDEFINED_TIME ) throw new RuntimeException( pe+" has not travel time" );
					// XXX no distance!
					cost += scoringParams.modeParams.get( leg.getMode() ).marginalUtilityOfTraveling_s * time;
				}
			}

			return cost;

		}

		public StageActivityTypes getStageActivities() {
			return delegate.getStageActivityTypes();
		}

		/**
		 * @return the number of time the calcRoute method should be called
		 * for each origin/destination pair. This should be 1 in most of the cases,
		 * but higher rates might be useful for randomized routers, such as bike sharing.
		 */
		public int getDesiredNumberOfCalls() {
			return desiredNumberOfCalls;
		}

		/**
		 * @return the radius within which the stations should be searched.
		 */
		public double getSearchRadius() {
			return searchRadius;
		}
	}

	private static class NodeFacility implements Facility {
		private final TransitRouterNetworkNode node;

		public NodeFacility(final TransitRouterNetworkNode node) {
			this.node = node;
		}

		@Override
		public Coord getCoord() {
			return node.getStop().getStopFacility().getCoord();
		}

		@Override
		public Id getId() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Map<String, Object> getCustomAttributes() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Id getLinkId() {
			return node.getStop().getStopFacility().getLinkId();
		}
	}
}


