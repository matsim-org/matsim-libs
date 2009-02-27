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

import org.matsim.interfaces.basic.v01.BasicAct;
import org.matsim.interfaces.basic.v01.BasicLeg;
import org.matsim.interfaces.basic.v01.BasicLocation;
import org.matsim.interfaces.basic.v01.BasicPerson;
import org.matsim.interfaces.basic.v01.BasicPlan;
import org.matsim.interfaces.basic.v01.BasicPopulation;
import org.matsim.interfaces.basic.v01.BasicPopulationBuilder;
import org.matsim.interfaces.basic.v01.BasicRoute;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.basic.v01.BasicLeg.Mode;

/**
 * @author dgrether
 */
public class BasicPopulationBuilderImpl implements BasicPopulationBuilder {

	private final BasicPopulation population;

	public BasicPopulationBuilderImpl(final BasicPopulation pop) {
		this.population = pop;
	}

	public BasicAct createAct(final BasicPlan basicPlan, final String currentActType,
			final BasicLocation currentlocation) {
		BasicActImpl act = new BasicActImpl(currentActType);
		basicPlan.addAct(act);
		if (currentlocation != null) {
			if (currentlocation.getCenter() != null) {
				act.setCoord(currentlocation.getCenter());
			}
			else if (currentlocation.getId() != null){
				if (currentlocation.getLocationType() == LocationType.FACILITY) {
					act.setFacilityId(currentlocation.getId());
				}
				else if (currentlocation.getLocationType() == LocationType.LINK) {
					act.setLinkId(currentlocation.getId());
				}
			}
		}
		return act;
	}

	public BasicLeg createLeg(final BasicPlan basicPlan, final Mode legMode) {
		BasicLegImpl leg = new BasicLegImpl(legMode);
		basicPlan.addLeg(leg);
		return leg;
	}

	@SuppressWarnings("unchecked")
	public BasicPerson createPerson(final Id id) throws Exception {
		BasicPerson p = new BasicPersonImpl(id);
		this.population.addPerson(p);
		return p;
	}

	public BasicPlan createPlan(final BasicPerson currentPerson) {
		BasicPlan plan = new BasicPlanImpl();
		currentPerson.addPlan(plan);
		return plan;
	}

	public BasicPlan createPlan(final BasicPerson person, final boolean selected) {
		BasicPlan p = createPlan(person);
		p.setSelected(true);
		return p;
	}

	public BasicRoute createRoute(final Id startLinkId, final Id endLinkId, final List<Id> currentRouteLinkIds) {
		BasicRouteImpl route = new BasicRouteImpl(startLinkId, endLinkId);
		if (!currentRouteLinkIds.isEmpty()) {
				List<Id> r = new ArrayList<Id>(currentRouteLinkIds);
				route.setLinkIds(r);
		}
		return route;
	}

	public BasicActivity createActivity(final String type, final BasicLocation currentlocation) {
		BasicActivityImpl ba = new BasicActivityImpl(type);
		ba.setLocation(currentlocation);
		return ba;
	}

	public BasicKnowledge createKnowledge(final List<BasicActivity> currentActivities) {
		BasicKnowledgeImpl kn = new BasicKnowledgeImpl();
		for (BasicActivity ba : currentActivities){
			kn.addActivity(ba);
		}
		return kn;
	}

//	public BasicAct createAct(BasicPlan basicPlan, String currentActType, Coord coord) {
//
//			BasicActImpl act = new BasicActImpl(currentActType);
//			basicPlan.addAct(act);
//			act.setCoord(coord);
//			return act;
//	}

}
