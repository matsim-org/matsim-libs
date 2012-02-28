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

import java.io.IOException;

import playground.johannes.sna.graph.Graph;
import playground.johannes.sna.graph.analysis.AnalyzerTask;
import playground.johannes.sna.graph.analysis.ComponentsTask;
import playground.johannes.sna.graph.analysis.Degree;
import playground.johannes.sna.graph.analysis.DegreeTask;
import playground.johannes.sna.graph.analysis.GraphAnalyzer;
import playground.johannes.sna.graph.analysis.GraphSizeTask;
import playground.johannes.sna.graph.analysis.TransitivityTask;
import playground.johannes.sna.graph.io.SparseGraphMLReader;


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
