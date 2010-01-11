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
package playground.johannes.socialnetworks.snowball;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.GraphProjection;
import org.matsim.contrib.sna.graph.VertexDecorator;
import org.matsim.contrib.sna.math.Distribution;

import playground.johannes.socialnetworks.graph.GraphStatistics;
import playground.johannes.socialnetworks.graph.GraphStatistics.GraphDistance;

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
	
	private BufferedWriter bcObsNormWriter;
	
	private BufferedWriter bcEstimNormWriter;
	
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
			bcObsNormWriter = openWriter("betweennessNorm.observed.txt");
			bcEstimNormWriter = openWriter("betweennessNorm.estimated.txt");
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
		GraphDistance gDistance = GraphStatistics.centrality(graph);
		
		Distribution ccObsStats = new Distribution();
		Distribution ccEstimStats = new Distribution();
		Distribution bcObsStats = new Distribution();
		Distribution bcEstimStats = new Distribution();
		Distribution bcObsNormStats = new Distribution();
		Distribution bcEstimNormStats = new Distribution();
		Distribution diameter = new Distribution();
		Distribution radius = new Distribution();
		
		for(VertexDecorator<SampledVertex> v : graph.getVertices()) {
			if(v.getDelegate().isSampled()) {
				double cc = gDistance.getVertexCloseness().get(v);
				double bc = gDistance.getVertexBetweennees().get(v);
				double bcNorm = gDistance.getVertexBetweenneesNormalized().get(v);
				
				ccObsStats.add(cc);
				bcObsStats.add(bc);
				bcObsNormStats.add(bcNorm);
				
				ccEstimStats.add(cc, v.getDelegate().getNormalizedWeight());
				bcEstimStats.add(bc, v.getDelegate().getNormalizedWeight());
				bcEstimNormStats.add(bcNorm, v.getDelegate().getNormalizedWeight());
			}
		}
		
		diameter.add(gDistance.getDiameter());
		radius.add(gDistance.getRadius());
		
		dumpStatistics(getStatisticsMap(ccObsStats), iteration, ccObsWriter);
		dumpStatistics(getStatisticsMap(ccEstimStats), iteration, ccEstimWriter);
		dumpStatistics(getStatisticsMap(bcObsStats), iteration, bcObsWriter);
		dumpStatistics(getStatisticsMap(bcEstimStats), iteration, bcEstimWriter);
		dumpStatistics(getStatisticsMap(bcObsNormStats), iteration, bcObsNormWriter);
		dumpStatistics(getStatisticsMap(bcEstimNormStats), iteration, bcEstimNormWriter);
		dumpStatistics(getStatisticsMap(diameter), iteration, diameterWriter);
		dumpStatistics(getStatisticsMap(radius), iteration, radiusWriter);
		
		dumpFrequency(ccObsStats, iteration, "closeness.observed");
		dumpFrequency(ccEstimStats, iteration, "closeness.estimated");
		dumpFrequency(bcObsStats, iteration, "betweenness.observed");
		dumpFrequency(bcEstimStats, iteration, "betweenness.estimated");
		dumpFrequency(bcObsNormStats, iteration, "betweennessNorm.observed");
		dumpFrequency(bcEstimNormStats, iteration, "betweennessNorm.estimated");
		
		return null;
	}

}
