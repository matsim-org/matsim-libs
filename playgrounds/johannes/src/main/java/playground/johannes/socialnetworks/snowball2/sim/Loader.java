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
import org.matsim.contrib.sna.math.DescriptivePiStatisticsFactory;
import org.matsim.contrib.sna.snowball.analysis.EstimatedDegree;
import org.matsim.contrib.sna.snowball.analysis.EstimatedTransitivity;
import org.matsim.contrib.sna.snowball.analysis.ObservedDegree;
import org.matsim.contrib.sna.snowball.analysis.ObservedTransitivity;
import org.matsim.contrib.sna.snowball.analysis.PiEstimator;
import org.matsim.contrib.sna.snowball.analysis.SimplePiEstimator;
import org.matsim.contrib.sna.snowball.sim.IterationSampleAnalyzer;
import org.matsim.contrib.sna.snowball.sim.Sampler;
import org.matsim.contrib.sna.snowball.sim.SamplerListenerComposite;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.utils.collections.Tuple;

import playground.johannes.socialnetworks.graph.analysis.AnalyzerTaskComposite;
import playground.johannes.socialnetworks.snowball2.analysis.HTStatsFactory;
import playground.johannes.socialnetworks.snowball2.analysis.ResponseRateTask;
import playground.johannes.socialnetworks.snowball2.analysis.WSMStatsFactory;
import playground.johannes.socialnetworks.snowball2.analysis.WaveSizeTask;

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
		Map<String, Tuple<PiEstimator, DescriptivePiStatisticsFactory>> estimators = new HashMap<String, Tuple<PiEstimator, DescriptivePiStatisticsFactory>>();
		Set<PiEstimator> estimatorSet = new HashSet<PiEstimator>();
		
		PiEstimator estim1 = new SimplePiEstimator(N);
		PiEstimator estim1Norm = new NormalizedEstimator(estim1, N);
		
		estimatorSet.add(estim1);
		estimatorSet.add(estim1Norm);
		
		WSMStatsFactory wsmFactory = new WSMStatsFactory();
		HTStatsFactory htFactory = new HTStatsFactory(N);
		estimators.put("estim1a", new Tuple<PiEstimator, DescriptivePiStatisticsFactory>(estim1, wsmFactory));
		estimators.put("estim1b", new Tuple<PiEstimator, DescriptivePiStatisticsFactory>(estim1, htFactory));
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
//		IntervalSampleAnalyzer intervalAnalyzer = new IntervalSampleAnalyzer(analyzers, estimatorSet, output);
		IterationSampleAnalyzer iterationAnalyzer = new IterationSampleAnalyzer(analyzers, estimatorSet, output);
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
//		listeners.addComponent(intervalAnalyzer);
		listeners.addComponent(iterationAnalyzer);
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
	
	private static Map<String, AnalyzerTask> loadAnalyzers(Map<String, Tuple<PiEstimator, DescriptivePiStatisticsFactory>> estimators) {
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
		for(Entry<String, Tuple<PiEstimator, DescriptivePiStatisticsFactory>> entry : estimators.entrySet()) {
			tasks = new AnalyzerTaskComposite();
			
			PiEstimator biasdDistr = entry.getValue().getFirst();
			DescriptivePiStatisticsFactory factory = entry.getValue().getSecond();
//			PopulationEstimator estimator = entry.getValue().vertexEstimator;
//			PopulationEstimator edgeEstimator = entry.getValue().edgeEstimator;
			
			DegreeTask estimDegree = new DegreeTask();
			estimDegree.setModule(new EstimatedDegree(biasdDistr, factory));
			tasks.addTask(estimDegree);
			
			TransitivityTask estimTransitivity = new TransitivityTask();
			EstimatedTransitivity trans = new EstimatedTransitivity(biasdDistr, factory, false);
			trans.enableCaching(true);
			estimTransitivity.setModule(trans);
			tasks.addTask(estimTransitivity);
			
			tasks.addTask(new EstimatorTask(entry.getValue().getFirst()));
			
			analyzers.put(entry.getKey(), tasks);
		}
		
		return analyzers;
	}
	
//	private static class EstimatorSet {
//		
//		private PiEstimator distribution;
//		
//		private PopulationEstimator vertexEstimator;
//		
//		private PopulationEstimator edgeEstimator;
//		
//		public EstimatorSet(PiEstimator distribution, PopulationEstimator vertexEstimator, PopulationEstimator edgeEstimator) {
//			this.distribution = distribution;
//			this.vertexEstimator = vertexEstimator;
//			this.edgeEstimator = edgeEstimator;
//		}
//	}
}
