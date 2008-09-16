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
package playground.johannes.snowball3;

import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TObjectDoubleHashMap;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.utils.io.IOUtils;

import playground.johannes.graph.GraphProjection;
import playground.johannes.graph.VertexDecorator;

/**
 * @author illenberger
 *
 */
public class SnowballStats extends GraphPropertyEstimator {

	private BufferedWriter edgeCountWriter;
	
	public SnowballStats(String outputDir) {
		super(outputDir);
		openStatsWriters("vertex");
		
		try {
			edgeCountWriter = IOUtils.getBufferedWriter(String.format("%1$s/edges.txt", outputDir));
			edgeCountWriter.write("iteration\tnumEdges\tfracEdges");
			edgeCountWriter.newLine();
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
		
		map.put("fracVertices", numVertices/(double)graph.getDelegate().getEdges().size());
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
		TIntDoubleHashMap degreeWeightStats = new TIntDoubleHashMap();
		TIntDoubleHashMap degreeProbaStats = new TIntDoubleHashMap();
		
		for(VertexDecorator<SampledVertex> v : graph.getVertices()) {
			if(!v.getDelegate().isAnonymous()) {
				degreeWeightStats.put(v.getEdges().size(), v.getDelegate().getNormalizedWeight());
				degreeProbaStats.put(v.getEdges().size(), v.getDelegate().getSampleProbability());
			}
		}
		
		try {
			dumpStats(degreeWeightStats, iteration, "weights");
			dumpStats(degreeProbaStats, iteration, "probas");
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
