/* *********************************************************************** *
 * project: org.matsim.*
 * HitchHikingDriverRoutingModule.java
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
import org.matsim.core.population.LegImpl;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.Facility;
import playground.thibautd.hitchiking.HitchHikingConfigGroup;
import playground.thibautd.hitchiking.HitchHikingConstants;
import playground.thibautd.hitchiking.HitchHikingSpots;
import playground.thibautd.hitchiking.population.HitchHikingDriverRoute;
import playground.thibautd.hitchiking.spotweights.SpotWeighter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * @author thibautd
 */
public class HitchHikingDriverRoutingModule implements RoutingModule {
	private final SpotWeighter spotWeighter;
	private final HitchHikingSpots spots;
	private final HitchHikingConfigGroup config;
	private final RoutingModule carRoutingModule;
	private final Random random;

	public HitchHikingDriverRoutingModule(
			final SpotWeighter spotWeighter,
			final RoutingModule carRoutingModule,
			final HitchHikingSpots spots,
			final HitchHikingConfigGroup config,
			final Random random) {
		this.spotWeighter = spotWeighter;
		this.spots = spots;
		this.config = config;
		this.carRoutingModule = carRoutingModule;
		this.random = random;
	}

	@Override
	public List<? extends PlanElement> calcRoute(
			final Facility fromFacility,
			final Facility toFacility,
			final double departureTime,
			final Person person) {
		Coord o = fromFacility.getCoord();
		Coord d = toFacility.getCoord();

		double centerX = (o.getX() + d.getX()) / 2d;
		double centerY = (o.getY() + d.getY()) / 2d;
		double maxBeeFlyDist = CoordUtils.calcEuclideanDistance( o , d ) * (1 + config.getMaximumDetourFraction());

		Collection<Link> closeSpots = spots.getSpots( centerX , centerY , maxBeeFlyDist / 2d );

		Link puSpot = getPuSpot( departureTime , o, d , toFacility.getLinkId() , closeSpots );

		if ( puSpot == null ) {
			return carRoutingModule.calcRoute( fromFacility , toFacility , departureTime , person );
		}

		List<Id<Link>> doSpots = getDoSpots(
				puSpot,
				d,
				closeSpots,
				maxBeeFlyDist - CoordUtils.calcEuclideanDistance( puSpot.getCoord() , d ));

		if (doSpots.size() == 0) {
			return carRoutingModule.calcRoute( fromFacility , toFacility , departureTime , person );
		}

		return createTrip(
				new HitchHikingDriverRoute(
					fromFacility.getLinkId(),
					toFacility.getLinkId(),
					puSpot.getId(),
					doSpots));
	}

	private List<? extends PlanElement> createTrip(
			final HitchHikingDriverRoute r) {
		Leg leg = new LegImpl( HitchHikingConstants.DRIVER_MODE );
		leg.setRoute( r );
		return Arrays.asList( leg );
	}

	private static List<Id<Link>> getDoSpots(
			final Link puSpot,
			final Coord destination,
			final Collection<Link> closeSpots,
			final double distanceBudget) {
		List<IdWithDistance> doSpots = new ArrayList<IdWithDistance>();
		Coord origin = puSpot.getCoord();
		for (Link l : closeSpots) {
			if (l != puSpot) {
				double dist = CoordUtils.calcEuclideanDistance( origin, l.getCoord() )
				+ CoordUtils.calcEuclideanDistance( l.getCoord() , destination );

				if (dist < distanceBudget) {
					doSpots.add( new IdWithDistance( l.getId() , dist ) );
				}
			}
		}

		// order in ascending order of distance
		Collections.sort(
				doSpots,
				new Comparator<IdWithDistance>() {
					@Override
					public int compare(
						final IdWithDistance first,
						final IdWithDistance second) {
						return Double.compare( first.distance , second.distance );
					}
				});

		List<Id<Link>> ids = new ArrayList<>();
		for (IdWithDistance id : doSpots) {
			ids.add( id.id );
		}
		return ids;
	}

	private Link getPuSpot(
			final double departureTime,
			final Coord origin,
			final Coord destination,
			final Id destinationId,
			final Collection<Link> spots1) {
		double maxDetour = CoordUtils.calcEuclideanDistance( origin , destination ) * ( 1  + config.getMaximumDetourFraction());
		List<Link> possibleSpots = new ArrayList<Link>();

		for (Link l : spots1) {
			double detour = CoordUtils.calcEuclideanDistance( origin, l.getCoord() )
				+ CoordUtils.calcEuclideanDistance( l.getCoord() , destination );
			if (detour <= maxDetour) {
				possibleSpots.add( l );
			}
		}

		if (possibleSpots.isEmpty()) {
			return null;
		}

		// choose according to weight
		double sum = 0;
		List<Double> weights = new ArrayList<Double>();

		for (Link l : possibleSpots) {
			double w = spotWeighter.weightDriverOrigin(
					departureTime,
					l.getId(),
					destinationId);
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

		return possibleSpots.get( choiceIndex );
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return EmptyStageActivityTypes.INSTANCE;
	}

	private static class IdWithDistance {
		private final Id<Link> id;
		private final double distance;

		private IdWithDistance(final Id<Link> id, final double distance) {
			this.id = id;
			this.distance = distance;
		}
	}
}

