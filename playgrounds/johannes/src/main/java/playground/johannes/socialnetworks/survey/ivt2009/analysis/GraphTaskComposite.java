/* *********************************************************************** *
 * project: org.matsim.*
 * GraphTaskComposite.java
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
package playground.johannes.socialnetworks.survey.ivt2009.analysis;

import java.util.ArrayList;
import java.util.List;

import org.matsim.contrib.sna.graph.Graph;

/**
 * @author illenberger
 *
 */
public class GraphTaskComposite<G extends Graph> implements GraphTask<G> {

	private List<GraphTask<G>> tasks = new ArrayList<GraphTask<G>>();
	
	public void addTask(GraphTask<G> task) {
		tasks.add(task);
	}
	
	@Override
	public G apply(G graph) {
		
		for(GraphTask<G> task : tasks) {
			graph = task.apply(graph);
		}
		
		return graph;
	}

}
