/* *********************************************************************** *
 * project: org.matsim.*
 * GraphFactory2.java
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
package playground.johannes.sna.graph;


/**
 * A graph factory is responsible for instantiating new graphs, vertices and
 * edges. It does not handle the connectivity of vertices and edges.
 * 
 * @author illenberger
 * 
 */
public interface GraphFactory<G extends Graph, V extends Vertex, E extends Edge> {

	/**
	 * Creates and returns an empty graph.
	 * 
	 * @return an empty graph.
	 */
	public G createGraph();
	
	/**
	 * Creates and returns an isolated vertex.
	 * 
	 * @return an isolated vertex.
	 */
	public V createVertex();
	
	/**
	 * Creates and returns an orphaned edge.
	 * 
	 * @return an orphaned edge.
	 */
	public E createEdge();
	
	public G copyGraph(G graph);
	
	public V copyVertex(V vertex);
	
	public E copyEdge(E edge);
	
}
