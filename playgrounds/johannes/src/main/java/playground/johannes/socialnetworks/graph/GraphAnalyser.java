/* *********************************************************************** *
 * project: org.matsim.*
 * GraphAnalyser.java
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
package playground.johannes.socialnetworks.graph;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;
import java.util.SortedSet;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.io.SparseGraphMLReader;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.core.utils.io.IOUtils;

import playground.johannes.socialnetworks.graph.GraphStatistics.GraphDistance;

/**
 * @author illenberger
 *
 */
public class GraphAnalyser {

	private static final Logger logger = Logger.getLogger(GraphAnalyser.class);
	
	public static final String SUMMARY_FILE = "summary.txt";
	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		String graphfile = args[0];
		String output = null;
		boolean extended = false;
		if(args.length > 1) {
			if(args[1].equals("-e"))
				extended = true;
			else
				output = args[1];
			if(args.length > 2) {
				if(args[2].equals("-e"))
					extended = true;
			}
		}
		logger.info(String.format("Loading graph %1$s...", graphfile));
		Graph g = new SparseGraphMLReader().readGraph(args[0]);
		
		if(!output.endsWith("/"))
			output = output + "/";
		analyze(g, output, extended);
	}
	
	public static void analyze(Graph g, String output, boolean extended) {
		try {
			
		int numEdges = g.getEdges().size();
		int numVertices = g.getVertices().size();
		logger.info(String.format("Loaded graph: %1$s vertices, %2$s edges.", numVertices, numEdges));
		/*
		 * degree
		 */
		Distribution degreeStats = GraphStatistics.degreeDistribution(g);
		double meanDegree = degreeStats.mean();
		logger.info(String.format("Mean degree is %1$s.", meanDegree));
		if(output != null)
			Distribution.writeHistogram(degreeStats.absoluteDistribution(), output + "degree.hist.txt");
		/*
		 * clustering - local
		 */
		Distribution clusteringStats = GraphStatistics.localClusteringDistribution(g.getVertices());
		double c_local = clusteringStats.mean();
		logger.info(String.format("Mean local clustering coefficient is %1$s.", c_local));
		if(output != null)
			Distribution.writeHistogram(clusteringStats.absoluteDistribution(0.05), output + "clustering.hist.txt");
		/*
		 * clustering - global
		 */
		double c_global = GraphStatistics.globalClusteringCoefficient(g);
		logger.info(String.format("Global clustering coefficient is %1$s.", c_global));
		/*
		 * mutuality
		 */
		double mutuality = GraphStatistics.mutuality(g);
		logger.info(String.format("Mutuality is %1$s.", mutuality));
		/*
		 * degree correlation
		 */
		double dcorrelation = GraphStatistics.degreeDegreeCorrelation(g);
		logger.info(String.format("Degree correlation is %1$s.", dcorrelation));
		/*
		 * components
		 */
//		SortedSet<Set<Vertex>> components = Partitions.disconnectedComponents(g); 
//		double numComponents = components.size();
//		logger.info(String.format("Number of disconnected components is %1$s.", numComponents));
//		if(output != null) {
//			Distribution stats = new Distribution();
//			for(Set<Vertex> component : components)
//				stats.add(component.size());
//			Distribution.writeHistogram(stats.absoluteDistribution(), output + "components.hist.txt");
//		}
				
		GraphDistance gDistance = new GraphStatistics.GraphDistance();
		if(extended) {
			gDistance = GraphStatistics.centrality(g);
			logger.info(String.format("Betweenness centrality is %1$s.", gDistance.getGraphBetweenness()));
			logger.info(String.format("Normalized betweenness centrality is %1$s.", gDistance.getGraphBetweennessNormalized()));
			logger.info(String.format("Closeness centrality is %1$s.", gDistance.getGraphCloseness()));
			logger.info(String.format("Diameter is %1$s.", gDistance.getDiameter()));
			logger.info(String.format("Radius is %1$s.", gDistance.getRadius()));
		}
		
		if(output != null) {
			BufferedWriter writer = IOUtils.getBufferedWriter(output + SUMMARY_FILE);
			
			writer.write("numVertices=");
			writer.write(String.valueOf(numVertices));
			writer.newLine();
			
			writer.write("numEdges=");
			writer.write(String.valueOf(numEdges));
			writer.newLine();
			
			writer.write("meanDegree=");
			writer.write(String.valueOf(meanDegree));
			writer.newLine();
			
			writer.write("clustering=");
			writer.write(String.valueOf(c_local));
			writer.newLine();
			
			writer.write("clustering_global=");
			writer.write(String.valueOf(c_global));
			writer.newLine();
			
			writer.write("mutuality=");
			writer.write(String.valueOf(mutuality));
			writer.newLine();
			
			writer.write("dcorrelation=");
			writer.write(String.valueOf(dcorrelation));
			writer.newLine();
			
//			writer.write("components=");
//			writer.write(String.valueOf(numComponents));
//			writer.newLine();

			writer.write("betweenness=");
			writer.write(String.valueOf(gDistance.getGraphBetweenness()));
			writer.newLine();

			writer.write("betweennessNorm=");
			writer.write(String.valueOf(gDistance.getGraphBetweennessNormalized()));
			writer.newLine();

			writer.write("closeness=");
			writer.write(String.valueOf(gDistance.getGraphCloseness()));
			writer.newLine();
			
			writer.write("diameter=");
			writer.write(String.valueOf(gDistance.getDiameter()));
			writer.newLine();

			writer.write("radius=");
			writer.write(String.valueOf(gDistance.getRadius()));
			writer.newLine();

			writer.close();
		}
		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
