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

import jakarta.inject.Inject;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.population.routes.RouteFactory;
import org.matsim.facilities.ActivityFacility;

/**
 * @author dgrether, mrieser
 */
/* deliberately package */ class PopulationFactoryImpl implements PopulationFactory {

	private final RouteFactories routeFactory;

    @Inject
	PopulationFactoryImpl(RouteFactories routeFactory) {
        this.routeFactory = routeFactory;
    }

    @Override
	public Person createPerson(final Id<Person> id) {
        return new PersonImpl(id) ;
	}

	@Override
	public Plan createPlan(){
		return new PlanImpl() ;
	}

	@Override
	public Activity createActivityFromCoord(final String actType, final Coord coord) {
        Activity act = new ActivityImpl(actType) ;
        act.setCoord(coord);
        return act ;
	}

	@Override
	public Activity createInteractionActivityFromCoord(final String actType, final Coord coord) {
        Activity act = new InteractionActivity(actType) ;
        act.setCoord(coord);
        return act ;
	}

	@Override
	public Activity createActivityFromLinkId(final String actType, final Id<Link> linkId) {
	        Activity act = new ActivityImpl(actType) ;
	        act.setLinkId(linkId);
	        return act ;
	}

	@Override
	public Activity createInteractionActivityFromLinkId(final String actType, final Id<Link> linkId) {
	        Activity act = new InteractionActivity(actType) ;
	        act.setLinkId(linkId);
	        return act ;
	}

	@Override
	public Activity createActivityFromActivityFacilityId( String actType, Id<ActivityFacility> activityFacilityId ){
		Activity act = new ActivityImpl( actType ) ;
		act.setFacilityId( activityFacilityId );
		return act ;
	}

	@Override
	public Activity createInteractionActivityFromActivityFacilityId( String actType, Id<ActivityFacility> activityFacilityId ){
		Activity act = new InteractionActivity( actType ) ;
		act.setFacilityId( activityFacilityId );
		return act ;
	}

	@Override
	public Leg createLeg(final String legMode) {
		return new LegImpl(legMode) ;
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

	@Override
	public RouteFactories getRouteFactories() {
		return this.routeFactory;
	}

}
