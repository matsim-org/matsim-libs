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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.AnalyzerTask;
import org.matsim.contrib.sna.graph.analysis.DegreeTask;
import org.matsim.contrib.sna.graph.analysis.FixedSizeRandomPartition;
import org.matsim.contrib.sna.graph.analysis.GraphSizeTask;
import org.matsim.contrib.sna.graph.analysis.RandomPartition;
import org.matsim.contrib.sna.graph.analysis.TransitivityTask;
import org.matsim.contrib.sna.graph.analysis.VertexFilter;
import org.matsim.contrib.sna.graph.io.SparseGraphMLReader;
import org.matsim.contrib.sna.snowball.sim.IntervalSampleAnalyzer;
import org.matsim.contrib.sna.snowball.sim.ProbabilityEstimator;
import org.matsim.contrib.sna.snowball.sim.Sampler;
import org.matsim.contrib.sna.snowball.sim.SamplerListenerComposite;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;

import playground.johannes.socialnetworks.graph.analysis.AnalyzerTaskComposite;
import playground.johannes.socialnetworks.snowball2.analysis.EstimatedDegree2;
import playground.johannes.socialnetworks.snowball2.analysis.EstimatedTransitivity;
import playground.johannes.socialnetworks.snowball2.analysis.ObservedDegree;
import playground.johannes.socialnetworks.snowball2.analysis.ObservedTransitivity;
import playground.johannes.socialnetworks.snowball2.analysis.ResponseRateTask;
import playground.johannes.socialnetworks.snowball2.analysis.WaveSizeTask;
import playground.johannes.socialnetworks.snowball2.sim.deprecated.HTEstimator;
import playground.johannes.socialnetworks.snowball2.sim.deprecated.PopulationEstimator;

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
		int numSeeds = Integer.parseInt(config.getParam(MODULENAME, "seeds"));
		VertexFilter seedGenerator = new FixedSizeRandomPartition(numSeeds, randomSeed);
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
		
		VertexFilter reponseGenerator = new RandomPartition(responseRate, randomSeed);
		/*
		 * Init estimators.
		 */
//		final int interval = Integer.parseInt(config.getParam(MODULENAME, "interval"));
		final int N = graph.getVertices().size();
		final int M = graph.getEdges().size();
		Map<String, EstimatorSet> estimators = new HashMap<String, EstimatorSet>();
		Set<ProbabilityEstimator> estimatorSet = new HashSet<ProbabilityEstimator>();
		
		ProbabilityEstimator estim1 = new Estimator1(N);
		ProbabilityEstimator estim1Norm = new NormalizedEstimator(estim1, N);
		
		estimatorSet.add(estim1);
		estimatorSet.add(estim1Norm);
		
		estimators.put("estim1a", new EstimatorSet(estim1Norm, new HTEstimator(N), new HTEstimator(M)));
//		estimators.put("estim1b", new EstimatorSet(estim1, new HTEstimator(N), new HTEstimator(M)));
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
		IntervalSampleAnalyzer intervalAnalyzer = new IntervalSampleAnalyzer(analyzers, estimatorSet, output);
//		IterationSampleAnalyzer iterationAnalyzer = new IterationSampleAnalyzer(analyzers, estimatorSet, output);
//		FinalSampleAnalyzer completeAnalyzer = new FinalSampleAnalyzer(analyzers, estimatorSet, output);
//		ConnectionSampleAnalyzer connectionAnalyzer = new ConnectionSampleAnalyzer(numSeeds, analyzers, output);
		/*
		 * Init sampler listener.
		 */
		SamplerListenerComposite listeners = new SamplerListenerComposite();
//		/*
//		 * Add estimators to listener.
//		 */
//		for(EstimatorSet estimator : estimators.values()) {
//			if(estimator.distribution instanceof SamplerListener) {
//				listeners.addComponent((SamplerListener)estimator.distribution);
//			}
//		}
		/*
		 * Add analyzers to listener.
		 */
		listeners.addComponent(intervalAnalyzer);
//		listeners.addComponent(iterationAnalyzer);
//		listeners.addComponent(completeAnalyzer);
//		listeners.addComponent(connectionAnalyzer);
		/*
		 * Init and run sampler.
		 */
		Sampler<Graph, Vertex, Edge> sampler = new Sampler<Graph, Vertex, Edge>();
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
	
	private static Map<String, AnalyzerTask> loadAnalyzers(Map<String, EstimatorSet> estimators) {
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
		
		TransitivityTask obsTransitivity = new TransitivityTask();
		obsTransitivity.setModule(new ObservedTransitivity());
		tasks.addTask(obsTransitivity);
		
		tasks.addTask(new ResponseRateTask());
//		tasks.addTask(new DegreeCorrelationTask());
//		tasks.addTask(new ComponentsTask());
		analyzers.put("obs", tasks);
		/*
		 * estimated
		 */
		for(Entry<String, EstimatorSet> entry : estimators.entrySet()) {
			tasks = new AnalyzerTaskComposite();
			
			ProbabilityEstimator biasdDistr = entry.getValue().distribution;
			PopulationEstimator estimator = entry.getValue().vertexEstimator;
			PopulationEstimator edgeEstimator = entry.getValue().edgeEstimator;
			
			DegreeTask estimDegree = new DegreeTask();
			estimDegree.setModule(new EstimatedDegree2(biasdDistr, estimator, edgeEstimator));
			tasks.addTask(estimDegree);
			
			TransitivityTask estimTransitivity = new TransitivityTask();
			EstimatedTransitivity trans = new EstimatedTransitivity(biasdDistr, estimator);
			trans.enableCaching(true);
			estimTransitivity.setModule(trans);
			tasks.addTask(estimTransitivity);
			
			tasks.addTask(new EstimatorTask(entry.getValue().distribution));
			
			analyzers.put(entry.getKey(), tasks);
		}
		
		return analyzers;
	}
	
	private static class EstimatorSet {
		
		private ProbabilityEstimator distribution;
		
		private PopulationEstimator vertexEstimator;
		
		private PopulationEstimator edgeEstimator;
		
		public EstimatorSet(ProbabilityEstimator distribution, PopulationEstimator vertexEstimator, PopulationEstimator edgeEstimator) {
			this.distribution = distribution;
			this.vertexEstimator = vertexEstimator;
			this.edgeEstimator = edgeEstimator;
		}
	}
}
