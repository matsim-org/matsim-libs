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

import org.matsim.basic.v01.BasicLeg.Mode;
import org.matsim.interfaces.basic.v01.BasicLocation;

/**
 * @author dgrether
 */
public class BasicPopulationBuilder implements PopulationBuilder {

	private BasicPopulation population;

	public BasicPopulationBuilder(BasicPopulation pop) {
		this.population = pop;
	}

	public BasicAct createAct(BasicPlan basicPlan, String currentActType,
			BasicLocation currentlocation) {
		BasicActImpl act = new BasicActImpl(currentActType);
		basicPlan.addAct(act);
		if (currentlocation != null) {
			if (currentlocation.getCoord() != null) {
				act.setCoord(currentlocation.getCoord());
			}
			else if (currentlocation.getLocationId() != null){
				if (currentlocation.isFacilityId()) {
					act.setFacilityId(currentlocation.getLocationId());
				}
				else if (currentlocation.isLinkId()) {
					act.setLinkId(currentlocation.getLocationId());
				}
			}
		}
		return act;
	}

	public BasicLeg createLeg(BasicPlan basicPlan, Mode legMode) {
		BasicLegImpl leg = new BasicLegImpl(legMode); 
		basicPlan.addLeg(leg);
		return leg;
	}

	@SuppressWarnings("unchecked")
	public BasicPerson createPerson(Id id) throws Exception {
		BasicPerson p = new BasicPersonImpl(id);
		this.population.addPerson(p);
		return p;
	}

	public BasicPlan createPlan(BasicPerson currentPerson) {
		BasicPlan plan = new BasicPlanImpl();
		currentPerson.addPlan(plan);
		return plan;
	}
	
	public BasicPlan createPlan(BasicPerson person, boolean selected) {
		BasicPlan p = createPlan(person);
		p.setSelected(true);
		return p;
	}
	
	public BasicRoute createRoute(List<Id> currentRouteLinkIds) {
		BasicRouteImpl route = new BasicRouteImpl();
		List<Id> r = new ArrayList<Id>(currentRouteLinkIds);
		route.setLinkIds(r);
		return route;
	}

	public BasicActivity createActivity(String type, BasicLocation currentlocation) {
		BasicActivityImpl ba = new BasicActivityImpl(type);
		ba.setLocation(currentlocation);
		return ba;
	}

	public BasicKnowledge createKnowledge(List<BasicActivity> currentActivities) {
		BasicKnowledgeImpl kn = new BasicKnowledgeImpl();
		for (BasicActivity ba : currentActivities){
			kn.addActivity(ba);
		}
		return kn;
	}

}
