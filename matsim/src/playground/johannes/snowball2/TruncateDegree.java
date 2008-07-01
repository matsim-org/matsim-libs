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
package playground.johannes.snowball2;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import playground.johannes.socialnets.PersonGraphMLFileHandler;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.io.GraphMLFile;

/**
 * @author illenberger
 *
 */
public class TruncateDegree {

	private static final Logger logger = Logger.getLogger(TruncateDegree.class);
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		logger.info("Loading social network from file...");
		PersonGraphMLFileHandler fileHandler = new PersonGraphMLFileHandler();
		GraphMLFile gmlFile = new GraphMLFile(fileHandler);
		Graph g = gmlFile.load(args[0]);
		
		int maxDegree = Integer.parseInt(args[1]);
		Set<Vertex> vertices = new HashSet<Vertex>(g.getVertices());
		for(Vertex v : vertices) {
			if(v.degree() > maxDegree) {
				Set<Edge> edges = v.getIncidentEdges();
				for(Edge e : edges) {
					g.removeEdge(e);
				}
				g.removeVertex(v);
			}
		}
		logger.info(String.format("Graph has %1$s vertices, %2$s edges, density = %3$s, mean degree = %4$s, clustering = %5$s.",
				g.numVertices(),
				g.numEdges(),
				g.numEdges()/((double)(g.numVertices() * (g.numVertices()-1))),
				playground.johannes.statistics.GraphStatistics.getDegreeStatistics(g).getMean(),
				playground.johannes.statistics.GraphStatistics.getClusteringStatistics(g).getMean()));
		logger.info("Saving social network...");
		gmlFile.save(g, args[0]);
		logger.info("Done.");
	}

}
