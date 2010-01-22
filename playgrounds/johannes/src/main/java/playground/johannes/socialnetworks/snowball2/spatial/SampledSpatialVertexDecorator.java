/* *********************************************************************** *
 * project: org.matsim.*
 * SampledSpatialVertexDecorator.java
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

import java.util.List;

import org.matsim.contrib.sna.graph.spatial.SpatialVertexDecorator;
import org.matsim.contrib.sna.snowball.spatial.SampledSpatialVertex;


/**
 * @author illenberger
 *
 */
public class SampledSpatialVertexDecorator<V extends SampledSpatialVertex> extends SpatialVertexDecorator<V> implements SampledSpatialVertex {

	protected SampledSpatialVertexDecorator(V delegate) {
		super(delegate);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<? extends SampledSpatialEdgeDecorator<?>> getEdges() {
		return (List<? extends SampledSpatialEdgeDecorator<?>>) super.getEdges();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<? extends SampledSpatialVertexDecorator<V>> getNeighbours() {
		return (List<? extends SampledSpatialVertexDecorator<V>>) super.getNeighbours();
	}

	@Override
	public void detect(int iteration) {
		getDelegate().detect(iteration);
	}

	@Override
	public int getIterationDetected() {
		return getDelegate().getIterationDetected();
	}

	@Override
	public int getIterationSampled() {
		return getDelegate().getIterationSampled();
	}

	@Override
	public boolean isDetected() {
		return getDelegate().isDetected();
	}

	@Override
	public boolean isSampled() {
		return getDelegate().isSampled();
	}

	@Override
	public void sample(int iteration) {
		getDelegate().sample(iteration);
	}

}
