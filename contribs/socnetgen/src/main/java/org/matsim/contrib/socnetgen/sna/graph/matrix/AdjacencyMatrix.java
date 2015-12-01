/* *********************************************************************** *
 * project: org.matsim.*
 * AdjacencyMatrix.java
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

/**
 * 
 */
package org.matsim.contrib.socnetgen.sna.graph.matrix;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.matsim.contrib.socnetgen.sna.graph.Edge;
import org.matsim.contrib.socnetgen.sna.graph.Graph;
import org.matsim.contrib.socnetgen.sna.graph.GraphBuilder;
import org.matsim.contrib.socnetgen.sna.graph.Vertex;
import org.matsim.core.utils.collections.Tuple;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Matrix-representation of an undirected and unweighted graph.
 * 
 * @author illenberger
 * 
 */
public class AdjacencyMatrix<V extends Vertex> {

	private ArrayList<TIntArrayList> rows;

	private ArrayList<TIntIntHashMap> commonNeighbors;

	private List<V> vertices;

	private TObjectIntHashMap<Vertex> vertexIndicies;

	/**
	 * Creates a new empty adjacency matrix.
	 */
	public AdjacencyMatrix() {
		this(false);
	}

	/**
	 * Creates a new empty adjacency matrix.
	 * 
	 * @param trackCommonNeighbors
	 *            Indicates if this matrix caches common neighbors of vertices
	 *            during insertion and removal of edges. The method
	 *            {@link #getCommonNeighbors(int, int)} requires this switch to
	 *            be set to <tt>true</tt>.
	 */
	public AdjacencyMatrix(boolean trackCommonNeighbors) {
		rows = new ArrayList<TIntArrayList>(1000);
		if (trackCommonNeighbors)
			commonNeighbors = new ArrayList<TIntIntHashMap>(1000);
	}

	/**
	 * Creates a new adjacency matrix out of graph <tt>g</tt>.
	 * 
	 * @param g
	 *            a graph
	 */
	public AdjacencyMatrix(Graph g) {
		this(g, false);
	}

	/**
	 * Creates a new adjacency matrix out of graph <tt>g</tt>.
	 * 
	 * @param g
	 *            a graph
	 * @param trackCommonNeighbors
	 *            Indicates if this matrix caches common neighbors of vertices
	 *            during insertion and removal of edges. The method
	 *            {@link #getCommonNeighbors(int, int)} requires this switch to
	 *            be set to <tt>true</tt>.
	 */
	@SuppressWarnings("unchecked")
	public AdjacencyMatrix(Graph g, boolean trackCommonNeighbors) {
		rows = new ArrayList<TIntArrayList>(g.getVertices().size());
		if (trackCommonNeighbors)
			commonNeighbors = new ArrayList<TIntIntHashMap>(g.getVertices().size());

		vertices = new ArrayList<V>(g.getVertices().size());
		/*
		 * create vertices
		 */
		vertexIndicies = new TObjectIntHashMap<Vertex>();
		int idx = 0;
		for (Vertex v : g.getVertices()) {
			vertexIndicies.put(v, idx);
			vertices.add((V) v);
			addVertex();
			idx++;
		}
		/*
		 * create edges
		 */
		for (Edge e : g.getEdges()) {
			Tuple<? extends Vertex, ? extends Vertex> p = e.getVertices();
			int i = -1;
			if (vertexIndicies.contains(p.getFirst()))
				i = vertexIndicies.get(p.getFirst());
			int j = -1;
			if (vertexIndicies.contains(p.getSecond()))
				j = vertexIndicies.get(p.getSecond());
			if (i > -1 && j > -1) {
				addEdge(i, j);
			} else {
				throw new IllegalArgumentException(String.format("Indices i=%1$s, j=%2$s not allowed!", i, j));
			}
		}
	}

	/**
	 * Returns the vertex object associated with index <tt>i</tt>.
	 * 
	 * @param i
	 *            a vertex index.
	 * @return the vertex object associated with index <tt>i</tt> or
	 *         <tt>null</tt> if there is no vertex with index <tt>i</tt>.
	 */
	public V getVertex(int i) {
		if (i >= vertices.size())
			return null;
		else
			return vertices.get(i);
	}

	/**
	 * If this matrix has been created from a graph, returns the index
	 * associated with <tt>vertex</tt>.
	 * 
	 * @param vertex
	 *            a vertex
	 * @return the index associated with <tt>vertex</tt>.
	 */
	public int getIndex(V vertex) {
		if (vertexIndicies.contains(vertex))
			return vertexIndicies.get(vertex);
		else
			return -1;
	}

	/**
	 * Creates a new graph out of this adjacency matrix.
	 * 
	 * @param <G>
	 *            the type parameter of the graph.
	 * @param <E>
	 *            the type parameter of the edges.
	 * @param builder
	 *            a graph builder used to create the graph.
	 * @return a new graph out of this adjacency matrix.
	 */
	public <G extends Graph, E extends Edge> G getGraph(GraphBuilder<G, V, E> builder) {
		G g = builder.createGraph();
		/*
		 * create vertices
		 */
		List<V> vertexList = new ArrayList<V>(getVertexCount());
		for (int i = 0; i < getVertexCount(); i++) {
			V v = builder.addVertex(g);
			if (v != null)
				vertexList.add(v);
			else
				throw new RuntimeException("Cannot add vertices to the graph.");

		}
		/*
		 * add edges
		 */
		addEdges(g, builder, vertexList);

		return g;
	}

	/**
	 * Synchronizes the edges of an existing graph with the same set of vertices
	 * with the edges of this matrix. This method will fail if either the number
	 * of vertices of this matrix or the set of vertices of the graph has
	 * changed since the construction of this matrix.
	 * 
	 * @param <G>
	 *            the type parameter of the graph.
	 * @param <E>
	 *            the type parameter of the edges.
	 * @param g
	 *            a graph with the same set of vertices.
	 * @param builder
	 *            a graph builder to add and remove edges to the graph.
	 */
	@SuppressWarnings("unchecked")
	public <G extends Graph, E extends Edge> void synchronizeEdges(G g, GraphBuilder<G, V, E> builder) {
		if (g.getVertices().size() == getVertexCount()) {
			/*
			 * remove all edges
			 */
			Set<Edge> edges = new HashSet<Edge>(g.getEdges());
			for (Edge edge : edges)
				builder.removeEdge((G) g, (E) edge);
			/*
			 * add edges
			 */
			addEdges(g, builder, vertices);
		} else {
			throw new UnsupportedOperationException("The size of the graph or matrix has changed.");
		}

	}

	private <G extends Graph, E extends Edge> void addEdges(G g, GraphBuilder<G, V, E> builder, List<V> vertexList) {
		for (int i = 0; i < rows.size(); i++) {
			TIntArrayList row = rows.get(i);
			for (int idx = 0; idx < row.size(); idx++) {
				int j = row.get(idx);
				if (j > i) {
					if (builder.addEdge(g, vertexList.get(i), vertexList.get(j)) == null)
						throw new UnsupportedOperationException(
								"There seems to be some inconsistency in the vertex set.");
				}
			}
		}
	}

	/**
	 * Returns the size of the matrix.
	 * 
	 * @return the size of the matrix.
	 */
	public int getVertexCount() {
		return rows.size();
	}

	/**
	 * Returns the number of edges.
	 * 
	 * @return the number of edges.
	 */
	public int countEdges() {
		int edges = 0;
		for (TIntArrayList row : rows) {
			edges += row.size();
		}
		return edges / 2;
	}

	/**
	 * Returns if vertex <tt>i</tt> and <tt>j</tt> are connected.
	 * 
	 * @param i
	 *            a vertex index.
	 * @param j
	 *            a vertex index.
	 * @return <tt>true</tt> if vertex <tt>i</tt> and <tt>j</tt> are connected,
	 *         <tt>false</tt> otherwise.
	 */
	public boolean getEdge(int i, int j) {
		return rows.get(i).contains(j);
	}

	/**
	 * Adds a new vertex to the matrix and returns its index.
	 * 
	 * @return the index of the new vertex.
	 */
	public int addVertex() {
		rows.add(new TIntArrayList(10));
		if (commonNeighbors != null)
			commonNeighbors.add(new TIntIntHashMap());

		return rows.size() - 1;
	}

	/**
	 * Connects vertex <tt>i</tt> and <tt>j</tt> with an edge.
	 * 
	 * @param i
	 *            a vertex index.
	 * @param j
	 *            a vertex index.
	 */
	public void addEdge(int i, int j) {
		if (i != j) {
			addEdgeInternal(i, j);
			addEdgeInternal(j, i);
		} else {
			throw new UnsupportedOperationException("Self-loops are not allowed.");
		}
	}

	private void addEdgeInternal(int i, int j) {
		TIntArrayList neighbors_i = rows.get(i);

		if (commonNeighbors != null) {
			TIntArrayList neighbors_j = rows.get(j);

			for (int k = 0; k < neighbors_j.size(); k++) {
				int neighbor = neighbors_j.get(k);
				if (neighbor != i) {
					adjustCommonNeighbors(i, neighbor, 1);
					adjustCommonNeighbors(neighbor, i, 1);
				}
			}
		}

		neighbors_i.add(j);
	}

	/**
	 * Removes the edge between <tt>i</tt> and <tt>j</tt>.
	 * 
	 * @param i
	 *            a vertex index.
	 * @param j
	 *            a vertex index.
	 */
	public void removeEdge(int i, int j) {
		removeEdgeInternal(i, j);
		removeEdgeInternal(j, i);
	}

	private void removeEdgeInternal(int i, int j) {
		TIntArrayList neighbors_i = rows.get(i);

		if (commonNeighbors != null) {
			TIntArrayList neighbors_j = rows.get(j);

			for (int k = 0; k < neighbors_j.size(); k++) {
				int neighbor = neighbors_j.get(k);
				if (neighbor != i) {
					adjustCommonNeighbors(i, neighbor, -1);
					adjustCommonNeighbors(neighbor, i, -1);
				}
			}
		}

		neighbors_i.removeAt(neighbors_i.indexOf(j));
	}

	/**
	 * Returns the number of vertices that are adjacent to <tt>i</tt> and
	 * <tt>j</tt>. The method requires the <tt>trackCommonNeighbors</tt>-switch
	 * to be set to <tt>true</tt> during instantiation of this matrix.
	 * 
	 * @see {@link AdjacencyMatrix#AdjacencyMatrix(boolean)}
	 * @see {@link AdjacencyMatrix#AdjacencyMatrix(Graph, boolean)}
	 * @param i
	 *            a vertex index.
	 * @param j
	 *            a vertex index.
	 * @return the number of vertices that are adjacent to <tt>i</tt> and
	 *         <tt>j</tt>.
	 */
	public int getCommonNeighbors(int i, int j) {
		if (commonNeighbors != null)
			return commonNeighbors.get(i).get(j);
		else
			throw new java.lang.UnsupportedOperationException("This matrix does not support counting common neighbors.");
	}

	private void adjustCommonNeighbors(int i, int j, int amount) {
		TIntIntHashMap neighbours_i = commonNeighbors.get(i);
		int n = neighbours_i.adjustOrPutValue(j, amount, amount);
		if (n < 1)
			neighbours_i.remove(j);
	}

	/**
	 * Returns the number of neighbors of vertex <tt>i</tt>.
	 * 
	 * @param i
	 *            a vertex index.
	 * @return the number of neighbors of vertex <tt>i</tt>.
	 */
	public int getNeighborCount(int i) {
		return rows.get(i).size();
	}

	/**
	 * Returns the indices of the neighbors of vertex <tt>i</tt>.
	 * 
	 * @param i
	 *            a vertex index.
	 * @return the indices of the neighbors of vertex <tt>i</tt>.
	 */
	public TIntArrayList getNeighbours(int i) {
		return rows.get(i);
	}

	/**
	 * Counts the number of triangles connected to vertex <tt>i</tt>.
	 * 
	 * @param i
	 *            a vertex index.
	 * @return the number of triangles connected to vertex <tt>i</tt>.
	 */
	public int countTriangles(int i) {
		int triangles = 0;
		TIntArrayList row = rows.get(i);
		for (int u = 0; u < row.size(); u++) {
			for (int v = u + 1; v < row.size(); v++) {
				if (getEdge(row.get(u), row.get(v)))
					triangles++;
			}
		}
		return triangles;
	}

	/**
	 * Counts the number of triples centered at vertex <tt>i</tt>.
	 * 
	 * @param i
	 *            a vertex index.
	 * @return the number of triples centered at vertex <tt>i</tt>.
	 */
	public int countTriples(int i) {
		int n = rows.get(i).size();
		return (int) (n * (n - 1) / 2.0);
	}
}
