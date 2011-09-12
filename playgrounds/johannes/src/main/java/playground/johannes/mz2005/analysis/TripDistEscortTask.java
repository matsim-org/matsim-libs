/* *********************************************************************** *
 * project: org.matsim.*
 * TripDistEscortTask.java
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
package playground.johannes.mz2005.analysis;

import gnu.trove.TDoubleObjectHashMap;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.contrib.sna.util.TXTWriter;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;

import playground.johannes.coopsim.analysis.TrajectoryAnalyzerTask;
import playground.johannes.coopsim.pysical.Trajectory;
import playground.johannes.coopsim.util.MatsimCoordUtils;
import playground.johannes.mz2005.io.EscortData;
import playground.johannes.socialnetworks.gis.DistanceCalculator;

/**
 * @author illenberger
 * 
 */
public class TripDistEscortTask extends TrajectoryAnalyzerTask {

	private final EscortData escortData;

	private final ActivityFacilities facilities;

	private final DistanceCalculator calculator;

	public TripDistEscortTask(EscortData escortData, ActivityFacilities facilities, DistanceCalculator calculator) {
		this.escortData = escortData;
		this.facilities = facilities;
		this.calculator = calculator;
	}

	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		TDoubleObjectHashMap<DescriptiveStatistics> statsMap = new TDoubleObjectHashMap<DescriptiveStatistics>();

		for (Trajectory trajectory : trajectories) {
			for (int i = 2; i < trajectory.getElements().size(); i += 2) {
				Activity destination = (Activity) trajectory.getElements().get(i);

				Id id = destination.getFacilityId();
				Coord dest = facilities.getFacilities().get(id).getCoord();

				Activity origin = (Activity) trajectory.getElements().get(i - 2);
				id = origin.getFacilityId();
				ActivityFacility fac = facilities.getFacilities().get(id);
				Coord source = fac.getCoord();

				double d = calculator.distance(MatsimCoordUtils.coordToPoint(source),
						MatsimCoordUtils.coordToPoint(dest));

				int escorts = escortData.getEscorts(trajectory.getPerson(), i - 1);

				DescriptiveStatistics stats = statsMap.get(escorts);
				if (stats == null) {
					stats = new DescriptiveStatistics();
					statsMap.put(escorts, stats);
				}
				
				if(d > 0)
					stats.addValue(d);
			}
		}

		try {
			TXTWriter.writeStatistics(statsMap, "escorts", getOutputDirectory() + "d_trip_escorts.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
