/* *********************************************************************** *
 * project: org.matsim.*
 * UnweightedDijkstra.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.johannes.graph;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;


/**
 * @author illenberger
 *
 */
public class UnweightedDijkstra<V extends Vertex> {

	private Queue<DijkstraVertex> unsettledVertices;
	
	private DijkstraGraph projection;
	
	public UnweightedDijkstra(Graph graph) {
		this.projection = new DijkstraGraph(graph);
	}
	
	public List<V> run(V source) {
		List<V> reachedVertices = new LinkedList<V>();
		DijkstraVertex dsource = projection.getVertex(source); 
		unsettledVertices = new PriorityQueue<DijkstraVertex>(projection.getVertices().size());
		for(VertexDecorator<V> dvertex : projection.getVertices())
			((DijkstraVertex)dvertex).reset();
		
		dsource.setDistance(0);
		unsettledVertices.add(dsource);
		
		DijkstraVertex dvertex;
		while((dvertex = unsettledVertices.poll()) != null) {
			if(!dvertex.isSettled()) {
				dvertex.setSettled(true);
				reachedVertices.add(dvertex.getDelegate());
				
				for(Vertex neighbour : dvertex.getNeighbours()) {
					DijkstraVertex dneighbour = (DijkstraVertex)neighbour;
					if(!dneighbour.isSettled()) {
						int d = dvertex.getDistance() + 1; 
						if(d < dneighbour.getDistance()) {
							dneighbour.setPredecessor(dvertex);
							dneighbour.setDistance(d);
							if(dneighbour.isVisited())
								unsettledVertices.remove(neighbour);
							else
								dneighbour.setVisited(true);
							
							unsettledVertices.add(dneighbour);
							
						} else if(d == dneighbour.getDistance()) {
							dneighbour.addPredecessor(dvertex);
						}
					}
				}
			}
		}
		
		
		return reachedVertices;
	}
	
	public class DijkstraGraph extends GraphProjection<Graph, V, Edge> {

		private Map<V, DijkstraVertex> vMapping = new HashMap<V, DijkstraVertex>();
		
		public DijkstraGraph(Graph g) {
			super(g);
			
			for(Vertex v : g.getVertices()) {
				vMapping.put((V)v, (DijkstraVertex) addVertex((V)v));
			}
			
			for(Edge e : g.getEdges()) {
				DijkstraVertex v1 = vMapping.get(e.getVertices().getFirst());
				DijkstraVertex v2 = vMapping.get(e.getVertices().getSecond());
				addEdge(v1, v2);
			}
		}
		
		public DijkstraVertex getVertex(V v) {
			return vMapping.get(v);
		}

		@Override
		protected VertexDecorator<V> newVertex() {
			return new DijkstraVertex();
		}
	}
	
	public class DijkstraVertex extends VertexDecorator<V> implements Comparable<DijkstraVertex>{
		
		private boolean isSettled;
		
		private boolean isVisited;
		
		private int distance;
		
		private LinkedList<DijkstraVertex> predecessors;
		
		public DijkstraVertex() {
			super();
		}
		
		public boolean isSettled() {
			return isSettled;
		}
		
		public void setSettled(boolean flag) {
			isSettled = flag;
		}
		
		public boolean isVisited() {
			return isVisited;
		}
		
		public void setVisited(boolean flag) {
			isVisited = flag;
		}
		
		public int getDistance() {
			return distance;
		}
		
		public void setDistance(int d) {
			distance = d;
		}
		
		public List<DijkstraVertex> getPredecessors() {
			return predecessors;
		}
		
		public void setPredecessor(DijkstraVertex v) {
			predecessors.clear();
			predecessors.add(v);
		}
		
		public void addPredecessor(DijkstraVertex v) {
			predecessors.add(v);
		}
		
		public void reset() {
			isSettled = false;
			isVisited = false;
			distance = Integer.MAX_VALUE;
			predecessors = new LinkedList<DijkstraVertex>();
		}
		
		public int compareTo(DijkstraVertex o) {
			int result = this.distance - o.distance;
			if(result == 0) {
				if(o.equals(this))
					return 0;
				else
					return this.hashCode() - o.hashCode();
			} else {
				return result;
			}
		}
	}
}
