/* *********************************************************************** *
 * project: org.matsim.*
 * ExtractBiggestComponent.java
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

import java.util.Set;

import org.apache.log4j.Logger;

import playground.johannes.socialnets.GraphStatistics;
import playground.johannes.socialnets.PersonGraphMLFileHandler;
import edu.uci.ics.jung.algorithms.cluster.ClusterSet;
import edu.uci.ics.jung.algorithms.cluster.WeakComponentClusterer;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.io.GraphMLFile;

/**
 * @author illenberger
 *
 */
public class ExtractBiggestComponent {

	private static final Logger logger = Logger.getLogger(ExtractBiggestComponent.class);
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		logger.info("Loading social network from file...");
		PersonGraphMLFileHandler fileHandler = new PersonGraphMLFileHandler();
		GraphMLFile gmlFile = new GraphMLFile(fileHandler);
		Graph g = gmlFile.load(args[0]);
		
		WeakComponentClusterer wcc = new WeakComponentClusterer();
		logger.info("Extacting components...");
		ClusterSet clusters = wcc.extract(g);
		logger.info(String.format("Graph has %1$s components.", clusters.size()));
		clusters.sort();
		Graph g2 = clusters.getClusterAsNewSubGraph(0);
		
		logger.info(String.format("Graph has %1$s vertices, %2$s edges, density = %3$s, mean degree = %4$s.",
				g2.numVertices(),
				g2.numEdges(),
				g2.numEdges()/((double)(g2.numVertices() * (g2.numVertices()-1))),
				GraphStatistics.createDegreeHistogram(g2, -1, -1, 0).getMean()));
		logger.info("Saving social network...");
		gmlFile.save(g2, args[1]);
		logger.info("Done.");
	}

}
