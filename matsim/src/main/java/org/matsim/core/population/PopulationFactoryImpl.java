/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.core.population;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.population.routes.RouteFactory;

/**
 * @author dgrether, mrieser
 */
public class PopulationFactoryImpl implements PopulationFactory {

	private final ModeRouteFactory routeFactory;

    PopulationFactoryImpl(ModeRouteFactory routeFactory) {
        this.routeFactory = routeFactory;
    }

    @Override
	public Person createPerson(final Id<Person> id) {
        return PersonImpl.createPerson(id);
	}

	@Override
	public Plan createPlan(){
		return new PlanImpl();
	}

	@Override
	public Activity createActivityFromCoord(final String actType, final Coord coord) {
        return new ActivityImpl(actType, coord);
	}

	@Override
	public Activity createActivityFromLinkId(final String actType, final Id<Link> linkId) {
        return new ActivityImpl(actType, linkId);
	}

	@Override
	public Leg createLeg(final String legMode) {
		return new LegImpl(legMode);
	}

	/**
	 * @param transportMode the transport mode the route should be for
	 * @param startLinkId the link where the route starts
	 * @param endLinkId the link where the route ends
	 * @return a new Route for the specified mode
	 *
	 * @see #setRouteFactory(String, RouteFactory)
	 */
	@Override
	public Route createRoute(final String transportMode, final Id<Link> startLinkId, final Id<Link> endLinkId) {
		return this.routeFactory.createRoute(transportMode, startLinkId, endLinkId);
	}

	/**
	 * Registers a {@link RouteFactory} for the specified mode. If <code>factory</code> is <code>null</code>,
	 * the existing entry for this <code>mode</code> will be deleted. If <code>mode</code> is <code>null</code>,
	 * then the default factory is set that is used if no specific RouteFactory for a mode is set.
	 *
	 */
	public void setRouteFactory(final String transportMode, final RouteFactory factory) {
		this.routeFactory.setRouteFactory(transportMode, factory);
	}

	public ModeRouteFactory getModeRouteFactory() {
		return this.routeFactory;
	}

}
