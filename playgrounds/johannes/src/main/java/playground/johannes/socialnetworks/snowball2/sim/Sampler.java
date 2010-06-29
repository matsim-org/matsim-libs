/* *********************************************************************** *
 * project: org.matsim.*
 * Sampler.java
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.EdgeDecorator;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.GraphProjection;
import org.matsim.contrib.sna.graph.GraphProjectionBuilder;
import org.matsim.contrib.sna.graph.GraphProjectionFactory;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.VertexDecorator;
import org.matsim.core.utils.collections.Tuple;

import playground.johannes.socialnetworks.snowball2.SampledEdgeDecorator;
import playground.johannes.socialnetworks.snowball2.SampledGraphProjection;
import playground.johannes.socialnetworks.snowball2.SampledGraphProjectionBuilder;
import playground.johannes.socialnetworks.snowball2.SampledVertexDecorator;

/**
 * @author illenberger
 *
 */
public class Sampler<G extends Graph, V extends Vertex, E extends Edge> {
	
	private static final Logger logger = Logger.getLogger(Sampler.class);
	
	private TaggedGraph taggedGraph;
	
	private SampledGraphProjection<G, V, E> sampledGraph;
	
	private SampledGraphProjectionBuilder<G, V, E> builder;
	
	private VertexPartition seedGenerator;
	
	private VertexPartition responseGenerator;
	
	private SamplerListener listener;
	
	private int iteration;
	
	private int numSampledVertices;
	
	private int numExpanded;

	public void setSeedGenerator(VertexPartition generator) {
		this.seedGenerator = generator;
	}
	
	public void setResponseGenerator(VertexPartition generator) {
		this.responseGenerator = generator;
	}
	
	public void setListener(SamplerListener listener) {
		this.listener = listener;
	}
	
	public int getIteration() {
		return iteration;
	}
	
	public int getNumSampledVertices() {
		return numSampledVertices;
	}
	
	public int getNumExpandedVertices() {
		return numExpanded;
	}
	
	public SampledGraphProjection<G, V, E> getSampledGraph() {
		return sampledGraph;
	}
	
	public void run(G graph) {
		Set<TaggedVertex> recruits = init(graph);
		/*
		 * loop until no recruits are available
		 */
		while(!recruits.isEmpty()) {
			logger.info(String.format("Sampling iteration %1$s.", iteration));
			
			numExpanded = 0;
			Set<TaggedVertex> newRecruits = new HashSet<TaggedVertex>();
			for(TaggedVertex vertex : recruits) {
				if(!listener.beforeSampling(this, vertex.getProjection()))
					return;
				/*
				 * check if the vertex is responsive
				 */
				if(vertex.isResponsive()) {
					/*
					 * expand and collect new recruits
					 */
					expand(vertex, newRecruits);
					numExpanded++;
				}
				/*
				 * notify listener and terminate if applicable
				 */
				if(!listener.afterSampling(this, vertex.getProjection()))
					return;
			}
			recruits = newRecruits;
			iteration++;
		}
		
		listener.endSampling(this);
		
		logger.info("Done.");
	}
	
	@SuppressWarnings("unchecked")
	private Set<TaggedVertex> init(G graph) {
		iteration = 0;
		numSampledVertices = 0;
		/*
		 * 
		 */
		if(listener == null)
			listener = new DefaultListener();
		/*
		 * create an internally tagged graph for the sampler 
		 */
		TaggedGraphBuilder tBuilder = new TaggedGraphBuilder();
		taggedGraph = tBuilder.decorateGraph(graph);
		/*
		 * create a sampled graph
		 */
		builder = new SampledGraphProjectionBuilder<G, V, E>();
		sampledGraph = builder.createGraph(graph);
		/*
		 * tag the responsive vertices
		 */
		if(responseGenerator == null)
			responseGenerator = new AllResponding();
		
		Set<V> responsive = (Set<V>) responseGenerator.partition(graph.getVertices());
		for(V vertex : responsive) {
			taggedGraph.getVertex(vertex).setResponsive(true);
		}
		/*
		 * draw the seed vertices
		 */
		Set<V> seeds = (Set<V>) seedGenerator.partition(responsive);
		Set<TaggedVertex> taggedSeeds = new HashSet<TaggedVertex>();
		for(V vertex : seeds) {
			TaggedVertex taggedVertex = taggedGraph.getVertex(vertex);
			SampledVertexDecorator<V> sampledVertex = builder.addVertex(sampledGraph, vertex);
			sampledVertex.detect(-1);
			sampledVertex.setSeed(sampledVertex);
			taggedVertex.setProjection(sampledVertex);
			taggedSeeds.add(taggedVertex);
			
		}
		
		return taggedSeeds;
	}
	
	private void expand(TaggedVertex vertex, Set<TaggedVertex> recruits) {
		/*
		 * sample vertex
		 */
		vertex.getProjection().sample(iteration);
		numSampledVertices++;
		/*
		 * expand to neighbors
		 */
		for(TaggedEdge edge : vertex.getEdges()) {
			if(edge.getProjection() == null) {
				/*
				 * edge has not been sampled
				 */
				TaggedVertex neighbour = edge.getOpposite(vertex);
				SampledVertexDecorator<V> sampledNeighbour;
				if(neighbour.getProjection() == null) {
					/*
					 * neighbor has not been sampled
					 */
					sampledNeighbour = builder.addVertex(sampledGraph, neighbour.getDelegate());
					sampledNeighbour.detect(iteration);
					sampledNeighbour.setSeed(vertex.getProjection().getSeed());
					neighbour.setProjection(sampledNeighbour);
					/*
					 * recruit
					 */
					recruits.add(neighbour);
				} else {
					sampledNeighbour = neighbour.getProjection();
				}
				/*
				 * sample edge
				 */
				SampledEdgeDecorator<E> sampledEdge = builder.addEdge(sampledGraph, vertex.getProjection(), sampledNeighbour, edge.getDelegate());
				edge.setProjection(sampledEdge);
			}
		}
	}
	
	private class TaggedGraph extends GraphProjection<G, V, E> {

		public TaggedGraph(G delegate) {
			super(delegate);
		}

		@SuppressWarnings("unchecked")
		@Override
		public Set<? extends TaggedEdge> getEdges() {
			return (Set<? extends TaggedEdge>) super.getEdges();
		}

		@SuppressWarnings("unchecked")
		@Override
		public Set<? extends TaggedVertex> getVertices() {
			return (Set<? extends TaggedVertex>) super.getVertices();
		}

		@Override
		public TaggedVertex getVertex(V v) {
			return (TaggedVertex) super.getVertex(v);
		}
		
	}
	
	private class TaggedVertex extends VertexDecorator<V> {

		private SampledVertexDecorator<V> projection;
		
		private boolean responsive;
		
		protected TaggedVertex(V delegate) {
			super(delegate);
		}
		
		public void setProjection(SampledVertexDecorator<V> projection) {
			this.projection = projection;
		}
		
		public SampledVertexDecorator<V> getProjection() {
			return projection;
		}
		
		public void setResponsive(boolean responive) {
			this.responsive = responive;
		}
		
		public boolean isResponsive() {
			return responsive;
		}

		@SuppressWarnings("unchecked")
		@Override
		public List<? extends TaggedEdge> getEdges() {
			return (List<? extends TaggedEdge>) super.getEdges();
		}

		@SuppressWarnings("unchecked")
		@Override
		public List<? extends TaggedVertex> getNeighbours() {
			return (List<? extends TaggedVertex>) super.getNeighbours();
		}
		
	}
	
	private class TaggedEdge extends EdgeDecorator<E> {

		private SampledEdgeDecorator<E> projection;
		
		protected TaggedEdge(E delegate) {
			super(delegate);
		}
		
		public void setProjection(SampledEdgeDecorator<E> projection) {
			this.projection = projection;
		}
		
		public SampledEdgeDecorator<E> getProjection() {
			return projection;
		}

		@Override
		public TaggedVertex getOpposite(Vertex v) {
			return (TaggedVertex) super.getOpposite(v);
		}

		@SuppressWarnings("unchecked")
		@Override
		public Tuple<? extends TaggedVertex, ? extends TaggedVertex> getVertices() {
			return (Tuple<? extends TaggedVertex, ? extends TaggedVertex>) super.getVertices();
		}
	}
	
	private class TaggedGraphFactory implements GraphProjectionFactory<G, V, E, TaggedGraph, TaggedVertex, TaggedEdge> {

		@Override
		public TaggedEdge createEdge(E delegate) {
			return new TaggedEdge(delegate);
		}

		@Override
		public TaggedGraph createGraph(G delegate) {
			return new TaggedGraph(delegate);
		}

		@Override
		public TaggedVertex createVertex(V delegate) {
			return new TaggedVertex(delegate);
		}
		
	}
	
	private class TaggedGraphBuilder extends GraphProjectionBuilder<G, V, E, TaggedGraph, TaggedVertex, TaggedEdge> {
		
		public TaggedGraphBuilder() {
			super(new TaggedGraphFactory());
		}
		
	}
	
	private static class AllResponding implements VertexPartition {

		@Override
		public <V extends Vertex> Set<V> partition(Set<V> vertices) {
			return vertices;
		}
		
	}
	
	private static class DefaultListener implements SamplerListener {

		@Override
		public boolean afterSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {
			return true;
		}

		@Override
		public boolean beforeSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {
			return true;
		}

		@Override
		public void endSampling(Sampler<?, ?, ?> sampler) {
		}
		
	}
}
