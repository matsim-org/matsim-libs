/* *********************************************************************** *
 * project: org.matsim.*
 * GraphProjection.java
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
package playground.johannes.socialnetworks.graph;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.SparseGraph;
import org.matsim.contrib.sna.graph.SparseVertex;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.core.utils.collections.Tuple;

/**
 * This class allows to extract subgraphs from existing graphs by projecting
 * specific vertices and edges onto a new graph. A GraphProjection works similar
 * to a decorator, however, the connectivity of vertices and edges can differ
 * from the original graph.
 * 
 * @author illenberger
 * 
 */
public class GraphProjection<G extends Graph, V extends Vertex, E extends Edge> extends SparseGraph {

	private G delegate;
	
	private Map<V, VertexDecorator<V>> vMapping = new HashMap<V, VertexDecorator<V>>();
	
	/**
	 * Creates a new empty projection from graph <tt>delegate</tt>.
	 * @param delegate the original graph.
	 */
	public GraphProjection(G delegate) {
		this.delegate = delegate;
	}
	
	/**
	 * Returns the original graph.
	 * @return the original graph.
	 */
	public G getDelegate() {
		return delegate;
	}
	
//	/**
//	 * Makes a complete projection for the entire original graph by decorating all vertices and edges.
//	 * @deprecated
//	 */
//	@SuppressWarnings("unchecked")
//	public void decorate() {
//		
//		for(Vertex v : delegate.getVertices()) {
//			addVertex((V) v);
//		}
//		
//		for(Edge e : delegate.getEdges()) {
//			VertexDecorator<V> v1 = vMapping.get(e.getVertices().getFirst());
//			VertexDecorator<V> v2 = vMapping.get(e.getVertices().getSecond());
//			addEdge(v1, v2, (E) e);
//		}
//	}
	
	/**
	 * Returns the decorator representing the projection of vertex <tt>v</tt>.
	 * @param v the original vertex.
	 * @return the decorater of vertex <tt>v</tt>, or <tt>null</tt> if there is no projection <tt>v</tt>.
	 */
	public VertexDecorator<V> getVertex(V v) {
		return vMapping.get(v);
	}

	void setMapping(V delegate, VertexDecorator<V> decorator) {
		vMapping.put(delegate, decorator);
	}
	/**
	 * Creates a new projection of edge <tt>delegate</tt> and inserts it between
	 * <tt>v1</tt> and <tt>v2</tt> obeying the constraints of
	 * {@link SparseGraph}.
	 * 
	 * @param v1
	 *            one of the two vertices the edge is to be connected to.
	 * @param v2
	 *            one of the two vertices the edge is to be connected to.
	 * @param delegate
	 *            the original edge.
	 * @return the decorator representing the projection of the edge, or
	 *         <tt>null</tt> if the projection could not be created without
	 *         violating the constraints of {@link SparseGraph}.
	 */
	/*
	 * joh 28/11/08: It should be sufficient to pass the delegate object and to
	 * internally find or create if necessary the corresponding vertex decorator
	 * objects.
	 */
//	public EdgeDecorator<E> addEdge(VertexDecorator<V> v1,
//			VertexDecorator<V> v2, E delegate) {
//		EdgeDecorator<E> edge = new EdgeDecorator<E>(delegate);
//		return addEdge(v1, v2, edge);
//	}
	
//	protected EdgeDecorator<E> addEdge(VertexDecorator<V> v1, VertexDecorator<V> v2, EdgeDecorator<E> edge) {
//		if (!v1.getNeighbours().contains(v2)) {
//			edge.setVertices(new Tuple<SparseVertex, SparseVertex>(v1, v2));
//			v1.addEdge(edge);
//			v2.addEdge(edge);
//			if(insertEdge(edge))
//				return edge;
//			else
//				return null;
//		} else
//			return null;
//	}
	
//	/**
//	 * Creates a new projection of vertex <tt>delegate</tt> and inserts it into
//	 * the graph.
//	 * 
//	 * @param delegate
//	 *            the original vertex.
//	 * @return the new vertex decorator representing the projection of vertex
//	 *         <tt>delegate</tt>, or <tt>null</tt> if there exists already a
//	 *         projection of vertex <tt>delegate</tt>.
//	 */
//	public VertexDecorator<V> addVertex(V delegate) {
//			VertexDecorator<V> v = new VertexDecorator<V>(delegate);
//			return addVertex(v);
//	}
//	
//	protected VertexDecorator<V> addVertex(VertexDecorator<V> v) {
//		if (getVertex(v.getDelegate()) == null) {
//			if (insertVertex(v)) {
//				vMapping.put(v.getDelegate(), v);
//				return v;
//			} else
//				return null;
//		} else
//			return null;
//	}

	/**
	 * @see {@link Graph#getEdges()}.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Set<? extends EdgeDecorator<E>> getEdges() {
		return (Set<? extends EdgeDecorator<E>>) super.getEdges();
	}

	/**
	 * @see {@link Graph#getVertices()}.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Set<? extends VertexDecorator<V>> getVertices() {
		return (Set<? extends VertexDecorator<V>>) super.getVertices();
	}
}
