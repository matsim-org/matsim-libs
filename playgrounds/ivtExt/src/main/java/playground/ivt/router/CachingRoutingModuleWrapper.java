/* *********************************************************************** *
 * project: org.matsim.*
 * CachingRoutingModuleWrapper.java
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
package playground.ivt.router;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.facilities.Facility;
import playground.ivt.router.TripSoftCache.Departure;
import playground.ivt.router.TripSoftCache.LocationType;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author thibautd
 */
public class CachingRoutingModuleWrapper implements RoutingModule {
	private static final Logger log =
		Logger.getLogger(CachingRoutingModuleWrapper.class);

	private final TripSoftCache cache;
	private RoutingModule wrapped;

	private final static AtomicLong routeCount = new AtomicLong( 0 );
	private final static AtomicLong calcCount = new AtomicLong( 0 );

	public CachingRoutingModuleWrapper(
			final boolean considerPerson,
			final LocationType locationType,
			final RoutingModule wrapped) {
		this.cache = new TripSoftCache( considerPerson, locationType );
		this.wrapped = wrapped;
	}

	public CachingRoutingModuleWrapper(
			final TripSoftCache cache,
			final RoutingModule wrapped) {
		this.cache = cache;
		this.wrapped = wrapped;
	}

	@Override
	public List<? extends PlanElement> calcRoute(
			final Facility fromFacility,
			final Facility toFacility,
			final double departureTime,
			final Person person) {
		routeCount.incrementAndGet();
		final Departure departure = cache.createDeparture( person , fromFacility , toFacility );
		final List<? extends PlanElement> cached = cache.get( departure );
		
		if ( cached != null ) return cached;

		calcCount.incrementAndGet();
		final List<? extends PlanElement> trip =
				wrapped.calcRoute(fromFacility, toFacility, departureTime,
				person);
		
		cache.put( departure , trip );
		
		return trip;
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return wrapped.getStageActivityTypes();
	}

	public static void logStats() {
		log.info( "CachingRoutingModuleWrapper stats:" );
		log.info( routeCount.get()+" route computations" );
		final long cached = routeCount.get() - calcCount.get();
		final double percentage = ((double) cached) / routeCount.get();
		log.info( cached+" ("+(100 * percentage)+"%) obtained from cache" );
	}
}

