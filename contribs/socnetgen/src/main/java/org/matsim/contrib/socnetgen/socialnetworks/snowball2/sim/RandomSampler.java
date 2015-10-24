/* *********************************************************************** *
 * project: org.matsim.*
 * RandomSampler.java
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
package org.matsim.contrib.socnetgen.socialnetworks.snowball2.sim;

import org.matsim.contrib.socnetgen.sna.graph.SparseEdge;
import org.matsim.contrib.socnetgen.sna.graph.SparseGraph;
import org.matsim.contrib.socnetgen.sna.graph.SparseVertex;
import org.matsim.contrib.socnetgen.sna.graph.analysis.AnalyzerTask;
import org.matsim.contrib.socnetgen.sna.graph.analysis.ComponentsTask;
import org.matsim.contrib.socnetgen.sna.graph.analysis.GraphSizeTask;
import org.matsim.contrib.socnetgen.sna.graph.analysis.RandomPartition;
import org.matsim.contrib.socnetgen.sna.graph.io.SparseGraphMLReader;
import org.matsim.contrib.socnetgen.sna.snowball.analysis.PiEstimator;
import org.matsim.contrib.socnetgen.sna.snowball.analysis.SimplePiEstimator;
import org.matsim.contrib.socnetgen.sna.snowball.sim.IterationSampleAnalyzer;
import org.matsim.contrib.socnetgen.sna.snowball.sim.SnowballSampler;
import org.matsim.contrib.socnetgen.socialnetworks.graph.analysis.AnalyzerTaskComposite;
import org.matsim.contrib.socnetgen.socialnetworks.snowball2.analysis.SeedAPLTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author illenberger
 *
 */
public class RandomSampler { 

	public static void main(String args[]) {
		SparseGraphMLReader reader = new SparseGraphMLReader();
		SparseGraph graph = reader.readGraph("/Users/jillenberger/Work/socialnets/data/graphs/cond-mat-2005-gc.graphml");
		
		SnowballSampler<SparseGraph, SparseVertex, SparseEdge> sampler = new SnowballSampler<SparseGraph, SparseVertex, SparseEdge>();
		
		sampler.setSeedGenerator(new RandomPartition<SparseVertex>(0.2));
		
		PiEstimator estimator = new SimplePiEstimator(graph.getVertices().size());
		Map<String, AnalyzerTask> analyzers = new HashMap<String, AnalyzerTask>();
		AnalyzerTaskComposite composite = new AnalyzerTaskComposite();
		composite.addTask(new GraphSizeTask());
		composite.addTask(new ComponentsTask());
		composite.addTask(new SeedAPLTask());
		List<PiEstimator> estimators = new ArrayList<PiEstimator>(1);
		estimators.add(estimator);
		analyzers.put("topo", composite);
		IterationSampleAnalyzer listener = new IterationSampleAnalyzer(analyzers, estimators, "/Users/jillenberger/Work/socialnets/snowball/output/");
		
		sampler.setListener(listener);
		
		sampler.run(graph);
	}
}
