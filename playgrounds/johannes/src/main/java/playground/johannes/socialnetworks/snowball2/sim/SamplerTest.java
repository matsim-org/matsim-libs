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

import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.io.SparseGraphMLReader;

import playground.johannes.socialnetworks.graph.analysis.DegreeTask;
import playground.johannes.socialnetworks.graph.analysis.GraphAnalyzerTaskComposite;
import playground.johannes.socialnetworks.graph.analysis.GraphSizeTask;
import playground.johannes.socialnetworks.snowball2.spatial.analysis.SampledDegree;
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
		
		GraphAnalyzerTaskComposite tasks = new GraphAnalyzerTaskComposite();
		DegreeTask task = new DegreeTask();
		task.setModule(new SampledDegree());
		tasks.addTask(task);
		tasks.addTask(new GraphSizeTask());
		tasks.addTask(new WaveSizeTask());
		
		
		IterationSampleAnalyzer listener = new IterationSampleAnalyzer("/Users/jillenberger/Work/work/socialnets/snowball/output", tasks, tasks);
		CompleteSampleAnalyzer cListener = new CompleteSampleAnalyzer(graph, "/Users/jillenberger/Work/work/socialnets/snowball/output", tasks, tasks);
		
		SamplerListenerComposite composite = new SamplerListenerComposite();
		composite.addComponent(listener);
		composite.addComponent(cListener);
		
		sampler.setListener(composite);
		
		sampler.run(graph);

	}

}
