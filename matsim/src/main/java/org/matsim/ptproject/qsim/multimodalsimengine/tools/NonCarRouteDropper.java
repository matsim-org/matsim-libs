/* *********************************************************************** *
 * project: org.matsim.*
 * NonCarRouteDropper.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.ptproject.qsim.multimodalsimengine.tools;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.groups.MultiModalConfigGroup;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

/*
 * Drops all non car routes which are specified in the multiModalConfigGroup
 * ("simulatedModes").
 */
public class NonCarRouteDropper extends AbstractPersonAlgorithm implements PlanAlgorithm {

	private static final Logger log = Logger.getLogger(NonCarRouteDropper.class);
	
	private Set<String> modesToDrop = new HashSet<String>();
	
	public NonCarRouteDropper(MultiModalConfigGroup multiModalConfigGroup) {

		if (!multiModalConfigGroup.isDropNonCarRoutes()) {
			log.warn("Dropping of non car routes is not enabled in the config group - routes will not be dropped!");
			return;
		}
		
		String simulatedModes = multiModalConfigGroup.getSimulatedModes();
		if (simulatedModes.contains("walk")) modesToDrop.add(TransportMode.walk);
		if (simulatedModes.contains("bike")) modesToDrop.add(TransportMode.bike);
		if (simulatedModes.contains("pt")) modesToDrop.add(TransportMode.pt);
		if (simulatedModes.contains("ride")) modesToDrop.add(TransportMode.ride);
	}
	
	@Override
	public void run(Plan plan) {
		for (PlanElement planElement : plan.getPlanElements()) {
			if (planElement instanceof Leg) {
				Leg leg = (Leg) planElement;
				if (modesToDrop.contains(leg.getMode())) {
					leg.setRoute(null);
				}
			}
		}
	}

	@Override
	public void run(Person person) {
		for (Plan plan : person.getPlans()) {
			run(plan);
		}
	}
}