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
package playground.thibautd.router;

import java.util.Arrays;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.population.LegImpl;
import org.matsim.core.router.LegRouter;
import org.matsim.core.router.util.PersonalizableTravelDisutility;
import org.matsim.core.router.util.PersonalizableTravelTime;

import playground.thibautd.router.StageActivityTypes;

/**
 * Class wrapping a {@link LegRouter} in a {@link RoutingModule}.
 *
 * @author thibautd
 */
public class LegRouterWrapper implements RoutingModule {
	private static final StageActivityTypes EMPTY_CHECKER = new StageActivityTypesImpl( null );

	private final String mode;
	private final PopulationFactory populationFactory;
	private final LegRouter wrapped;
	private final PersonalizableTravelDisutility travelCost;
	private final PersonalizableTravelTime travelTime;

	/**
	 * Initialises a wrapper.
	 *
	 * @param mode the mode to route
	 * @param toWrap the {@link LegRouter} to wrap
	 * @param travelCost if the {@link LegRouter} uses (probably indirectly) a
	 * {@link PersonalizableTravelDisutility}, it should be provided here. Otherwise,
	 * it can be null. The person will be set at each travel time estimation.
	 * @param travelTime  if the {@link LegRouter} uses (probably indirectly) a
	 * {@link PersonalizableTravelTime}, it should be provided here. Otherwise,
	 * it can be null. The person will be set at each travel time estimation.
	 */
	public LegRouterWrapper(
			final String mode,
			final PopulationFactory populationFactory,
			final LegRouter toWrap,
			final PersonalizableTravelDisutility travelCost,
			final PersonalizableTravelTime travelTime) {
		this.mode = mode;
		this.populationFactory = populationFactory;
		this.wrapped = toWrap;
		this.travelCost = travelCost;
		this.travelTime = travelTime;
	}

	@Override
	public List<PlanElement> calcRoute(
			final Facility fromFacility,
			final Facility toFacility,
			final double departureTime,
			final Person person) {
		if (travelCost != null) {
			travelCost.setPerson( person );
		}
		if (travelTime != null) {
			travelTime.setPerson( person );
		}

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

		return Arrays.asList( new PlanElement[]{ newLeg } );
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return EMPTY_CHECKER;
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
		public Id getLinkId() {
			return wrapped.getLinkId();
		}

		@Override
		public Id getFacilityId() {
			throw new UnsupportedOperationException( "only facility fields access are supported" );
		}
	}
}
