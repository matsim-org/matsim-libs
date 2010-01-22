/* *********************************************************************** *
 * project: org.matsim.*
 * SampledDistance.java
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
package playground.johannes.socialnetworks.snowball2.spatial.analysis;

import java.util.Set;

import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.contrib.sna.snowball.spatial.SampledSpatialVertex;

import playground.johannes.socialnetworks.graph.spatial.Distance;

/**
 * @author illenberger
 *
 */
public class SampledDistance extends Distance {

	@SuppressWarnings("unchecked")
	@Override
	public Distribution distribution(Set<? extends SpatialVertex> vertices) {
		/*
		 * I think it makes no difference to directly calling super(vertices)
		 * since each edge is only counted once. joh13/1/10
		 */
		return super.distribution(SnowballPartitions.<SampledSpatialVertex>createSampledPartition((Set<SampledSpatialVertex>)vertices));
	}

	@SuppressWarnings("unchecked")
	@Override
	public Distribution vertexAccumulatedDistribution(Set<? extends SpatialVertex> vertices) {
		return super.vertexAccumulatedDistribution(SnowballPartitions.<SampledSpatialVertex>createSampledPartition((Set<SampledSpatialVertex>)vertices));
	}

}
