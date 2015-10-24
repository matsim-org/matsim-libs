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
package playground.johannes.studies.snowball;

import org.matsim.contrib.socnetgen.sna.graph.Edge;
import org.matsim.contrib.socnetgen.sna.graph.Graph;
import org.matsim.contrib.socnetgen.sna.graph.Vertex;
import org.matsim.contrib.socnetgen.sna.graph.analysis.*;
import org.matsim.contrib.socnetgen.sna.graph.io.SparseGraphMLReader;
import org.matsim.contrib.socnetgen.sna.math.DescriptivePiStatisticsFactory;
import org.matsim.contrib.socnetgen.sna.snowball.analysis.*;
import org.matsim.contrib.socnetgen.sna.snowball.sim.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import playground.johannes.socialnetworks.graph.analysis.AnalyzerTaskComposite;
import playground.johannes.socialnetworks.snowball2.analysis.IterationTask;
import playground.johannes.socialnetworks.snowball2.analysis.ResponseRateTask;
import playground.johannes.socialnetworks.snowball2.analysis.WSMStatsFactory;
import playground.johannes.socialnetworks.snowball2.analysis.WaveSizeTask;
import playground.johannes.socialnetworks.snowball2.sim.EstimatorTask;
import playground.johannes.socialnetworks.snowball2.sim.RDSEstimator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author illenberger
 */
public class SnowballSim {

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
//		VertexFilter<Vertex> seedGenerator = new FixedSizeRandomPartition<Vertex>(numSeeds, randomSeed);
//		Object dummy = new Object();
//		Profiler.start(dummy);
        VertexFilter<Vertex> seedGenerator = new PropDegreeFixedSizePartition<Vertex>(numSeeds, randomSeed);
//		Profiler.stopAll();
		/*
		 * Init response rate generator.
		 */
        double responseRate;
        String str = config.getParam(MODULENAME, "responseRate");
        if (str.endsWith("%")) {
            str = str.substring(0, str.length() - 1);
            responseRate = Double.parseDouble(str) / 100.0;
        } else
            responseRate = Double.parseDouble(str);

        VertexFilter<Vertex> reponseGenerator = new RandomPartition<Vertex>(responseRate, randomSeed);
		/*
		 * Init estimators.
		 */
        PiEstimator estimator = new SimplePiEstimator(graph.getVertices().size());
        RDSEstimator rdsEstimator = new RDSEstimator(graph.getVertices().size());
        Map<String, AnalyzerTask> analyzers = loadAnalyzers(graph, estimator, rdsEstimator);
        List<PiEstimator> estimators = new ArrayList<PiEstimator>(2);
        estimators.add(estimator);
        estimators.add(rdsEstimator);
		/*
		 * Get output directory.
		 */
        String output = config.getParam(MODULENAME, "output");
		/*
		 * Init sample analyzers.
		 */
//		final int interval = Integer.parseInt(config.getParam(MODULENAME, "interval"));
//		IntervalSampleAnalyzer intervalAnalyzer = new IntervalSampleAnalyzer(analyzers, estimators, output);
        IntervalSampleAnalyzer intervalAnalyzer = new LogIntervalSampleAnalyzer(analyzers, estimators, output, 1.5, 100, numSeeds);
        IterationSampleAnalyzer iterationAnalyzer = new IterationSampleAnalyzer(analyzers, estimators, output);
        FinalSampleAnalyzer completeAnalyzer = new FinalSampleAnalyzer(analyzers, estimators, output);
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
        listeners.addComponent(iterationAnalyzer);
        listeners.addComponent(completeAnalyzer);
//		listeners.addComponent(connectionAnalyzer);
//		listeners.addComponent(new DegreeGrowth(output));
//		listeners.addComponent(new DegreeShare(output));
		/*
		 * Init and run sampler.
		 */
        SnowballSampler<Graph, Vertex, Edge> sampler = new SnowballSampler<Graph, Vertex, Edge>(randomSeed);
        sampler.setSeedGenerator(seedGenerator);
        sampler.setResponseGenerator(reponseGenerator);
        sampler.setListener(listeners);

        sampler.run(graph);
    }

    public static Config loadConfig(String file) {
        Config config = new Config();
        config.addCoreModules();
        ConfigReader reader = new ConfigReader(config);
        reader.readFile(file);
        return config;
    }

    private static Graph loadGraph(String file) {
        SparseGraphMLReader reader = new SparseGraphMLReader();
        return reader.readGraph(file);
    }

    private static Map<String, AnalyzerTask> loadAnalyzers(Graph graph, PiEstimator estimator, RDSEstimator rdsEstimator) {
        Map<String, AnalyzerTask> analyzers = new HashMap<String, AnalyzerTask>();
		/*
		 * observed 1
		 */
        AnalyzerTaskComposite tasks = new AnalyzerTaskComposite();
        tasks.addTask(new GraphSizeTask());
        tasks.addTask(new WaveSizeTask());
        tasks.addTask(new IterationTask());

        DegreeTask obsDegree = new DegreeTask();
        obsDegree.setModule(ObservedDegree.getInstance());
        tasks.addTask(obsDegree);

        TransitivityTask obsTransitivity = new TransitivityTask();
        obsTransitivity.setModule(ObservedTransitivity.getInstance());
        tasks.addTask(obsTransitivity);

        tasks.addTask(new ResponseRateTask());

        analyzers.put("obs", tasks);
		/*
		 * observed 2
		 */
//		tasks = new AnalyzerTaskComposite();
//		tasks.addTask(new TransitivityTask());
//		analyzers.put("obs2", tasks);
		/*
		 * estimated
		 */
        DescriptivePiStatisticsFactory wsmFactory = new WSMStatsFactory();
        analyzers.put("wsm", createDefaultEstimAnalyzer(estimator, wsmFactory));

        analyzers.put("rds", createDefaultEstimAnalyzer(rdsEstimator, wsmFactory));

//		DescriptivePiStatisticsFactory htFactory = new HTStatsFactory(graph.getVertices().size());
//		analyzers.put("ht", createDefaultEstimAnalyzer(estimator, htFactory));
		/*
		 * estimated transitivity without edge estim
		 */
//		tasks = new AnalyzerTaskComposite();
//		TransitivityTask estimTransitivity = new TransitivityTask();
//		EstimatedTransitivity trans = new EstimatedTransitivity(estimator, wsmFactory, false);
//		trans.enableCaching(false);
//		estimTransitivity.setModule(trans);
//		tasks.addTask(estimTransitivity);
//		analyzers.put("transNoEdgeWSM", tasks);
		/*
		 * estimated transitivity without edge estim
		 */
//		tasks = new AnalyzerTaskComposite();
//		estimTransitivity = new TransitivityTask();
//		trans = new EstimatedTransitivity(estimator, htFactory, false);
//		trans.enableCaching(false);
//		estimTransitivity.setModule(trans);
//		tasks.addTask(estimTransitivity);
//		analyzers.put("transNoEdgeHT", tasks);

        return analyzers;
    }

    private static AnalyzerTask createDefaultEstimAnalyzer(PiEstimator estimator, DescriptivePiStatisticsFactory factory) {
        AnalyzerTaskComposite tasks = new AnalyzerTaskComposite();

        DegreeTask estimDegree = new DegreeTask();
        estimDegree.setModule(new EstimatedDegree(estimator, factory));
        tasks.addTask(estimDegree);

        TransitivityTask estimTransitivity = new TransitivityTask();
        EstimatedTransitivity trans = new EstimatedTransitivity(estimator, factory, true);
        trans.enableCaching(false);
        estimTransitivity.setModule(trans);
        tasks.addTask(estimTransitivity);

        tasks.addTask(new EstimatorTask(estimator));

        return tasks;
    }
}
