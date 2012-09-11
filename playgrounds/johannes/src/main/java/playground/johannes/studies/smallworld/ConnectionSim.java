/* *********************************************************************** *
 * project: org.matsim.*
 * ConnectionSim.java
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
package playground.johannes.studies.smallworld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;

import playground.johannes.sna.graph.Edge;
import playground.johannes.sna.graph.Graph;
import playground.johannes.sna.graph.Vertex;
import playground.johannes.sna.graph.analysis.AnalyzerTask;
import playground.johannes.sna.graph.analysis.ComponentsTask;
import playground.johannes.sna.graph.analysis.FixedSizeRandomPartition;
import playground.johannes.sna.graph.analysis.GraphSizeTask;
import playground.johannes.sna.graph.analysis.RandomPartition;
import playground.johannes.sna.graph.analysis.VertexFilter;
import playground.johannes.sna.graph.io.SparseGraphMLReader;
import playground.johannes.sna.snowball.analysis.PiEstimator;
import playground.johannes.sna.snowball.analysis.SimplePiEstimator;
import playground.johannes.sna.snowball.sim.Sampler;
import playground.johannes.sna.snowball.sim.SamplerListenerComposite;
import playground.johannes.sna.util.MultiThreading;
import playground.johannes.socialnetworks.graph.analysis.AnalyzerTaskComposite;
import playground.johannes.socialnetworks.snowball2.analysis.IterationTask;
import playground.johannes.socialnetworks.snowball2.analysis.SeedAPLTask;
import playground.johannes.socialnetworks.snowball2.analysis.WaveSizeTask;
import playground.johannes.socialnetworks.snowball2.sim.ConnectionSampleAnalyzer;

/**
 * @author illenberger
 *
 */
public class ConnectionSim {
	
	private static final String MODULENAME = "snowballsim";
	
	public static void main(String[] args) {
		MultiThreading.setNumAllowedThreads(1);
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
		int numSeeds = Integer.parseInt(config.getParam(MODULENAME, "seeds"));
		VertexFilter<Vertex> seedGenerator = new FixedSizeRandomPartition<Vertex>(numSeeds, randomSeed);
		/*
		 * Init response rate generator.
		 */
		double responseRate;
		String str = config.getParam(MODULENAME, "responseRate");
		if(str.endsWith("%")) {
			str = str.substring(0, str.length() - 1);
			responseRate = Double.parseDouble(str)/100.0;
		} else
			responseRate = Double.parseDouble(str);
		
		VertexFilter<Vertex> reponseGenerator = new RandomPartition<Vertex>(responseRate, randomSeed);
		/*
		 * Init estimators.
		 */
		PiEstimator estimator = new SimplePiEstimator(graph.getVertices().size());
		Map<String, AnalyzerTask> analyzers = loadAnalyzers(graph, estimator);
		List<PiEstimator> estimators = new ArrayList<PiEstimator>(1);
		estimators.add(estimator);
		/*
		 * Get output directory.
		 */
		String output = config.getParam(MODULENAME, "output");
		/*
		 * Init sample analyzers.
		 */
		ConnectionSampleAnalyzer connectionAnalyzer = new ConnectionSampleAnalyzer(numSeeds, analyzers, estimators, output);
		/*
		 * Init sampler listener.
		 */
		SamplerListenerComposite listeners = new SamplerListenerComposite();
		/*
		 * Add analyzers to listener.
		 */
		listeners.addComponent(connectionAnalyzer);
		/*
		 * Init and run sampler.
		 */
		Sampler<Graph, Vertex, Edge> sampler = new Sampler<Graph, Vertex, Edge>(randomSeed);
		sampler.setSeedGenerator(seedGenerator);
		sampler.setResponseGenerator(reponseGenerator);
		sampler.setListener(listeners);
		
		sampler.run(graph);
	}

	public static Config loadConfig(String file) {
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
	
	private static Map<String, AnalyzerTask> loadAnalyzers(Graph graph, PiEstimator estimator) {
		Map<String, AnalyzerTask> analyzers = new HashMap<String, AnalyzerTask>();
		AnalyzerTaskComposite composite = new AnalyzerTaskComposite();
		composite.addTask(new GraphSizeTask());
		composite.addTask(new WaveSizeTask());
		composite.addTask(new ComponentsTask());
		composite.addTask(new IterationTask());
//		composite.addTask(new ClosenessSeed2Seed());
		composite.addTask(new SeedAPLTask());
		analyzers.put("topo", composite);
		return analyzers;
	}

}
