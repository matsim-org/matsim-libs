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

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.facilities.ActivityFacility;

/**
 * @author dziemke
 */
class AccessibilityAggregator implements FacilityDataExchangeInterface {
	private final Logger LOG = Logger.getLogger(AccessibilityAggregator.class);

	private Map<Tuple<ActivityFacility, Double>, Map<String,Double>> accessibilitiesMap = new HashMap<>();

	@Override
	public void setFacilityAccessibilities(ActivityFacility measurePoint, Double timeOfDay, String mode, double accessibility) {
		Tuple<ActivityFacility, Double> key = new Tuple<>(measurePoint, timeOfDay);
		if (!accessibilitiesMap.containsKey(key)) {
			Map<String,Double> accessibilitiesByMode = new HashMap<>();
			accessibilitiesMap.put(key, accessibilitiesByMode);
		}
		accessibilitiesMap.get(key).put(mode, accessibility);
	}

	@Override
	public void finish() {
	}

	public Map<Tuple<ActivityFacility, Double>, Map<String,Double>> getAccessibilitiesMap() {
		Map<Tuple<ActivityFacility, Double>, Map<String, Double>> accessibilitiesMap2 = sortMeasurePointsByYAndXCoord();
		return accessibilitiesMap2;
	}

	private Map<Tuple<ActivityFacility, Double>, Map<String, Double>> sortMeasurePointsByYAndXCoord() {
		LOG.info("Start sorting measure points.");
		Map<Double, List<Double>> coordMap = new TreeMap<>();
		List<Double> yValues = new LinkedList<>();

		for (Tuple<ActivityFacility, Double> tuple : accessibilitiesMap.keySet()) {
			ActivityFacility activityFacility = tuple.getFirst();
			double y = activityFacility.getCoord().getY();
			if (!yValues.contains(y)) {
				yValues.add(y);
			}
		}
		yValues.sort(Comparator.naturalOrder());

		for (double yGiven : yValues) {
			List<Double> xValues = new LinkedList<>();
			for (Tuple<ActivityFacility, Double> tuple : accessibilitiesMap.keySet()) {
				ActivityFacility activityFacility = tuple.getFirst();
				double y = activityFacility.getCoord().getY();
				if (y == yGiven) {
					double x = activityFacility.getCoord().getX();
					if (!xValues.contains(x)) {
						xValues.add(x);
					}
				}
			}
			xValues.sort(Comparator.naturalOrder());
			coordMap.put(yGiven, xValues);
		}

		Map<Tuple<ActivityFacility, Double>, Map<String,Double>> accessibilitiesMap2 = new LinkedHashMap<>();
		for (double y : coordMap.keySet()) {
			for (double x : coordMap.get(y)) {
				for (Tuple<ActivityFacility, Double> tuple : accessibilitiesMap.keySet()) {
					Coord coord = tuple.getFirst().getCoord();
					if (coord.getX() == x && coord.getY() == y) {
						accessibilitiesMap2.put(tuple, accessibilitiesMap.get(tuple));
					}
				}
			}
		}
		LOG.info("Finish sorting measure points.");
		return accessibilitiesMap2;
	}
}
