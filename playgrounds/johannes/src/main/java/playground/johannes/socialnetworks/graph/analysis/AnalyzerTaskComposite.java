/* *********************************************************************** *
 * project: org.matsim.*
 * StandardAnalyzerTask.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.graph.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.matsim.contrib.sna.graph.Graph;

/**
 * @author illenberger
 *
 */
public class AnalyzerTaskComposite extends AnalyzerTask {

	private List<AnalyzerTask> tasks;
	
	public AnalyzerTaskComposite() {
		tasks = new ArrayList<AnalyzerTask>();
	}
	
	public void addTask(AnalyzerTask task) {
		tasks.add(task);
	}
	
	@Override
	public void setOutputDirectoy(String output) {
		for(AnalyzerTask task : tasks) {
			task.setOutputDirectoy(output);
		}
	}
	
	@Override
	public void analyze(Graph graph, Map<String, Double> stats) {
		for(AnalyzerTask task : tasks)
			task.analyze(graph, stats);
	}

}
