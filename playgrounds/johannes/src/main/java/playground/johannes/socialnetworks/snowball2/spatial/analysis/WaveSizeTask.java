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
package playground.johannes.socialnetworks.snowball2.spatial.analysis;

import gnu.trove.TIntIntHashMap;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.snowball.SampledVertex;

import playground.johannes.socialnetworks.graph.analysis.AbstractGraphAnalyzerTask;

/**
 * @author illenberger
 *
 */
public class WaveSizeTask extends AbstractGraphAnalyzerTask {

	public WaveSizeTask(String output) {
		super(output);
	}

	@Override
	public void analyze(Graph graph, Map<String, Object> analyzers, Map<String, Double> stats) {
		if(getOutputDirectory() != null) {
			TIntIntHashMap detected = new TIntIntHashMap();
			TIntIntHashMap sampled = new TIntIntHashMap();
			for(Vertex v : graph.getVertices()) {
				sampled.adjustOrPutValue(((SampledVertex)v).getIterationSampled(), 1, 1);
				detected.adjustOrPutValue(((SampledVertex)v).getIterationDetected(), 1, 1);
			}
			
			try {
				write(sampled, getOutputDirectory() + "/sampled.txt");
				write(detected, getOutputDirectory() + "/detected.txt");
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
