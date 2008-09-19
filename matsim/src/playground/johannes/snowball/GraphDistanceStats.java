/* *********************************************************************** *
 * project: org.matsim.*
 * GraphDistanceStats.java
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

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;

import playground.johannes.graph.GraphProjection;
import playground.johannes.graph.GraphStatistics;
import playground.johannes.graph.VertexDecorator;
import playground.johannes.graph.GraphStatistics.GraphDistance;
import playground.johannes.statistics.WeightedStatistics;

/**
 * @author illenberger
 *
 */
public class GraphDistanceStats extends GraphPropertyEstimator {
	
	private static final Logger logger = Logger.getLogger(GraphDistanceStats.class);

	private BufferedWriter ccObsWriter;
	
	private BufferedWriter ccEstimWriter;
	
	private BufferedWriter bcObsWriter;
	
	private BufferedWriter bcEstimWriter;
	
	private BufferedWriter diameterWriter;
	
	private BufferedWriter radiusWriter;
	
	/**
	 * @param outputDir
	 */
	public GraphDistanceStats(String outputDir) {
		super(outputDir);
		try {
			ccObsWriter = openWriter("closeness.observed.txt");
			ccEstimWriter = openWriter("closeness.estimated.txt");
			bcObsWriter = openWriter("betweenness.observed.txt");
			bcEstimWriter = openWriter("betweenness.estimated.txt");
			diameterWriter = openWriter("diameter.txt");
			radiusWriter = openWriter("radius.txt");
		} catch (IOException e) {
			logger.fatal("IOException occured!", e);
		}
		
	}

	@Override
	public DescriptiveStatistics calculate(
			GraphProjection<SampledGraph, SampledVertex, SampledEdge> graph,
			int iteration) {
		GraphDistance gDistance = GraphStatistics.getCentrality(graph);
		
		WeightedStatistics ccObsStats = new WeightedStatistics();
		WeightedStatistics ccEstimStats = new WeightedStatistics();
		WeightedStatistics bcObsStats = new WeightedStatistics();
		WeightedStatistics bcEstimStats = new WeightedStatistics();
		WeightedStatistics diameter = new WeightedStatistics();
		WeightedStatistics radius = new WeightedStatistics();
		
		for(VertexDecorator<SampledVertex> v : graph.getVertices()) {
			if(v.getDelegate().isSampled()) {
				double cc = gDistance.getVertexCloseness().get(v);
				double bc = gDistance.getVertexBetweennees().get(v);
				
				ccObsStats.add(cc);
				bcObsStats.add(bc);
				
				ccEstimStats.add(cc, v.getDelegate().getNormalizedWeight());
				bcEstimStats.add(bc, v.getDelegate().getNormalizedWeight());
			}
		}
		
		diameter.add(gDistance.getDiameter());
		radius.add(gDistance.getRadius());
		
		dumpStatistics(getStatisticsMap(ccObsStats), iteration, ccObsWriter);
		dumpStatistics(getStatisticsMap(ccEstimStats), iteration, ccEstimWriter);
		dumpStatistics(getStatisticsMap(bcObsStats), iteration, bcObsWriter);
		dumpStatistics(getStatisticsMap(bcEstimStats), iteration, bcEstimWriter);
		dumpStatistics(getStatisticsMap(diameter), iteration, diameterWriter);
		dumpStatistics(getStatisticsMap(radius), iteration, radiusWriter);
		
		dumpFrequency(ccObsStats, iteration, "closeness.observed");
		dumpFrequency(ccEstimStats, iteration, "closeness.estimated");
		dumpFrequency(bcObsStats, iteration, "betweenness.observed");
		dumpFrequency(bcEstimStats, iteration, "betweenness.estimated");
		
		return null;
	}

}
