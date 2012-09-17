/* *********************************************************************** *
 * project: org.matsim.*
 * PassengerRoutingModule.java
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
package playground.thibautd.jointtrips.router;

import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;


/**
 * @author thibautd
 */
public class PassengerRoutingModule implements RoutingModule {
	private final PopulationFactory popFactory;
	private final ModeRouteFactory routeFactory;
	private final String modeName;

	public PassengerRoutingModule(
			final String modeName,
			final PopulationFactory popFactory,
			final ModeRouteFactory routeFactory) {
		this.modeName = modeName;
		this.popFactory = popFactory;
		this.routeFactory = routeFactory;
	}

	@Override
	public List<? extends PlanElement> calcRoute(
			final Facility fromFacility,
			final Facility toFacility,
			final double departureTime,
			final Person person) {
		Leg l = popFactory.createLeg( modeName );
		l.setDepartureTime( departureTime );
		Route r = routeFactory.createRoute( modeName , fromFacility.getLinkId() , toFacility.getLinkId() );
		l.setRoute( r );
		return Collections.singletonList( l );
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return EmptyStageActivityTypes.INSTANCE;
	}
}

