/* *********************************************************************** *
 * project: org.matsim.*
 * IMDBColabNet.java
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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.matsim.utils.io.IOUtils;

import playground.johannes.socialnets.UserDataKeys;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.UndirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.impl.UndirectedSparseVertex;
import edu.uci.ics.jung.io.GraphMLFile;

/**
 * @author illenberger
 *
 */
public class IMDBColabNet {

	private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(IMDBColabNet.class);
	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		Graph g = new UndirectedSparseGraph();
		List<int[]> edges = new LinkedList<int[]>();
		/*
		 * (1) Read movies...
		 */
		logger.info("Reading file...");
		BufferedReader reader = IOUtils.getBufferedReader(args[0]);
		String line;
		int maxId = 0;
		while((line = reader.readLine()) != null) {
			String[] actorIdStr = line.split(" ");
			int ids[] = new int[actorIdStr.length];
			for(int i = 0; i < ids.length; i++) {
				if(!actorIdStr[i].equals("")) {
					int id = Integer.parseInt(actorIdStr[i].trim());
					maxId = Math.max(maxId, id);
					ids[i] = id;
				}
			}
			
			for(int i = 0; i < ids.length; i++) {
				for(int k = i+1; k < ids.length; k++) {
					edges.add(new int[]{ids[i], ids[k]});
				}
			}
		}
		logger.info("Creating network...");
		Vertex[] vertices = new Vertex[maxId+1];
		int count = 0;
		for(int[] edge : edges) {
			Vertex v1 = vertices[edge[0]];
			if(v1 == null) {
				v1 = createVertex(edge[0]);
				vertices[edge[0]] = v1;
				g.addVertex(v1);
			}
			Vertex v2 = vertices[edge[1]];
			if(v2 == null) {
				v2 = createVertex(edge[1]);
				vertices[edge[1]] = v2;
				g.addVertex(v2);
			}
			
			try {
				Edge e = new UndirectedSparseEdge(v1, v2);
				g.addEdge(e);
				
			} catch (Exception e) {
//				logger.warn("Failed to insert edge...");
			}
			
			count++;
			if(count % 10000 == 0)
				logger.info(String.format("Inserted %1$s edges - %2$s %%", count, count/(double)edges.size()));
		}
		
		
		logger.info(String.format("Network has %1$s veritces and %2$s edges.", g.numVertices(), g.numEdges()));
		
		logger.info("Saving network...");
		new GraphMLFile().save(g, args[1]);
		logger.info("Done.");
	}

	private static Vertex createVertex(int id) {
		Vertex v = new UndirectedSparseVertex();
		v.setUserDatum(UserDataKeys.ID, id, UserDataKeys.COPY_ACT);
		v.setUserDatum(UserDataKeys.X_COORD, Math.random(), UserDataKeys.COPY_ACT);
		v.setUserDatum(UserDataKeys.Y_COORD, Math.random(), UserDataKeys.COPY_ACT);
		return v;
	}
}
