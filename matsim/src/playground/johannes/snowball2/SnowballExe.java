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
package playground.johannes.snowball2;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.utils.io.IOUtils;

import cern.colt.list.IntArrayList;

import playground.johannes.snowball.Histogram;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.io.GraphMLFile;

/**
 * @author illenberger
 *
 */
public class SnowballExe {

	private static final Logger logger = Logger.getLogger(SnowballExe.class);
	
	private long randomSeed;
	
	private String outputDir;
	
	private String graphFile;
	
	private String rExePath;
	
	private String tmpDir;
	
	private int numSeeds;
	
	private double pResponse;
	
	private double pFOF;
	
	private List<String> statsKeys;
	
	private static final String ISOLATES_KEY = "isolates";
	
	private static final String DEGREE_KEY = "degree";
	
	private static final String CLUSTERING_KEY = "clustering";
	
	private static final String BETWEENNESS_KEY = "betweenness";
	
	private static final String COMPONENTS_KEY = "components";
	
	private static final String DEGREE_CORRELATION_KEY = "dcorrelation";
	
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
		exe.pResponse = Double.parseDouble(config.getParam(MODULE_NAME, "pResponse"));
		exe.pFOF = Double.parseDouble(config.getParam(MODULE_NAME, "pFOF"));
		
		exe.outputDir = config.getParam(MODULE_NAME, "outputDir");
		exe.randomSeed = config.global().getRandomSeed();
		exe.rExePath = config.getParam(MODULE_NAME, "RExePath");
		exe.tmpDir = config.getParam(MODULE_NAME, "tmpDir");
		
		String str = config.getParam(MODULE_NAME, "statistics");
		String[] tokens = str.split(",");
		exe.statsKeys = new ArrayList<String>(tokens.length);
		for(String token : tokens) {
			exe.statsKeys.add(token.trim());
		}
		
		exe.run();
	}
	
	public void run() throws FileNotFoundException, IOException {
		logger.info("Loading network...");
		Graph g = loadGraph(graphFile);
		
		Map<String, GraphStatistic> statistics = new LinkedHashMap<String, GraphStatistic>();
		Map<String, GraphStatistic> sampleStatistics = new LinkedHashMap<String, GraphStatistic>();
		
		for(String key : statsKeys) {
			if(ISOLATES_KEY.equalsIgnoreCase(key)) {
				statistics.put(key, new CountIsolates());
				sampleStatistics.put(key, new CountIsolates());
			} else if(DEGREE_KEY.equalsIgnoreCase(key)) {
				statistics.put(key, new Degree());
				sampleStatistics.put(key, new DegreeSampled());
			} else if(BETWEENNESS_KEY.equalsIgnoreCase(key)) {
				statistics.put(key, new BetweennessRScript(rExePath, tmpDir));
				sampleStatistics.put(key, new BetweennessRScript(rExePath, tmpDir));
			} else if(CLUSTERING_KEY.equalsIgnoreCase(key)) {
				statistics.put(key, new Clustering());
				sampleStatistics.put(key, new ClusteringSampled());
			} else if(COMPONENTS_KEY.equalsIgnoreCase(key)) {
				CountComponents cc = new CountComponents();
				statistics.put(key, cc);
				sampleStatistics.put(key, cc);
			} else if(DEGREE_CORRELATION_KEY.equalsIgnoreCase(key)) {
				statistics.put(key, new DegreeCorrelation());
				sampleStatistics.put(key, new DegreeCorrelationSampled());
			} else {
				logger.warn(String.format("No class found for statistics \"%1$s\"!", key));
			}
			new File(outputDir + key).mkdirs();
		}
		
		Map<String, double[]> references = new HashMap<String, double[]>();
		logger.info("Computing original network statistics...");
		Map<String, Double> meanValues = computeStatistics(g, statistics, references, -1);
		
		BufferedWriter meanWriter = IOUtils.getBufferedWriter(outputDir + "meanValues.txt");
		meanWriter.write("it");
		meanWriter.write("\tnVertex");
		meanWriter.write("\tnEdges");
		meanWriter.write("\tfVertex");
		meanWriter.write("\tfEdges");
		for(String key : meanValues.keySet()) {
			meanWriter.write("\t");
			meanWriter.write(key);
		}
		meanWriter.newLine();
		
		meanWriter.write(String.format("-1\t%1$s\t%2$s\t1\t1", g.numVertices(), g.numEdges()));
		for(String key : meanValues.keySet()) {
			meanWriter.write("\t");
			meanWriter.write(String.valueOf((float)(double)meanValues.get(key)));
		}
		meanWriter.newLine();
		meanWriter.flush();
//		System.exit(0);
		Sampler sampler = new Sampler(g, numSeeds, randomSeed);
		sampler.setPResponse(pResponse);
		sampler.setPFOF(pFOF);
		
		int numSampledVertices = 0;
		int lastNumSampledVertices = 0;
		int lastNumSampledVertices2 = 0;
		SampledGraph sample = null;
		while(numSampledVertices < g.numVertices()) {
			logger.info(String.format("Running iteration %1$s...", sampler.getCurrentWave() + 1));
			sample = sampler.runWave();
			logger.info("Computing sampled network statistics...");
			numSampledVertices = countSampledVertices(sample);
			logger.info("Sampled " + (numSampledVertices/(float)g.numVertices()) + " % of all vertices.");
			
			sampler.calculateSampleProbas(g, lastNumSampledVertices, lastNumSampledVertices2, g.numVertices());
			
			meanWriter.write(String.valueOf(sampler.getCurrentWave()));
			meanWriter.write("\t");
			meanWriter.write(String.valueOf(numSampledVertices));
			meanWriter.write("\t");
			meanWriter.write(String.valueOf(sample.numEdges()));
			meanWriter.write("\t");
			meanWriter.write(String.valueOf(numSampledVertices/(float)g.numVertices()));
			meanWriter.write("\t");
			meanWriter.write(String.valueOf(sample.numEdges()/(float)g.numEdges()));
			
			meanValues = computeStatistics(sample, sampleStatistics, references, sampler.getCurrentWave());
			for(String key : meanValues.keySet()) {
				meanWriter.write("\t");
				meanWriter.write(String.valueOf((float)(double)meanValues.get(key)));
			}
			meanWriter.newLine();
			meanWriter.flush();
			
			if(lastNumSampledVertices == numSampledVertices) {
				logger.warn("Aborted sampling because the maximum amount of vertices that can be sampled have been sampled!");
				break;
			}
			lastNumSampledVertices2 = lastNumSampledVertices;
			lastNumSampledVertices = numSampledVertices;
		}
		meanWriter.close();
		logger.info("Making coverage chart...");
		makeCoverageCharts(sample, sampler.getCurrentWave());
		logger.info("Sampling done.");
	}

	private Graph loadGraph(String filename) {
		return new GraphMLFile().load(filename); 
	}
	
	private Map<String, Double> computeStatistics(Graph g, Map<String, GraphStatistic> statistics, Map<String, double[]> references, int iteration) {
		Map<String, Double> meanValues = new LinkedHashMap<String, Double>();
		for(String key : statistics.keySet()) {
			GraphStatistic s = statistics.get(key);
			meanValues.put(key, s.run(g));
		}
		
		for(String key : statistics.keySet()) {
			GraphStatistic s = statistics.get(key);
			if(s instanceof VertexStatistic) {
				double[] ref = references.get(key);
				Histogram hist;
				if(ref == null) {
					hist = ((VertexStatistic)s).getHistogram();
					ref = new double[3];
					double[] minmax = hist.getMinMax();
					ref[0] = hist.getMean();
					ref[1] = minmax[0];
					ref[2] = minmax[1];
					references.put(key, ref);
				} else {
					hist = ((VertexStatistic)s).getHistogram(ref[1], ref[2]);
				}
				String filename = String.format("%1$s%2$s/%3$s%4$s.png", outputDir, key, iteration, key);
				try {
					hist.plot(filename, key);
				} catch (IOException e) {
					logger.warn("Plotting histogram failed!", e);
				}
				
				meanValues.put(key + "_gamma", calcGammaExponent(hist));
			}
		}
		
		return meanValues;
	}
	
	private int countSampledVertices(SampledGraph g) {
		int numSampledVertices = 0;
		Set<SampledVertex> vertices = g.getVertices();
		for(SampledVertex v : vertices) {
			if(!v.isAnonymous())
				numSampledVertices++;
		}
		return numSampledVertices;
	}
	
	private double calcGammaExponent(Histogram hist) {
		double minVal = hist.getBinLowerBound(hist.getMaxBin());
		if(minVal == 0)
			minVal = Double.MIN_VALUE;
		double wsum = 0;
		double logsum = 0;
		for(int i = 0; i < hist.getValues().size(); i++) {
			if(hist.getValues().get(i) >= minVal) {
				double w = hist.getWeights().get(i); 
				logsum += Math.log(hist.getValues().get(i)/minVal) * w;
				wsum += w;
			}
		}
		
		return 1 + (wsum/logsum);
	}
	
	private void makeCoverageCharts(SampledGraph sample, int numWaves) {
		IntArrayList totalNumVertices = new IntArrayList();
		Map<Integer, IntArrayList> curves = new LinkedHashMap<Integer, IntArrayList>();
		IntArrayList samplesPerWave = new IntArrayList();
		samplesPerWave.setSize(numWaves + 1);
		int totalVertices = sample.numVertices();
		
		for(SampledVertex v : sample.getVertices()) {
			int idx = v.degree();
			if(totalNumVertices.size() <= idx)
				totalNumVertices.setSize(idx+1);
			totalNumVertices.set(idx, totalNumVertices.get(idx) + 1);
			
			IntArrayList curve = curves.get(v.degree());
			if(curve == null) {
				curve = new IntArrayList();
				curve.setSize(numWaves+1);
				curves.put(v.degree(), curve);
			}
			int wave = v.getWaveSampled();
//			if(curve.size() > wave)
//				curve.setSize(wave + 1);
			curve.set(wave, curve.get(wave) + 1);
			
			samplesPerWave.set(wave, samplesPerWave.get(wave) + 1);
		}
		
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(outputDir + "coverage.txt");
			
			writer.write("it");
			for(Integer k : curves.keySet()) {
				writer.write("\t");
				writer.write("k=" + String.valueOf(k));
			}
			writer.newLine();
			
			Map<Integer, Integer> sums = new HashMap<Integer, Integer>();
			for(Integer k : curves.keySet()) {
				sums.put(k, 0);
			}
			for(int i = 0; i< numWaves; i++) {
				writer.write(String.valueOf(i));
				for(Integer k : curves.keySet()) {
					writer.write("\t");
					int count = curves.get(k).get(i);
					int sum = count + sums.get(k);
					sums.put(k, sum);
					int total = totalNumVertices.get(k);
					writer.write(String.valueOf(sum/(double)total));
				}
				writer.newLine();
			}
			writer.close();
			
			
			writer = IOUtils.getBufferedWriter(outputDir + "coverage.analytical.txt");
			writer.write("it");
			for(Integer k : curves.keySet()) {
				writer.write("\t");
				writer.write("k=" + String.valueOf(k));
			}
			writer.newLine();
			
			int[] sampleSums = new int[samplesPerWave.size()];
			for(int i = 0; i < samplesPerWave.size(); i++) {
				for(int k = i; k > -1; k--)
					sampleSums[i] += samplesPerWave.get(k);
			}
			Map<Integer, Double> probas = new HashMap<Integer, Double>();
			
			for(int i = 0; i< numWaves; i++) {
				writer.write(String.valueOf(i));
				for(Integer k : curves.keySet()) {
					if(i == 0) {
						writer.write("\t");
						double p = numSeeds/(double)totalVertices;
						writer.write(String.valueOf(p));
						probas.put(k, p);
					} else {
						
						double p = 1 - Math.pow(1 - (sampleSums[i-1]/(double)totalVertices), k);
						double p_minus1 = (Double)probas.get(k);
						double p_accum = p + p_minus1 - (p * p_minus1);
						probas.put(k, p_accum);
						writer.write("\t");
						writer.write(String.valueOf(p_accum));
					}
				}
				writer.newLine();
			}
			writer.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
