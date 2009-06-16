/* *********************************************************************** *
 * project: org.matsim.*
 * SocialNetworkAnalyzer.java
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

/**
 * 
 */
package playground.johannes.socialnetworks.graph.spatial;

import gnu.trove.TDoubleObjectHashMap;
import gnu.trove.TDoubleObjectIterator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

import org.apache.log4j.Logger;

import playground.johannes.socialnetworks.graph.GraphAnalyser;
import playground.johannes.socialnetworks.graph.Partitions;
import playground.johannes.socialnetworks.graph.spatial.io.SpatialGraphMLReader;
import playground.johannes.socialnetworks.statistics.Correlations;
import playground.johannes.socialnetworks.statistics.Distribution;

/**
 * @author illenberger
 *
 */
public class SpatialGraphAnalyzer {

	private static final Logger logger = Logger.getLogger(SpatialGraphAnalyzer.class);
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		String graphfile = args[0];
		String gridfile = null;
		String output = null;
		boolean extended = false;
		if(args.length > 1) {
			if(args[1].equals("-e"))
				extended = true;
			else
				gridfile = args[1];
			if(args.length > 2) {
				if(args[2].equals("-e"))
					extended = true;
				else
					output = args[2];
				
				if(args.length > 3) {
					if(args[3].equals("-e"))
						extended = true;	
				}
			}
		}
		
		logger.info(String.format("Loading graph %1$s...", graphfile));
		SpatialGraphMLReader reader = new SpatialGraphMLReader();
		SpatialGraph g = reader.readGraph(graphfile);
		
		if(!output.endsWith("/"))
			output = output + "/";
		
		SpatialGrid<Double> grid = null;
		if(gridfile != null)
			grid = SpatialGrid.readFromFile(gridfile);
		
		analyze(g, output, extended, grid);
		
	}

	@SuppressWarnings("unchecked")
	public static void analyze(SpatialGraph graph, String output, boolean extended, SpatialGrid<Double> densityGrid) {
		GraphAnalyser.analyze(graph, output, extended);

		double binsize = 1000.0;
		if(densityGrid != null)
			binsize = densityGrid.getResolution();
		
		try {
			/*
			 * edge length distribution
			 */
			Distribution edgeLengthDistr = SpatialGraphStatistics.edgeLengthDistribution(graph);
			double d_mean = edgeLengthDistr.mean();
			logger.info("Mean edge length is " + d_mean);	
		
			if(output != null) {
				/*
				 * edge length histogram
				 */
				Distribution.writeHistogram(edgeLengthDistr.absoluteDistribution(binsize), output + "edgelength.hist.txt");
				/*
				 * edge length degree correlation
				 */
				Correlations.writeToFile(SpatialGraphStatistics.edgeLengthDegreeCorrelation(graph), output + "edgelength_k.txt", "k", "edgelength");
				/*
				 * density correlations
				 */
				if(densityGrid != null) {
					Correlations.writeToFile(SpatialGraphStatistics.degreeDensityCorrelation(
							graph.getVertices(), densityGrid), output + "k_rho.txt", "density", "k");
					Correlations.writeToFile(SpatialGraphStatistics.clusteringDensityCorrelation(
							graph.getVertices(), densityGrid), output + "c_rho.txt", "density", "c");
					Correlations.writeToFile(SpatialGraphStatistics.densityCorrelation(
							SpatialGraphStatistics.meanEdgeLength(graph), densityGrid, binsize), output + "edgelength_rho.txt", "rho", "mean_edgelength");
				}
				/*
				 * degree partitions
				 */
				TDoubleObjectHashMap<?> kPartitions = Partitions.createDegreePartitions(graph.getVertices());
				TDoubleObjectIterator<?> it = kPartitions.iterator();
				String patitionOutput = output + "/kPartitions"; 
				new File(patitionOutput).mkdirs();
				for(int i = 0; i < kPartitions.size(); i++) {
					it.advance();
					String filename = String.format("%1$s/edgelength.%2$s.hist.txt", patitionOutput, (int)it.key());
					Distribution.writeHistogram(SpatialGraphStatistics.edgeLengthDistribution((Set<SpatialVertex>)it.value()).absoluteDistribution(binsize), filename);
				}
				/*
				 * density partitions
				 */			
				TDoubleObjectHashMap<?> rhoPartitions = SpatialGraphStatistics.createDensityPartitions(graph.getVertices(), densityGrid, binsize);
				it = rhoPartitions.iterator();
				patitionOutput = output + "/rhoPartitions"; 
				new File(patitionOutput).mkdirs();
				for(int i = 0; i < rhoPartitions.size(); i++) {
					it.advance();
					String filename = String.format("%1$s/edgelength.%2$s.hist.txt", patitionOutput, (int)it.key());
					Distribution.writeHistogram(SpatialGraphStatistics.edgeLengthDistribution((Set<SpatialVertex>)it.value()).absoluteDistribution(binsize), filename);
				}
			}
			
			if (output != null) {
				BufferedWriter writer = new BufferedWriter(new FileWriter(output + GraphAnalyser.SUMMARY_FILE, true));
				writer.write("mean edge length=");
				writer.write(Double.toString(d_mean));
				writer.newLine();
				writer.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
