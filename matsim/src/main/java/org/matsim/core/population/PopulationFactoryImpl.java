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

import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.misc.RouteUtils;

/**
 * @author dgrether
 */
public class PopulationFactoryImpl implements PopulationFactory {

	private final Scenario scenario;

	public PopulationFactoryImpl(final Scenario scenario) {
		this.scenario = scenario;
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

	public Activity createActivityFromFacilityId(final String actType, final Id facilityId) {
		ActivityImpl act = new ActivityImpl(actType);
		act.setFacilityId(facilityId);
		return act;
	}

	@Override
	public Activity createActivityFromLinkId(final String actType, final Id linkId) {
		ActivityImpl act = new ActivityImpl(actType, linkId);
		return act;
	}

	@Override
	public Leg createLeg(final String legMode) {
		return new LegImpl(legMode);
	}

	public Route createRoute(final Id startLinkId, final Id endLinkId, final List<Id> currentRouteLinkIds) {
		NetworkRoute route = new LinkNetworkRouteImpl(startLinkId, endLinkId);
		route.setLinkIds(startLinkId, currentRouteLinkIds, endLinkId);
		route.setDistance(RouteUtils.calcDistance(route, this.scenario.getNetwork()));
		return route;
	}

}
