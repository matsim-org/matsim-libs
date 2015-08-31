/* *********************************************************************** *
 * project: org.matsim.*
 * SampledGraphProjectionBuilder.java
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
package playground.johannes.sna.snowball;

import playground.johannes.sna.graph.Edge;
import playground.johannes.sna.graph.Graph;
import playground.johannes.sna.graph.GraphProjectionBuilder;
import playground.johannes.sna.graph.GraphProjectionFactory;
import playground.johannes.sna.graph.Vertex;

/**
 * An GraphBuilder implementation to build sampled graph projections.
 * 
 * @author illenberger
 * 
 */
public class SampledGraphProjectionBuilder<G extends Graph, V extends Vertex, E extends Edge>
		extends
		GraphProjectionBuilder<G, V, E, SampledGraphProjection<G, V, E>, SampledVertexDecorator<V>, SampledEdgeDecorator<E>> {

	/**
	 * Creates a new sampled graph projection builder.
	 */
	public SampledGraphProjectionBuilder() {
		super(new SampledGraphProjectionFactory<G, V, E>());
	}

	/**
	 * Creates a new sampled graph projection builder.
	 * 
	 * @param factory
	 *            the factory for creating SampledGraphProjection,
	 *            SampledVertexDecorator and SampledEdgeDecorator.
	 */
	public SampledGraphProjectionBuilder(
			GraphProjectionFactory<G, V, E, SampledGraphProjection<G, V, E>, SampledVertexDecorator<V>, SampledEdgeDecorator<E>> factory) {
		super(factory);
	}
}
