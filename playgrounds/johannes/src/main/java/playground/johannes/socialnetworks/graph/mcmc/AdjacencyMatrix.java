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
package playground.johannes.socialnetworks.graph.mcmc;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectIntHashMap;

import java.util.ArrayList;

import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.GraphBuilder;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.core.utils.collections.Tuple;


/**
 * @author illenberger
 *
 */
public class AdjacencyMatrix {
	
	private ArrayList<TIntArrayList> rows;
	
	private ArrayList<TIntIntHashMap> commonNeighours;
	
	public AdjacencyMatrix() {	
		rows = new ArrayList<TIntArrayList>(1000);
		commonNeighours = new ArrayList<TIntIntHashMap>(1000);
	}
	
	public AdjacencyMatrix(Graph g) {
		rows = new ArrayList<TIntArrayList>(g.getVertices().size());
		commonNeighours = new ArrayList<TIntIntHashMap>(g.getVertices().size());
		
		TObjectIntHashMap<Vertex> vertexIndicies = new TObjectIntHashMap<Vertex>();
		
		int idx = 0;
		for(Vertex v : g.getVertices()) {
			vertexIndicies.put(v, idx);
			addVertex();
			idx++;
		}
		
		for(Edge e : g.getEdges()) {
			Tuple<? extends Vertex, ? extends Vertex> p = e.getVertices();
			int i = -1;
			if(vertexIndicies.contains(p.getFirst()))
				i = vertexIndicies.get(p.getFirst());
			int j = -1;
			if(vertexIndicies.contains(p.getSecond()))
				j = vertexIndicies.get(p.getSecond());
			if(i > -1 && j > -1) {
				addEdge(i, j);
			} else {
				throw new IllegalArgumentException(String.format("Indices i=%1$s, j=%2$s not allowed!", i, j));
			}
		}
	}
	
	public <G extends Graph, V extends Vertex, E extends Edge> G getGraph(GraphBuilder<G, V, E> builder) {
		G g = builder.createGraph();

		TIntObjectHashMap<V> vertexIdx = new TIntObjectHashMap<V>();
		for(int i = 0; i < rows.size(); i++) {
			V v = builder.addVertex(g);
			vertexIdx.put(i, v);
		}
		
		for(int i = 0; i < rows.size(); i++) {
			TIntArrayList row = rows.get(i);
			if(row != null) {
				for(int idx = 0; idx < row.size(); idx++) {
					int j = row.get(idx);
					if(j > i) {
						if(builder.addEdge(g, vertexIdx.get(i), vertexIdx.get(j)) == null)
							throw new RuntimeException();
					}
				}
			}
		}
		
		return g;
	}
	
	public int getVertexCount() {
		return rows.size();
	}
	
	public int getEdgeCount() {
		int edges = 0;
		for(TIntArrayList row : rows) {
			edges += row.size();
		}
		return edges/2;
	}
	
	public boolean getEdge(int i, int j) {		
		return rows.get(i).contains(j);
	}
	
	public int addVertex() {
		rows.add(new TIntArrayList(10));
		commonNeighours.add(new TIntIntHashMap());
		
		return rows.size() - 1;
	}
	
	public void addEdge(int i, int j) {
		addEdgeInternal(i, j);
		addEdgeInternal(j, i);
	}
	
	private void addEdgeInternal(int i, int j) {
		TIntArrayList neighbours_i = rows.get(i);
		TIntArrayList neighbours_j = rows.get(j);
		
		for(int k = 0; k < neighbours_j.size(); k++) {
			int neighbour = neighbours_j.get(k);
			if(neighbour != i) {
				adjustCommonNeighbours(i, neighbour, 1);
				adjustCommonNeighbours(neighbour, i, 1);
			}
		}
		
		neighbours_i.add(j);
	}
	
	public void removeEdge(int i, int j) {
		removeEdgeInternal(i, j);
		removeEdgeInternal(j, i);
	}
	
	private void removeEdgeInternal(int i, int j) {
		TIntArrayList neighbours_i = rows.get(i);
		TIntArrayList neighbours_j = rows.get(j);
		
		for(int k = 0; k < neighbours_j.size(); k++) {
			int neighbour = neighbours_j.get(k);
			if(neighbour != i) {
				adjustCommonNeighbours(i, neighbour, -1);
				adjustCommonNeighbours(neighbour, i, -1);
			}
		}
		
		neighbours_i.remove(neighbours_i.indexOf(j));
	}
	
	public int countCommonNeighbours(int i, int j) {
		return commonNeighours.get(i).get(j);
	}
	
	private void adjustCommonNeighbours(int i, int j, int amount) {
		TIntIntHashMap neighbours_i = commonNeighours.get(i); 
		int n = neighbours_i.adjustOrPutValue(j, amount, amount);
		if(n < 1)
			neighbours_i.remove(j);
	}
	
	public int countNeighbours(int i) {
		return rows.get(i).size();
	}
	
	public TIntArrayList getNeighbours(int i) {
		return rows.get(i);
	}
	
	public int countTriangles(int i) {
		int triangles = 0;
		TIntArrayList row = rows.get(i);
		for(int u = 0; u < row.size(); u++) {
			for(int v = u + 1; v < row.size(); v++) {
				if(getEdge(row.get(u), row.get(v)))
					triangles++;
			}
		}
		return triangles;
	}
	
	public int countTripples(int i) {
		int N = rows.get(i).size();
		return (int) (N * (N - 1) / 2.0);
	}
}
