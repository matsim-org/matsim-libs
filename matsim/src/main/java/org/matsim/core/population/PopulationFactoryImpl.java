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

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;

/**
 * @author dgrether
 */
public class PopulationFactoryImpl implements PopulationFactory {

//	private static final Logger log = Logger.getLogger(PopulationBuilderImpl.class);

	private final Scenario scenario;

	public PopulationFactoryImpl(final Scenario scenario) {
		this.scenario = scenario;
	}

	public PersonImpl createPerson(final Id id) {
		PersonImpl p = new PersonImpl(id);
		return p;
	}

	public Plan createPlan(){
		return new PlanImpl();
	}

	public ActivityImpl createActivityFromCoord(final String actType, final Coord coord) {
		ActivityImpl act = new ActivityImpl(actType, coord);
		return act;
	}

	public ActivityImpl createActivityFromFacilityId(final String actType, final Id facilityId) {
		ActivityImpl act = new ActivityImpl(actType, ((ScenarioImpl) this.scenario).getActivityFacilities().getFacilities().get(facilityId));
		return act;
	}

	public ActivityImpl createActivityFromLinkId(final String actType, final Id linkId) {
		ActivityImpl act = new ActivityImpl(actType, (LinkImpl) this.scenario.getNetwork().getLinks().get(linkId));
		return act;
	}

	public LegImpl createLeg(final TransportMode legMode) {
		return new LegImpl(legMode);
	}

	public Route createRoute(final Id startLinkId, final Id endLinkId, final List<Id> currentRouteLinkIds) {
		Link start = this.scenario.getNetwork().getLinks().get(startLinkId);
		Link end = this.scenario.getNetwork().getLinks().get(endLinkId);
		if (start == null) {
			throw new IllegalStateException("Can't create Route with StartLink Id " + startLinkId + " because the Link cannot be found in the loaded Network.");
		}
		if (end == null) {
			throw new IllegalStateException("Can't create Route with EndLink Id " + startLinkId + " because the Link cannot be found in the loaded Network.");
		}
		NetworkRouteWRefs route = new LinkNetworkRouteImpl(start, end);
		List<Link> links = new ArrayList<Link>();
		Link link;
		for (Id id : currentRouteLinkIds) {
			link = this.scenario.getNetwork().getLinks().get(id);
			if (link == null) {
				throw new IllegalStateException("Can't create Route over Link with Id " + id + " because the Link cannot be found in the loaded Network.");
			}
			links.add(this.scenario.getNetwork().getLinks().get(id));
		}
		route.setLinks(start, links, end);
		return route;
	}

}
