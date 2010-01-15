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

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.contrib.sna.graph.Graph;

/**
 * @author illenberger
 *
 */
public class GraphAnalyzerTaskComposite extends AbstractGraphAnalyzerTask {

	private Set<GraphAnalyzerTask> tasks;
	
	public GraphAnalyzerTaskComposite(String output) {
		super(output);
		tasks = new LinkedHashSet<GraphAnalyzerTask>();
	}
	
	public void addTasks(GraphAnalyzerTask task) {
		tasks.add(task);
	}
	
	
	@Override
	public void analyze(Graph graph, Map<String, Object> analyzers, Map<String, Double> stats) {
		for(GraphAnalyzerTask task : tasks)
			task.analyze(graph, analyzers, stats);
	}

}
