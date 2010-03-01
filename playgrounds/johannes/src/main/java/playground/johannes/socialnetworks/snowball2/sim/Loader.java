/* *********************************************************************** *
 * project: org.matsim.*
 * Loader.java
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
import java.util.Map.Entry;

import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.io.SparseGraphMLReader;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;

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
public class Loader {

	private static final String MODULENAME = "snowballsim";
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		/*
		 * Load config.
		 */
		Config config = loadConfig(args[0]);
		/*
		 * Load graph.
		 */
		Graph graph = loadGraph(config.getParam(MODULENAME, "graphFile"));
		/*
		 * Get random seed.
		 */
		long randomSeed = config.global().getRandomSeed();
		/*
		 * Init random seed generator.
		 */
		VertexPartition seedGenerator = new RandomSeedGenerator(Integer.parseInt(config.getParam(MODULENAME, "seeds")), randomSeed);
		/*
		 * Init estimators.
		 */
		final int interval = Integer.parseInt(config.getParam(MODULENAME, "interval"));
		Map<String, Estimator> estimators = new HashMap<String, Estimator>();
		estimators.put("estim1", new Estimator1(graph.getVertices().size()));
		estimators.put("estim2", new Estimator2(graph.getVertices().size(), interval));
		estimators.put("estim3", new Estimator3(interval, graph.getVertices().size()));
		/*
		 * Load analyzers.
		 */
		Map<String, AnalyzerTask> analyzers = loadAnalyzers(estimators);
		/*
		 * Get output directory.
		 */
		String output = config.getParam(MODULENAME, "output");
		/*
		 * Init sample analyzers.
		 */
		IntervalSampleAnalyzer intervalAnalyzer = new IntervalSampleAnalyzer(interval, analyzers, output);
		IterationSampleAnalyzer iterationAnalyzer = new IterationSampleAnalyzer(analyzers, output);
		CompleteSampleAnalyzer completeAnalyzer = new CompleteSampleAnalyzer(graph, analyzers, output);
		/*
		 * Init sampler listener.
		 */
		SamplerListenerComposite listeners = new SamplerListenerComposite();
		/*
		 * Add estimators to listener.
		 */
		for(Estimator estimator : estimators.values()) {
			if(estimator instanceof SamplerListener) {
				listeners.addComponent((SamplerListener)estimator);
			}
		}
		/*
		 * Add analyzers to listener.
		 */
		listeners.addComponent(intervalAnalyzer);
		listeners.addComponent(iterationAnalyzer);
		listeners.addComponent(completeAnalyzer);
		/*
		 * Init and run sampler.
		 */
		Sampler<Graph, Vertex, Edge> sampler = new Sampler<Graph, Vertex, Edge>();
		sampler.setSeedGenerator(seedGenerator);
		sampler.setListener(listeners);
		
		sampler.run(graph);
	}

	private static Config loadConfig(String file) {
		Config config = new Config();
		config.addCoreModules();
		MatsimConfigReader reader = new MatsimConfigReader(config);
		reader.readFile(file);
		return config;
	}
	
	private static Graph loadGraph(String file) {
		SparseGraphMLReader reader = new SparseGraphMLReader();
		return reader.readGraph(file);
	}
	
	private static Map<String, AnalyzerTask> loadAnalyzers(Map<String, Estimator> estimators) {
		Map<String, AnalyzerTask> analyzers = new HashMap<String, AnalyzerTask>();
		/*
		 * observed
		 */
		AnalyzerTaskComposite tasks = new AnalyzerTaskComposite();
		tasks.addTask(new GraphSizeTask());
		tasks.addTask(new WaveSizeTask());
		DegreeTask obsDegree = new DegreeTask();
		obsDegree.setModule(new ObservedDegree());
		tasks.addTask(obsDegree);
		analyzers.put("obs", tasks);
		/*
		 * estimated
		 */
		for(Entry<String, Estimator> entry : estimators.entrySet()) {
			tasks = new AnalyzerTaskComposite();
			DegreeTask estimDegree = new DegreeTask();
			estimDegree.setModule(new EstimatedDegree(entry.getValue()));
			tasks.addTask(estimDegree);
			tasks.addTask(new EstimatorTask(entry.getValue()));
			analyzers.put(entry.getKey(), tasks);
		}
		
		return analyzers;
	}
}
