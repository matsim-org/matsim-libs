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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * Representation of an unweighted Dijkstra best path algorithm. This algorithm
 * assigns each edge a weight of 1.
 * 
 * @author illenberger
 * 
 */
public class UnweightedDijkstra<V extends Vertex> {

	private Queue<DijkstraVertex> unsettledVertices;
	
	private DijkstraGraph projection;

	/**
	 * Creates and initializes a new Dijkstra object.
	 * 
	 * @param graph
	 *            The graph on which best path searches should be performed.
	 */
	public UnweightedDijkstra(Graph graph) {
		this.projection = new DijkstraGraph(graph);
	}

	/**
	 * Spans up a complete best path tree with <tt>source</tt> as its root node.
	 * 
	 * @param source
	 *            the root node.
	 * @return a list of reachable vertices.
	 * 
	 * @see {@link #run(Vertex, Vertex)}
	 */
	public List<DijkstraVertex> run(V source) {
		return run(source, null);
	}
	
	/**
	 * Runs the Dijkstra algorithm starting from <tt>source</tt> until
	 * <tt>target</tt> has been reached and returns a list of all reachable
	 * vertices wrapped into DijkstraVertex objects. A DijkstraVertex
	 * knows all its predecessors (if there are multiple best paths from
	 * <tt>source</tt> to <tt>target</tt>). Use {@link #getPath(Vertex, Vertex)}
	 * to obtain a specific path.
	 * 
	 * @param source
	 *            the root node.
	 * @param target
	 *            the destination node.
	 * @return a list of all reachable vertices.
	 */
	public List<DijkstraVertex> run(V source, V target) {
		List<DijkstraVertex> reachedVertices = new ArrayList<DijkstraVertex>();
		DijkstraVertex dsource = projection.getVertex(source); 
		DijkstraVertex dtarget = projection.getVertex(target);
		unsettledVertices = new PriorityQueue<DijkstraVertex>(projection.getVertices().size());
		for(VertexDecorator<V> dvertex : projection.getVertices())
			((DijkstraVertex)dvertex).reset();
		
		dsource.setDistance(0);
		unsettledVertices.add(dsource);
		
		DijkstraVertex dvertex;
	
		while((dvertex = unsettledVertices.poll()) != null) {
			if(dvertex == dtarget)
				break;
			
			if(!dvertex.isSettled()) {
				dvertex.setSettled(true);
				reachedVertices.add(dvertex);
				
				int cnt = dvertex.getNeighbours().size();
				for(int i = 0; i < cnt; i++) {
					Vertex neighbour = dvertex.getNeighbours().get(i);
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
		
		reachedVertices.remove(dsource);
		return reachedVertices;
	}
	
	/**
	 * Retrieves a path from <tt>source</tt> to <tt>target</tt>.
	 * 
	 * @param source
	 *            the source vertex.
	 * @param target
	 *            the target vertex.
	 * @return a list of vertices in the oder in which they are passed when
	 *         traversing the path from <tt>source</tt> to <tt>target</tt>
	 *         excluding the source vertex and including the target vertex.
	 *         Returns <tt>null</tt> if <tt>source==target</tt>, or if there exists
	 *         no path from <tt>source</tt> to <tt>target</tt>, or if
	 *         {@link #run(Vertex)} or {@link #run(Vertex, Vertex)} has never
	 *         been called before.
	 */
	public List<V> getPath(V source, V target) {
		LinkedList<V> path = new LinkedList<V>();
		
		if(source == target)
			return path;
		
		DijkstraVertex v = projection.getVertex(target);
		if(v.getPredecessors().length == 0)
			return null;
		
		DijkstraVertex dsource = projection.getVertex(source);
		while(v != dsource) {
			path.addFirst(v.getDelegate());
			if(v.getPredecessors().length > 0)
				v = v.getPredecessors()[0];
			else
				return null;
		}
		return path;
	}
	
	/**
	 * A decorator class for a graph to store Dijkstra related information.
	 * 
	 * @author illenberger
	 * 
	 */
	public class DijkstraGraph extends GraphProjection<Graph, V, Edge> {
	
		/**
		 * Creates a new DijkstraGraph out of <tt>delegate</tt>.
		 * @param delegate the graph to be decorated.
		 */
		public DijkstraGraph(Graph delegate) {
			super(delegate);
			decorate();
		}

		/**
		 * @see {@link GraphProjection#addVertex(Vertex)}
		 */
		@Override
		public VertexDecorator<V> addVertex(V delegate) {
			VertexDecorator<V> v = new DijkstraVertex(delegate);
			if(insertVertex(v))
				return v;
			else 
				return null;
		}

		/**
		 * @see {@link GraphProjection#getVertex(Vertex)}
		 */
		public DijkstraVertex getVertex(V v) {
			return (DijkstraVertex) super.getVertex(v);
		}
	}
	
	/**
	 * A decorator call for vertices to store Dijkstra related information.
	 * 
	 * @author illenberger
	 * 
	 */
	public class DijkstraVertex extends VertexDecorator<V> implements
			Comparable<DijkstraVertex> {

		private boolean isSettled;

		private boolean isVisited;

		private int distance;

		private DijkstraVertex[] predecessors;

		/**
		 * Creates a new DijkstraVertex.
		 * 
		 * @param delegate
		 *            the vertex to decorate.
		 */
		public DijkstraVertex(V delegate) {
			super(delegate);
		}

		/**
		 * Returns if this vertex is settled.
		 * 
		 * @return <tt>true</tt> if this vertex is settled, <tt>false</tt>
		 *         otherwise.
		 */
		private boolean isSettled() {
			return isSettled;
		}

		/**
		 * Sets if this vertex is settled.
		 * 
		 * @param flag
		 *            <tt>true</tt> if this vertex is settled, <tt>false</tt>
		 *            otherwise.
		 */
		private void setSettled(boolean flag) {
			isSettled = flag;
		}

		private boolean isVisited() {
			return isVisited;
		}

		private void setVisited(boolean flag) {
			isVisited = flag;
		}

		private int getDistance() {
			return distance;
		}

		private void setDistance(int d) {
			distance = d;
		}

		/**
		 * Returns an array of predecessors of this vertex.
		 * 
		 * @return an array of predecessors of this vertex.
		 */
		public DijkstraVertex[] getPredecessors() {
			return predecessors;
		}

		@SuppressWarnings("unchecked")
		private void setPredecessor(DijkstraVertex v) {
			if (predecessors.length != 1)
				predecessors = new UnweightedDijkstra.DijkstraVertex[1];
			predecessors[0] = v;
		}

		@SuppressWarnings("unchecked")
		private void addPredecessor(DijkstraVertex v) {
			DijkstraVertex[] newPredecessors = new UnweightedDijkstra.DijkstraVertex[predecessors.length + 1];
			for (int i = 0; i < predecessors.length; i++)
				newPredecessors[i] = predecessors[i];
			newPredecessors[predecessors.length] = v;
			predecessors = newPredecessors;
		}

		@SuppressWarnings("unchecked")
		private void reset() {
			isSettled = false;
			isVisited = false;
			distance = Integer.MAX_VALUE;
			predecessors = new UnweightedDijkstra.DijkstraVertex[0];
		}

		/**
		 * Compares this vertex with vertex <tt>o</tt> according to their
		 * distance attribute. If distances are equal, both vertices are
		 * compared according to their hash code.
		 */
		public int compareTo(DijkstraVertex o) {
			int result = this.distance - o.distance;
			if (result == 0) {
				if (o.equals(this))
					return 0;
				else
					return this.hashCode() - o.hashCode();
			} else {
				return result;
			}
		}
	}
}
