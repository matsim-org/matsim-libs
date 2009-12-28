/* *********************************************************************** *
 * project: org.matsim.*
 * GraphProjectionBuilder.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.graph;

import org.matsim.contrib.sna.graph.AbstractSparseGraphBuilder;
import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;

/**
 * @author illenberger
 *
 */
public class GraphProjectionBuilder<G2 extends Graph, V2 extends Vertex, E2 extends Edge,
									G extends GraphProjection<G2, V2, E2>, V extends VertexDecorator<V2>, E extends EdgeDecorator<E2>>
									extends AbstractSparseGraphBuilder<G, V, E> {

	private GraphProjectionFactory<G2, V2, E2, G, V, E> factory;
	
	public GraphProjectionBuilder() {
		super(null);
	}
	
	public GraphProjectionBuilder(GraphProjectionFactory<G2, V2, E2, G, V, E> factory) {
		super(null);
		this.factory = factory;
	}

	@Override
	public E addEdge(G graph, V vI, V vJ) {
		throw new UnsupportedOperationException();
	}
	
	public E addEdge(G graph, V v_i, V v_j, E2 delegate) {
		E edge = factory.createEdge(delegate);
		if(insertEdge(graph, v_i, v_j, edge))
			return edge;
		else
			return null;
	}

	@Override
	public V addVertex(G graph) {
		throw new UnsupportedOperationException();
	}

	public V addVertex(G graph, V2 delegate) {
		V vertex = factory.createVertex(delegate);
		if(insertVertex(graph, vertex))
			return vertex;
		else
			return null;
	}
	
	@Override
	public G createGraph() {
		throw new UnsupportedOperationException();
	}
	
	public G createGraph(G2 delegate) {
		return factory.createGraph(delegate);
	}

	public G decorateGraph(G2 delegate) {
		G projection = createGraph(delegate);
		
		for(Vertex v : delegate.getVertices()) {
			V v2 = this.addVertex(projection, (V2) v);
			projection.setMapping((V2)v, v2);
		}
		
		for(Edge e : delegate.getEdges()) {
			V v_i = (V) projection.getVertex((V2) e.getVertices().getFirst());
			V v_j = (V) projection.getVertex((V2) e.getVertices().getSecond());
			this.addEdge(projection, v_i, v_j, (E2) e);
		}
		
		return projection;
	}
}
