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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.population.LegImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.thibautd.hitchiking.HitchHikingConfigGroup;
import playground.thibautd.hitchiking.HitchHikingConstants;
import playground.thibautd.hitchiking.HitchHikingSpots;
import playground.thibautd.hitchiking.population.HitchHikingDriverRoute;
import playground.thibautd.router.EmptyStageActivityTypes;
import playground.thibautd.router.RoutingModule;
import playground.thibautd.router.StageActivityTypes;

/**
 * @author thibautd
 */
public class HitchHikingDriverRoutingModule implements RoutingModule {
	private final HitchHikingSpots spots;
	private final HitchHikingConfigGroup config;
	private final RoutingModule carRoutingModule;

	public HitchHikingDriverRoutingModule(
			final RoutingModule carRoutingModule,
			final HitchHikingSpots spots,
			final HitchHikingConfigGroup config) {
		this.spots = spots;
		this.config = config;
		this.carRoutingModule = carRoutingModule;
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
		double maxBeeFlyDist = CoordUtils.calcDistance( o , d ) * (1 + config.getMaximumDetourFraction());

		Collection<Link> closeSpots = spots.getSpots( centerX , centerY , maxBeeFlyDist / 2d );

		if (closeSpots.size() < 2) {
			return carRoutingModule.calcRoute( fromFacility , toFacility , departureTime , person );
		}

		Link puSpot = getPuSpot( o, d , closeSpots );
		List<Id> doSpots = getDoSpots(
				puSpot,
				d,
				closeSpots,
				maxBeeFlyDist - CoordUtils.calcDistance( puSpot.getCoord() , d ));

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

	private static List<Id> getDoSpots(
			final Link puSpot,
			final Coord destination,
			final Collection<Link> closeSpots,
			final double distanceBudget) {
		List<IdWithDistance> doSpots = new ArrayList<IdWithDistance>();
		Coord origin = puSpot.getCoord();
		for (Link l : closeSpots) {
			if (l != puSpot) {
				double dist = CoordUtils.calcDistance( origin, l.getCoord() )
				+ CoordUtils.calcDistance( l.getCoord() , destination );

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
						return first.id.compareTo( second.id );
					}
				});

		List<Id> ids = new ArrayList<Id>();
		for (IdWithDistance id : doSpots) {
			ids.add( id.id );
		}
		return ids;
	}

	private static Link getPuSpot(
			final Coord origin,
			final Coord destination,
			final Collection<Link> spots) {
		double currentDetour = Double.POSITIVE_INFINITY;
		Link pu = null;

		for (Link l : spots) {
			double detour = CoordUtils.calcDistance( origin, l.getCoord() )
				+ CoordUtils.calcDistance( l.getCoord() , destination );
			if (detour < currentDetour) {
				pu = l;
				currentDetour = detour;
			}
		}

		return pu;
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return EmptyStageActivityTypes.INSTANCE;
	}

	private static class IdWithDistance {
		private final Id id;
		private final double distance;

		private IdWithDistance(final Id id, final double distance) {
			this.id = id;
			this.distance = distance;
		}
	}
}

