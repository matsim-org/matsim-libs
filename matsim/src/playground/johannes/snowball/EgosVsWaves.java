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
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.config.Config;
import org.matsim.controler.ScenarioData;
import org.matsim.gbl.Gbl;
import org.matsim.utils.io.IOUtils;

import playground.johannes.socialnets.GraphStatistics;
import playground.johannes.socialnets.PersonGraphMLFileHandler;
import edu.uci.ics.jung.algorithms.cluster.WeakComponentClusterer;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.io.GraphMLFile;

/**
 * @author illenberger
 * 
 */
public class EgosVsWaves {

	private static final Logger logger = Logger.getLogger(EgosVsWaves.class);

	public static void main(String[] args) {
		Config config = Gbl.createConfig(args);
//		ScenarioData data = new ScenarioData(config);

		final String MODULE_NAME = "snowballsampling";
		String graphFile = config.getParam(MODULE_NAME, "graphFile");
		// String graphPajekFile = config.getParam(MODULE_NAME,
		// "graphPajekFile");
		// String sampledPajekFile = config.getParam(MODULE_NAME,
		// "sampledPajekFile");

		/*
		 * Load the social network...
		 */
		logger.info("Loading social network...");
		PersonGraphMLFileHandler fileHandler = new PersonGraphMLFileHandler();
		GraphMLFile gmlFile = new GraphMLFile(fileHandler);
		Graph g = gmlFile.load(graphFile);

		System.out.println("Graph has " + g.numVertices() +" vertices, " + g.numEdges() + " edges, cluster coefficient: " +
				GraphStatistics.meanClusterCoefficient(g) + ", mean degree: " + GraphStatistics.meanDegree(g) + ", components: " +
				new WeakComponentClusterer().extract(g).size());
		
		String outputDir = "/Users/fearonni/vsp-work/socialnets/devel/snowball/";
		
		int seeds = 10;
		int waves = 3;
		float[][] sampledVertices = new float[waves][seeds];
		float[][] verticesSampledTwice = new float[waves][seeds];
		float[][] sampledEdges = new float[waves][seeds];
		float[][] sampledEdgesTwice = new float[waves][seeds];
		float[][] degrees = new float[waves][seeds];
		float[][] clusterCoefficients = new float[waves][seeds];
		float[][] weakComponents = new float[waves][seeds];

		Sampler sampler = new Sampler();
		for (int wave = 1; wave < waves+1; wave++) {
			for (int egos = 1; egos < seeds+1; egos++) {
				sampler.run(g, wave, egos);
				
				Map<String, Integer> sampledVerticesMap = SampleStatistics
				.countSampledVertices(g);
				
				int vertices = sampledVerticesMap.get("totalSampled");
				sampledVertices[wave - 1][egos - 1] = vertices / (float)g.numVertices();
				
				int multiple = sampledVerticesMap.get("totalMultipleSampled");
				verticesSampledTwice[wave - 1][egos - 1] = multiple/(float)vertices;
				
				Map<String, Integer> sampledEdgesMap = SampleStatistics.countSampledEdges(g);
				
				int edges = sampledEdgesMap.get("totalSampled");
				sampledEdges[wave-1][egos-1] = edges/(float)g.numEdges();
				
				int multipleEdges = sampledEdgesMap.get("totalMultipleSampled");
				sampledEdgesTwice[wave-1][egos-1] = multipleEdges/(float)edges;
				
//				Graph extGraph = sampler.extractSampledGraph(g, true);
				Graph reducedGraph = sampler.extractSampledGraph(g, false);
				
				degrees[wave-1][egos-1] = (float) GraphStatistics.meanDegreeSampled(reducedGraph);				
				weakComponents[wave-1][egos-1] = new WeakComponentClusterer().extract(reducedGraph).size();
				sampler.removeDeadEnds(reducedGraph);
				clusterCoefficients[wave-1][egos-1] = (float) GraphStatistics.meanClusterCoefficientSampled(reducedGraph);
			}
		}

		dump(sampledVertices, seeds, waves, outputDir + "sampledVertices.txt");
		dump(verticesSampledTwice, seeds, waves, outputDir + "sampledVerticesTwice.txt");
		dump(sampledEdges, seeds, waves, outputDir + "sampledEdges.txt");
		dump(sampledEdgesTwice, seeds, waves, outputDir + "sampledEdgesTwice.txt");
		dump(degrees, seeds, waves, outputDir + "sampledMeanDegree.txt");
		dump(clusterCoefficients, seeds, waves, outputDir + "sampledClusterCoef.txt");
		dump(weakComponents, seeds, waves, outputDir + "sampledComponents.txt");

	}

	static private void dump(float[][] matrix, int seeds, int waves, String filename) {
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(filename);
			for (int wave = 1; wave < waves+1; wave++) {
				for (int egos = 1; egos < seeds+1; egos++) {
					writer.write(String.valueOf(matrix[wave - 1][egos - 1]));
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
