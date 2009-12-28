/* *********************************************************************** *
 * project: org.matsim.*
 * GeneralStats.java
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

import gnu.trove.TIntArrayList;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TObjectDoubleHashMap;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.core.utils.io.IOUtils;

import playground.johannes.socialnetworks.graph.GraphProjection;
import playground.johannes.socialnetworks.graph.VertexDecorator;

/**
 * @author illenberger
 *
 */
public class SnowballStats extends GraphPropertyEstimator {

	private BufferedWriter edgeCountWriter;
	
	private BufferedWriter coverageVarianceWriter;
	
	public SnowballStats(String outputDir) {
		super(outputDir);
		openStatsWriters("vertex");
		
		try {
			edgeCountWriter = IOUtils.getBufferedWriter(String.format("%1$s/edges.txt", outputDir));
			edgeCountWriter.write("iteration\tnumEdges\tfracEdges");
			edgeCountWriter.newLine();
			
			coverageVarianceWriter = IOUtils.getBufferedWriter(String.format("%1$s/coverageVariance.txt", outputDir));
			coverageVarianceWriter.write("iter\tvar_obs\tvar_estim");
			coverageVarianceWriter.newLine();
			
			
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public DescriptiveStatistics calculate(
			GraphProjection<SampledGraph, SampledVertex, SampledEdge> graph,
			int iteration) {
		/*
		 * (1) sampled vertices (default statistics)
		 */
		DescriptiveStatistics stats = new DescriptiveStatistics();
		int numVertices = 0;
		for(VertexDecorator<SampledVertex> v : graph.getVertices()) {
			if(v.getDelegate().isSampled())
				numVertices++;
		}
		stats.addValue(numVertices);
		TObjectDoubleHashMap<String> map = getStatisticsMap(stats);
		
		map.put("fracVertices", numVertices/(double)graph.getDelegate().getVertices().size());
		dumpObservedStatistics(map, iteration);
		/*
		 *  sampled edges
		 */
		try {
			edgeCountWriter.write(String.valueOf(iteration));
			edgeCountWriter.write(TAB);
			edgeCountWriter.write(String.valueOf(graph.getEdges().size()));
			edgeCountWriter.write(TAB);
			edgeCountWriter.write(String.format(Locale.US, FLOAT_FORMAT, graph.getEdges().size()/(double)graph.getDelegate().getEdges().size()));
			edgeCountWriter.newLine();
			edgeCountWriter.flush();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		/*
		 * vertex weights
		 */
		TIntIntHashMap n_k_0 = new TIntIntHashMap();
		for(SampledVertex v : graph.getDelegate().getVertices()) {
			n_k_0.adjustOrPutValue(v.getEdges().size(), 1, 1);
		}
		
		TIntDoubleHashMap degreeWeightStats = new TIntDoubleHashMap();
		TIntDoubleHashMap degreeProbaStats = new TIntDoubleHashMap();
		TIntArrayList ksequence = new TIntArrayList();
		TIntIntHashMap n_k = new TIntIntHashMap();
		
		for(VertexDecorator<SampledVertex> v : graph.getVertices()) {
			if(!v.getDelegate().isAnonymous()) {
				int k = v.getEdges().size();
				degreeWeightStats.put(k, v.getDelegate().getNormalizedWeight());
				degreeProbaStats.put(k, v.getDelegate().getSampleProbability());
				
				ksequence.add(k);
				n_k.adjustOrPutValue(k, 1, 1);
			}
		}
		
		int[] keys = n_k_0.keys();
		Arrays.sort(keys);
		for(int k : keys) {
			degreeWeightStats.adjustOrPutValue(k, 0, 0);
			degreeProbaStats.adjustOrPutValue(k, 0, 0);
		}
			
		try {
			dumpStats(degreeWeightStats, iteration, "weights");
			dumpStats(degreeProbaStats, iteration, "probas");
		} catch (IOException e) {
			e.printStackTrace();
		}
		/*
		 * Degree sequence
		 */
		try {
			BufferedWriter ksequenceWriter = IOUtils.getBufferedWriter(String.format("%1$s/%2$s.degreesequence.txt", outputDir, iteration));
			for(int i = 0; i < ksequence.size(); i++) {
				ksequenceWriter.write(String.valueOf(ksequence.get(i)));
				ksequenceWriter.newLine();
			}
			ksequenceWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		/*
		 * Degree coverage
		 */

		
		DescriptiveStatistics coverageStats = new DescriptiveStatistics();
		DescriptiveStatistics probaStats = new DescriptiveStatistics();
		try {
			BufferedWriter coverageWriter = IOUtils.getBufferedWriter(String.format("%1$s/%2$s.degreecoverage.txt", outputDir, iteration));
			coverageWriter.write("k\tp(k)");
			coverageWriter.newLine();
			
			for(int k : keys) {
				coverageWriter.write(String.valueOf(k));
				coverageWriter.write("\t");
				coverageWriter.write(String.valueOf(n_k.get(k) /(double)n_k_0.get(k)));
				coverageWriter.newLine();
				
				coverageStats.addValue(n_k.get(k) /(double)n_k_0.get(k));
				probaStats.addValue(degreeProbaStats.get(k));
			}
			coverageWriter.close();
			
			coverageVarianceWriter.write(String.format("%1$s\t%2$s\t%3$s", iteration, coverageStats.getVariance(), probaStats.getVariance()));
			coverageVarianceWriter.newLine();
			coverageVarianceWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		return null;
	}

	private void dumpStats(TIntDoubleHashMap stats, int iter, String name) throws FileNotFoundException, IOException {
		BufferedWriter writer = IOUtils.getBufferedWriter(String.format("%1$s/%2$s.%3$s.txt", outputDir, iter, name));
		
		int[] keys = stats.keys();
		Arrays.sort(keys);
		for(int k : keys) {
			writer.write(String.valueOf(k));
			writer.write(TAB);
			writer.write(String.format(Locale.US, FLOAT_FORMAT, stats.get(k)));
			writer.newLine();
		}
		
		writer.close();
	}

	/* (non-Javadoc)
	 * @see playground.johannes.snowball3.GraphProperty#getStatisticsKeys()
	 */
	@Override
	protected List<String> getStatisticsKeys() {
		List<String> keys = super.getStatisticsKeys();
		keys.add("fracVertices");
		return keys;
	}
}
