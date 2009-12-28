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

import org.matsim.api.core.v01.Coord;

import playground.johannes.socialnetworks.graph.spatial.SpatialSparseVertex;
import playground.johannes.socialnetworks.snowball2.SnowballAttributes;

/**
 * @author illenberger
 *
 */
public class SampledSpatialSparseVertex extends SpatialSparseVertex implements
		SampledSpatialVertex {

	private SnowballAttributes attributes;
	
	protected SampledSpatialSparseVertex(Coord coord) {
		super(coord);
		attributes = new SnowballAttributes();
	}

	public void detect(int iteration) {
		attributes.detect(iteration);
	}

	public int getIterationDetected() {
		return attributes.getIterationDeteted();
	}

	public int getIterationSampled() {
		return attributes.getIterationSampled();
	}

	public boolean isDetected() {
		return attributes.isDetected();
	}

	public boolean isSampled() {
		return attributes.isSampled();
	}

	public void sample(int iteration) {
		attributes.sample(iteration);
	}

	@Override
	public List<? extends SampledSpatialSparseEdge> getEdges() {
		// TODO Auto-generated method stub
		return (List<? extends SampledSpatialSparseEdge>) super.getEdges();
	}

	@Override
	public List<? extends SampledSpatialSparseVertex> getNeighbours() {
		// TODO Auto-generated method stub
		return (List<? extends SampledSpatialSparseVertex>) super.getNeighbours();
	}

}
