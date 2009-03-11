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

import org.matsim.interfaces.basic.v01.BasicActivityOption;
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

	public BasicActivityOption createActivity(final String type, final BasicLocation currentlocation) {
		BasicActivityOptionImpl ba = new BasicActivityOptionImpl(type);
		ba.setLocation(currentlocation);
		return ba;
	}

	public BasicKnowledge createKnowledge(final List<BasicActivityOption> currentActivities) {
		BasicKnowledgeImpl kn = new BasicKnowledgeImpl();
		for (BasicActivityOption ba : currentActivities){
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
