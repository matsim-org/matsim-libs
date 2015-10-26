/* *********************************************************************** *
 * project: org.matsim.*
 * EgoCentricSampler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.johannes.studies.smallworld;

import org.matsim.contrib.socnetgen.sna.graph.Edge;
import org.matsim.contrib.socnetgen.sna.graph.Graph;
import org.matsim.contrib.socnetgen.sna.graph.Vertex;
import org.matsim.contrib.socnetgen.sna.snowball.SampledGraphProjection;
import org.matsim.contrib.socnetgen.sna.snowball.SampledGraphProjectionBuilder;
import org.matsim.contrib.socnetgen.sna.snowball.SampledVertexDecorator;
import org.matsim.contrib.socnetgen.sna.snowball.sim.Sampler;
import org.matsim.contrib.socnetgen.sna.snowball.sim.SamplerListener;

import java.util.*;

/**
 * @author illenberger
 * 
 */
public class EgoCentricSampler<G extends Graph, V extends Vertex, E extends Edge>
		implements Sampler<G, V, E> {

	private SampledGraphProjection<G, V, E> sampledGraph;

	private SampledGraphProjectionBuilder<G, V, E> builder = new SampledGraphProjectionBuilder<G, V, E>();

	private SamplerListener listener;

	private int count;

	private int iteration;

	public void setListiner(SamplerListener listener) {
		this.listener = listener;
	}
	@Override
	public int getIteration() {
		return iteration;
	}

	@Override
	public int getNumSampledVertices() {
		return count;
	}

	@Override
	public SampledGraphProjection<G, V, E> getSampledGraph() {
		return sampledGraph;
	}

	public void run(G graph, double responseRate, int seeds, Random random) {
		sampledGraph = builder.createGraph(graph);
		
		List<V> vertices = new LinkedList<V>(
				(Collection<? extends V>) graph.getVertices());

		Map<V, SampledVertexDecorator<V>> vertexProjections = new HashMap<V, SampledVertexDecorator<V>>();

		Collections.shuffle(vertices, random);

		for (V i : vertices) {
			if (responseRate > random.nextDouble()) {
				SampledVertexDecorator<V> i_proj = vertexProjections.get(i);
				if (i_proj == null) {
					i_proj = builder.addVertex(sampledGraph, i);
					vertexProjections.put(i, i_proj);
				}

				if (listener.beforeSampling(this, i_proj)) {

					if (count > seeds)
						iteration = 1;

					i_proj.detect(iteration - 1);
					i_proj.sample(iteration);
					i_proj.setSeed(i_proj);
					count++;

					for (Edge e : i.getEdges()) {
						V j = (V) e.getOpposite(i);
						SampledVertexDecorator<V> j_proj = vertexProjections
								.get(j);
						if (j_proj == null) {
							j_proj = builder.addVertex(sampledGraph, j);
							j_proj.detect(iteration);
							if(j_proj.getSeed() == null) {
								j_proj.setSeed(i_proj);
							}
							vertexProjections.put(j, j_proj);
						}

						builder.addEdge(sampledGraph, i_proj, j_proj, (E) e);
					}

					if (!listener.afterSampling(this, i_proj))
						return;
				} else {
					return;
				}
			}
		}
		listener.endSampling(this);
	}
}
