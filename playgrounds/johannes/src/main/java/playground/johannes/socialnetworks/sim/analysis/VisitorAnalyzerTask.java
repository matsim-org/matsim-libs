/* *********************************************************************** *
 * project: org.matsim.*
 * VisitorAnalyzerTask.java
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
import org.matsim.contrib.sna.math.DummyDiscretizer;

import playground.johannes.socialnetworks.sim.interaction.JointActivityScoringFunctionFactory;

/**
 * @author illenberger
 * 
 */
public class VisitorAnalyzerTask extends TrajectoryAnalyzerTask {

	private JointActivityScoringFunctionFactory factory;

	public VisitorAnalyzerTask(JointActivityScoringFunctionFactory factory) {
		this.factory = factory;
	}

	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		DescriptiveStatistics stats = factory.visitorStatistics();
		results.put("visitors", stats);
		if (outputDirectoryNotNull()) {
			try {
				writeHistograms(stats, new DummyDiscretizer(), "visitors", false);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
