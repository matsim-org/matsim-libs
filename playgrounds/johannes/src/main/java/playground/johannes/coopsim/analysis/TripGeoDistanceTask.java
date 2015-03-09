/* *********************************************************************** *
 * project: org.matsim.*
 * TripDistanceTask.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.coopsim.analysis;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.facilities.ActivityFacilities;

import playground.johannes.coopsim.pysical.Trajectory;
import playground.johannes.socialnetworks.gis.DistanceCalculator;
import playground.johannes.socialnetworks.gis.OrthodromicDistanceCalculator;

/**
 * @author illenberger
 * 
 */
public class TripGeoDistanceTask extends TrajectoryAnalyzerTask {

	private final ActivityFacilities facilities;

	private final DistanceCalculator calculator;

	public TripGeoDistanceTask(ActivityFacilities facilities) {
		this.facilities = facilities;
		calculator = OrthodromicDistanceCalculator.getInstance();
	}

	public TripGeoDistanceTask(ActivityFacilities facilities, DistanceCalculator calculator) {
		this.facilities = facilities;
		this.calculator = calculator;
	}

	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		Map<String, PlanElementConditionComposite<Leg>> map = Conditions.getLegConditions(trajectories);

		TripDistanceMean tripDistance = new TripDistanceMean(facilities, calculator);

		for (Entry<String, PlanElementConditionComposite<Leg>> entry : map.entrySet()) {
			tripDistance.setCondition(entry.getValue());
			DescriptiveStatistics stats = tripDistance.statistics(trajectories, true);

			String key = String.format("d.trip.%s", entry.getKey());
			results.put(key, stats);
			try {
				writeHistograms(stats, key, 100, 50);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
