/* *********************************************************************** *
 * project: org.matsim.*
 * SampledGraphStatistics.java
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
package playground.johannes.socialnetworks.survey.ivt2009;

import java.util.Set;

import org.matsim.api.basic.v01.Coord;

import playground.johannes.socialnetworks.graph.GraphStatistics;
import playground.johannes.socialnetworks.graph.spatial.SpatialGraph;
import playground.johannes.socialnetworks.graph.spatial.SpatialGraphStatistics;
import playground.johannes.socialnetworks.graph.spatial.SpatialVertex;
import playground.johannes.socialnetworks.statistics.Distribution;

/**
 * @author illenberger
 *
 */
public class SampledGraphStatistics {

	public static Distribution degreeDistribution(SampledGraph g) {
		return GraphStatistics.degreeDistribution(SnowballPartitions.createSampledPartition(g));
	}
	
	public static Distribution localClusteringDistribution(SampledGraph g) {
		return GraphStatistics.localClusteringDistribution(SnowballPartitions.createSampledPartition(g));
	}
	
	public static Distribution edgeLenghtDistribution(SampledSocialNet<?> g) {
		return SpatialGraphStatistics.edgeLengthDistribution((Set<? extends SpatialVertex>)SnowballPartitions.createSampledPartition(g));
	}
	
	public static Distribution normalizedEdgeLengthDistribution(SampledSocialNet<?> g, SpatialGraph g2, double descretization) {
		return SpatialGraphStatistics.normalizedEdgeLengthDistribution((Set<? extends SpatialVertex>) SnowballPartitions.createSampledPartition(g), g2, descretization);
	}
}
