/* *********************************************************************** *
 * project: org.matsim.*
 * RBetweenness.java
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.utils.io.IOUtils;
import org.matsim.utils.misc.ExeRunner;

import cern.colt.list.DoubleArrayList;

import playground.johannes.socialnets.GraphStatistics;
import playground.johannes.socialnets.UserDataKeys;
import edu.uci.ics.jung.algorithms.importance.BetweennessCentrality;
import edu.uci.ics.jung.algorithms.importance.NodeRanking;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.UndirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.impl.UndirectedSparseVertex;

/**
 * @author illenberger
 *
 */
public class RBetweenness {

	public static Map<Vertex, Double> calculate(Graph g) {
		try {
			System.out.println("Exporting network data...");
			GraphWriterTable writer = new GraphWriterTable(g);
			writer.write("vertices.txt", "edges.txt");
			
			/*
			 * Run R-Script
			 */
			System.out.println("Executing R-script...");
			ExeRunner.run("R CMD BATCH calcBetweenness.r", "stdout.txt", Integer.MAX_VALUE);
			
			System.out.println("Importing computed data from R...");
			BufferedReader reader = IOUtils.getBufferedReader("scores.txt");
			Map<Vertex, Double> scores = new HashMap<Vertex, Double>();
			String line = reader.readLine();
			int index = 0;
			while((line = reader.readLine()) != null) {
				double score = Double.parseDouble(line.split(" ")[1]);
				scores.put(writer.getVertex(index), score);
				index++;
			}
			
			return scores;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static void main(String[] args) {
//		PersonGraphMLFileHandler fileHandler = new PersonGraphMLFileHandler();
//		GraphMLFile gmlFile = new GraphMLFile(fileHandler);
//		System.out.println("Loading network from file...");
//		Graph g = gmlFile.load(args[0]);
		
		Graph g = new UndirectedSparseGraph();
		Vertex v1 = new UndirectedSparseVertex();
		g.addVertex(v1);
		Vertex v2 = new UndirectedSparseVertex();
		g.addVertex(v2);
		Vertex v3 = new UndirectedSparseVertex();
		g.addVertex(v3);
		
		Edge e1 = new UndirectedSparseEdge(v1, v2);
		g.addEdge(e1);
		Edge e2 = new UndirectedSparseEdge(v3, v2);
		g.addEdge(e2);
		
		System.out.println("Calculating betweenness...");
		long time = System.currentTimeMillis();
		
//		Map<Vertex, Double> scores = calculate(g);
		
		BetweennessCentrality bc = new BetweennessCentrality(g, true, false);
		bc.evaluate();
		
		System.out.println("Done. (" + (System.currentTimeMillis() - time)/1000 + " s)");
		double sum = 0;
		
		List<NodeRanking> rankings = bc.getRankings();
		for(NodeRanking r : rankings)
			sum += r.rankScore;
		
//		for(Double d : scores.values()) {
//			sum += d;
//		}
//		System.out.println("Mean betweenness is " + sum/(double)scores.size() + ".");
		System.out.println("Mean betweenness is " + sum/(double)rankings.size() + ".");
	}
}
