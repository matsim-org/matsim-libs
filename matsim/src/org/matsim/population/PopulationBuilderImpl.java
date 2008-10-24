/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
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

package org.matsim.population;

import java.util.ArrayList;
import java.util.List;

import org.matsim.basic.v01.BasicAct;
import org.matsim.basic.v01.BasicLeg;
import org.matsim.basic.v01.BasicLocationImpl;
import org.matsim.basic.v01.BasicPerson;
import org.matsim.basic.v01.BasicPlan;
import org.matsim.basic.v01.BasicRoute;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.BasicLeg.Mode;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.interfaces.basic.v01.PopulationBuilder;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;

/**
 * @author dgrether
 */
public class PopulationBuilderImpl implements PopulationBuilder {

	private Population population;
	private NetworkLayer network;
	private Facilities facilities;

	public PopulationBuilderImpl(NetworkLayer network, Population population, Facilities facilities) {
		this.network = network;
		this.population = population;
		this.facilities = facilities;
	}

	public BasicAct createAct(BasicPlan basicPlan, String currentActType,
			BasicLocationImpl currentlocation) {
		Act act = null;
		if (currentlocation != null) {
			if (currentlocation.getCoord() != null) {
				act = ((Plan)basicPlan).createAct(currentActType, currentlocation.getCoord());
			}
			else if (currentlocation.getLocationId() != null){
				if (currentlocation.isFacilityId()) {
					Facility fac = facilities.getFacilities().get(currentlocation.getLocationId()); 
					act = ((Plan)basicPlan).createAct(currentActType, fac);
				}
				else if (currentlocation.isLinkId()) {
					Link link = this.network.getLink(currentlocation.getLocationId());
					act = ((Plan)basicPlan).createAct(currentActType, link);
				}
			}
		}
		return act;
	}

	public BasicLeg createLeg(BasicPlan basicPlan, Mode legMode) {
		return ((Plan)basicPlan).createLeg(legMode);
	}

	public BasicPerson createPerson(Id id) {
		Person p = new Person(id);
		this.population.addPerson(p);
		return p;
	}

	public BasicPlan createPlan(BasicPerson person, boolean selected) {
		Person p = (Person) person;
		return p.createPlan(false);
	}

	public BasicPlan createPlan(BasicPerson currentPerson) {
		return createPlan(currentPerson, false);
	}

	public BasicRoute createRoute(List<Id> currentRouteLinkIds) {
		Route route = new Route();
		List<Link> links = new ArrayList<Link>();
		for (Id id : currentRouteLinkIds) {
			links.add(this.network.getLink(id));
		}
		route.setLinkRoute(links);
		return route;
	}

}
