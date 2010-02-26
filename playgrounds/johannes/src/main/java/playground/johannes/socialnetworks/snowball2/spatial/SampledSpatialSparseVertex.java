/* *********************************************************************** *
 * project: org.matsim.*
 * SampledSpatialVertex.java
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
package playground.johannes.socialnetworks.snowball2.spatial;

import java.util.List;

import org.matsim.contrib.sna.graph.spatial.SpatialSparseVertex;
import org.matsim.contrib.sna.snowball.SnowballAttributes;

import com.vividsolutions.jts.geom.Point;

/**
 * Implementation of {@link SampledSpatialVertex} with a {@link SpatialSparseVertex}.
 * 
 * @author illenberger
 *
 */
public class SampledSpatialSparseVertex extends SpatialSparseVertex implements
		SampledSpatialVertex {

	private SnowballAttributes attributes;
	
	/**
	 * @see {@link SpatialSparseVertex()}.
	 */
	protected SampledSpatialSparseVertex(Point point) {
		super(point);
		attributes = new SnowballAttributes();
	}

	/**
	 * @see {@link SnowballAttributes#detect(int)}
	 */
	public void detect(int iteration) {
		attributes.detect(iteration);
	}

	/**
	 * @see {@link SnowballAttributes#getIterationDeteted()}
	 */
	public int getIterationDetected() {
		return attributes.getIterationDeteted();
	}

	/**
	 * @see {@link SnowballAttributes#getIterationSampled()}
	 */
	public int getIterationSampled() {
		return attributes.getIterationSampled();
	}

	/**
	 * @see {@link SnowballAttributes#isDetected()}
	 */
	public boolean isDetected() {
		return attributes.isDetected();
	}

	/**
	 * @see {@link SnowballAttributes#isSampled()}
	 */
	public boolean isSampled() {
		return attributes.isSampled();
	}

	/**
	 * @see {@link SnowballAttributes#sample(int)}
	 */
	public void sample(int iteration) {
		attributes.sample(iteration);
	}

	/**
	 * @see {@link SpatialSparseVertex#getEdges()}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<? extends SampledSpatialSparseEdge> getEdges() {
		return (List<? extends SampledSpatialSparseEdge>) super.getEdges();
	}

	/**
	 * @see {@link SpatialSparseVertex#getNeighbours()}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<? extends SampledSpatialSparseVertex> getNeighbours() {
		return (List<? extends SampledSpatialSparseVertex>) super.getNeighbours();
	}

}
