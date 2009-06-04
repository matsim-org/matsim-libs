/* *********************************************************************** *
 * project: org.matsim.*
 * SnowballExe.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.socialnetworks.snowball;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author illenberger
 *
 */
public class SnowballExe {

	private static final Logger logger = Logger.getLogger(SnowballExe.class);
	
	private long randomSeed;
	
	private String outputDir;
	
	private String graphFile;
	
	private int numSeeds;
	
	private double responseRate;
	
	private List<String> statisticsKeys;
	
	private double maxSamplingFraction;
	
	private int maxNumSamples;
	
	private boolean useEstimator2;
	
	private static final String DEGREE_KEY = "degree";
	
	private static final String CLUSTERING_KEY = "clustering";
	
//	private static final String BETWEENNESS_KEY = "betweenness";
//	
//	private static final String CLOSENESS_KEY = "closeness";
//	
//	private static final String COMPONENTS_KEY = "components";
	
	private static final String DEGREE_CORRELATION_KEY = "dcorrelation";
	
//	private static final String TYPE_CLUSTERS_KEY = "typeClusters";
	
	private static final String COMPONENT_KEY = "components";
	
	private static final String MUTUALITY_KEY = "mutuality";
	
	private static final String DISTANCE_KEY = "distance";
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		Config config = Gbl.createConfig(args);
		
		final String MODULE_NAME = "snowballsampling";
	
		SnowballExe exe = new SnowballExe();
		exe.graphFile = config.getParam(MODULE_NAME, "inputGraph");
		exe.numSeeds = Integer.parseInt(config.getParam(MODULE_NAME, "seeds"));
		exe.responseRate = Double.parseDouble(config.getParam(MODULE_NAME, "pResponse"));
		exe.outputDir = config.getParam(MODULE_NAME, "outputDir");
		exe.randomSeed = config.global().getRandomSeed();
		exe.maxSamplingFraction = Double.parseDouble(config.getParam(MODULE_NAME, "maxSamplingFraction"));
		exe.maxNumSamples = Integer.parseInt(config.getParam(MODULE_NAME, "maxNumSamples"));
		exe.useEstimator2 = Boolean.parseBoolean(config.findParam(MODULE_NAME, "useEstimator2"));
		
		String str = config.getParam(MODULE_NAME, "statistics");
		String[] tokens = str.split(",");
		exe.statisticsKeys = new ArrayList<String>(tokens.length);
		for(String token : tokens) {
			exe.statisticsKeys.add(token.trim());
		}
		
		exe.run();
	}

	public void run() throws FileNotFoundException, IOException {
		/*
		 * (1) Load grpah...
		 */
		logger.info(String.format("Loading graph %1$s...", graphFile));
		SampledGraph graph = new SampledGraphMLReader().readGraph(graphFile);
		/*
		 * (2) Initialize sampler...
		 */
		Sampler sampler = new Sampler(graph, randomSeed);
		sampler.setResponseRate(responseRate);
		sampler.setUseEstimator2(useEstimator2);
		/*
		 * (3) Draw initial respondents...
		 */
		sampler.drawRandomSeedVertices(numSeeds);
		sampler.setNumMaxSamples(maxNumSamples);
		/*
		 * (4) Load statistical property classes...
		 */
		Map<String, GraphPropertyEstimator> statistics = new LinkedHashMap<String, GraphPropertyEstimator>();
		
		for(String key : statisticsKeys) {
			new File(outputDir + key).mkdirs();
			if(DEGREE_KEY.equalsIgnoreCase(key)) {
				statistics.put(key, new DegreeStats(outputDir + key));
			} else if(CLUSTERING_KEY.equalsIgnoreCase(key)) {
				statistics.put(key, new ClusteringStats(outputDir + key, responseRate));
			} else if("clustering2".equalsIgnoreCase(key)) {
				statistics.put(key, new ClusteringStats2(outputDir + key, responseRate));
			} else if("global-clustering".equalsIgnoreCase(key)) {
				statistics.put(key, new GlobalClusteringStats(outputDir + key, responseRate));
			} else if(DEGREE_CORRELATION_KEY.equalsIgnoreCase(key)) {
				statistics.put(key, new DegreeCorrelationStats(outputDir + key, responseRate));
			} else if("dcorrelation-weighted".equalsIgnoreCase(key)) {
				statistics.put(key, new CorrelationStatsWeighted(outputDir + key, responseRate));
			} else if(MUTUALITY_KEY.equalsIgnoreCase(key)) {
				statistics.put(key, new MutualityStats(outputDir + key, responseRate));
			} else if(COMPONENT_KEY.equalsIgnoreCase(key)) {
				statistics.put(key, new ComponentStats(outputDir + key));
			} else if(DISTANCE_KEY.equalsIgnoreCase(key)) {
				statistics.put(key, new GraphDistanceStats(outputDir + key));
			} else {
				logger.warn(String.format("No class found for statistics \"%1$s\"!", key));
			}
		}
		new File(outputDir + "snowball").mkdirs();
		statistics.put("snowball", new SnowballStats(outputDir + "snowball"));
		
		BufferedWriter statsFileWriter = IOUtils.getBufferedWriter(outputDir+"/statsfiles.txt");
		for(GraphPropertyEstimator e : statistics.values()) {
			for(String s : e.getFiles()) {
				statsFileWriter.write(s);
				statsFileWriter.newLine();
			}
		}
		statsFileWriter.close();
		/*
		 * (5) Run sampler...
		 */
		while(sampler.getNumSampledVertices(sampler.getIteration()) < graph.getVertices().size()) {
			/*
			 * (a) Run one iteration
			 */
			logger.info(String.format("Running iteration %1$s...", sampler.getIteration() + 1));
			sampler.runIteration();
			int samples = sampler.getNumSampledVertices(sampler.getIteration());
			int detected = sampler.getProjection().getVertices().size();
			int total = graph.getVertices().size();
			logger.info(String.format(Locale.US, "Sampled %1$s of %2$s vertices (%3$.4f).", samples, total, samples/(double)total));
			logger.info(String.format(Locale.US, "Detected %1$s of %2$s vertices (%3$.4f).", detected, total, samples/(double)total));
			/*
			 * (b) Calculate statistics
			 */
			for(String key : statistics.keySet()) {
				logger.info(String.format("Calculating statistics... %1$s", key));
				statistics.get(key).calculate(sampler.getProjection(), sampler.getIteration());
			}
			/*
			 * (c) Check aborting criterion
			 */
			if(samples/(double)total >= maxSamplingFraction) {
				logger.info("Sampled required fraction of vertices.");
				break;
			}
			if(samples >= maxNumSamples) {
				logger.info("Sampled required amount of vertices.");
				break;
			}
			if(sampler.getEgos().size() == 0) {
				logger.warn("No new respondents!");
				break;
			}
			
		}
		logger.info("Sampling done.");
	}
}
