/* *********************************************************************** *
 * project: org.matsim.*
 * NSensitivity.java
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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.GraphBuilder;
import org.matsim.contrib.sna.graph.SparseEdge;
import org.matsim.contrib.sna.graph.SparseGraph;
import org.matsim.contrib.sna.graph.SparseGraphBuilder;
import org.matsim.contrib.sna.graph.SparseVertex;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.Degree;
import org.matsim.contrib.sna.graph.analysis.FixedSizeRandomPartition;
import org.matsim.contrib.sna.graph.analysis.VertexFilter;
import org.matsim.contrib.sna.graph.generators.ErdosRenyiGenerator;
import org.matsim.contrib.sna.snowball.SampledGraph;
import org.matsim.contrib.sna.snowball.SampledVertexDecorator;
import org.matsim.contrib.sna.snowball.analysis.PiEstimator;
import org.matsim.contrib.sna.snowball.analysis.SimplePiEstimator;
import org.matsim.contrib.sna.snowball.sim.Sampler;
import org.matsim.contrib.sna.snowball.sim.SamplerListener;

import playground.johannes.socialnetworks.snowball2.analysis.WSMStatsFactory;
import playground.johannes.socialnetworks.snowball2.sim.deprecated.EstimatedDegree2;
import playground.johannes.socialnetworks.snowball2.sim.deprecated.NormalizedEstimator;

/**
 * @author illenberger
 *
 */
public class NSensitivity {

	private static final int MIN_SIZE = 3000;
	
	private static final int MAX_SIZE = 30000;
	
	private static final int STEP_SIZE = 3000;
	
	private static final float PHI_MIN = 0.5f;
	
	private static final float PHI_MAX = 1.5f;
	
	private static final float PHI_STEP = 0.1f;
	
	private static final long RND_SEED = 4711;
	
	private static final int SEED_SIZE = 10;
	
	private static final int MAX_SAMPLE_SIZE = 500;
	
	private static final int ENSEMBLE_SIZE = 100;
	
	private static final double k = 10;
	
	public static void main(String[] args) throws IOException {
		NSensitivity ntest = new NSensitivity();
		double[][] kMatrix = ntest.run();
		ntest.write(kMatrix, args[0]);
	}
	
	private double[][] run() {
		int rows = (MAX_SIZE - MIN_SIZE)/STEP_SIZE + 1;
		int cols = Math.round(((PHI_MAX - PHI_MIN)/PHI_STEP)) + 1;
		
		double[][] kMatrix = new double[rows][cols];
		
		for(int size = MIN_SIZE; size <= MAX_SIZE; size += STEP_SIZE) {
			int rowIdx = (size - MIN_SIZE)/STEP_SIZE;
			
			Graph graph = createGraph(size);
			VertexFilter<Vertex> seedGen = new FixedSizeRandomPartition<Vertex>(SEED_SIZE, RND_SEED);
			
			for(float phi = PHI_MIN; phi <= PHI_MAX+0.0001; phi += PHI_STEP) {
				phi = (float) (Math.round(phi * 100)/100.0);
				int colIdx = Math.round(((phi - PHI_MIN)/PHI_STEP));
				
				int approxSize = (int) (phi * graph.getVertices().size());
				Listener listener = new Listener(approxSize);
				
				double k_mean = 0;
				for (int i = 0; i < ENSEMBLE_SIZE; i++) {
					Sampler<Graph, Vertex, Edge> sampler = new Sampler<Graph, Vertex, Edge>();
					sampler.setSeedGenerator(seedGen);
					sampler.setListener(listener);
					sampler.run(graph);
					k_mean += listener.getKMean();
				}
				k_mean = k_mean/(double)ENSEMBLE_SIZE;
				if(k_mean == 0) {
					System.err.println("k_mean = 0");
					System.exit(-1);
				}
				System.err.println("row = " + rowIdx + ", col = " + colIdx);
				kMatrix[rowIdx][colIdx] = k_mean;
			}
		}
		
		return kMatrix;
	}
	
	private Graph createGraph(int size) {
		GraphBuilder<SparseGraph, SparseVertex, SparseEdge> builder = new SparseGraphBuilder();
		ErdosRenyiGenerator<SparseGraph, SparseVertex, SparseEdge> generator = new ErdosRenyiGenerator<SparseGraph, SparseVertex, SparseEdge>(builder);
		generator.setRandomDrawMode(true);
		
		double p = k/(double)(size - 1);
			
		return generator.generate(size, p, RND_SEED);
	}
	
	private void write(double[][] kMatrix, String file) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		
		int rows = (MAX_SIZE - MIN_SIZE)/STEP_SIZE + 1;
		int cols = Math.round(((PHI_MAX - PHI_MIN)/PHI_STEP)) + 1;
		
		for(int col = 0; col < cols; col++) {
			writer.write("\t");
			writer.write(String.valueOf(col * PHI_STEP + PHI_MIN));
		}
		writer.newLine();
		
		for(int row = 0; row < rows; row++) {
			writer.write(String.valueOf(row * STEP_SIZE + MIN_SIZE));
			for(int col = 0; col < cols; col++) {
				writer.write("\t");
				writer.write(String.valueOf(kMatrix[row][col]));
			}
			writer.newLine();
		}
		writer.close();
	}
	
	private static class Listener implements SamplerListener {
		
		private final Degree degree;
		
		private double k_mean;
		
		private PiEstimator estim1Norm;
		
		public Listener(int approxSize) {
			PiEstimator estim1 = new SimplePiEstimator(approxSize);
			estim1Norm = new NormalizedEstimator(estim1, approxSize);
			
			degree = new EstimatedDegree2(estim1Norm, new WSMStatsFactory()); 
		}
		
		@Override
		public boolean afterSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {
			if(sampler.getNumSampledVertices() >= MAX_SAMPLE_SIZE) {
				analyze(sampler.getSampledGraph());
				return false;
			} else
				return true;
		}

		@Override
		public boolean beforeSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {
			return true;
		}

		@Override
		public void endSampling(Sampler<?, ?, ?> sampler) {
	
		}
		
		private void analyze(SampledGraph graph) {
			estim1Norm.update(graph);
			k_mean = degree.distribution(graph.getVertices()).getMean();
			
		}
		
		public double getKMean() {
			return k_mean;
		}
	}
}
