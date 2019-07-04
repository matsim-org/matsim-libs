
/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractTransitRouter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.pt.router;

import java.util.ArrayList;
import java.util.List;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class AbstractTransitRouter {

	private TransitRouterConfig trConfig;
	private TransitTravelDisutility travelDisutility;

	protected AbstractTransitRouter (TransitRouterConfig transitRouterConfig){
		this.trConfig = transitRouterConfig;
	}

    protected AbstractTransitRouter(TransitRouterConfig config, TransitTravelDisutility transitTravelDisutility){
		this.trConfig = config;
		this.travelDisutility = transitTravelDisutility;
	}

	// a setter is required for default PT router, I dont see any other way to make AbstractTransitRouter 'general purpose'.
	protected void setTransitTravelDisutility(TransitTravelDisutility transitTravelDisutility){
		this.travelDisutility = transitTravelDisutility;
	}

	// methods
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

	private Leg createAccessTransitWalkLeg(Coord fromCoord, RouteSegment routeSegement) {
		Leg leg = this.createTransitWalkLeg(fromCoord, routeSegement.fromStop.getCoord());
		Route walkRoute = RouteUtils.createGenericRouteImpl(null, routeSegement.fromStop.getLinkId());
		walkRoute.setTravelTime(leg.getTravelTime() );
		walkRoute.setDistance(trConfig.getBeelineDistanceFactor() * NetworkUtils.getEuclideanDistance(fromCoord, routeSegement.fromStop.getCoord()));
		leg.setRoute(walkRoute);
		return leg;
	}

	private Leg createEgressTransitWalkLeg(RouteSegment routeSegement, Coord toCoord) {
		Leg leg = this.createTransitWalkLeg(routeSegement.toStop.getCoord(), toCoord);
		Route walkRoute = RouteUtils.createGenericRouteImpl(routeSegement.toStop.getLinkId(), null);
		walkRoute.setTravelTime(leg.getTravelTime() );
		walkRoute.setDistance(trConfig.getBeelineDistanceFactor() * NetworkUtils.getEuclideanDistance(routeSegement.toStop.getCoord(), toCoord));
		leg.setRoute(walkRoute);
		return leg;
	}

	private Leg createTransferTransitWalkLeg(RouteSegment routeSegement) {
		Leg leg = this.createTransitWalkLeg(routeSegement.getFromStop().getCoord(), routeSegement.getToStop().getCoord());
		Route walkRoute = RouteUtils.createGenericRouteImpl(routeSegement.getFromStop().getLinkId(), routeSegement.getToStop().getLinkId());
//		walkRoute.setTravelTime(leg.getTravelTime() );
		// transit walk leg should include additional transfer time; Amit, Aug'17
		leg.setTravelTime( getTransferTime(null, routeSegement.getFromStop().getCoord(), routeSegement.getToStop().getCoord()) );
		walkRoute.setTravelTime(getTransferTime(null, routeSegement.getFromStop().getCoord(), routeSegement.getToStop().getCoord()) );
		walkRoute.setDistance(trConfig.getBeelineDistanceFactor() * NetworkUtils.getEuclideanDistance(routeSegement.fromStop.getCoord(), routeSegement.toStop.getCoord()));
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
			//TODO: what if first leg extends the walking distance to more than first routeSegment i.e., (accessLeg, transfer, transfer ...). Amit Jan'18
//			accessLeg = createTransitWalkLeg(fromCoord, p.getRoute().get(0).getToStop().getCoord());
			accessLeg = createAccessTransitWalkLeg(fromCoord, p.getRoute().get(0));
			p.getRoute().remove(0);
		} else {
			// do not extend it - add a regular walk leg
			//
//			accessLeg = createTransitWalkLeg(fromCoord, p.getRoute().get(0).getFromStop().getCoord());
			accessLeg = createAccessTransitWalkLeg(fromCoord, p.getRoute().get(0));
		}

		// egress leg
		Leg egressLeg;
		// check if first leg extends walking distance
		if (p.getRoute().get(p.getRoute().size() - 1).getRouteTaken() == null) {
			// route starts with transfer - extend initial walk to that stop
//			egressLeg = createTransitWalkLeg(p.getRoute().get(p.getRoute().size() - 1).getFromStop().getCoord(), toCoord);
			egressLeg = createEgressTransitWalkLeg(p.getRoute().get(p.getRoute().size() - 1), toCoord);
			p.getRoute().remove(p.getRoute().size() - 1);
		} else {
			// do not extend it - add a regular walk leg
			// access leg
//			egressLeg = createTransitWalkLeg(p.getRoute().get(p.getRoute().size() - 1).getToStop().getCoord(), toCoord);
			egressLeg = createEgressTransitWalkLeg(p.getRoute().get(p.getRoute().size() - 1), toCoord);
		}


		// add very first leg
		legs.add(accessLeg);

		// route segments are now in pt-walk-pt sequence
		for (RouteSegment routeSegement : p.getRoute()) {
			if (routeSegement.getRouteTaken() == null) {// transfer
				if (!routeSegement.fromStop.equals(routeSegement.toStop)) { // same to/from stop => no transfer. Amit Feb'18
					legs.add(createTransferTransitWalkLeg(routeSegement));
				}
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
		ptRoute.setTravelTime(routeSegment.travelTime);
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

	protected final TransitRouterConfig getConfig() {
		return trConfig;
	}

	protected final double getWalkDisutility(Person person, Coord coord, Coord toCoord) {
		return getTravelDisutility().getWalkTravelDisutility(person, coord, toCoord);
	}

	protected final TransitTravelDisutility getTravelDisutility() {
		return travelDisutility;
	}

}