/* *********************************************************************** *
 * project: org.matsim.*
 * CustomPajekReader.java
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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.utils.io.IOUtils;

import playground.johannes.socialnets.UserDataKeys;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.UndirectedGraph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.UndirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.impl.UndirectedSparseVertex;

/**
 * @author illenberger
 *
 */
public class CustomPajekReader {

	private static final Logger logger = Logger.getLogger(CustomPajekReader.class);
	
	public static UndirectedGraph read(String filename) {
		try {
			logger.info("Loading graph...");
			BufferedReader reader = IOUtils.getBufferedReader(filename);
			UndirectedGraph g = new UndirectedSparseGraph();
			
			String line = null;
			while((line = reader.readLine()) != null) {
				if(line.startsWith("*Vertices"))
					break;
			}
			/*
			 * Load vertices...
			 */
			logger.info("Loading vertices...");
			Map<String, Vertex> vertexMapping = new HashMap<String, Vertex>();
			while((line = reader.readLine()) != null) {
				if(line.startsWith("*Edges"))
					break;
				else {
					String[] tokens = line.split(" ");
					if(tokens.length > 3) {
						Vertex v = new UndirectedSparseVertex();
						vertexMapping.put(tokens[0], v);
						
						v.addUserDatum(UserDataKeys.ID, tokens[1].replaceAll("\"", ""), UserDataKeys.COPY_ACT);
						v.addUserDatum(UserDataKeys.X_COORD, Double.parseDouble(tokens[2]), UserDataKeys.COPY_ACT);
						v.addUserDatum(UserDataKeys.Y_COORD, Double.parseDouble(tokens[3]), UserDataKeys.COPY_ACT);
						
						g.addVertex(v);
					} else {
						logger.warn("Skipped line with invalid vertex definition!");
					}
				}
			}
			/*
			 * Load edges...
			 */
			logger.info("Loading edges...");
			while((line = reader.readLine()) != null) {
				String[] tokens = line.split(" ");
				if(tokens.length > 1) {
					Vertex v1 = vertexMapping.get(tokens[1]); // Ugly Pajek file format!
					Vertex v2 = vertexMapping.get(tokens[2]);
					Edge e = new UndirectedSparseEdge(v1, v2);
					g.addEdge(e);
				} else {
					logger.warn("Skipped line with invalid edge definition!");
				}
			}
			
			logger.info(String.format(
									"Loaded %1$s vertices and %2$s edges. Density = %3$s.",
									g.numVertices(), g.numEdges(), g.numEdges()
									/ (double) (g.numVertices() * (g.numVertices() - 1))));
			return g;
		} catch (IOException e) {
			logger.fatal("IOException!", e);
			return null;
		}
	}
}
