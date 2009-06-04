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
package playground.johannes.socialnetworks.graph.social;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.population.BasicPerson;
import org.matsim.core.api.population.Person;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.johannes.socialnetworks.graph.GraphAnalyser;
import playground.johannes.socialnetworks.graph.social.io.SNGraphMLReader;
import playground.johannes.socialnetworks.graph.spatial.SpatialGraphStatistics;
import playground.johannes.socialnetworks.graph.spatial.SpatialGrid;
import playground.johannes.socialnetworks.graph.spatial.SpatialStatistics;
import playground.johannes.socialnetworks.statistics.Correlations;
import playground.johannes.socialnetworks.statistics.Distribution;

/**
 * @author illenberger
 *
 */
public class SocialNetworkAnalyzer {

	private static final Logger logger = Logger.getLogger(SocialNetworkAnalyzer.class);
	
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
			SocialNetwork<? extends BasicPerson<?>> socialnet,
			String output, boolean extended, SpatialGrid<Double> densityGrid) {
		GraphAnalyser.analyze(socialnet, output, extended);

		double binsize = 10;
		try {
			/*
			 * edge length distribution
			 */
			Distribution edgeLengthDistr = SpatialGraphStatistics.edgeLengthDistribution(socialnet);
			double d_mean = edgeLengthDistr.mean();
			logger.info("Mean edge length is " + d_mean);	
		
			if(output != null) {
				Distribution.writeHistogram(SpatialGraphStatistics.edgeLengthDistribution(socialnet).absoluteDistribution(binsize), output + "edgelength.hist.txt");
				Correlations.writeToFile(SpatialGraphStatistics.edgeLengthDegreeCorrelation(socialnet), output + "edgelength_k.txt", "k", "edge length");
				
				if(densityGrid != null) {
					Correlations.writeToFile(SpatialStatistics.degreeDensityCorrelation(socialnet.getVertices(), densityGrid), output + "k_rho.txt", "density", "k");
					Correlations.writeToFile(SpatialStatistics.clusteringDensityCorrelation(socialnet.getVertices(), densityGrid), output + "c_rho.txt", "density", "c");
					
					Correlations.writeToFile(SpatialStatistics.getDensityCorrelation(
							SpatialGraphStatistics.meanEdgeLength(socialnet), densityGrid, binsize), output + "edgelength_rho", "rho", "mean_edgelength");
				}
				
//				Correlations.writeToFile(SocialNetworkStatistics.edgeLengthMSEDegreeCorrelation(socialnet.getVertices()), output + "edgelenghtMSE_k.txt", "degree", "MSE(d)");
//				
//				TDoubleObjectHashMap<?> kPartitions = Partitions.createDegreePartitions(socialnet.getVertices());
//				for(int k = 5; k < 16; k++) {
//					Set<? extends Ego<? extends BasicPerson<?>>> partition = (Set<? extends Ego<? extends BasicPerson<?>>>) kPartitions.get((double)k);
//					Distribution.writeHistogram(SocialNetworkStatistics.edgeLengthDistribution(partition).absoluteDistribution(10), output + "edgelength.k="+k+".hist.txt");
//				}
//				
//				TDoubleObjectHashMap<?> rhoPartitions = SpatialStatistics.createDensityPartitions(socialnet.getVertices(), densityGrid, 10);
//				for(double rho = 10; rho < 200; rho += 10) {
//					Set<? extends Ego<? extends BasicPerson<?>>> partition = (Set<? extends Ego<? extends BasicPerson<?>>>) rhoPartitions.get(rho);
//					if(partition != null)
//						Distribution.writeHistogram(SocialNetworkStatistics.edgeLengthDistribution(partition).absoluteDistribution(10), output + "edgelength.rho="+rho+".hist.txt");
//				}
				
				double xmin = 8.298482309439372;
				double xmax = 8.848994325098269;
				double ymin = 47.20737601071456;
				double ymax = 47.50074681594523;
//				double xmin = 40;
//				double xmax = 60;
//				double ymin = 40;
//				double ymax = 60;
				Coord c1 = new CoordImpl(xmin, ymin);
				Coord c2 = new CoordImpl(xmax, ymax);
//				CoordinateTransformation trans = new WGS84toCH1903LV03();
//				c1 = trans.transform(c1);
//				c2 = trans.transform(c2);
				
				Set<Ego<?>> egos = new HashSet<Ego<?>>();
				for(Ego<?> e : socialnet.getVertices()) {
					Coord c = e.getCoordinate();
					
					if(c.getX() >= c1.getX() && c.getX() <= c2.getX() && c.getY() >= c1.getY() && c.getY() <= c2.getY()) {
						egos.add(e);
					}
				}
				Distribution.writeHistogram(SpatialGraphStatistics.edgeLengthDistribution(egos).absoluteDistribution(binsize), output + "edgelength.center.hist.txt");
				Correlations.writeToFile(SpatialStatistics.degreeDensityCorrelation(egos, densityGrid), output + "k_rho.center.txt", "density", "k");
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
