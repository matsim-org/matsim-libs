/* *********************************************************************** *
 * project: org.matsim.*
 * PlanAnalyzerTaskComposite.java
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.population.Plan;

/**
 * @author illenberger
 *
 */
public class PlansAnalyzerTaskComposite extends PlansAnalyzerTask {

	private List<PlansAnalyzerTask> tasks;
	
	public PlansAnalyzerTaskComposite() {
		tasks = new ArrayList<PlansAnalyzerTask>();
	}
	
	public void addTask(PlansAnalyzerTask task) {
		tasks.add(task);
	}
	
	@Override
	public void setOutputDirectory(String output) {
		for(PlansAnalyzerTask task : tasks) {
			task.setOutputDirectory(output);
		}
	}
	
	@Override
	public void analyze(Set<Plan> plans, Map<String, DescriptiveStatistics> results) {
		for(PlansAnalyzerTask task : tasks)
			task.analyze(plans, results);
	}

	@Override
	public void analyzeTrajectories(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		for(PlansAnalyzerTask task : tasks)
			task.analyzeTrajectories(trajectories, results);
	}

}
