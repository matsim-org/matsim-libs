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
package playground.thibautd.router;

import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;

import playground.thibautd.router.TripLruCache.Departure;
import playground.thibautd.router.TripLruCache.LocationType;

/**
 * @author thibautd
 */
public class CachingRoutingModuleWrapper implements RoutingModule {
	private final TripLruCache cache;
	private RoutingModule wrapped;

	public CachingRoutingModuleWrapper(
			final boolean considerPerson,
			final LocationType locationType,
			final int cacheSize,
			final RoutingModule wrapped) {
		this.cache = new TripLruCache( considerPerson, locationType, cacheSize );
		this.wrapped = wrapped;
	}

	@Override
	public List<? extends PlanElement> calcRoute(
			final Facility fromFacility,
			final Facility toFacility,
			final double departureTime,
			final Person person) {
		final Departure departure = cache.createDeparture( person , fromFacility , toFacility );
		final List<? extends PlanElement> cached = cache.get( departure );
		
		if ( cached != null ) return cached;
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
}

