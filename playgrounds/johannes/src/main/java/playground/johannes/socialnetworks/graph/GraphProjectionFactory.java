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
package playground.johannes.socialnetworks.graph;

import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;

/**
 * A graph projection factory is responsible for instantiating new graph projections, vertex decorators and
 * edge decorators. It does not handle the connectivity of vertices and edges.
 * 
 * @author illenberger
 *
 */
public interface GraphProjectionFactory<G2 extends Graph, V2 extends Vertex, E2 extends Edge,
										G extends GraphProjection<G2, V2, E2>, V extends VertexDecorator<V2>, E extends EdgeDecorator<E2>> {

	/**
	 * Creates and returns an empty graph projection on <tt>delegate</tt>.
	 * 
	 * @param delegate the original graph.
	 * 
	 * @return an empty graph projection.
	 */
	public G createGraph(G2 delegate);
	
	/**
	 * Creates and returns an isolated vertex decorator that decorates <tt>delegate</tt>.
	 * 
	 * @param delegate the original vertex.
	 * 
	 * @return an isolated vertex decorator.
	 */
	public V createVertex(V2 delegate);
	
	/**
	 * Creates and returns an orphaned edge decorator that decorates <tt>delegate</tt>.
	 * 
	 * @param delegate the original edge.
	 * 
	 * @return an orphaned edge decorator.
	 */
	public E createEdge(E2 delegate);
	
}
