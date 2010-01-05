/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialEdgeDecorator.java
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
package playground.johannes.socialnetworks.graph.spatial;

import org.matsim.contrib.sna.graph.EdgeDecorator;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.core.utils.collections.Tuple;

public class SpatialEdgeDecorator<E extends SpatialEdge> extends EdgeDecorator<E> implements
		SpatialEdge {

	protected SpatialEdgeDecorator(E delegate) {
		super(delegate);
	}

	@Override
	public SpatialVertexDecorator<?> getOpposite(Vertex v) {
		return (SpatialVertexDecorator<?>) super.getOpposite(v);
	}

	@Override
	public Tuple<? extends SpatialVertexDecorator<?>, ? extends SpatialVertexDecorator<?>> getVertices() {
		return (Tuple<? extends SpatialVertexDecorator<?>, ? extends SpatialVertexDecorator<?>>) super.getVertices();
	}

	public double length() {
		return getDelegate().length();
	}

}
