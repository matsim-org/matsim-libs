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
 * @author illenberger
 *
 */
public class SparseGraphProjectionFactory<G extends Graph, V extends Vertex, E extends Edge> implements GraphProjectionFactory<G, V, E, GraphProjection<G,V,E>, VertexDecorator<V>, EdgeDecorator<E>>{

	public EdgeDecorator<E> createEdge(E delegate) {
		return new EdgeDecorator<E>(delegate);
	}

	public GraphProjection<G, V, E> createGraph(G delegate) {
		return new GraphProjection<G, V, E>(delegate);
	}

	public VertexDecorator<V> createVertex(V delegate) {
		return new VertexDecorator<V>(delegate);
	}

}
