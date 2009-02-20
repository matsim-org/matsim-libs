/* *********************************************************************** *
 * project: org.matsim.*
 * ClusteringStats.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.snowball;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import playground.johannes.graph.GraphProjection;
import playground.johannes.graph.GraphStatistics;
import playground.johannes.graph.Vertex;
import playground.johannes.graph.VertexDecorator;

/**
 * @author illenberger
 * 
 */
public class GlobalClusteringStats extends GraphPropertyEstimator {

	private double globalResponseRate;
	
	public GlobalClusteringStats(String outputDir, double responseRate) {
		super(outputDir);
		this.globalResponseRate = responseRate;
		openStatsWriters("global-clustering");
	}

	@Override
	public DescriptiveStatistics calculate(
			GraphProjection<SampledGraph, SampledVertex, SampledEdge> graph,
			int iteration) {
		DescriptiveStatistics observed = new DescriptiveStatistics();
		DescriptiveStatistics estimated = new DescriptiveStatistics();
		
		observed.addValue(GraphStatistics.getGlobalClusteringCoefficient(graph));
		
		dumpObservedStatistics(getStatisticsMap(observed), iteration);
		dumpEstimatedStatistics(getStatisticsMap(estimated), iteration);

		return observed;
	}

}
