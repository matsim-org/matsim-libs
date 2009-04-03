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
package playground.johannes.graph;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.IOUtils;

import playground.johannes.graph.GraphStatistics.GraphDistance;
import playground.johannes.graph.io.PlainGraphMLReader;

/**
 * @author illenberger
 *
 */
public class GraphAnalyser {

	private static final Logger logger = Logger.getLogger(GraphAnalyser.class);
	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		logger.info("Loading graph " + args[0] + "...");
		Graph g = new PlainGraphMLReader().readGraph(args[0]);
		Gbl.printMemoryUsage();
		
		int numEdges = g.getEdges().size();
		int numVertices = g.getVertices().size();
		logger.info(String.format("Loaded graph: %1$s vertices, %2$s edges.", numVertices, numEdges));

		double meanDegree = GraphStatistics.getDegreeStatistics(g).getMean();
		logger.info(String.format("Mean degree is %1$s.", meanDegree));
		
//		long time = System.currentTimeMillis();
		double c_local = GraphStatistics.getClusteringStatistics(g).getMean();
		logger.info(String.format("Mean local clustering coefficient is %1$s.", c_local));
		
		double c_global = GraphStatistics.getGlobalClusteringCoefficient(g);
		logger.info(String.format("Global clustering coefficient is %1$s.", c_global));
//		System.out.println(System.currentTimeMillis() - time);
		
//		TObjectDoubleHashMap<Vertex> cc = (TObjectDoubleHashMap<Vertex>) GraphStatistics.getClustringCoefficients(g);
//		TDoubleDoubleHashMap map = new TDoubleDoubleHashMap();
//		TIntIntHashMap degrees = new TIntIntHashMap();
//		for(Vertex v : g.getVertices()) {
//			double c = cc.get(v);
//			map.adjustOrPutValue(v.getNeighbours().size(), c, c);
//			degrees.adjustOrPutValue(v.getNeighbours().size(), 1, 1);
//		}
//		for(int k : degrees.keys()) {
//			double sum = map.get(k);
//			map.put(k, sum/(double)degrees.get(k));
//		}
//		WeightedStatistics.writeHistogram(map, "/Volumes/hertz:ils-raid/socialnets/cluster.hist.txt");
		
		double mutuality = GraphStatistics.getMutuality(g);
		logger.info(String.format("Mutuality is %1$s.", mutuality));
		
		double dcorrelation = GraphStatistics.getDegreeCorrelation(g);
		logger.info(String.format("Degree correlation is %1$s.", dcorrelation));
		
		double numComponents = GraphStatistics.getComponents(g).size();
		logger.info(String.format("Number of disconnected components is %1$s.", numComponents));
		
//		time = System.currentTimeMillis();
		
//		System.out.println(System.currentTimeMillis() - time);
		
		boolean extended = false;
		for(String arg : args) {
			if(arg.equalsIgnoreCase("-e")) {
				extended = true;
				break;
			}
		}
		
		GraphDistance gDistance = new GraphStatistics.GraphDistance();
		if(extended) {
			gDistance = GraphStatistics.getCentrality(g);
			logger.info(String.format("Betweenness centrality is %1$s.", gDistance.getGraphBetweenness()));
			logger.info(String.format("Normalized betweenness centrality is %1$s.", gDistance.getGraphBetweennessNormalized()));
			logger.info(String.format("Closeness centrality is %1$s.", gDistance.getGraphCloseness()));
			logger.info(String.format("Diameter is %1$s.", gDistance.getDiameter()));
			logger.info(String.format("Radius is %1$s.", gDistance.getRadius()));
		}
		
		if(args.length > 1) {
			BufferedWriter writer = IOUtils.getBufferedWriter(args[1]);
			
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
			
			writer.write("components=");
			writer.write(String.valueOf(numComponents));
			writer.newLine();

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
	}
}
