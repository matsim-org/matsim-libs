/* *********************************************************************** *
 * project: org.matsim.*
 * GrowthSim.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

import java.io.IOException;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.SparseGraph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.FixedSizeRandomPartition;
import org.matsim.contrib.sna.graph.analysis.RandomPartition;
import org.matsim.contrib.sna.graph.analysis.VertexFilter;
import org.matsim.contrib.sna.graph.io.SparseGraphMLReader;
import org.matsim.contrib.sna.snowball.SampledVertexDecorator;
import org.matsim.contrib.sna.snowball.sim.Sampler;
import org.matsim.contrib.sna.snowball.sim.SamplerListener;

import playground.johannes.studies.mcmc.KeyMatrix;

/**
 * @author illenberger
 * 
 */
public class GrowthSim {

	private static final Logger logger = Logger.getLogger(GrowthSim.class);

	public static void main(String args[]) throws IOException {
		int ensembleSize = 50;

		KeyMatrix<Double> vertexMatrix = new KeyMatrix<Double>();
		KeyMatrix<Double> sampleMatrix = new KeyMatrix<Double>();

		Random random = new Random(4711);
		for (int i = 0; i < ensembleSize; i++) {

			long randomSeed = random.nextLong();

			int alphaStart = 10;
			int alphaEnd = 100;
			int alphaStep = 5;

			int seedStart = 10;
			int seedEnd = 100;
			int seedStep = 5;

			SparseGraphMLReader reader = new SparseGraphMLReader();
			SparseGraph graph = reader
					.readGraph(args[0]);

			for (double alpha = alphaStart; alpha <= alphaEnd; alpha += alphaStep) {
				for (int seed = seedStart; seed <= seedEnd; seed += seedStep) {
					logger.info(String.format("Simulating configuration alpha = %1$s, seed = %2$s.", alpha, seed));

					VertexFilter<Vertex> seedGenerator = new FixedSizeRandomPartition<Vertex>(seed, randomSeed);
					VertexFilter<Vertex> reponseGenerator = new RandomPartition<Vertex>(alpha / 100.0, randomSeed);

					Sampler<Graph, Vertex, Edge> sampler = new Sampler<Graph, Vertex, Edge>(randomSeed);
					sampler.setSeedGenerator(seedGenerator);
					sampler.setResponseGenerator(reponseGenerator);
					sampler.setListener(new Listener());

					sampler.run(graph);

					Double alphaKey = new Double(alpha / 100.0);
					Double seedKey = new Double(seed);
					/*
					 * vertices
					 */
					double vertices = 0;
					if (i > 0) {
						vertices = vertexMatrix.getValue(alphaKey, seedKey);
						vertices = vertices * i;
					}

					vertices += sampler.getSampledGraph().getVertices().size();
					vertices = vertices / (double) (i + 1);

					vertexMatrix.putValue(vertices, alphaKey, seedKey);
					/*
					 * samples
					 */
					double samples = 0;
					if (i > 0) {
						samples = sampleMatrix.getValue(alphaKey, seedKey);
						samples = samples * i;
					}

					samples += sampler.getNumSampledVertices();
					samples = samples / (double) (i + 1);

					sampleMatrix.putValue(samples, alphaKey, seedKey);
				}
			}
		}

		vertexMatrix.write(args[1] + "/vertices.txt");
		sampleMatrix.write(args[1] + "/samples.txt");

		logger.info("Done.");
	}

	private static class Listener implements SamplerListener {

		@Override
		public boolean beforeSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {
			if (sampler.getIteration() == 3) {
				return false;
			}

			return true;
		}

		@Override
		public boolean afterSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {
			return true;
		}

		@Override
		public void endSampling(Sampler<?, ?, ?> sampler) {

		}

	}
}
