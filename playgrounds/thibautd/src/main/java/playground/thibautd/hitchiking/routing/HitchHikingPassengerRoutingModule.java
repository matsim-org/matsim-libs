/* *********************************************************************** *
 * project: org.matsim.*
 * HitchHikingPassengerRoutingModule.java
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
package playground.thibautd.hitchiking.routing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;

import playground.thibautd.hitchiking.HitchHikingConstants;
import playground.thibautd.hitchiking.HitchHikingSpots;
import playground.thibautd.router.RoutingModule;
import playground.thibautd.router.StageActivityTypes;

/**
 * @author thibautd
 */
public class HitchHikingPassengerRoutingModule implements RoutingModule {
	private final static double BEEFLY_ESTIMATED_SPEED = 25000d / 3600d;
	private final RoutingModule ptRoutingModule;
	private final ModeRouteFactory routeFactory;
	private final HitchHikingSpots spots;
	private final Random random;

	public HitchHikingPassengerRoutingModule(
			final RoutingModule ptRoutingModule,
			final HitchHikingSpots spots,
			final ModeRouteFactory factory,
			final Random random) {
		this.ptRoutingModule = ptRoutingModule;
		this.routeFactory = factory;
		this.spots = spots;
		this.random = random;
	}

	@Override
	public List<? extends PlanElement> calcRoute(
			final Facility fromFacility,
			final Facility toFacility,
			final double departureTime,
			final Person person) {
		Link puSpot = spots.getNearestSpot( fromFacility.getCoord() );
		Link doSpot = spots.getNearestSpot( toFacility.getCoord() );
		double distance = CoordUtils.calcDistance( puSpot.getCoord() , doSpot.getCoord() );

		List<PlanElement> trip = new ArrayList<PlanElement>();

		trip.addAll( ptRoutingModule.calcRoute(
					fromFacility,
					new LinkFacility( puSpot ),
					departureTime,
					person ) );

		Leg leg = new LegImpl( HitchHikingConstants.PASSENGER_MODE );
		Route route = routeFactory.createRoute(
					HitchHikingConstants.PASSENGER_MODE,
					puSpot.getId(),
					doSpot.getId());
		route.setDistance( distance );
		leg.setRoute( route );

		trip.add( leg );

		Leg lastPtLeg = (Leg) trip.get( trip.size() -2 );
		double egressDeparture = lastPtLeg.getDepartureTime() + lastPtLeg.getTravelTime();

		egressDeparture = egressDeparture == Time.UNDEFINED_TIME ?
			CoordUtils.calcDistance( fromFacility.getCoord() , doSpot.getCoord() ) * BEEFLY_ESTIMATED_SPEED :
			egressDeparture + distance * BEEFLY_ESTIMATED_SPEED;

		trip.addAll( ptRoutingModule.calcRoute(
					new LinkFacility( doSpot ),
					toFacility,
					egressDeparture,
					person ) );

		return trip;
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return ptRoutingModule.getStageActivityTypes();
	}

	private final class LinkFacility implements Facility {
		private final Link link;
		public LinkFacility(final Link l) {
			this.link = l;
		}
		@Override
		public Coord getCoord() {
			return link.getCoord();
		}
		@Override
		public Id getId() {
			throw new UnsupportedOperationException();
		}
		@Override
		public Map<String, Object> getCustomAttributes() {
			// TODO Auto-generated method stub
			return null;
		}
		@Override
		public Id getLinkId() {
			return link.getId();
		}
	}
}

