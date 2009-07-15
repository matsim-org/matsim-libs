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

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.basic.v01.population.BasicPerson;
import org.matsim.core.api.experimental.ScenarioImpl;
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

	private static final Logger log = Logger.getLogger(PopulationBuilderImpl.class);

	private final NetworkLayer network;
	private final ActivityFacilities facilities;

	@Deprecated
	public PopulationBuilderImpl(NetworkLayer network, PopulationImpl population, ActivityFacilities facilities) {
		this.network = network;
		this.facilities = facilities;
	}
	
	public PopulationBuilderImpl(final ScenarioImpl scenario) {
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
	
	public PlanImpl createPlan(BasicPerson person) {
		if (!(person instanceof PersonImpl)) {
			throw new IllegalArgumentException("person must be of type Person.");
		}
		return new PlanImpl((PersonImpl) person);
	}

	public ActivityImpl createActivityFromCoord(String actType, Coord coord) {
		ActivityImpl act = new ActivityImpl(actType, coord);
		return act;
	}

	public ActivityImpl createActivityFromFacilityId(String actType, Id facilityId) {
		ActivityImpl act = new ActivityImpl(actType, this.facilities.getFacilities().get(facilityId));
		return act;
	}

	public ActivityImpl createActivityFromLinkId(String actType, Id linkId) {
		ActivityImpl act = new ActivityImpl(actType, this.network.getLinks().get(linkId));
		return act;
	}
	
	public LegImpl createLeg(final TransportMode legMode) {
		return new LegImpl(legMode);
	}

	public Route createRoute(final Id startLinkId, final Id endLinkId, final List<Id> currentRouteLinkIds) {
		LinkImpl start = this.network.getLink(startLinkId);
		LinkImpl end = this.network.getLink(endLinkId);
		if (start == null) {
			throw new IllegalStateException("Cann't create Route with StartLink Id " + startLinkId + " because the Link cannot be found in the loaded Network.");
		}
		if (end == null) {
			throw new IllegalStateException("Cann't create Route with EndLink Id " + startLinkId + " because the Link cannot be found in the loaded Network.");
		}
		NetworkRoute route = new LinkNetworkRoute(start, end);
		List<LinkImpl> links = new ArrayList<LinkImpl>();
		LinkImpl link;
		for (Id id : currentRouteLinkIds) {
			link = this.network.getLink(id);
			if (link == null) {
				throw new IllegalStateException("Cann't create Route over Link with Id " + id + " because the Link cannot be found in the loaded Network.");
			}
			links.add(this.network.getLink(id));
		}
		route.setLinks(start, links, end);
		return route;
	}

}
