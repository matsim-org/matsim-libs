/* *********************************************************************** *
 * project: org.matsim.*
 * SamplerTest.java
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
package playground.johannes.socialnetworks.snowball2.sim;

import java.util.HashMap;
import java.util.Map;

import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.io.SparseGraphMLReader;

import playground.johannes.socialnetworks.graph.analysis.AnalyzerTask;
import playground.johannes.socialnetworks.graph.analysis.DegreeTask;
import playground.johannes.socialnetworks.graph.analysis.AnalyzerTaskComposite;
import playground.johannes.socialnetworks.graph.analysis.GraphSizeTask;
import playground.johannes.socialnetworks.snowball2.analysis.EstimatedDegree;
import playground.johannes.socialnetworks.snowball2.analysis.ObservedDegree;
import playground.johannes.socialnetworks.snowball2.spatial.analysis.WaveSizeTask;

/**
 * @author illenberger
 *
 */
public class SamplerTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SparseGraphMLReader reader = new SparseGraphMLReader();
		Graph graph = reader.readGraph("/Users/jillenberger/Work/work/socialnets/snowball/data/networks/cond-mat-2005-gc.graphml");
		
		Sampler<Graph, Vertex, Edge> sampler = new Sampler<Graph, Vertex, Edge>();
		
		VertexPartition seedGen = new RandomSeedGenerator(50, 4711);
		sampler.setSeedGenerator(seedGen);
		
		AnalyzerTaskComposite obsTasks = new AnalyzerTaskComposite();
		DegreeTask task = new DegreeTask();
		task.setModule(new ObservedDegree());
		obsTasks.addTask(task);
		obsTasks.addTask(new GraphSizeTask());
		obsTasks.addTask(new WaveSizeTask());
		
		Estimator1 estimator = new Estimator1(graph.getVertices().size());
		AnalyzerTaskComposite estimTasks = new AnalyzerTaskComposite();
		task = new DegreeTask();
		task.setModule(new EstimatedDegree(estimator));
		estimTasks.addTask(task);
		estimTasks.addTask(new GraphSizeTask());
		estimTasks.addTask(new WaveSizeTask());
		
		Map<String, AnalyzerTask> tasks = new HashMap<String, AnalyzerTask>();
		tasks.put("obs", obsTasks);
		tasks.put("estim", estimTasks);
		
		IterationSampleAnalyzer listener = new IterationSampleAnalyzer(tasks, "/Users/jillenberger/Work/work/socialnets/snowball/output");
		CompleteSampleAnalyzer cListener = new CompleteSampleAnalyzer(graph, tasks, "/Users/jillenberger/Work/work/socialnets/snowball/output");
		IntervalSampleAnalyzer interval = new IntervalSampleAnalyzer(10000, tasks, "/Users/jillenberger/Work/work/socialnets/snowball/output");
		
		SamplerListenerComposite composite = new SamplerListenerComposite();
		composite.addComponent(estimator);
		composite.addComponent(listener);
		composite.addComponent(cListener);
		composite.addComponent(interval);
		
		
		sampler.setListener(composite);
		
		sampler.run(graph);

	}

}
