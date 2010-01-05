/* *********************************************************************** *
 * project: org.matsim.*
 * GraphProjectionFactory.java
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
package org.matsim.contrib.sna.graph;


/**
 * Implementation of GraphProjectionFimport org.matsim.contrib.sna.graph.EdgeDecorator;
actory to creates instances of GraphProjectimport org.matsim.contrib.sna.graph.GraphProjection;
import org.matsim.contrib.sna.graph.GraphProjectionFactory;
ion,
 * VertexDecorator and EdgeDecorator.
 import org.matsim.contrib.sna.graph.VertexDecorator;
* 
 * @author illenberger
 *
 */
public class SparseGraphProjectionFactory<G extends Graph, V extends Vertex, E extends Edge> implements GraphProjectionFactory<G, V, E, GraphProjection<G,V,E>, VertexDecorator<V>, EdgeDecorator<E>>{

	/**
	 * Creates and returns an edge decorator that decorates <tt>delegate</tt>.
	 * 
	 * @param delegate the original edge.
	 * 
	 * @return an edge decorator.
	 */
	public EdgeDecorator<E> createEdge(E delegate) {
		return new EdgeDecorator<E>(delegate);
	}

	/**
	 * Creates and returns an empty graph projection on <tt>delegate</tt>.
	 * 
	 * @param delegate the original graph.
	 * 
	 * @return an empty graph projection.
	 */
	public GraphProjection<G, V, E> createGraph(G delegate) {
		return new GraphProjection<G, V, E>(delegate);
	}

	/**
	 * Creates and returns a vertex decorator that decorates <tt>delegate</tt>.
	 * 
	 * @param delegate the original vertex.
	 * 
	 * @return a new vertex decorator.
	 */
	public VertexDecorator<V> createVertex(V delegate) {
		return new VertexDecorator<V>(delegate);
	}

}
