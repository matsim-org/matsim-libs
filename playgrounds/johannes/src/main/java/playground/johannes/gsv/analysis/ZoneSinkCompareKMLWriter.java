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

/**
 * 
 */
package playground.johannes.gsv.analysis;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.contrib.socnetgen.sna.gis.Zone;
import org.matsim.contrib.socnetgen.sna.gis.ZoneLayer;
import org.matsim.contrib.socnetgen.util.MatsimCoordUtils;
import playground.johannes.coopsim.analysis.TrajectoryAnalyzerTask;
import playground.johannes.coopsim.pysical.Trajectory;

import java.util.Map;
import java.util.Set;

/**
 * @author johannes
 *
 */
public class ZoneSinkCompareKMLWriter extends TrajectoryAnalyzerTask {

	private ZoneLayer<Double> arrivals;
	
	/* (non-Javadoc)
	 * @see playground.johannes.coopsim.analysis.TrajectoryAnalyzerTask#analyze(java.util.Set, java.util.Map)
	 */
	@Override
	public void analyze(Set<Trajectory> trajectories,
			Map<String, DescriptiveStatistics> results) {
		for(Trajectory traj : trajectories) {
			Activity target = (Activity) traj.getElements().get(2);
			Zone<Double> zone = arrivals.getZone(MatsimCoordUtils.coordToPoint(target.getCoord()));
			Double count = zone.getAttribute();
			if(count == null)
				count = 0.0;
			count++;
			zone.setAttribute(count);
		}
	}

}
