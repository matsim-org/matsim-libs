/* *********************************************************************** *
 * project: org.matsim.*
 * ConnectionSampleAnalyzer.java
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

import gnu.trove.TObjectIntHashMap;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.AnalyzerTask;
import org.matsim.contrib.sna.snowball.SampledVertex;
import org.matsim.contrib.sna.snowball.SampledVertexDecorator;
import org.matsim.contrib.sna.snowball.analysis.PiEstimator;
import org.matsim.contrib.sna.snowball.sim.SampleAnalyzer;
import org.matsim.contrib.sna.snowball.sim.Sampler;


/**
 * @author illenberger
 *
 */
public class ConnectionSampleAnalyzer extends SampleAnalyzer {

	private TObjectIntHashMap<Vertex> indices = new TObjectIntHashMap<Vertex>();
	
	private int maxIndex;
	
	private boolean[][] matrix;
	
	private int count;
	
	private int maxConnections;
	
	private BufferedWriter writer;
	
	private static final Logger logger = Logger.getLogger(ConnectionSampleAnalyzer.class);
	
	public ConnectionSampleAnalyzer(int numSeeds, Map<String, AnalyzerTask> tasks, Collection<PiEstimator> estimators, String rootDirectory) {
		super(tasks, estimators, rootDirectory);
		matrix = new boolean[numSeeds][numSeeds];
		try {
			writer = new BufferedWriter(new FileWriter(getRootDirectory() + "/connects.txt"));
			writer.write("it\tn_connect");
			writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		maxConnections = (int) (numSeeds * (numSeeds - 1) / 2.0);
	}

	@Override
	public boolean afterSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {
		for(Vertex neighbor : vertex.getNeighbours()) {
			if(((SampledVertex)neighbor).getSeed() != vertex.getSeed()) {
				int i = getIndex(vertex.getSeed());
				int j = getIndex(((SampledVertex)neighbor).getSeed());
				if(!matrix[i][j]) {
					matrix[i][j] = true;
					matrix[j][i] = true;
					count++;

					if(count % 10 == 0) {
						logger.info(String.format("Detected %1$s of %2$s connections (%3$s)", count, maxConnections, count/(double)maxConnections));
					}
					
					try {
						writer.write(String.valueOf(sampler.getIteration()));
						writer.write("\t");
						writer.write(String.valueOf(count));
						writer.newLine();
						writer.flush();
					} catch (IOException e) {
						e.printStackTrace();
					}

					File file = makeDirectories(String.format("%1$s/connect.%2$s", getRootDirectory(), count));
					analyze(sampler.getSampledGraph(), file.getAbsolutePath());
				}
			}
		}
		
		
		return true;
	}

	@Override
	public boolean beforeSampling(Sampler<?, ?, ?> sampler,	SampledVertexDecorator<?> vertex) {
		return true;
	}
	
	private int getIndex(Vertex vertex) {
		int idx;
		if(indices.contains(vertex))
			idx = indices.get(vertex);
		else {
			indices.put(vertex, maxIndex);
			idx = maxIndex;
			maxIndex++;
		}
		return idx;
	}

	@Override
	public void endSampling(Sampler<?, ?, ?> sampler) {
	}

}
