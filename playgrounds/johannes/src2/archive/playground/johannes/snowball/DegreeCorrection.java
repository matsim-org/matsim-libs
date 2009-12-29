/* *********************************************************************** *
 * project: org.matsim.*
 * DegreeCorrection.java
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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
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
public class DegreeCorrection {

	private static Logger logger = Logger.getLogger(DegreeCorrection.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		logger.info("Loading social network from file...");
		PersonGraphMLFileHandler fileHandler = new PersonGraphMLFileHandler();
		GraphMLFile gmlFile = new GraphMLFile(fileHandler);
		Graph g = gmlFile.load(args[0]);
		
		
		final int targetMeanDegree = Integer.parseInt(args[2]);
		
		Map<Vertex, Integer> targetDegrees = new LinkedHashMap<Vertex, Integer>();
		Set<Vertex> vertices = new LinkedHashSet<Vertex>(g.getVertices());
		int meanDegree = 0;
		for(Vertex v : vertices) {
			meanDegree += v.degree();
		}
		meanDegree = meanDegree/vertices.size();
		
		logger.info(String.format("n=%1$s, m=%2$s, k=%3$s.", g.numVertices(), g.numEdges(), meanDegree));
		
		double factor = targetMeanDegree/(double)meanDegree;
		for(Vertex v : vertices) {
			targetDegrees.put(v, (int) (v.degree() * factor));
		}
		
		logger.info("Removing edges...");
		
		for(Vertex v : vertices) {
			while(v.degree() > targetDegrees.get(v) && v.degree() > 1) {
				g.removeEdge((Edge) v.getIncidentEdges().iterator().next());
			}
		}
		
		for(Vertex v : vertices) {
			if(v.degree() == 0)
				g.removeVertex(v);
		}
		
		meanDegree = 0;
		for(Vertex v : vertices) {
			meanDegree += v.degree();
		}
		meanDegree = meanDegree/vertices.size();
		logger.info(String.format("n=%1$s, m=%2$s, k=%3$s.", g.numVertices(), g.numEdges(), meanDegree));
		
		logger.info("Saving network...");
		gmlFile.save(g, args[1]);
		logger.info("Done.");
	}

}
