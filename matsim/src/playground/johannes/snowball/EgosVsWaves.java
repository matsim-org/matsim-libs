/* *********************************************************************** *
 * project: org.matsim.*
 * EgosVsWaves.java
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

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Plans;
import org.matsim.utils.io.IOUtils;

import playground.johannes.socialnets.PersonGraphMLFileHandler;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.io.GraphMLFile;

/**
 * @author illenberger
 *
 */
public class EgosVsWaves {

	private static final Logger logger = Logger.getLogger(EgosVsWaves.class);
	
	public static void main(String[] args) {
		Config config = new Config();
		config.addCoreModules();
		Gbl.setConfig(config);
		Gbl.createWorld();

		String networkFile = args[0];
		String plansFile = args[1];
		String graphFile = args[2];
		/*
		 * Load the traffic network...
		 */
		logger.info("Loading network...");
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(networkFile);
		Gbl.getWorld().setNetworkLayer(network);
		/*
		 * Load the population...
		 */
		logger.info("Loading plans...");
		Plans plans = new Plans();
		MatsimPlansReader reader = new MatsimPlansReader(plans);
		try {
			reader.parse(plansFile);
			/*
			 * Load the social network...
			 */
			logger.info("Loading social network...");
			PersonGraphMLFileHandler fileHandler = new PersonGraphMLFileHandler(
					plans);
			GraphMLFile gmlFile = new GraphMLFile(fileHandler);
			Graph g = gmlFile.load(graphFile);
			
			int[][] sampledVertices = new int[5][10];
			
			Sampler sampler = new Sampler();
			for(int wave = 1; wave < 6; wave++) {
				for(int egos = 1; egos < 11; egos++) {
					sampler.run(g, wave, egos);
					sampledVertices[wave-1][egos-1] = SampleStatistics.countSampledVertices(g).get("totalSampled");
				}
			}
			
			dump(sampledVertices, "/Users/fearonni/vsp-work/socialnets/devel/snowball/sampledVertices.txt");
		} catch (Exception e) {
			logger.fatal("Exception occured!", e);
		}
	}

	static private void dump(int[][] matrix, String filename) {
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(filename);
			for(int wave = 1; wave < 6; wave++) {
				for(int egos = 1; egos < 11; egos++) {
					writer.write(String.valueOf(matrix[wave-1][egos-1]));
					writer.write("\t");
				}
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			logger.fatal("IOException!", e);
		}
	}
}
