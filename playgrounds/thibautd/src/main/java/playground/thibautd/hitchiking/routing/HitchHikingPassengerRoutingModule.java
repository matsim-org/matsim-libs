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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.RouteFactoryImpl;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.Facility;
import playground.thibautd.hitchiking.HitchHikingConfigGroup;
import playground.thibautd.hitchiking.HitchHikingConstants;
import playground.thibautd.hitchiking.HitchHikingSpots;
import playground.thibautd.hitchiking.spotweights.SpotWeighter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * @author thibautd
 */
public class HitchHikingPassengerRoutingModule implements RoutingModule {
	private final static double BEEFLY_ESTIMATED_SPEED = 25000d / 3600d;
	private final RoutingModule ptRoutingModule;
	private final RouteFactoryImpl routeFactory;
	private final HitchHikingSpots spots;
	private final SpotWeighter spotWeighter;
	private final HitchHikingConfigGroup config;
	private final Random random;

	public HitchHikingPassengerRoutingModule(
			final RoutingModule ptRoutingModule,
			final HitchHikingSpots spots,
			final RouteFactoryImpl factory,
			final SpotWeighter spotWeighter,
			final HitchHikingConfigGroup config,
			final Random random) {
		this.ptRoutingModule = ptRoutingModule;
		this.routeFactory = factory;
		this.spots = spots;
		this.spotWeighter = spotWeighter;
		this.config = config;
		this.random = random;
	}

	@Override
	public List<? extends PlanElement> calcRoute(
			final Facility fromFacility,
			final Facility toFacility,
			final double departureTime,
			final Person person) {
		Link doSpot = spots.getNearestSpot( toFacility.getCoord() );
		Link puSpot = getPickUpSpot(
				departureTime,
				fromFacility,
				toFacility,
				doSpot);
		double distance = CoordUtils.calcEuclideanDistance( puSpot.getCoord() , doSpot.getCoord() );

		List<PlanElement> trip = new ArrayList<PlanElement>();

		trip.addAll( ptRoutingModule.calcRoute(
					fromFacility,
					new LinkFacility( puSpot ),
					departureTime,
					person ) );

		Leg leg = new LegImpl( HitchHikingConstants.PASSENGER_MODE );
		Route route = routeFactory.createRoute(
					Route.class, // HitchHikingConstants.PASSENGER_MODE,
					puSpot.getId(),
					doSpot.getId());
		route.setDistance( distance );
		leg.setRoute( route );

		trip.add( leg );

		Leg lastPtLeg = (Leg) trip.get( trip.size() -2 );
		double egressDeparture = lastPtLeg.getDepartureTime() + lastPtLeg.getTravelTime();

		egressDeparture = egressDeparture == Time.UNDEFINED_TIME ?
			CoordUtils.calcEuclideanDistance( fromFacility.getCoord() , doSpot.getCoord() ) * BEEFLY_ESTIMATED_SPEED :
			egressDeparture + distance * BEEFLY_ESTIMATED_SPEED;

		trip.addAll( ptRoutingModule.calcRoute(
					new LinkFacility( doSpot ),
					toFacility,
					egressDeparture,
					person ) );

		return trip;
	}

	private List<Link> getPossiblePickUps(
			final Facility fromFacility,
			final Facility toFacility,
			final Coord doPoint) {
		Coord o = fromFacility.getCoord();
		Coord d = toFacility.getCoord();

		double centerX = (o.getX() + d.getX()) / 2d;
		double centerY = (o.getY() + d.getY()) / 2d;
		double maxBeeFlyDist = CoordUtils.calcEuclideanDistance( o , d ) * (1 + config.getMaximumDetourFraction());

		List<Link> disk = new ArrayList<Link>( spots.getSpots( centerX , centerY , maxBeeFlyDist / 2d ) );

		Iterator<Link> it = disk.iterator();
		while (it.hasNext()) {
			Link l = it.next();
			double dist = CoordUtils.calcEuclideanDistance( o, l.getCoord() )
				+ CoordUtils.calcEuclideanDistance( l.getCoord() , doPoint )
				+ CoordUtils.calcEuclideanDistance( doPoint , d );
			if (dist > maxBeeFlyDist) it.remove();
		}

		return disk;
	}

	private Link getPickUpSpot(
			final double departureTime,
			final Facility fromFacility,
			final Facility toFacility,
			final Link dropOffLink) {
		List<Link> possiblePus = getPossiblePickUps(
				fromFacility,
				toFacility,
				dropOffLink.getCoord());

		if (possiblePus.isEmpty()) {
			return spots.getNearestSpot( fromFacility.getCoord() );
		}

		// choose according to weight
		double sum = 0;
		List<Double> weights = new ArrayList<Double>();

		for (Link l : possiblePus) {
			double w = spotWeighter.weightPassengerOrigin(
					departureTime,
					l.getId(),
					dropOffLink.getId());
			sum += w;
			weights.add( w );
		}

		double choice = random.nextDouble() * sum;

		sum = 0;
		int choiceIndex = -1;
		for (double level : weights) {
			sum += level;
			choiceIndex++;
			if (choice <= level) break;
		}

		return possiblePus.get( choiceIndex );
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

