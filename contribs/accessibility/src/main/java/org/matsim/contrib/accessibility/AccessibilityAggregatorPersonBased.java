/* *********************************************************************** *
 * project: org.matsim.*												   *
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
package org.matsim.contrib.accessibility;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.facilities.ActivityFacility;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author dziemke
 */
class AccessibilityAggregatorPersonBased implements PersonDataExchangeInterface {
	private final Logger LOG = LogManager.getLogger(AccessibilityAggregatorPersonBased.class);

	// ((Person, timeOfDay),(mode -> accessibility))
	private final Map<Tuple<Person, Double>, Map<String,Double>> accessibilitiesMap = new ConcurrentHashMap<>();

	@Override
	public void setPersonAccessibilities(Person person, Double timeOfDay, String mode, double accessibility) {
		Tuple<Person, Double> key = new Tuple<>(person, timeOfDay);
		if (!accessibilitiesMap.containsKey(key)) {
			Map<String,Double> accessibilitiesByMode = new HashMap<>();
			accessibilitiesMap.put(key, accessibilitiesByMode);
		}
		accessibilitiesMap.get(key).put(mode, accessibility);
	}

	@Override
	public void finish() {
	}

	public Map<Tuple<Person, Double>, Map<String,Double>> getAccessibilitiesMap() {
		return accessibilitiesMap;
	}


}
