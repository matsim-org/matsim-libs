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
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.facilities.Facilities;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Network;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.api.population.PopulationBuilder;
import org.matsim.core.api.population.Route;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.routes.LinkNetworkRoute;

/**
 * @author dgrether
 */
public class PopulationBuilderImpl implements PopulationBuilder {

	private static final Logger log = Logger.getLogger(PopulationBuilderImpl.class);

	private final Population population;
	private final Network network;
	private final Facilities facilities;
	private final Scenario scenario;

	@Deprecated
	public PopulationBuilderImpl(NetworkLayer network, Population population, Facilities facilities) {
		this.network = network;
		this.population = population;
		this.facilities = facilities;
		this.scenario = null;
	}
	
	public PopulationBuilderImpl(final Scenario scenario) {
		this.scenario = scenario;
		this.network = scenario.getNetwork();
		this.population = scenario.getPopulation();
		this.facilities = null; // TODO [MR]
	}

	public Person createPerson(final Id id) {
		Person p = new PersonImpl(id);
		return p;
	}
	
	public Plan createPlan(BasicPerson person) {
		if (!(person instanceof Person)) {
			throw new IllegalArgumentException("person must be of type Person.");
		}
		return new PlanImpl((Person) person);
	}

	public Activity createActivityFromCoord(String actType, Coord coord) {
		ActivityImpl act = new ActivityImpl(actType, coord);
		return act;
	}

	public Activity createActivityFromFacilityId(String actType, Id facilityId) {
		ActivityImpl act = new ActivityImpl(actType, this.facilities.getFacilities().get(facilityId));
		return act;
	}

	public Activity createActivityFromLinkId(String actType, Id linkId) {
		ActivityImpl act = new ActivityImpl(actType, this.network.getLinks().get(linkId));
		return act;
	}
	
	public Leg createLeg(final TransportMode legMode) {
		return new LegImpl(legMode);
	}

	public Route createRoute(final Id startLinkId, final Id endLinkId, final List<Id> currentRouteLinkIds) {
		Link start = this.network.getLink(startLinkId);
		Link end = this.network.getLink(endLinkId);
		if (start == null) {
			throw new IllegalStateException("Cann't create Route with StartLink Id " + startLinkId + " because the Link cannot be found in the loaded Network.");
		}
		if (end == null) {
			throw new IllegalStateException("Cann't create Route with EndLink Id " + startLinkId + " because the Link cannot be found in the loaded Network.");
		}
		NetworkRoute route = new LinkNetworkRoute(start, end);
		List<Link> links = new ArrayList<Link>();
		Link link;
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
