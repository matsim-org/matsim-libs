/* *********************************************************************** *
 * project: org.matsim.*
 * PersonAgeTask.java
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
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import org.matsim.core.population.PersonUtils;
import playground.johannes.coopsim.pysical.Trajectory;
import playground.johannes.sna.math.DummyDiscretizer;

/**
 * @author illenberger
 *
 */
public class PersonAgeTask extends TrajectoryAnalyzerTask {

	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for(Trajectory t : trajectories) {
			stats.addValue(PersonUtils.getAge(t.getPerson()));
		}
		
		results.put("person_age", stats);
		try {
			writeHistograms(stats, new DummyDiscretizer(), "age", false);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
