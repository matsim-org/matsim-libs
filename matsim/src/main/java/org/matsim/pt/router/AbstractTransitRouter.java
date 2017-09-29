package org.matsim.pt.router;

import java.util.*;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.InitialNode;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkNode;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.*;

public class AbstractTransitRouter {

	private final TransitRouterNetwork transitNetwork;
	private final TransitRouterConfig trConfig;
	private final TransitTravelDisutility travelDisutility;
	private final TravelTime travelTime;
	private final PreparedTransitSchedule preparedTransitSchedule;

	protected AbstractTransitRouter(TransitRouterConfig trConfig, TransitSchedule schedule) {
		this.preparedTransitSchedule = new PreparedTransitSchedule(schedule);
		TransitRouterNetworkTravelTimeAndDisutility transitRouterNetworkTravelTimeAndDisutility = new TransitRouterNetworkTravelTimeAndDisutility(trConfig, getPreparedTransitSchedule());
		this.travelTime = transitRouterNetworkTravelTimeAndDisutility;
		this.trConfig = trConfig;
		this.travelDisutility = transitRouterNetworkTravelTimeAndDisutility;
		this.transitNetwork = TransitRouterNetwork.createFromSchedule(schedule, trConfig.getBeelineWalkConnectionDistance());
	}

	protected AbstractTransitRouter(TransitRouterConfig config, PreparedTransitSchedule preparedTransitSchedule,
			TransitRouterNetwork routerNetwork, TravelTime travelTime,
			TransitTravelDisutility travelDisutility) {
		this.trConfig = config;
		this.transitNetwork = routerNetwork;
		this.travelTime = travelTime;
		this.travelDisutility = travelDisutility;
		this.preparedTransitSchedule = preparedTransitSchedule;
	}

	protected final double getWalkTime(Person person, Coord coord, Coord toCoord) {
		return getTravelDisutility().getWalkTravelTime(person, coord, toCoord);
	}

	protected final double getTransferTime(Person person, Coord coord, Coord toCoord) {
		return getTravelDisutility().getWalkTravelTime(person, coord, toCoord) + this.getConfig().getAdditionalTransferTime();
	}

	//same is used in raptor router
	protected final List<Leg> createDirectWalkLegList(Person person, Coord fromCoord, Coord toCoord) {
		List<Leg> legs = new ArrayList<>();
		Leg leg = PopulationUtils.createLeg(TransportMode.transit_walk);
		double walkTime = getWalkTime(person, fromCoord, toCoord);
		leg.setTravelTime(walkTime);
		Route walkRoute = RouteUtils.createGenericRouteImpl(null, null);
		walkRoute.setTravelTime(walkTime);
		leg.setRoute(walkRoute);
		legs.add(leg);
		return legs;
	}

	private Leg createTransferTransitWalkLeg(RouteSegment routeSegement) {
		Leg leg = this.createTransitWalkLeg(routeSegement.getFromStop().getCoord(), routeSegement.getToStop().getCoord());
		Route walkRoute = RouteUtils.createGenericRouteImpl(routeSegement.getFromStop().getLinkId(), routeSegement.getToStop().getLinkId());
//		walkRoute.setTravelTime(leg.getTravelTime() );
		// transit walk leg should include additional transfer time; Amit, Aug'17
		leg.setTravelTime( getTransferTime(null, routeSegement.getFromStop().getCoord(), routeSegement.getToStop().getCoord()) );
		walkRoute.setTravelTime(getTransferTime(null, routeSegement.getFromStop().getCoord(), routeSegement.getToStop().getCoord()) );
		leg.setRoute(walkRoute);

		return leg;
	}

	protected List<Leg> convertPathToLegList(double departureTime, TransitPassengerRoute p, Coord fromCoord, Coord toCoord, Person person) {
		// convert the route into a sequence of legs
		List<Leg> legs = new ArrayList<>();

		// access leg
		Leg accessLeg;
		// check if first leg extends walking distance
		if (p.getRoute().get(0).getRouteTaken() == null) {
			// route starts with transfer - extend initial walk to that stop
			accessLeg = createTransitWalkLeg(fromCoord, p.getRoute().get(0).getToStop().getCoord());
			p.getRoute().remove(0);
		} else {
			// do not extend it - add a regular walk leg
			//
			accessLeg = createTransitWalkLeg(fromCoord, p.getRoute().get(0).getFromStop().getCoord());
		}

		// egress leg
		Leg egressLeg;
		// check if first leg extends walking distance
		if (p.getRoute().get(p.getRoute().size() - 1).getRouteTaken() == null) {
			// route starts with transfer - extend initial walk to that stop
			egressLeg = createTransitWalkLeg(p.getRoute().get(p.getRoute().size() - 1).getFromStop().getCoord(), toCoord);
			p.getRoute().remove(p.getRoute().size() - 1);
		} else {
			// do not extend it - add a regular walk leg
			// access leg
			egressLeg = createTransitWalkLeg(p.getRoute().get(p.getRoute().size() - 1).getToStop().getCoord(), toCoord);
		}


		// add very first leg
		legs.add(accessLeg);

		// route segments are now in pt-walk-pt sequence
		for (RouteSegment routeSegement : p.getRoute()) {
			if (routeSegement.getRouteTaken() == null) {
				// transfer
				legs.add(createTransferTransitWalkLeg(routeSegement));
			} else {
				// pt leg
				legs.add(createTransitLeg(routeSegement));
			}
		}

		// add last leg
		legs.add(egressLeg);

		return legs;
	}

	private Leg createTransitLeg(RouteSegment routeSegment) {
		Leg leg = PopulationUtils.createLeg(TransportMode.pt);

		TransitStopFacility accessStop = routeSegment.getFromStop();
		TransitStopFacility egressStop = routeSegment.getToStop();

		ExperimentalTransitRoute ptRoute = new ExperimentalTransitRoute(accessStop, egressStop, routeSegment.getLineTaken(), routeSegment.getRouteTaken());
		leg.setRoute(ptRoute);

		leg.setTravelTime(routeSegment.getTravelTime());
		return leg;
	}

	private Leg createTransitWalkLeg(Coord fromCoord, Coord toCoord) {
		Leg leg = PopulationUtils.createLeg(TransportMode.transit_walk);
		double walkTime = getWalkTime(null, fromCoord, toCoord);
		leg.setTravelTime(walkTime);
		return leg;
	}

	// it would be better to have TransitPassengerRoute as an argument rather than path (see Raptor...). Amit Sep'17
	protected final List<Leg> convertPathToLegList(double departureTime, Path path, Coord fromCoord, Coord toCoord, Person person) {
			// yy would be nice if the following could be documented a bit better.  kai, jul'16
			
			// now convert the path back into a series of legs with correct routes
			double time = departureTime;
			List<Leg> legs = new ArrayList<>();
			Leg leg;
			TransitLine line = null;
			TransitRoute route = null;
			TransitStopFacility accessStop = null;
			TransitRouteStop transitRouteStart = null;
			TransitRouterNetworkLink prevLink = null;
			double currentDistance = 0;
			int transitLegCnt = 0;
			for (Link ll : path.links) {
				TransitRouterNetworkLink link = (TransitRouterNetworkLink) ll;
				if (link.line == null) {
					// (it must be one of the "transfer" links.) finish the pt leg, if there was one before...
					TransitStopFacility egressStop = link.fromNode.stop.getStopFacility();
					if (route != null) {
						leg = PopulationUtils.createLeg(TransportMode.pt);
						ExperimentalTransitRoute ptRoute = new ExperimentalTransitRoute(accessStop, line, route, egressStop);
						double arrivalOffset = (link.getFromNode().stop.getArrivalOffset() != Time.UNDEFINED_TIME) ? link.fromNode.stop.getArrivalOffset() : link.fromNode.stop.getDepartureOffset();
						double arrivalTime = this.getPreparedTransitSchedule().getNextDepartureTime(route, transitRouteStart, time) + (arrivalOffset - transitRouteStart.getDepartureOffset());
						ptRoute.setTravelTime(arrivalTime - time);
	
	//					ptRoute.setDistance( currentDistance );
						ptRoute.setDistance( link.getLength() );
						// (see MATSIM-556)
						
						leg.setRoute(ptRoute);
						leg.setTravelTime(arrivalTime - time);
						time = arrivalTime;
						legs.add(leg);
						transitLegCnt++;
						accessStop = egressStop;
					}
					line = null;
					route = null;
					transitRouteStart = null;
					currentDistance = link.getLength();
				} else {
					// (a real pt link)
					currentDistance += link.getLength();
					if (link.route != route) {
						// the line changed
						TransitStopFacility egressStop = link.fromNode.stop.getStopFacility();
						if (route == null) {
							// previously, the agent was on a transfer, add the walk leg
							transitRouteStart = ((TransitRouterNetworkLink) ll).getFromNode().stop;
							if (accessStop != egressStop) {
								if (accessStop != null) {
									leg = PopulationUtils.createLeg(TransportMode.transit_walk);
									//							    double walkTime = getWalkTime(person, accessStop.getCoord(), egressStop.getCoord());
									double transferTime = getTransferTime(person, accessStop.getCoord(), egressStop.getCoord());
									Route walkRoute = RouteUtils.createGenericRouteImpl(
											accessStop.getLinkId(), egressStop.getLinkId());
									// (yy I would have expected this from egressStop to accessStop. kai, jul'16)
									
									//							    walkRoute.setTravelTime(walkTime);
									walkRoute.setTravelTime(transferTime);
									
	//								walkRoute.setDistance( currentDistance );
									walkRoute.setDistance( getConfig().getBeelineDistanceFactor() * 
											NetworkUtils.getEuclideanDistance(accessStop.getCoord(), egressStop.getCoord()) );
									// (see MATSIM-556)
	
									leg.setRoute(walkRoute);
									//							    leg.setTravelTime(walkTime);
									leg.setTravelTime(transferTime);
									//							    time += walkTime;
									time += transferTime;
									legs.add(leg);
								} else { // accessStop == null, so it must be the first walk-leg
									leg = PopulationUtils.createLeg(TransportMode.transit_walk);
									double walkTime = getWalkTime(person, fromCoord, egressStop.getCoord());
									Route walkRoute = RouteUtils.createGenericRouteImpl(null,
											egressStop.getLinkId());
									walkRoute.setTravelTime(walkTime);
									
	//								walkRoute.setDistance( currentDistance );
									walkRoute.setDistance(getConfig().getBeelineDistanceFactor() * 
											NetworkUtils.getEuclideanDistance(fromCoord, egressStop.getCoord()) );
									// (see MATSIM-556)
	
									leg.setRoute(walkRoute);
									leg.setTravelTime(walkTime);
									time += walkTime;
									legs.add(leg);
								}
							}
							currentDistance = 0;
						}
						line = link.line;
						route = link.route;
						accessStop = egressStop;
					}
				}
				prevLink = link;
			}
			if (route != null) {
				// the last part of the path was with a transit route, so add the pt-leg and final walk-leg
				leg = PopulationUtils.createLeg(TransportMode.pt);
				TransitStopFacility egressStop = prevLink.toNode.stop.getStopFacility();
				ExperimentalTransitRoute ptRoute = new ExperimentalTransitRoute(accessStop, line, route, egressStop);
	//			ptRoute.setDistance( currentDistance );
				ptRoute.setDistance( getConfig().getBeelineDistanceFactor() * NetworkUtils.getEuclideanDistance(accessStop.getCoord(), egressStop.getCoord() ) );
				// (see MATSIM-556)
				leg.setRoute(ptRoute);
				double arrivalOffset = ((prevLink).toNode.stop.getArrivalOffset() != Time.UNDEFINED_TIME) ?
						(prevLink).toNode.stop.getArrivalOffset()
						: (prevLink).toNode.stop.getDepartureOffset();
						double arrivalTime = this.getPreparedTransitSchedule().getNextDepartureTime(route, transitRouteStart, time) + (arrivalOffset - transitRouteStart.getDepartureOffset());
						leg.setTravelTime(arrivalTime - time);
						ptRoute.setTravelTime( arrivalTime - time );
						legs.add(leg);
						transitLegCnt++;
						accessStop = egressStop;
			}
			if (prevLink != null) {
				leg = PopulationUtils.createLeg(TransportMode.transit_walk);
				double walkTime;
				if (accessStop == null) {
					walkTime = getWalkTime(person, fromCoord, toCoord);
				} else {
					walkTime = getWalkTime(person, accessStop.getCoord(), toCoord);
				}
				leg.setTravelTime(walkTime);
				legs.add(leg);
			}
			if (transitLegCnt == 0) {
				// it seems, the agent only walked
				legs.clear();
				leg = PopulationUtils.createLeg(TransportMode.transit_walk);
				double walkTime = getWalkTime(person, fromCoord, toCoord);
				leg.setTravelTime(walkTime);
				legs.add(leg);
			}
			return legs;
		}

	public final TransitRouterNetwork getTransitRouterNetwork() {
		// publicly used in 2 places.  kai, jul'17
		return this.transitNetwork;
	}

	protected final TransitRouterConfig getConfig() {
		return trConfig;
	}

	protected final double getWalkDisutility(Person person, Coord coord, Coord toCoord) {
		return getTravelDisutility().getWalkTravelDisutility(person, coord, toCoord);
	}

	// "default" PT router uses TransitRouterNetworkNode (an implementation of Node) whereas "raptor" uses TransitStopFacility. Amit' Sep 17
	protected final Map<Node, InitialNode> locateWrappedNearestTransitNodes(Person person, Coord coord, double departureTime) {
		Collection<TransitRouterNetworkNode> nearestNodes = this.getTransitRouterNetwork().getNearestNodes(coord, this.getConfig().getSearchRadius());
		if (nearestNodes.size() < 2) {
			// also enlarge search area if only one stop found, maybe a second one is near the border of the search area
			TransitRouterNetworkNode nearestNode = this.getTransitRouterNetwork().getNearestNode(coord);
			if ( nearestNode != null ) { // transit schedule might be completely empty!
				double distance = CoordUtils.calcEuclideanDistance(coord, nearestNode.stop.getStopFacility().getCoord());
				nearestNodes = this.getTransitRouterNetwork().getNearestNodes(coord, distance + this.getConfig().getExtensionRadius());
			}
		}
		Map<Node, InitialNode> wrappedNearestNodes = new LinkedHashMap<>();
		for (TransitRouterNetworkNode node : nearestNodes) {
			Coord toCoord = node.stop.getStopFacility().getCoord();
			double initialTime = getWalkTime(person, coord, toCoord);
			double initialCost = getWalkDisutility(person, coord, toCoord);
			wrappedNearestNodes.put(node, new InitialNode(initialCost, initialTime + departureTime));
		}
		return wrappedNearestNodes;
	}

	protected final TransitTravelDisutility getTravelDisutility() {
		return travelDisutility;
	}

	protected final TravelTime getTravelTime() {
		return travelTime;
	}

	protected final PreparedTransitSchedule getPreparedTransitSchedule() {
		return preparedTransitSchedule;
	}

}