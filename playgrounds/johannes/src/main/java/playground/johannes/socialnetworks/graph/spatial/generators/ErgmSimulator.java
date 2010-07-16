/* *********************************************************************** *
 * project: org.matsim.*
 * KMLWriter.java
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
package playground.johannes.socialnetworks.graph.spatial.generators;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.graph.generators.ErdosRenyiGenerator;
import org.matsim.contrib.sna.graph.matrix.AdjacencyMatrix;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseEdge;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraphBuilder;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseVertex;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.xml.sax.SAXException;

import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.graph.mcmc.Ergm;
import playground.johannes.socialnetworks.graph.mcmc.ErgmDensity;
import playground.johannes.socialnetworks.graph.mcmc.GibbsEdgeSwitch;
import playground.johannes.socialnetworks.graph.mcmc.GibbsSampler;
import playground.johannes.socialnetworks.graph.spatial.analysis.EdgeCostsTask;
import playground.johannes.socialnetworks.graph.spatial.io.Population2SpatialGraph;

/**
 * @author illenberger
 *
 */
public class ErgmSimulator {
	
	private static final Logger logger = Logger.getLogger(ErgmSimulator.class);

	private static final String MODULE_NAME = "gravityGenerator";
	/**
	 * @param args
	 * @throws IOException 
	 * @throws ParserConfigurationException 
	 * @throws SAXException 
	 */
	public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException {
		Config config = new Config();
		MatsimConfigReader creader = new MatsimConfigReader(config);
		creader.parse(args[0]);
		
		Population2SpatialGraph reader = new Population2SpatialGraph(CRSUtils.getCRS(21781));
		SpatialSparseGraph graph = reader.read(config.findParam("plans", "inputPlansFile"));
		
		long randomSeed = Long.parseLong(config.getParam("global", "randomSeed"));

		double k_mean = Double.parseDouble(config.getParam(MODULE_NAME, "meanDegree"));
		long burnin = (long)Double.parseDouble(config.getParam(MODULE_NAME, "burnin"));
		long logInterval = (long)Double.parseDouble(config.getParam(MODULE_NAME, "loginterval"));
		long sampleInterval = (long)Double.parseDouble(config.getParam(MODULE_NAME, "sampleinterval"));
		String outputDir = config.getParam(MODULE_NAME, "output");
		
		double totalCost = Double.parseDouble(config.getParam(MODULE_NAME, "totalCost"));
		double theta_edge = Double.parseDouble(config.getParam(MODULE_NAME, "thetaEdge"));
		double gamma = Double.parseDouble(config.getParam(MODULE_NAME, "gamma"));
		
		logger.info("Creating random graph...");
		SpatialSparseGraphBuilder builder = new SpatialSparseGraphBuilder(graph.getCoordinateReferenceSysten());
		ErdosRenyiGenerator<SpatialSparseGraph, SpatialSparseVertex, SpatialSparseEdge> generator = new ErdosRenyiGenerator<SpatialSparseGraph, SpatialSparseVertex, SpatialSparseEdge>(builder);
		int k = (int)k_mean;
		double p = k / (double)graph.getVertices().size();
		generator.setRandomDrawMode(true);
		generator.generate(graph, p, randomSeed);
		/*
		 * convert graph to matrix
		 */
		AdjacencyMatrix<SpatialSparseVertex> y = new AdjacencyMatrix<SpatialSparseVertex>(graph);
		/*
		 * setup ergm terms.
		 */
		logger.info("Initializing ERGM...");
		EdgeCostFunction costFunction = new GravityEdgeCostFunction(gamma, 0.0, new CartesianDistanceCalculator());
		ErgmEdgeCost edgeCost = new ErgmEdgeCost(y, costFunction, totalCost, outputDir + "/thetas.txt", theta_edge);
//		ErgmEdgeCost2 edgeCost = new ErgmEdgeCost2(y, 1.6, 60, outputDir + "/thetas.txt", theta_edge);
		
		ErgmDensity density = new ErgmDensity();
		density.setTheta(theta_edge);
		
		Ergm ergm = new Ergm();
		ergm.addComponent(density);
		ergm.addComponent(edgeCost);
		/*
		 * setup gibbs sampler.
		 */
//		GibbsSampler sampler = new GibbsEdgeSwitch(randomSeed);
		GibbsSampler sampler = new GibbsSampler(randomSeed);
		sampler.setInterval(1000000);
		
		DumpHandler handler = new DumpHandler(graph, builder, outputDir);
		handler.getAnalyzerTaks().addTask(new EdgeCostsTask(costFunction));
		handler.setBurnin(burnin);
		handler.setDumpInterval(sampleInterval);
		handler.setLogInterval(logInterval);
		handler.analyze(y, 0);
		logger.info(String.format("Starting gibbs sampler. Burnin time: %1$s iterations.", burnin));
		sampler.sample(y, ergm, handler);
		logger.info("Gibbs sampler terminated.");
	}

	
}
