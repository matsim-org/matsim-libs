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

package org.matsim.contrib.multimodal.tools;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.core.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.utils.collections.CollectionUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Drops all non car routes which are specified in the multiModalConfigGroup
 * ("simulatedModes").
 */
class NonCarRouteDropper extends AbstractPersonAlgorithm implements PlanAlgorithm {

	private static final Logger log = Logger.getLogger(NonCarRouteDropper.class);

	private final Set<String> modesToDrop = new HashSet<>();

	public NonCarRouteDropper(MultiModalConfigGroup multiModalConfigGroup) {

		if (!multiModalConfigGroup.isDropNonCarRoutes()) {
			log.warn("Dropping of non car routes is not enabled in the config group - routes will not be dropped!");
			return;
		}

        Collections.addAll(this.modesToDrop, CollectionUtils.stringToArray(multiModalConfigGroup.getSimulatedModes()));
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