/* *********************************************************************** *
 * project: org.matsim.*
 * DegreeStats.java
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

import org.apache.commons.math.stat.Frequency;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import playground.johannes.graph.GraphProjection;
import playground.johannes.graph.VertexDecorator;

/**
 * @author illenberger
 *
 */
public class DegreeStats extends GraphPropertyEstimator {

	public DegreeStats(String outputDir) {
		super(outputDir);
		openStatsWriters("degree");
	}
	
	@Override
	public DescriptiveStatistics calculate(GraphProjection<SampledGraph, SampledVertex, SampledEdge> graph, int iteration) {
		DescriptiveStatistics observed = new DescriptiveStatistics();
		Frequency observedDistr = new Frequency();
		
		DescriptiveStatistics estimated = new DescriptiveStatistics();
		Frequency estimatedDistr = new Frequency();
		
		for(VertexDecorator<SampledVertex> v : graph.getVertices()) {
			if(!v.getDelegate().isAnonymous()) {
				
				estimated.addValue(v.getEdges().size() * v.getDelegate().getNormalizedWeight());
				estimatedDistr.addValue(v.getEdges().size() * v.getDelegate().getNormalizedWeight());
				
				observed.addValue(v.getEdges().size());
				observedDistr.addValue(v.getEdges().size());
				
			}
		}
		
		dumpObservedStatistics(getStatisticsMap(observed), iteration);
		dumpEstimatedStatistics(getStatisticsMap(estimated), iteration);
		dumpFrequency(observedDistr, iteration, "observed");
		dumpFrequency(estimatedDistr, iteration, "estimated");
		
		return observed;
	}

}
