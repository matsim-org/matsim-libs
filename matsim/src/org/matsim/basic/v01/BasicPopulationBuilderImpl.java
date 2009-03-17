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

package org.matsim.basic.v01;

import java.util.ArrayList;
import java.util.List;

import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.basic.v01.population.BasicActivity;
import org.matsim.interfaces.basic.v01.population.BasicLeg;
import org.matsim.interfaces.basic.v01.population.BasicPerson;
import org.matsim.interfaces.basic.v01.population.BasicPlan;
import org.matsim.interfaces.basic.v01.population.BasicPopulation;
import org.matsim.interfaces.basic.v01.population.BasicPopulationBuilder;
import org.matsim.interfaces.basic.v01.population.BasicRoute;
import org.matsim.interfaces.basic.v01.population.BasicLeg.Mode;

/**
 * @author dgrether
 */
public class BasicPopulationBuilderImpl implements BasicPopulationBuilder {

	private final BasicPopulation population;

	public BasicPopulationBuilderImpl(final BasicPopulation pop) {
		this.population = pop;
	}

	public BasicPerson createPerson(final Id id) {
		return new BasicPersonImpl(id);
	}
	
	public BasicPlan createPlan(BasicPerson person) {
		return new BasicPlanImpl(person);
	}
	
	public BasicActivity createActivityFromCoord(String actType, Coord coord) {
		BasicActivityImpl act = new BasicActivityImpl(actType);
		act.setCoord(coord);
		return act;
	}

	public BasicActivity createActivityFromFacilityId(String actType, Id facilityId) {
		BasicActivityImpl act = new BasicActivityImpl(actType);
		act.setFacilityId(facilityId);
		return act;
	}

	public BasicActivity createActivityFromLinkId(String actType, Id linkId) {
		BasicActivityImpl act = new BasicActivityImpl(actType);
		act.setLinkId(linkId);
		return act;
	}
	
	public BasicLeg createLeg(final Mode legMode) {
		BasicLegImpl leg = new BasicLegImpl(legMode);
		return leg;
	}

	public BasicRoute createRoute(final Id startLinkId, final Id endLinkId, final List<Id> currentRouteLinkIds) {
		BasicRouteImpl route = new BasicRouteImpl(startLinkId, endLinkId);
		if (!currentRouteLinkIds.isEmpty()) {
				List<Id> r = new ArrayList<Id>(currentRouteLinkIds);
				route.setLinkIds(r);
		}
		return route;
	}

}
