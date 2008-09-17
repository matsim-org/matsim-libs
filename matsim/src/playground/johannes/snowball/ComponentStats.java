/* *********************************************************************** *
 * project: org.matsim.*
 * ComponentStats.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

import java.util.Set;
import java.util.SortedSet;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import playground.johannes.graph.GraphProjection;
import playground.johannes.graph.GraphStatistics;
import playground.johannes.graph.Vertex;

/**
 * @author illenberger
 *
 */
public class ComponentStats extends GraphPropertyEstimator {

	/**
	 * @param outputDir
	 */
	public ComponentStats(String outputDir) {
		super(outputDir);
		openStatsWriters("components");
	}

	/* (non-Javadoc)
	 * @see playground.johannes.snowball.GraphPropertyEstimator#calculate(playground.johannes.graph.GraphProjection, int)
	 */
	@Override
	public DescriptiveStatistics calculate(
			GraphProjection<SampledGraph, SampledVertex, SampledEdge> graph,
			int iteration) {
		SortedSet<Set<Vertex>> components = GraphStatistics.getComponents(graph);
		DescriptiveStatistics stats = new DescriptiveStatistics();
		stats.addValue(components.size());
		dumpObservedStatistics(getStatisticsMap(stats), iteration);
		return stats;
	}

}
