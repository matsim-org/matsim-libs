package org.matsim.pt.router;

import java.util.ArrayList;
import java.util.List;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class AbstractTransitRouter {

	private final TransitRouterNetwork transitNetwork; // specific to default pt router
	private final TravelTime travelTime ; // specific to default pt router

	private final TransitRouterConfig trConfig;
	private final TransitTravelDisutility travelDisutility;


	//used mainly as MATSim default PT router
	protected AbstractTransitRouter(
			TransitRouterConfig trConfig,
			TransitSchedule schedule) {

		TransitRouterNetworkTravelTimeAndDisutility transitRouterNetworkTravelTimeAndDisutility = new TransitRouterNetworkTravelTimeAndDisutility(trConfig, new PreparedTransitSchedule(schedule));

		this.trConfig = trConfig;
		this.travelDisutility = transitRouterNetworkTravelTimeAndDisutility;

		this.transitNetwork = TransitRouterNetwork.createFromSchedule(schedule, trConfig.getBeelineWalkConnectionDistance());
		this.travelTime = transitRouterNetworkTravelTimeAndDisutility;
	}

	protected AbstractTransitRouter(
			TransitRouterConfig config,
			TransitRouterNetwork routerNetwork,
			TravelTime travelTime,
			TransitTravelDisutility travelDisutility) {
		this.trConfig = config;
		this.transitNetwork = routerNetwork;
		this.travelTime = travelTime;
		this.travelDisutility = travelDisutility;
	}

	// for other routers which does not use TransitRouterNetwork e.g. raptor. Amit Oct'17
	protected AbstractTransitRouter(
			TransitRouterConfig config,
			TransitTravelDisutility travelDisutility) {
		this.trConfig = config;
		this.travelDisutility = travelDisutility;
		// not necessary for raptor
		this.travelTime = null;
		this.transitNetwork = null;
	}

	protected final double getWalkTime(Person person, Coord coord, Coord toCoord) {
		return getTravelDisutility().getWalkTravelTime(person, coord, toCoord);
	}

	protected final double getTransferTime(Person person, Coord coord, Coord toCoord) {
		return getTravelDisutility().getWalkTravelTime(person, coord, toCoord) + this.getConfig().getAdditionalTransferTime();
	}

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

	protected List<Leg> convertPassengerRouteToLegList(double departureTime, TransitPassengerRoute p, Coord fromCoord, Coord toCoord, Person person) {
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

	protected final TransitTravelDisutility getTravelDisutility() {
		return travelDisutility;
	}

	// specific to default pt router. Amit Oct'17
	protected final TravelTime getTravelTime() {
		return travelTime;
	}

}