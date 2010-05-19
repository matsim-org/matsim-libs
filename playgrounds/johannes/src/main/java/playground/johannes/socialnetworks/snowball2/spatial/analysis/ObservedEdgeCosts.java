/* *********************************************************************** *
 * project: org.matsim.*
 * ObservedEdgeCosts.java
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
package playground.johannes.socialnetworks.snowball2.spatial.analysis;

import java.util.Set;

import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.math.Distribution;

import playground.johannes.socialnetworks.graph.spatial.analysis.EdgeCosts;
import playground.johannes.socialnetworks.graph.spatial.generators.EdgeCostFunction;
import playground.johannes.socialnetworks.snowball2.analysis.SnowballPartitions;
import playground.johannes.socialnetworks.snowball2.spatial.SpatialSampledVertexDecorator;

/**
 * @author illenberger
 *
 */
public class ObservedEdgeCosts extends EdgeCosts {

	public ObservedEdgeCosts(EdgeCostFunction costFunction) {
		super(costFunction);
	}

	@Override
	public Distribution distribution(Set<? extends SpatialVertex> vertices) {
		Set<SpatialSampledVertexDecorator<SpatialVertex>> spatialVertices = (Set<SpatialSampledVertexDecorator<SpatialVertex>>)vertices;
		return super.distribution(SnowballPartitions.createSampledPartition(spatialVertices));
	}

	@Override
	public Distribution vertexCostsSum(Set<? extends SpatialVertex> vertices) {
		Set<SpatialSampledVertexDecorator<SpatialVertex>> spatialVertices = (Set<SpatialSampledVertexDecorator<SpatialVertex>>)vertices;
		return super.vertexCostsSum(SnowballPartitions.createSampledPartition(spatialVertices));
	}

}
