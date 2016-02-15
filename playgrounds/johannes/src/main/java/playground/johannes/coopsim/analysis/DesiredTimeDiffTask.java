/* *********************************************************************** *
 * project: org.matsim.*
 * DesiredTimeDiffTask.java
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

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import playground.johannes.coopsim.mental.ActivityDesires;
import playground.johannes.coopsim.pysical.Trajectory;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * @author illenberger
 *
 */
public class DesiredTimeDiffTask extends TrajectoryAnalyzerTask {

	private final Map<Person, ActivityDesires> desires;
	
	public DesiredTimeDiffTask(Map<Person, ActivityDesires> desires) {
		this.desires = desires;
	}
	
	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		DescriptiveStatistics durStats = new DescriptiveStatistics();
		DescriptiveStatistics arrStats = new DescriptiveStatistics();
		
		for (Trajectory t : trajectories) {
			Activity act = (Activity) t.getElements().get(2);
			String type = act.getType();
			if (!type.equals("idle")) {
				ActivityDesires desire = desires.get(t.getPerson());

				double desiredDuration = desire.getActivityDuration(type);
				double desiredStartTime = desire.getActivityStartTime(type);

				double realizedDuration = t.getTransitions().get(3) - t.getTransitions().get(2);
				double realizedStartTime = t.getTransitions().get(2);

				durStats.addValue(Math.abs(realizedDuration - desiredDuration));
				arrStats.addValue(Math.abs(realizedStartTime - desiredStartTime));
			}
		}
		results.put("dur_diff", durStats);
		results.put("arr_diff", arrStats);
		
		
		try {
			writeHistograms(durStats, "dur_diff", 50, 50);
			writeHistograms(arrStats, "arr_diff", 50, 50);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
