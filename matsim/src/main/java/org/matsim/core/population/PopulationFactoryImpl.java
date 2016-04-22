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
import org.matsim.core.population.routes.RouteFactoryImpl;
import org.matsim.core.population.routes.RouteFactory;

import javax.inject.Inject;

/**
 * @author dgrether, mrieser
 */
public class PopulationFactoryImpl implements PopulationFactory {

	private final RouteFactoryImpl routeFactory;

    @Inject
	PopulationFactoryImpl(RouteFactoryImpl routeFactory) {
        this.routeFactory = routeFactory;
    }

    @Override
	public Person createPerson(final Id<Person> id) {
        return PopulationUtils.createPerson(id);
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
	 * @param routeType the type of the requested route
	 * @param startLinkId the link where the route starts
	 * @param endLinkId the link where the route ends
	 * @return a new Route for the specified mode
	 *
	 * @see #setRouteFactory(Class, RouteFactory)
	 */
	@Override
	public <R extends Route> R createRoute(final Class<R> routeType, final Id<Link> startLinkId, final Id<Link> endLinkId) {
		return this.routeFactory.createRoute(routeType, startLinkId, endLinkId);
	}

	/**
	 * Registers a {@link RouteFactory} for the specified route type. If <code>factory</code> is <code>null</code>,
	 * the existing entry for this <code>routeType</code> will be deleted. If <code>routeType</code> is <code>null</code>,
	 * then the default factory is set that is used if no specific RouteFactory for a routeType is set.
	 *
	 */
	public void setRouteFactory(final Class<? extends Route> routeType, final RouteFactory factory) {
		this.routeFactory.setRouteFactory(routeType, factory);
	}

	@Deprecated // "createRoute(...)", which is already in the official PopulationFactory interface, should be able to achieve the same thing. kai, apr'16
	public RouteFactoryImpl getRouteFactory() {
		return this.routeFactory;
	}

}
