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
package playground.johannes.socialnetworks.snowball;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.sna.graph.Vertex;

import playground.johannes.socialnetworks.graph.GraphProjection;
import playground.johannes.socialnetworks.graph.VertexDecorator;

/**
 * @author illenberger
 * 
 */
public class ClusteringStats2 extends GraphPropertyEstimator {

	private double globalResponseRate;
	
	public ClusteringStats2(String outputDir, double responseRate) {
		super(outputDir);
		this.globalResponseRate = responseRate;
		openStatsWriters("clustering-global");
	}

	@Override
	public DescriptiveStatistics calculate(
			GraphProjection<SampledGraph, SampledVertex, SampledEdge> graph,
			int iteration) {
		DescriptiveStatistics observed = new DescriptiveStatistics();
		DescriptiveStatistics estimated = new DescriptiveStatistics();
		
		for (VertexDecorator<SampledVertex> v : graph.getVertices()) {
			if (!v.getDelegate().isAnonymous()) {
				int k = v.getEdges().size();
				if (k == 0 || k == 1) {
					observed.addValue(0.0);
					estimated.addValue(0.0);
				} else {
					int edgecount = 0;
					Set<Vertex> n1s = new HashSet<Vertex>(v.getNeighbours());
					for (Vertex n1 : v.getNeighbours()) {
						for (Vertex n2 : n1.getNeighbours()) {
							if (n2 != v) {
								if (n1s.contains(n2)) {
									edgecount++;
								}
							}
						}
						n1s.remove(n1);
					}
					observed.addValue(2	* edgecount	/ (double) (k * (k - 1)));
//					double localResponseRate = getLocalResponseRate(v, globalResponseRate);
					double localResponseRate = globalResponseRate;
					if(localResponseRate > 0) {
						double edgecountCorrected = Math.min(k * (k - 1), edgecount / (2 * localResponseRate - Math.pow(localResponseRate, 2)));
						estimated.addValue(2 *  edgecountCorrected / (double) (k * (k - 1)) * v.getDelegate().getNormalizedWeight());
					}
				}
			}
		}
		
		dumpObservedStatistics(getStatisticsMap(observed), iteration);
		dumpEstimatedStatistics(getStatisticsMap(estimated), iteration);

		return observed;
	}

}
