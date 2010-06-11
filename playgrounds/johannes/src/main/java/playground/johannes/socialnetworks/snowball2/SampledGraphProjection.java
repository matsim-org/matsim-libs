/* *********************************************************************** *
 * project: org.matsim.*
 * SampledGraphProjection.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.snowball2;

import java.util.Set;

import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.GraphProjection;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.snowball.SampledGraph;

/**
 * @author illenberger
 *
 */
public class SampledGraphProjection<G extends Graph, V extends Vertex, E extends Edge> extends GraphProjection<G, V, E> implements SampledGraph {

	public SampledGraphProjection(G delegate) {
		super(delegate);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Set<? extends SampledEdgeDecorator<E>> getEdges() {
		return (Set<? extends SampledEdgeDecorator<E>>) super.getEdges();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Set<? extends SampledVertexDecorator<V>> getVertices() {
		return (Set<? extends SampledVertexDecorator<V>>) super.getVertices();
	}
}
