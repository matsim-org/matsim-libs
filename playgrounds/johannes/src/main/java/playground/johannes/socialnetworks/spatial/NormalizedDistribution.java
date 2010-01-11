/* *********************************************************************** *
 * project: org.matsim.*
 * KMLWriter.java
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
package playground.johannes.socialnetworks.spatial;

import java.io.IOException;

import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraph;
import org.matsim.contrib.sna.graph.spatial.io.SpatialGraphMLReader;

import playground.johannes.socialnetworks.graph.spatial.SpatialGraphStatistics;
import playground.johannes.socialnetworks.statistics.Distribution;

/**
 * @author illenberger
 *
 */
public class NormalizedDistribution {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		SpatialSparseGraph graph;
		SpatialGraphMLReader reader = new SpatialGraphMLReader();
		graph = reader.readGraph(args[0]);
		
		ZoneLayer zones = ZoneLayer.createFromShapeFile(args[1]);
		
		String output = args[2];
		
		Distribution distr = SpatialGraphStatistics.normalizedEdgeLengthDistribution(graph.getVertices(), graph, 1000, zones);
		Distribution.writeHistogram(distr.absoluteDistribution(1000), output + "distance.norm.txt");
		Distribution.writeHistogram(distr.normalizedDistribution(distr.absoluteDistribution(1000)), output + "distance.norm.norm.txt");
		Distribution.writeHistogram(distr.normalizedDistribution(distr.absoluteDistributionLog2(1000)), output + "distance.norm.log2.norm.txt");
	}

}
