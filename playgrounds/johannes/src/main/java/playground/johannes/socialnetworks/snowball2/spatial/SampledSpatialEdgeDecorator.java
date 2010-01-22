/* *********************************************************************** *
 * project: org.matsim.*
 * SampledSpatialEdgeDecorator.java
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
package playground.johannes.socialnetworks.snowball2.spatial;

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.spatial.SpatialEdgeDecorator;
import org.matsim.contrib.sna.snowball.spatial.SampledSpatialEdge;
import org.matsim.core.utils.collections.Tuple;


/**
 * @author illenberger
 *
 */
public class SampledSpatialEdgeDecorator<E extends SampledSpatialEdge> extends SpatialEdgeDecorator<E>
		implements SampledSpatialEdge {

	protected SampledSpatialEdgeDecorator(E delegate) {
		super(delegate);
	}

	@Override
	public SampledSpatialVertexDecorator<?> getOpposite(Vertex v) {
		return (SampledSpatialVertexDecorator<?>) super.getOpposite(v);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Tuple<? extends SampledSpatialVertexDecorator<?>, ? extends SampledSpatialVertexDecorator<?>> getVertices() {
		return (Tuple<? extends SampledSpatialVertexDecorator<?>, ? extends SampledSpatialVertexDecorator<?>>) super.getVertices();
	}

}
