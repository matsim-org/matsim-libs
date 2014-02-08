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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
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
		RouteFactory factory = null;
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

	@Deprecated // please get the factory from population.getFactory(). kai, feb'14
	public PopulationFactoryImpl(Scenario scenario) {
		this( scenario.getConfig(), scenario.getNetwork() ) ;
	}

	PopulationFactoryImpl(Config config) {
		this( config, null ) ; // the idea is that this allows everything except setting compressed routes. kai, feb'14
	}

	@Override
	public Person createPerson(final Id id) {
		PersonImpl p = new PersonImpl(id);
		return p;
	}

	@Override
	public Plan createPlan(){
		return new PlanImpl();
	}

	@Override
	public Activity createActivityFromCoord(final String actType, final Coord coord) {
		ActivityImpl act = new ActivityImpl(actType, coord);
		return act;
	}

//	private Activity createActivityFromFacilityIdAndSetCoordAndLinkId(final String actType, final Id facilityId) {
//		ActivityImpl act = new ActivityImpl(actType);
//		act.setFacilityId(facilityId);
//
//		final ActivityFacility activityFacility = this.scenario.getActivityFacilities().getFacilities().get( facilityId);
//		act.setCoord( activityFacility.getCoord() ) ;
//		act.setLinkId( activityFacility.getLinkId() ) ;
//		// yyyyyy I am pretty sure that this is something we had decided to avoid: containers should not depend on each other.
//		// kai, dec'13
//		
//		return act;
//	}
	// does not seem to be used. kai, feb'14

	@Override
	public Activity createActivityFromLinkId(final String actType, final Id linkId) {
		ActivityImpl act = new ActivityImpl(actType, linkId);
		return act;
	}

	@Override
	public Leg createLeg(final String legMode) {
		return new LegImpl(legMode);
	}

	/**
	 * @param transportMode the transport mode the route should be for
	 * @param startLink the link where the route starts
	 * @param endLink the link where the route ends
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
	 * @param transportMode
	 * @param factory
	 */
	public void setRouteFactory(final String transportMode, final RouteFactory factory) {
		this.routeFactory.setRouteFactory(transportMode, factory);
	}

	public ModeRouteFactory getModeRouteFactory() {
		return this.routeFactory;
	}

}
