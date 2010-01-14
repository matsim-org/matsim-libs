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
public abstract class GraphAnalyzerTaskComposite implements GraphAnalyzerTask {

	private Set<GraphAnalyzerTask> tasks;
	
	public GraphAnalyzerTaskComposite() {
		tasks = new LinkedHashSet<GraphAnalyzerTask>();
		addTasks(tasks);
	}
	
	abstract protected void addTasks(Set<GraphAnalyzerTask> taskSet);
		
	@Override
	public void analyze(Graph graph, GraphPropertyFactory factory, Map<String, Double> stats) {
		for(GraphAnalyzerTask task : tasks)
			task.analyze(graph, factory, stats);
	}

}
