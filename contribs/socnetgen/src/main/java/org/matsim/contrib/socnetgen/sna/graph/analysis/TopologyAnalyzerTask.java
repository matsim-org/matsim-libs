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
package org.matsim.contrib.socnetgen.sna.graph.analysis;

import org.matsim.contrib.socnetgen.sna.graph.Graph;
import org.matsim.contrib.socnetgen.sna.graph.io.SparseGraphMLReader;

import java.io.IOException;


/**
 * @author illenberger
 *
 */
public class TopologyAnalyzerTask extends AnalyzerTaskComposite {

	public TopologyAnalyzerTask() {
		addTask(new GraphSizeTask());
		addTask(new DegreeTask());
		addTask(new TransitivityTask());
		addTask(new ComponentsTask());
		
		PropertyDegreeTask task = new PropertyDegreeTask();
		task.setModule(Degree.getInstance());
		addTask(task);
	}

	public static void main(String args[]) throws IOException {
		SparseGraphMLReader reader = new SparseGraphMLReader();
		Graph graph = reader.readGraph(args[0]);
		String output = null;
		if(args.length > 1) {
			output = args[1];
		}
		
		AnalyzerTask task = new TopologyAnalyzerTask();
		if(output != null)
			task.setOutputDirectoy(output);
		
		GraphAnalyzer.analyze(graph, task, output);
	}
}
