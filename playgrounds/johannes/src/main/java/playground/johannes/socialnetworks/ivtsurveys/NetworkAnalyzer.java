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
package playground.johannes.socialnetworks.ivtsurveys;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;

import playground.johannes.socialnetworks.graph.GraphAnalyser;
import playground.johannes.socialnetworks.graph.social.SocialNetwork;
import playground.johannes.socialnetworks.graph.social.io.SNGraphMLReader;
import playground.johannes.socialnetworks.graph.spatial.SpatialGraphStatistics;
import playground.johannes.socialnetworks.graph.spatial.SpatialGrid;
import playground.johannes.socialnetworks.statistics.Correlations;
import playground.johannes.socialnetworks.statistics.Distribution;

/**
 * @author illenberger
 *
 */
public class NetworkAnalyzer {

	private static final Logger logger = Logger.getLogger(NetworkAnalyzer.class);
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		String graphfile = args[1];
		String gridfile = null;
		String output = null;
		boolean extended = false;
		if(args.length > 2) {
			if(args[2].equals("-e"))
				extended = true;
			else
				gridfile = args[2];
			if(args.length > 3) {
				if(args[3].equals("-e"))
					extended = true;
				else
					output = args[3];
				
				if(args.length > 4) {
					if(args[4].equals("-e"))
						extended = true;	
				}
			}
		}
		
		logger.info(String.format("Loading graph %1$s...", graphfile));
		SocialNetwork<Person> g = SNGraphMLReader.loadFromConfig(args[0], graphfile);
		
		if(!output.endsWith("/"))
			output = output + "/";
		
		SpatialGrid<Double> grid = null;
		if(gridfile != null)
			grid = SpatialGrid.readFromFile(gridfile);
		
		analyze(g, output, extended, grid);
		
	}

	public static void analyze(
			SocialNetwork<? extends Person> socialnet,
			String output, boolean extended, SpatialGrid<Double> densityGrid) {
		GraphAnalyser.analyze(socialnet, output, extended);

		try {
			/*
			 * edge length distribution
			 */
			Distribution edgeLengthDistr = SpatialGraphStatistics.edgeLengthDistribution(socialnet);
			double d_mean = edgeLengthDistr.mean();
			logger.info("Mean edge length is " + d_mean);	
		
			if(output != null) {
				Distribution.writeHistogram(SpatialGraphStatistics.edgeLengthDistribution(socialnet).absoluteDistribution(1000), output + "edgelength.hist.txt");
				Correlations.writeToFile(SpatialGraphStatistics.edgeLengthDegreeCorrelation(socialnet), output + "edgelength_k.txt", "k", "edge length");
				
				if(densityGrid != null) {
					Correlations.writeToFile(SpatialGraphStatistics.degreeDensityCorrelation(socialnet.getVertices(), densityGrid), output + "k_rho.txt", "density", "k");
					Correlations.writeToFile(SpatialGraphStatistics.clusteringDensityCorrelation(socialnet.getVertices(), densityGrid), output + "c_rho.txt", "density", "c");
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
