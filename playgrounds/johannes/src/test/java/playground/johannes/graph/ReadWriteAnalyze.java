/* *********************************************************************** *
 * project: org.matsim.*
 * ReadWriteAnalyze.java
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
package playground.johannes.graph;

import java.io.IOException;

import org.matsim.contrib.sna.graph.SparseGraph;
import org.matsim.contrib.sna.graph.io.SparseGraphMLReader;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestCase;

import playground.johannes.socialnetworks.graph.GraphStatistics;
import playground.johannes.socialnetworks.graph.Partitions;
import playground.johannes.socialnetworks.graph.GraphStatistics.GraphDistance;
import playground.johannes.socialnetworks.graph.io.GraphMLWriter;

/**
 * @author illenberger
 *
 */
public class ReadWriteAnalyze extends MatsimTestCase {

	private static final String GRAPH_INPUT_FILE = "berlin.8571.k=11.graphml.gz";
	
	private static final String GRAPH_OUTPUT_FILE = "tmpgraph.graphml";
	
	private static final int NUM_VERTICES = 8571;
	
	private static final int NUM_EDGES = 48332;
	
	private static final double MEAN_DEGREE = 11.278030568195076;
	
	private static final double MEAN_CLUSTERING = 0.1459862408584786;
	
	private static final double MUTUALITY = 0.7920715441627442;
	
	private static final double DEGREECORRELATION = 0.4700814025944884;
	
	private static final int NUM_COMPONENTS = 156;
	
	private static final double CLOSENESS = 4.2462339222975505;
	
	private static final double BETWEENNESS = 28895.98190305937;
	
	private static final int DIAMETER = 12;
	
	private static final int RADIUS = 0;
	
	private static final double ASSERT_DELTA = 0.000000000000001;
	/**
	 * 
	 */
	public void testReadWriteAnalyze() {
		SparseGraph g = new SparseGraphMLReader().readGraph(getPackageInputDirectory() + GRAPH_INPUT_FILE);
		
		assertEquals(NUM_VERTICES, g.getVertices().size());
		assertEquals(NUM_EDGES, g.getEdges().size());
		
		assertEquals(MEAN_DEGREE, GraphStatistics.degreeDistribution(g).mean(), ASSERT_DELTA);
		assertEquals(MEAN_CLUSTERING, GraphStatistics.localClusteringDistribution(g).mean(), ASSERT_DELTA);
		assertEquals(MUTUALITY, GraphStatistics.mutuality(g), ASSERT_DELTA);
		assertEquals(DEGREECORRELATION, GraphStatistics.degreeDegreeCorrelation(g), ASSERT_DELTA);
		assertEquals(NUM_COMPONENTS, Partitions.disconnectedComponents(g).size());
		
		GraphDistance gd = GraphStatistics.centrality(g);
		assertEquals(CLOSENESS, gd.getGraphCloseness(), ASSERT_DELTA);
		assertEquals(BETWEENNESS, gd.getGraphBetweenness(), 0.0000000001);
		assertEquals(DIAMETER, gd.getDiameter());
		assertEquals(RADIUS, gd.getRadius());
		
		try {
			new GraphMLWriter().write(g, getOutputDirectory() + GRAPH_OUTPUT_FILE);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		double reference = CRCChecksum.getCRCFromFile(getPackageInputDirectory() + GRAPH_INPUT_FILE);
		double actual = CRCChecksum.getCRCFromFile(getOutputDirectory() + GRAPH_OUTPUT_FILE);
		
		assertEquals(reference, actual);
	}

}
