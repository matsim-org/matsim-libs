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
package playground.johannes.socialnetworks.snowball;

import java.util.Set;
import java.util.SortedSet;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.sna.graph.GraphProjection;
import org.matsim.contrib.sna.graph.Vertex;

import playground.johannes.socialnetworks.graph.Partitions;

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
		SortedSet<Set<Vertex>> components = Partitions.disconnectedComponents(graph);
		DescriptiveStatistics stats = new DescriptiveStatistics();
		stats.addValue(components.size());
		dumpObservedStatistics(getStatisticsMap(stats), iteration);
		return stats;
	}

}
