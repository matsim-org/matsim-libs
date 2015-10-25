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
package org.matsim.contrib.socnetgen.sna.snowball.spatial.analysis;

import gnu.trove.TObjectDoubleHashMap;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.socnetgen.sna.graph.spatial.analysis.Distance;
import org.matsim.contrib.socnetgen.sna.math.Distribution;
import org.matsim.contrib.socnetgen.sna.snowball.analysis.SnowballPartitions;
import org.matsim.contrib.socnetgen.sna.snowball.spatial.SpatialSampledVertexDecorator;

import java.util.Set;

/**
 * @author illenberger
 *
 */
public class ObservedDistance extends Distance {

	private static ObservedDistance instance;
	
	public static ObservedDistance getInstance() {
		if(instance == null)
			instance = new ObservedDistance();
		return instance;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Distribution distribution(Set<? extends SpatialVertex> vertices) {
		Set<SpatialSampledVertexDecorator<SpatialVertex>> spatialVertices = (Set<SpatialSampledVertexDecorator<SpatialVertex>>)vertices;
		return super.distribution(SnowballPartitions.createSampledPartition(spatialVertices));
	}

	@SuppressWarnings("unchecked")
	@Override
	public Distribution vertexAccumulatedDistribution(Set<? extends SpatialVertex> vertices) {
		Set<SpatialSampledVertexDecorator<SpatialVertex>> spatialVertices = (Set<SpatialSampledVertexDecorator<SpatialVertex>>)vertices;
		return super.vertexAccumulatedDistribution(SnowballPartitions.createSampledPartition(spatialVertices));
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public Distribution vertexMeanDistribution(Set<? extends SpatialVertex> vertices) {
		Set<SpatialSampledVertexDecorator<SpatialVertex>> spatialVertices = (Set<SpatialSampledVertexDecorator<SpatialVertex>>)vertices;
		return super.vertexMeanDistribution(SnowballPartitions.createSampledPartition(spatialVertices));
	}

	@SuppressWarnings("unchecked")
	@Override
	public TObjectDoubleHashMap<SpatialVertex> vertexMean(Set<? extends SpatialVertex> vertices) {
		Set<SpatialSampledVertexDecorator<SpatialVertex>> spatialVertices = (Set<SpatialSampledVertexDecorator<SpatialVertex>>)vertices;
		return super.vertexMean(SnowballPartitions.createSampledPartition(spatialVertices));
	}
}
