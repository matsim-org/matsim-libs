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

import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.analysis.AnalyzerTask;
import org.matsim.contrib.sna.graph.analysis.DegreeTask;
import org.matsim.contrib.sna.graph.analysis.GraphAnalyzer;
import org.matsim.contrib.sna.graph.analysis.GraphSizeTask;
import org.matsim.contrib.sna.graph.analysis.TransitivityTask;
import org.matsim.contrib.sna.graph.io.SparseGraphMLReader;
import org.matsim.contrib.sna.snowball.analysis.ObservedDegree;


/**
 * @author illenberger
 *
 */
public class TopologyAnalyzerTask extends AnalyzerTaskComposite {

	public TopologyAnalyzerTask() {
		addTask(new GraphSizeTask());
		DegreeTask t = new DegreeTask();
		t.setModule(ObservedDegree.getInstance());
		addTask(t);
		addTask(new TransitivityTask());
//		addTask(new ComponentsTask());
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
