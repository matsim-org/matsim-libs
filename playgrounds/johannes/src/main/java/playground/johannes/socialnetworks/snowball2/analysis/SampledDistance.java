/* *********************************************************************** *
 * project: org.matsim.*
 * SampledDistance.java
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
package playground.johannes.socialnetworks.snowball2.analysis;

import java.util.HashSet;
import java.util.Set;

import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.math.Distribution;

import playground.johannes.socialnetworks.graph.spatial.Distance;
import playground.johannes.socialnetworks.snowball2.SampledVertexDecorator;

/**
 * @author illenberger
 *
 */
public class SampledDistance extends Distance {

	@Override
	public Distribution distribution(Set<? extends SpatialVertex> vertices) {
		return super.distribution(extractDelegates(vertices));
	}

	@Override
	public Distribution vertexAccumulatedDistribution(
			Set<? extends SpatialVertex> vertices) {
		return super.vertexAccumulatedDistribution(extractDelegates(vertices));
	}

	private Set<SpatialVertex> extractDelegates(Set<? extends SpatialVertex> vertices) {
//		Set<SampledVertexDecorator<? extends SpatialVertex>> partition =
//			SnowballPartitions.<SampledVertexDecorator<? extends SpatialVertex>>
//			createSampledPartition((Set<SampledVertexDecorator<? extends SpatialVertex>>)vertices);
//		
//		Set<SpatialVertex> delegates = new HashSet<SpatialVertex>();
//		for(SampledVertexDecorator<? extends SpatialVertex> v : partition)
//			delegates.add(v.getDelegate());
//		
//		return delegates;
		return null;
	}
}
