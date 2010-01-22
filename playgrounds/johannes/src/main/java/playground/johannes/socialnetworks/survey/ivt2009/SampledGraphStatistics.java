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

import gnu.trove.TDoubleDoubleHashMap;

import java.util.Collection;
import java.util.Set;

import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.contrib.sna.snowball.SampledGraph;
import org.matsim.contrib.sna.snowball.SampledVertex;

import playground.johannes.socialnetworks.graph.GraphStatistics;
import playground.johannes.socialnetworks.graph.spatial.SpatialGraphStatistics;
import playground.johannes.socialnetworks.snowball2.spatial.analysis.SnowballPartitions;
import playground.johannes.socialnetworks.spatial.ZoneLayerLegacy;

/**
 * @author illenberger
 *
 */
public class SampledGraphStatistics {

	public static Distribution degreeDistribution(SampledGraph g) {
		return GraphStatistics.degreeDistribution(SnowballPartitions.createSampledPartition((Set<? extends SampledVertex>)g.getVertices()));
	}
	
	public static Distribution localClusteringDistribution(SampledGraph g) {
		return GraphStatistics.localClusteringDistribution(SnowballPartitions.createSampledPartition((Set<? extends SampledVertex>)g.getVertices()));
	}
	
	public static <V extends SampledVertex & SpatialVertex, G extends SpatialGraph & SampledGraph> Distribution edgeLenghtDistribution(Graph g) {
		return SpatialGraphStatistics.edgeLengthDistribution((Set<? extends SpatialVertex>) SnowballPartitions.createSampledPartition((Collection<? extends SampledVertex>) (g.getVertices())));
	}
	
	public static <V extends SampledVertex & SpatialVertex> Distribution edgeLenghtDistribution(Graph g, ZoneLayerLegacy zones) {
		return SpatialGraphStatistics.edgeLengthDistribution((Set<? extends SpatialVertex>) SnowballPartitions.createSampledPartition((Set<V>)g.getVertices()), zones);
	}
	
	public static <V extends SampledVertex & SpatialVertex> TDoubleDoubleHashMap edgeLengthDegreeCorrelation(Set<V> vertices) {
		return SpatialGraphStatistics.edgeLengthDegreeCorrelation((Set<? extends SpatialVertex>) SnowballPartitions.createSampledPartition(vertices));
	}
	
//	public static Distribution normalizedEdgeLengthDistribution(SampledSocialNet<?> g, SpatialGraph g2, double descretization) {
//		return SpatialGraphStatistics.normalizedEdgeLengthDistribution((Set) SnowballPartitions.createSampledPartition(g.getVertices()), g2, descretization);
//	}
}
