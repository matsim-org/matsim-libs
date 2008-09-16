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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.utils.io.IOUtils;

import playground.johannes.snowball.Histogram;
import cern.colt.list.IntArrayList;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.io.GraphMLFile;
import gnu.trove.TObjectDoubleHashMap;

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
	
	private double pResponse;
	
	private double pFOF;
	
	private List<String> statisticsKeys;
	
	private boolean calcOriginalStats;
	
	private double maxSamplingFraction;
	
	private static final String ISOLATES_KEY = "isolates";
	
	private static final String DEGREE_KEY = "degree";
	
	private static final String W_DEGREE_KEY = "wdegree";
	
	private static final String CLUSTERING_KEY = "clustering";
	
	private static final String BETWEENNESS_KEY = "betweenness";
	
	private static final String CLOSENESS_KEY = "closeness";
	
	private static final String COMPONENTS_KEY = "components";
	
	private static final String DEGREE_CORRELATION_KEY = "dcorrelation";
	
	private static final String TYPE_CLUSTERS_KEY = "typeClusters";
	
	private static final String COMPONENT_STATS_KEY = "componentStats";
	
	private static final String MUTUALITY_KEY = "mutuality";
	
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
		exe.calcOriginalStats = Boolean.parseBoolean(config.getParam(MODULE_NAME, "calcOriginalStats"));
		exe.maxSamplingFraction = Double.parseDouble(config.getParam(MODULE_NAME, "maxSamplingFraction"));
		
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
		 * (1) Load graph...
		 */
		logger.info("Loading network...");
		Graph g = loadGraph(graphFile);
		/*
		 * (2) Initialize the sampler...
		 */
		Sampler sampler = new Sampler(g, numSeeds, randomSeed);
		sampler.setPResponse(pResponse);
		sampler.setPFOF(pFOF);
		/*
		 * (3) Load statistical property classes...
		 */
		Map<String, GraphStatistic> statistics = new LinkedHashMap<String, GraphStatistic>();
		Centrality centrality = new Centrality();
		
		for(String key : statisticsKeys) {
			new File(outputDir + key).mkdirs();
			if(ISOLATES_KEY.equalsIgnoreCase(key)) {
				statistics.put(key, new CountIsolates(outputDir + key));
			} else if(DEGREE_KEY.equalsIgnoreCase(key)) {
				statistics.put(key, new Degree(outputDir + key));
			} else if(W_DEGREE_KEY.equalsIgnoreCase(key)) {
				Degree d = new Degree(outputDir + key);
				d.setBiasCorrection(true);
				statistics.put(key, d);			
			} else if(BETWEENNESS_KEY.equalsIgnoreCase(key)) {
				statistics.put(key, new Betweenness(centrality, outputDir + key));
			} else if(CLOSENESS_KEY.equalsIgnoreCase(key)) {
				statistics.put(key, new Closeness(centrality, outputDir + key));
			} else if(CLUSTERING_KEY.equalsIgnoreCase(key)) {
				statistics.put(key, new Clustering(outputDir + key));
			} else if(COMPONENTS_KEY.equalsIgnoreCase(key)) {
				statistics.put(key, new CountComponents(outputDir + key));
			} else if(DEGREE_CORRELATION_KEY.equalsIgnoreCase(key)) {
				statistics.put(key, new DegreeCorrelation(outputDir + key));
			} else if(TYPE_CLUSTERS_KEY.equalsIgnoreCase(key)) {
				statistics.put(key, new TypeClusters(outputDir + key));
			} else if(COMPONENT_STATS_KEY.equalsIgnoreCase(key)) {
				statistics.put(key, new ComponentsStats(sampler.getSeeds(), outputDir + key));
			} else if(MUTUALITY_KEY.equalsIgnoreCase(key)) {
				statistics.put(key, new Mutuality(outputDir + key));
			} else {
				logger.warn(String.format("No class found for statistics \"%1$s\"!", key));
			}
			
		}
		/*
		 * (3b) Write the sampled graph after each wave as pajek file.
		 */
		PajekVisWriter visWriter = new PajekVisWriter();
		new File(outputDir + "pajek").mkdirs();
		/*
		 * (4) Open writer for statistical summary.
		 */
		BufferedWriter summaryWriter = IOUtils.getBufferedWriter(outputDir + "meanValues.txt");
		summaryWriter.write("it");
		summaryWriter.write("\tnVertex");
		summaryWriter.write("\tnEdges");
		summaryWriter.write("\tfVertex");
		summaryWriter.write("\tfEdges");
		summaryWriter.write("\tgrowth");
		summaryWriter.write("\tefficiency");
		for(String key : statisticsKeys) {
			summaryWriter.write("\t");
			summaryWriter.write(key);
		}
		summaryWriter.newLine();
		/*
		 * (5) Compute the statistical properties of the original network.
		 */
		Map<String, DescriptiveStatistics> references = new HashMap<String, DescriptiveStatistics>();
		TObjectDoubleHashMap<String> meanValues;
		if (calcOriginalStats) {
			logger.info("Computing original network statistics...");
			meanValues = computeStatistics(g, statistics, references, -1);

			summaryWriter.write(String.format("-1\t%1$s\t%2$s\t1\t1\t0\t0", g
					.numVertices(), g.numEdges()));
			for (String key : statisticsKeys) {
				summaryWriter.write("\t");
				summaryWriter.write(String.format(Locale.US, "%1.4f", meanValues.get(key)));
			}
			summaryWriter.newLine();
			summaryWriter.flush();
		}
		
		int numSampledVertices = 0;
		int numVisitedVertices = 0;
		int lastNumSampledVertices = 0;
		int lastNumSampledVertices2 = 0;
		int lastNumVisitedVertices = 0;
		int deltaLastVisited = 0;
		SampledGraph sample = null;
		/*
		 * (6) Run waves...
		 */
		while(numSampledVertices < g.numVertices()) {
			/*
			 * (6a) Run one wave.
			 */
			logger.info(String.format("Running iteration %1$s...", sampler.getCurrentWave() + 1));
			sample = sampler.runWave();
			/*
			 * (6b) Calculate basic properties.
			 */
			numSampledVertices = countSampledVertices(sample);
			numVisitedVertices = countVisitedVertices(sample);
			logger.info(String.format(Locale.US, "Sampled %1.4f percent of all vertices.", (numSampledVertices/(double)g.numVertices())));
			
			int deltaSampled = numSampledVertices - lastNumSampledVertices;
			int deltaVisited = numVisitedVertices - lastNumVisitedVertices;
			double efficiency = deltaSampled/(double)deltaLastVisited;
			deltaLastVisited = deltaVisited;
			logger.info(String.format(Locale.US, "Sampling efficiency is %1.4f", efficiency));
			
//			sampler.calculateSampleProbas(g, lastNumSampledVertices, lastNumSampledVertices2, g.numVertices());
			sampler.calculateSampleProbas(g, lastNumSampledVertices, lastNumSampledVertices2, g.numVertices());
			
			summaryWriter.write(String.valueOf(sampler.getCurrentWave()));
			summaryWriter.write("\t");
			summaryWriter.write(String.valueOf(numSampledVertices));
			summaryWriter.write("\t");
			summaryWriter.write(String.valueOf(sample.numEdges()));
			summaryWriter.write("\t");
			summaryWriter.write(String.format(Locale.US, "%1.4f", numSampledVertices/(double)g.numVertices()));
			summaryWriter.write("\t");
			summaryWriter.write(String.format(Locale.US, "%1.4f", sample.numEdges()/(double)g.numEdges()));
			summaryWriter.write("\t");
			summaryWriter.write(String.format(Locale.US, "%1.4f", numSampledVertices/(double)lastNumSampledVertices));
			summaryWriter.write("\t");
			summaryWriter.write(String.format(Locale.US, "%1.4f", efficiency));
			/*
			 * (6c) Calculate network properties.
			 */
			logger.info("Computing sampled network statistics...");
			meanValues = computeStatistics(sample, statistics, references, sampler.getCurrentWave());
			for(String key : statisticsKeys) {
				summaryWriter.write("\t");
				summaryWriter.write(String.format(Locale.US, "%1.8f", meanValues.get(key)));
			}
			summaryWriter.newLine();
			summaryWriter.flush();
			/*
			 * (6d) Write sampled graph as pajek file.
			 */
			visWriter.write(sample, outputDir + "pajek/" + sampler.getCurrentWave() + "pajek.net");
			/*
			 * (6e) Check if we sampled all reachable vertices. 
			 */
			if(lastNumSampledVertices == numSampledVertices) {
				logger.warn("Aborted sampling because all reachable vertices have been sampled!");
				break;
			}
			if(numSampledVertices/(double)g.numVertices() >= maxSamplingFraction) {
				logger.info("Reached maximum sampling fraction.");
				break;
			}
			lastNumSampledVertices2 = lastNumSampledVertices;
			lastNumSampledVertices = numSampledVertices;
			lastNumVisitedVertices = numVisitedVertices;
			/*
			 * (6f) Important! Reset the centrality measure.
			 */
//			centrality.reset();
		}
		summaryWriter.close();
		logger.info("Making coverage chart...");
		makeCoverageCharts(sample, sampler.getCurrentWave());
		logger.info("Sampling done.");
	}

	private Graph loadGraph(String filename) {
		return new GraphMLFile().load(filename); 
	}
	
	private TObjectDoubleHashMap<String> computeStatistics(Graph g, Map<String, GraphStatistic> statistics, Map<String, DescriptiveStatistics> references, int iteration) {
		TObjectDoubleHashMap<String> meanValues = new TObjectDoubleHashMap<String>();
		for(String key : statisticsKeys) {
			logger.info(String.format("Calculating statistics... %1$s.", key));
			GraphStatistic s = statistics.get(key);
			if(s != null) {
			DescriptiveStatistics ref = references.get(key);
			DescriptiveStatistics stats = s.calculate(g, iteration, ref);
			if(ref == null && iteration == -1)
				references.put(key, stats);

			meanValues.put(key, stats.getMean());
			} else {
				meanValues.put(key, Double.NaN);
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
	
	private int countVisitedVertices(SampledGraph g) {
		int numVisitedVertices = 0;
		Set<SampledVertex> vertices = g.getVertices();
		for(SampledVertex v : vertices) {
				numVisitedVertices += v.getVisited();
		}
		return numVisitedVertices;
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
		IntArrayList numVPerDegree = new IntArrayList();
		Map<Integer, IntArrayList> curves = new LinkedHashMap<Integer, IntArrayList>();
		IntArrayList numVPerWave = new IntArrayList();
		numVPerWave.setSize(numWaves + 1);
		int numTotalV = sample.numVertices();
		
		for(SampledVertex v : sample.getVertices()) {
			int idx = v.degree();
			if(numVPerDegree.size() <= idx)
				numVPerDegree.setSize(idx+1);
			numVPerDegree.set(idx, numVPerDegree.get(idx) + 1);
			
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
			
			numVPerWave.set(wave, numVPerWave.get(wave) + 1);
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
			for(int i = 0; i<= numWaves; i++) {
				writer.write(String.valueOf(i));
				for(Integer k : curves.keySet()) {
					writer.write("\t");
					int count = curves.get(k).get(i);
					int sum = count + sums.get(k);
					sums.put(k, sum);
					int total = numVPerDegree.get(k);
					writer.write(String.valueOf(sum/(double)total));
//					writer.write("\t");
//					writer.write(String.valueOf(sum));
				}
				writer.newLine();
			}
			writer.close();
			
			/*
			 * ============================================================================
			 */
			
			writer = IOUtils.getBufferedWriter(outputDir + "coverage.analytical.txt");
			writer.write("it");
			for(Integer k : curves.keySet()) {
				writer.write("\t");
				writer.write("k=" + String.valueOf(k));
			}
			writer.newLine();
			
			int[] numVPerWaveAccum = new int[numVPerWave.size()];
			for(int i = 0; i < numVPerWave.size(); i++) {
				for(int k = i; k > -1; k--)
					numVPerWaveAccum[i] += numVPerWave.get(k);
			}
			Map<Integer, Double> probasPerWaveAccum = new HashMap<Integer, Double>();
//			Map<Integer, Integer> numVPerDegreeCatAccum = new HashMap<Integer, Integer>();
			
			List<int[]> numNeighboursPerDegreeWave = new ArrayList<int[]>();
			for(int i = 0; i<= numWaves; i++) {
				writer.write(String.valueOf(i));
//				Map<Integer, Integer> neighbourPorbas = getNeighbourProba(sample, i);
//				int[][] correlationMatrix = getCorrelationMatrix(sample, numVPerDegree, i);
				
				int[] numVerticesPerDegree = getNumVertexPerDegree(sample, i);
				int[][] degreeCorrelation = getDegreeCorrelation(sample, i+1, numVPerDegree.size()- 1);
				int[] numNeighboursPerDegree = getNeighboursPerDegree(degreeCorrelation);
				numNeighboursPerDegreeWave.add(numNeighboursPerDegree);
				
				for(Integer k : curves.keySet()) {
					if(i == 0) {
						writer.write("\t");
						double p = numSeeds/(double)numTotalV;
						writer.write(String.valueOf(p));
//						if(k >= numVerticesPerDegree.length)
//							writer.write("0");
//						else
//							writer.write(String.valueOf(numVerticesPerDegree[k]*p));
						probasPerWaveAccum.put(k, p);
//					} else if(i == 1 || i ==2) {
//						writer.write("\t");
////						double p = 1 - Math.pow(1 - numSeeds/(double)numTotalV, k);
//						double p = 1 - Math.pow(1 - (numVPerWaveAccum[i-1]/(double)numTotalV), k);
//						
//						double p_minus1 = (Double)probasPerWaveAccum.get(k);
//						double p_accum = p + p_minus1 - (p * p_minus1);
//						probasPerWaveAccum.put(k, p_accum);
//						writer.write(String.valueOf(p_accum));
					} else {
						/*
						 * Original form ===================================================================
						 */
						double p = 1 - Math.pow(1 - (numVPerWaveAccum[i-1]/(double)numTotalV), k);

						/*
						 * Degree categorization ===================================================================
						 */
//						double product = 1;
//						for(Integer k_cat : numVPerDegreeCatAccum.keySet()) {
//							int numVPerK_cat = numVPerDegreeCatAccum.get(k_cat);
//							double base = 1 - (numVPerK_cat/(double)numTotalV);
//							double exp = (numVPerK_cat / (double)numVPerWaveAccum[i-1]) * k;
////							double exp = correlationMatrix[k][k_cat] ;
//							product *= Math.pow(base, exp);
//						}
//						double p = 1 - product;
					
						/*
						 * Neighbour proba ===================================================================
						 */
//						if(k==6 && i ==3)
//							System.out.print(true);
//						double p = 0;
//						int numNeighbours = numNeighboursPerDegree[k];
//						if (numNeighbours > 0) {
//							double sum = numNeighboursPerDegree[k];
//							for (int k2 = 0; k2 < numVerticesPerDegree.length; k2++) {
////								int numVertexPerDegree = numVerticesPerDegree[k2];
//								int numVertexNeighbours = degreeCorrelation[k2][k];
////								sum += numVertexPerDegree * numVertexNeighbours;
//								sum += numVertexNeighbours;
//							}
//							p = 1 - Math.pow(1 - (sum / (double)(numTotalV * numNeighbours)), k);
//							p = 1 - Math.pow(1 - (sum / (double)(numTotalV)), k);
//							p = 1 - Math.pow(1 - ((numNeighboursPerDegreeWave.get(i-1)[k]/(double)numTotalV) / (numNeighboursPerDegree[k]/(double)numVPerWaveAccum[i])), k);
//						}
						/*
						 * accumulate
						 */
//						double p_minus1 = (Double)probasPerWaveAccum.get(k);
//						double p_accum = p + p_minus1 - (p * p_minus1);
//						probasPerWaveAccum.put(k, p_accum);
						probasPerWaveAccum.put(k, p);
						
						writer.write("\t");
//						writer.write(String.valueOf(p_accum));
						writer.write(String.valueOf(p));
//						writer.write("\t");
//						if(k >= numVerticesPerDegree.length)
//							writer.write("0");
//						else
//							writer.write(String.valueOf(numVerticesPerDegree[k]*p));
					}
				}
//				numVPerDegreeCatAccum = countSamplesPerDegree(sample, i, numVPerDegreeCatAccum);
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
	
	private Map<Integer, Integer> countSamplesPerDegree(SampledGraph sample,
			int wave, Map<Integer, Integer> samplesPerDegree) {
		Map<Integer, Integer> samples = new HashMap<Integer, Integer>(samplesPerDegree);
		for (SampledVertex v : sample.getVertices()) {
			if (!v.isAnonymous() && v.getWaveSampled() == wave) {
				Integer count = samples.get(v.degree());
				if (count == null)
					count = 0;
				count++;
				samples.put(v.degree(), count);
			}
		}

		return samples;
	}
	
	private Set<Integer> incidentDegrees(Vertex v) {
		Set<Integer> degrees = new HashSet<Integer>();
		for(Object v2 : v.getNeighbors()) {
			degrees.add(((Vertex)v2).degree());
		}
		return degrees;
	}
	
	private int[][] getCorrelationMatrix(SampledGraph graph, IntArrayList numVPerDegree, int wave) {
		int dim = numVPerDegree.size();
		int[][] numNeighbours = new int[dim][dim];
		
		Set<SampledVertex> vertices = graph.getVertices();
		for(SampledVertex v : vertices) {
			if(!v.isAnonymous() && v.getWaveSampled() < wave) {
				Set<SampledVertex> neighbours = v.getNeighbors();
				for(SampledVertex v2 : neighbours) {
					if(!v2.isAnonymous() && v2.getWaveSampled() < wave) {
						numNeighbours[v.degree()][v2.degree()]++;
					}
				}
			}
		}
		
//		double[][] correlationMatrix = new double[dim][dim];
//		for(int i = 0; i < dim; i++) {
//			for(int k = 0; k < dim; k++) {
////				double num_k = (double)numVPerDegree.get(k);
//				double num_i = (double)numVPerDegree.get(i);
//				if(num_i != 0)
////					correlationMatrix[i][k] = (numNeighbours[i][k] /num_k) / num_i;
//					correlationMatrix[i][k] = (numNeighbours[i][k] /num_i);
//				else
//					correlationMatrix[i][k] = 0;
//			}
//		}
//		
//		return correlationMatrix;
		return numNeighbours;
	}
	
	private Map<Integer, Integer> getNeighbourProba(SampledGraph graph, int wave) {
		Map<Integer, Integer> neighboursPerDegree = new HashMap<Integer, Integer>();
		
		Set<SampledVertex> vertices = graph.getVertices();
		int numVertices = 0;
		for(SampledVertex v : vertices) {
			if(!v.isAnonymous() && v.getWaveSampled() < wave) {
				numVertices++;
				Set<SampledVertex> neighbours = v.getNeighbors();
				for(SampledVertex v2 : neighbours) {
					if(!v2.isAnonymous() && v2.getWaveSampled() < wave) {
						Integer count = neighboursPerDegree.get(v2.degree());
						int cnt = 0;
						if(count != null)
							cnt = count;
						cnt++;
						neighboursPerDegree.put(v2.degree(), cnt);
					}
				}
			}
		}
		
//		Map<Integer, Double> neighbourProbas = new HashMap<Integer, Double>();
//		for(Integer k : neighboursPerDegree.keySet()) {
//			Integer count = neighboursPerDegree.get(k);
//			double proba = count / (double)numVertices;
//			neighbourProbas.put(k, proba);
//		}
//		
//		return neighbourProbas;
		return neighboursPerDegree;
	}
	
	private int[] getNumVertexPerDegree(SampledGraph g, int wave) {
		IntArrayList verticesPerDegree = new IntArrayList();
		Set<SampledVertex> vertices = g.getVertices();
		for(SampledVertex v : vertices) {
			if(v.getWaveSampled() < wave && !v.isAnonymous()) {
				int idx = v.degree();
				if(verticesPerDegree.size() <= idx)
					verticesPerDegree.setSize(idx+1);
				int count =	verticesPerDegree.get(idx);
				count++;
				verticesPerDegree.set(idx, count);
			}
		}
		
		int[] counts = new int[verticesPerDegree.size()];
		for(int i = 0; i < verticesPerDegree.size(); i++) {
			counts[i] = verticesPerDegree.get(i);
		}
		return counts;
	}
	
	private int[][] getDegreeCorrelation(SampledGraph g, int wave, int maxDegree) {
		int[][] numVertices = new int[maxDegree + 1][maxDegree + 1];
		Set<SampledVertex> vertices = g.getVertices();
		for (SampledVertex v1 : vertices) {
			
			if (v1.getWaveSampled() < wave && !v1.isAnonymous()) {
				Set<SampledVertex> neighbours = v1.getNeighbors();
				Set<Integer> degreesFound = new HashSet<Integer>();
				
				for (SampledVertex v2 : neighbours) {
					if(v2.getWaveSampled() < wave && !v2.isAnonymous()) {
						degreesFound.add(v2.degree());
					}
				}
				
				for(Integer k : degreesFound) {
					numVertices[v1.degree()][k]++;
				}
			}
		}
//		for(SampledEdge e : g.getEdges()) {
//			Pair p = e.getEndpoints();
//			SampledVertex v1 = (SampledVertex) p.getFirst();
//			SampledVertex v2 = (SampledVertex) p.getSecond();
//			if(v1.getWaveSampled() < wave && !v1.isAnonymous() && v2.getWaveSampled() < wave && !v2.isAnonymous()) {
//				numVertices[v1.degree()][v2.degree()]++;
//				numVertices[v2.degree()][v1.degree()]++;
//			}
//		}
		
		return numVertices;
	}
	
	private int[] getNeighboursPerDegree(int[][] degreeCorrelation) {
		int[] numVertices = new int[degreeCorrelation.length];
		for(int k = 0; k < degreeCorrelation.length; k++) {
			int sum = 0;
			for(int i = 0; i < degreeCorrelation.length; i++)
				sum += degreeCorrelation[i][k];
			numVertices[k] = sum;
		}
		
		return numVertices;
	}
}
