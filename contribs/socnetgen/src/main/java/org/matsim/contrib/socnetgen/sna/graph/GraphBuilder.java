/* *********************************************************************** *
 * project: org.matsim.*
 * GraphBuilder.java
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
package org.matsim.contrib.socnetgen.sna.graph;


/**
 * A GraphBuilder allows to create and modify graphs. It ensures that the
 * connectivity of vertices and edges conforms to the definition of the
 * underlying graph implementation.
 * 
 * @author illenberger
 * 
 */
public interface GraphBuilder <G extends Graph, V extends Vertex, E extends Edge> {

	/**
	 * Creates and returns an empty graph.
	 * 
	 * @return an empty graph.
	 */
	public G createGraph();

	/**
	 * Creates and adds a new vertex to a graph.
	 * 
	 * @param graph
	 *            the graph a new vertex should be inserted in.
	 * @return the newly inserted vertex, or <tt>null</tt> is the insertion of a
	 *         vertex would violate the definition of the underlying graph.
	 */
	public V addVertex(G graph);

	/**
	 * Creates and inserts a new edge into the graph and assures the correct
	 * connectivity between vertices and edges.
	 * 
	 * @param graph
	 *            the graph a new edge should be inserted in.
	 * @param vertex1
	 *            ,vertex2 the vertices between which the edge should be
	 *            inserted.
	 * @return the newly inserted edge, or <tt>null</tt> if the insertion of an
	 *         edge at the specified position would violate the definition of
	 *         the underlying graph.
	 */
	public E addEdge(G graph, V vertex1, V vertex2);
	
	/**
	 * Removes a vertex from a graph.
	 * 
	 * @param graph
	 *            the graph the vertex should be removed from.
	 * @param vertex
	 *            the vertex to be removed.
	 * @return <tt>true</tt> if the vertex has been removed, or <tt>false</tt>
	 *         if the vertex is not part of graph <tt>graph</tt> or the vertex
	 *         is not isolated.
	 */
	public boolean removeVertex(G graph, V vertex);

	/**
	 * Removes an edge from a graph and assures the correct release of
	 * connections between vertices and edges.
	 * 
	 * @param graph
	 *            the graph the edge should be removed from.
	 * @param edge
	 *            the edge to be removed.
	 * @return <tt>true</tt> if the edge has been removed, or <tt>false</tt> if
	 *         the edge is not part of graph <tt>graph</tt> or if the removal
	 *         would violate the definition of the underlying graph.
	 */
	public boolean removeEdge(G graph, E edge);
	
	public G copyGraph(G graph);
	
}
