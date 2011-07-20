/* *********************************************************************** *
 * project: org.matsim.*
 * JoinTimeAnalyzerTask.java
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
package playground.johannes.socialnetworks.sim.analysis;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.sna.math.LinearDiscretizer;

import playground.johannes.socialnetworks.sim.interaction.JointActivityScoringFunctionFactory;

/**
 * @author illenberger
 *
 */
public class JoinTimeAnalyzerTask extends TrajectoryAnalyzerTask {

	private JointActivityScoringFunctionFactory factory;
	
	public JoinTimeAnalyzerTask(JointActivityScoringFunctionFactory factory) {
		this.factory = factory;
	}
	
	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		DescriptiveStatistics stats = factory.joinTimeStatistics();
		results.put("jointime", stats);
		
		if(outputDirectoryNotNull()) {
			try {
				writeHistograms(stats, new LinearDiscretizer(900), "jointime", false);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}
