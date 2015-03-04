/* *********************************************************************** *

 * project: org.matsim.*
 * LegRouterWrapper.java
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
package org.matsim.core.router.old;

import java.util.Arrays;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;

/**
 * Class wrapping a {@link LegRouter} in a {@link RoutingModule}.
 * <br>
 * It is meant only at using legacy code without adaptation.
 * If you want to implement a new routing module, implement
 * a {@link RoutingModule} directly!
 *
 * @author thibautd
 */
@Deprecated // implement RoutingModule directly. This class here is only a backwards adapter (and for that reason not public). kai, mar'15
final class LegRouterWrapper implements RoutingModule {

	private final String mode;
	private final PopulationFactory populationFactory;
	private final LegRouter wrapped;

	/**
	 * Initialises a wrapper.
	 *
	 * @param mode the mode to route
	 * @param populationFactory the factory to use to create the return leg instance
	 * @param toWrap the {@link LegRouter} to wrap
	 */
	@Deprecated // implement RoutingModule directly.  this class here is only a backwards adapter (and for that reason not public). kai, mar'15
	 LegRouterWrapper(
			final String mode,
			final PopulationFactory populationFactory,
			final LegRouter toWrap) {
		this.mode = mode;
		this.populationFactory = populationFactory;
		this.wrapped = toWrap;
	}

	@Override
	public List<? extends PlanElement> calcRoute(
			final Facility fromFacility,
			final Facility toFacility,
			final double departureTime,
			final Person person) {
		Leg newLeg = populationFactory.createLeg( mode );
		newLeg.setDepartureTime( departureTime );

		double travTime = wrapped.routeLeg(
				person,
				newLeg,
				new FacilityWrapper( fromFacility ),
				new FacilityWrapper( toFacility ),
				departureTime);

		// otherwise, information may be lost
		newLeg.setTravelTime( travTime );

		return Arrays.asList( newLeg );
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return EmptyStageActivityTypes.INSTANCE;
	}

	@Override
	public String toString() {
		return "[LegRouterWrapper: mode="+mode+"; delegate="+wrapped+"]";
	}

	private static class FacilityWrapper implements Activity {
		private final Facility wrapped;

		public FacilityWrapper(final Facility toWrap) {
			this.wrapped = toWrap;
		}

		@Override
		public double getEndTime() {
			throw new UnsupportedOperationException( "only facility fields access are supported" );
		}

		@Override
		public void setEndTime(double seconds) {
			throw new UnsupportedOperationException( "only facility fields access are supported" );
		}

		@Override
		public String getType() {
			throw new UnsupportedOperationException( "only facility fields access are supported" );
		}

		@Override
		public void setType(String type) {
			throw new UnsupportedOperationException( "only facility fields access are supported" );
		}

		@Override
		public Coord getCoord() {
			return wrapped.getCoord();
		}

		@Override
		public double getStartTime() {
			throw new UnsupportedOperationException( "only facility fields access are supported" );
		}

		@Override
		public void setStartTime(double seconds) {
			throw new UnsupportedOperationException( "only facility fields access are supported" );
		}

		@Override
		public double getMaximumDuration() {
			throw new UnsupportedOperationException( "only facility fields access are supported" );
		}

		@Override
		public void setMaximumDuration(double seconds) {
			throw new UnsupportedOperationException( "only facility fields access are supported" );
		}

		@Override
		public Id<Link> getLinkId() {
			return wrapped.getLinkId();
		}

		@Override
		public Id<ActivityFacility> getFacilityId() {
			throw new UnsupportedOperationException( "only facility fields access are supported" );
		}

		@Override
		public String toString() {
			return "[FacilityWrapper: wrapped="+wrapped+"]";
		}
	}
}
