/* *********************************************************************** *
 * project: org.matsim.*
 * WaveSizeTask.java
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
package playground.johannes.socialnetworks.snowball2.analysis;

import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntIntIterator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.AnalyzerTask;
import org.matsim.contrib.sna.snowball.SampledVertex;


/**
 * @author illenberger
 *
 */
public class WaveSizeTask extends AnalyzerTask {

	private static final Logger logger = Logger.getLogger(WaveSizeTask.class);
	
	public static final String NUM_DETECTED = "detected";
	
	public static final String NUM_SAMPLED = "sampled";
	
	@Override
	public void analyze(Graph graph, Map<String, Double> stats) {
		TIntIntHashMap detected = new TIntIntHashMap();
		TIntIntHashMap sampled = new TIntIntHashMap();
		for(Vertex v : graph.getVertices()) {
			if(((SampledVertex)v).isSampled())
				sampled.adjustOrPutValue(((SampledVertex)v).getIterationSampled(), 1, 1);
			
			if(((SampledVertex)v).isDetected())
				detected.adjustOrPutValue(((SampledVertex)v).getIterationDetected(), 1, 1);
		}
		
		TIntIntHashMap detectedEdges = new TIntIntHashMap();
		TIntIntHashMap sampledEdges = new TIntIntHashMap();
		for(Edge e : graph.getEdges()) {
			SampledVertex v_i = (SampledVertex)e.getVertices().getFirst();
			SampledVertex v_j = (SampledVertex)e.getVertices().getSecond();
			if(v_i.isSampled() && v_j.isSampled()) {
				int it = Math.max(v_i.getIterationSampled(), v_j.getIterationSampled());
				sampledEdges.adjustOrPutValue(it, 1, 1);
			}
			int it = Integer.MAX_VALUE;
			if(v_i.isSampled())
				it = Math.min(it, v_i.getIterationSampled());
			if(v_j.isSampled())
				it = Math.min(it, v_j.getIterationSampled());
			detectedEdges.adjustOrPutValue(it, 1, 1);
		}
		
		int detectedTotal = 0;
		TIntIntIterator it = detected.iterator();
		for(int i = 0; i < detected.size(); i++) {
			it.advance();
			detectedTotal += it.value();
		}
		
		int sampledTotal = 0;
		it = sampled.iterator();
		for(int i = 0; i < sampled.size(); i++) {
			it.advance();
			sampledTotal += it.value();
		}

		int detectedEdgesTotal = 0;
		it = detectedEdges.iterator();
		for(int i = 0; i < detectedEdges.size(); i++) {
			it.advance();
			detectedEdgesTotal += it.value();
		}
		
		int sampledEdgesTotal = 0;
		it = sampledEdges.iterator();
		for(int i = 0; i < sampledEdges.size(); i++) {
			it.advance();
			sampledEdgesTotal += it.value();
		}
		
		stats.put(NUM_DETECTED, new Double(detectedTotal));
		stats.put(NUM_SAMPLED, new Double(sampledTotal));
		stats.put("detectedEdges", new Double(detectedEdgesTotal));
		stats.put("sampledEdges", new Double(sampledEdgesTotal));
		
		
		logger.info(String.format("%1$s vertices sampled, %2$s vertices detected.", sampledTotal, detectedTotal));
		
		if(getOutputDirectory() != null) {
			try {
				write(sampled, getOutputDirectory() + "/sampled.txt");
				write(detected, getOutputDirectory() + "/detected.txt");
				write(detectedEdges, getOutputDirectory() + "/detected_edges.txt");
				write(sampledEdges, getOutputDirectory() + "/sampled_edges.txt");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void write(TIntIntHashMap map, String filename) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
		writer.write("iteration\tcount");
		writer.newLine();
		
		int[] keys = map.keys();
		Arrays.sort(keys);
		for(int i = 0; i < keys.length; i++) {
			writer.write(String.valueOf(keys[i]));
			writer.write("\t");
			writer.write(String.valueOf(map.get(keys[i])));
			writer.newLine();
		}
		writer.close();
	}
}
