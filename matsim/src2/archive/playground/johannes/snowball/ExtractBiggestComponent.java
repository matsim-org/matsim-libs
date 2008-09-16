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

import java.util.Collection;
import java.util.SortedSet;

import org.apache.log4j.Logger;

import playground.johannes.socialnets.PersonGraphMLFileHandler;
import playground.johannes.statistics.GraphStatistics;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
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
		
		
		logger.info("Extacting components...");
		SortedSet<Collection<Vertex>> clusters = playground.johannes.statistics.GraphStatistics.getDisconnectedComponents(g);
		logger.info(String.format("Graph has %1$s components.", clusters.size()));
		
		
		StringBuilder builder = new StringBuilder();
		builder.append("Component summary:\n");
		int i = 0;
		for(Collection<Vertex> cluster : clusters) {
			i++;
			builder.append("\t");
			builder.append(String.valueOf(i));
			builder.append(" : ");
			builder.append(String.valueOf(cluster.size()));
			builder.append("\n");
		}
		logger.info(builder.toString());
		
		Graph g2 = GraphStatistics.extractGraphFromCluster(clusters.first());
		
		logger.info(String.format("Graph has %1$s vertices, %2$s edges, density = %3$s, mean degree = %4$s.",
				g2.numVertices(),
				g2.numEdges(),
				g2.numEdges()/((double)(g2.numVertices() * (g2.numVertices()-1))),
				GraphStatistics.getDegreeStatistics(g2).getMean()));
		logger.info("Saving social network...");
		gmlFile.save(g2, args[1]);
		logger.info("Done.");
	}

}
