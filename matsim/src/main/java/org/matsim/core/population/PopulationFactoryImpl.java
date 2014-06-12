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
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.population.routes.CompressedNetworkRouteFactory;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.population.routes.RouteFactory;

/**
 * @author dgrether, mrieser
 */
public class PopulationFactoryImpl implements PopulationFactory {

	private final ModeRouteFactory routeFactory;

	PopulationFactoryImpl(final Config config, final Network network ) {
		this.routeFactory = new ModeRouteFactory();
		String networkRouteType = config.plans().getNetworkRouteType();
		RouteFactory factory;
		if (PlansConfigGroup.NetworkRouteType.LinkNetworkRoute.equals(networkRouteType)) {
			factory = new LinkNetworkRouteFactory();
		} else if (PlansConfigGroup.NetworkRouteType.CompressedNetworkRoute.equals(networkRouteType)) {
			factory = new CompressedNetworkRouteFactory( network );
		} else {
			throw new IllegalArgumentException("The type \"" + networkRouteType + "\" is not a supported type for network routes.");
		}
		for (String transportMode : config.plansCalcRoute().getNetworkModes()) {
			this.routeFactory.setRouteFactory(transportMode, factory);
		}
	}

    PopulationFactoryImpl(Config config) {
		this( config, null ) ; // the idea is that this allows everything except setting compressed routes. kai, feb'14
	}

    @Override
	public Person createPerson(final Id id) {
        return new PersonImpl(id);
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
	public Activity createActivityFromLinkId(final String actType, final Id linkId) {
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
	public Route createRoute(final String transportMode, final Id startLinkId, final Id endLinkId) {
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
