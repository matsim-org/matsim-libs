/* *********************************************************************** *
 * project: org.matsim.*
 * TransitRouterWrapper.java
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
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.pt.PtConstants;
import org.matsim.pt.router.TransitRouter;

/**
 * Wraps a {@link TransitRouter}.
 *
 * @author thibautd
 */
public class TransitRouterWrapper implements RoutingModeHandler {
	private static final StageActivityTypes CHECKER =
		new StageActivityTypesImpl(
				Arrays.asList( new String[]{ PtConstants.TRANSIT_ACTIVITY_TYPE } ) );
	private final TransitRouter router;

	/**
	 * Initialises an instance
	 * @param toWrap the router to add
	 */
	public TransitRouterWrapper(
			final TransitRouter toWrap) {
		this.router = toWrap;
	}

	/**
	 * Just links to {@link TransitRouter#calcRoute(Coord, Coord, double, Person)}.
	 * @return the list of legs returned by the transit router.
	 */
	@Override
	public List<? extends PlanElement> calcRoute(
			final Facility fromFacility,
			final Facility toFacility,
			final double departureTime,
			final Person person) {
		return router.calcRoute(
				fromFacility.getCoord(),
				toFacility.getCoord(),
				departureTime,
				person);
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return CHECKER;
	}
}

