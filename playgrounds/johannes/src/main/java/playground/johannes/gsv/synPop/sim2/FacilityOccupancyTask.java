/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop.sim2;

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntIterator;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;

import playground.johannes.coopsim.analysis.TrajectoryAnalyzerTask;
import playground.johannes.coopsim.pysical.Trajectory;
import playground.johannes.sna.util.TXTWriter;
import playground.johannes.socialnetworks.statistics.Correlations;

/**
 * @author johannes
 *
 */
public class FacilityOccupancyTask extends TrajectoryAnalyzerTask {

	private final ActivityFacilities facilities;
	
	public FacilityOccupancyTask(ActivityFacilities facilities) {
		this.facilities = facilities;
	}
	
	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		TObjectIntHashMap<ActivityOption> occupancy = new TObjectIntHashMap<ActivityOption>(facilities.getFacilities().size() * 5);
		for(Trajectory t : trajectories) {
			for(int i = 0; i < t.getElements().size(); i += 2) {
				Activity act = (Activity) t.getElements().get(i);
				ActivityFacility fac = facilities.getFacilities().get(act.getFacilityId());
				ActivityOption opt = fac.getActivityOptions().get(act.getType());
				occupancy.adjustOrPutValue(opt, 1, 1);
			}
		}

		TObjectIntIterator<ActivityOption> it = occupancy.iterator();
		double val1[] = new double[occupancy.size()];
		double val2[] = new double[occupancy.size()];
		for(int i = 0; i < occupancy.size(); i++) {
			it.advance();
			val2[i] = it.value();
			val1[i] = it.key().getCapacity();
		}
		
		TDoubleDoubleHashMap corel = Correlations.mean(val1, val2);
		try {
			TXTWriter.writeMap(corel, "capacity", "occupancy", getOutputDirectory() + "/facility.load.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
