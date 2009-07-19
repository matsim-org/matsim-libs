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

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.basic.v01.population.BasicPerson;
import org.matsim.core.api.experimental.Scenario;
import org.matsim.core.api.experimental.network.Link;
import org.matsim.core.api.experimental.network.Network;
import org.matsim.core.api.experimental.population.Plan;
import org.matsim.core.api.experimental.population.PopulationBuilder;
import org.matsim.core.api.experimental.population.Route;
import org.matsim.core.facilities.ActivityFacilities;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.routes.LinkNetworkRoute;
import org.matsim.core.population.routes.NetworkRoute;

/**
 * @author dgrether
 */
public class PopulationBuilderImpl implements PopulationBuilder {

//	private static final Logger log = Logger.getLogger(PopulationBuilderImpl.class);

	private final Network network;
	private final ActivityFacilities facilities;

	@Deprecated
	public PopulationBuilderImpl(final NetworkLayer network, final PopulationImpl population, final ActivityFacilities facilities) {
		this.network = network;
		this.facilities = facilities;
	}

	public PopulationBuilderImpl(final Scenario scenario) {
//		this.scenario = scenario;
		this.network = scenario.getNetwork();
//		this.population = scenario.getPopulation();
		this.facilities = null; // TODO [MR]
	}

	public PersonImpl createPerson(final Id id) {
		PersonImpl p = new PersonImpl(id);
		return p;
	}

	public Plan createPlan(){
		return new PlanImpl();
	}

	public PlanImpl createPlan(final BasicPerson person) {
		if (!(person instanceof PersonImpl)) {
			throw new IllegalArgumentException("person must be of type PersonImpl.");
		}
		return new PlanImpl((PersonImpl) person);
	}

	public ActivityImpl createActivityFromCoord(final String actType, final Coord coord) {
		ActivityImpl act = new ActivityImpl(actType, coord);
		return act;
	}

	public ActivityImpl createActivityFromFacilityId(final String actType, final Id facilityId) {
		ActivityImpl act = new ActivityImpl(actType, this.facilities.getFacilities().get(facilityId));
		return act;
	}

	public ActivityImpl createActivityFromLinkId(final String actType, final Id linkId) {
		ActivityImpl act = new ActivityImpl(actType, (LinkImpl) this.network.getLinks().get(linkId));
		return act;
	}

	public LegImpl createLeg(final TransportMode legMode) {
		return new LegImpl(legMode);
	}

	public Route createRoute(final Id startLinkId, final Id endLinkId, final List<Id> currentRouteLinkIds) {
		Link start = this.network.getLinks().get(startLinkId);
		Link end = this.network.getLinks().get(endLinkId);
		if (start == null) {
			throw new IllegalStateException("Can't create Route with StartLink Id " + startLinkId + " because the Link cannot be found in the loaded Network.");
		}
		if (end == null) {
			throw new IllegalStateException("Can't create Route with EndLink Id " + startLinkId + " because the Link cannot be found in the loaded Network.");
		}
		NetworkRoute route = new LinkNetworkRoute(start, end);
		List<Link> links = new ArrayList<Link>();
		Link link;
		for (Id id : currentRouteLinkIds) {
			link = this.network.getLinks().get(id);
			if (link == null) {
				throw new IllegalStateException("Can't create Route over Link with Id " + id + " because the Link cannot be found in the loaded Network.");
			}
			links.add(this.network.getLinks().get(id));
		}
		route.setLinks(start, links, end);
		return route;
	}

}
