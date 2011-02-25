/* *********************************************************************** *
 * project: org.matsim.*
 * SampledGraphProjFilter.java
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
package playground.johannes.socialnetworks.survey.ivt2009.analysis.deprecated;

import java.util.HashSet;
import java.util.Set;

import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.VertexFilter;
import org.matsim.contrib.sna.snowball.SampledGraphProjection;
import org.matsim.contrib.sna.snowball.SampledGraphProjectionBuilder;
import org.matsim.contrib.sna.snowball.SampledVertexDecorator;
import org.matsim.contrib.sna.snowball.sim.Sampler;
import org.matsim.contrib.sna.snowball.sim.SamplerListener;

import playground.johannes.socialnetworks.graph.analysis.GraphFilter;
import playground.johannes.socialnetworks.snowball2.sim.SampleStats;

/**
 * @author illenberger
 *
 */
public class SampledGraphProjFilter<G extends Graph, V extends Vertex, E extends Edge> implements GraphFilter<SampledGraphProjection<G, V, E>> {

	private int maxIteration;
	
	private SampledGraphProjectionBuilder<G, V, E> builder;
		
	public SampledGraphProjFilter(int iteration) {
		this.maxIteration = iteration;
	}
	
	public void setBuilder(SampledGraphProjectionBuilder<G, V, E> builder) {
		this.builder = builder;
	}
	
	@Override
	public SampledGraphProjection<G, V, E> apply(SampledGraphProjection<G, V, E> graph) {
		Sampler<G, V, E> sampler = new Sampler<G, V, E>();
		sampler.setResponseGenerator(new ResponseFilter(graph));
		sampler.setSeedGenerator(new SeedFilter(graph));
		sampler.setListener(new Listener());
		
		if(builder == null)
			builder = new SampledGraphProjectionBuilder<G, V, E>();
		sampler.setBuilder(builder);
		
		sampler.run(graph.getDelegate());
		SampleStats stats = new SampleStats(sampler.getSampledGraph());
		return sampler.getSampledGraph();
	}

	private class ResponseFilter implements VertexFilter<V> {

		private SampledGraphProjection<G, V, E> graph;
		
		public ResponseFilter(SampledGraphProjection<G, V, E> graph) {
			this.graph = graph;
		}
		
		@Override
		public Set<V> apply(Set<V> vertices) {
			Set<V> responding = new HashSet<V>();
			for(V vertex : vertices) {
				SampledVertexDecorator<V> decorator = graph.getVertex(vertex);
				if(decorator.isSampled())
					responding.add(vertex);
			}
			
			return responding;
		}
		
	}
	
	private class SeedFilter implements VertexFilter<V> {

		private SampledGraphProjection<G, V, E> graph;
		
		public SeedFilter(SampledGraphProjection<G, V, E> graph) {
			this.graph = graph;
		}
		
		@Override
		public Set<V> apply(Set<V> vertices) {
			Set<V> seeds = new HashSet<V>();
			for(V vertex : vertices) {
				SampledVertexDecorator<V> decorator = graph.getVertex(vertex);
				if(decorator.isSampled() && decorator.getIterationSampled() == 0)
					seeds.add(vertex);
			}
			
			return seeds;
		}
		
	}
	
	private class Listener implements SamplerListener {

		private int lastIteration;
		
		@Override
		public boolean afterSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {
			return true;
		}

		@Override
		public boolean beforeSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {
			if(sampler.getIteration() > lastIteration) {
				lastIteration = sampler.getIteration();
				if(lastIteration > maxIteration)
					return false;
			}
			return true;
		}

		@Override
		public void endSampling(Sampler<?, ?, ?> sampler) {
		}
		
	}
}
