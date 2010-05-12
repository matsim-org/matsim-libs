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

import playground.johannes.socialnetworks.graph.mcmc.Ergm;
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
		
//		String zonesFile = config.findParam(MODULE_NAME, "zonesFile");
//		String densityFile = config.findParam(MODULE_NAME, "densityFile");

		
//		ZoneLayerDouble zones = null;
//		ZoneLayerLegacy layer = null;

//		if(zonesFile != null && densityFile != null) {
//			layer = ZoneLayerLegacy.createFromShapeFile(zonesFile);
//			zones = ZoneLayerDouble.createFromFile(new HashSet<ZoneLegacy>(layer.getZones()), densityFile);
//		}
		
		long randomSeed = Long.parseLong(config.getParam("global", "randomSeed"));

		double k_mean = Double.parseDouble(config.getParam(MODULE_NAME, "meanDegree"));
		long burnin = (long)Double.parseDouble(config.getParam(MODULE_NAME, "burnin"));
		long logInterval = (long)Double.parseDouble(config.getParam(MODULE_NAME, "loginterval"));
		long sampleInterval = (long)Double.parseDouble(config.getParam(MODULE_NAME, "sampleinterval"));
		String outputDir = config.getParam(MODULE_NAME, "output");
		
		double totalCost = Double.parseDouble(config.getParam(MODULE_NAME, "totalCost"));
//		double beta = Double.parseDouble(config.getParam(MODULE_NAME, "beta"));
//		boolean onlyCost = Boolean.parseBoolean(config.getParam(MODULE_NAME, "onlyCost"));
//		boolean linear = Boolean.parseBoolean(config.getParam(MODULE_NAME, "linear"));
//		boolean adjustCost = Boolean.parseBoolean(config.getParam(MODULE_NAME, "adjustCost"));
//		double descretization = Double.parseDouble(config.getParam(MODULE_NAME, "descretization"));
		
		logger.info("Creating random graph...");
		SpatialSparseGraphBuilder builder = new SpatialSparseGraphBuilder(graph.getCoordinateReferenceSysten());
		ErdosRenyiGenerator<SpatialSparseGraph, SpatialSparseVertex, SpatialSparseEdge> generator = new ErdosRenyiGenerator<SpatialSparseGraph, SpatialSparseVertex, SpatialSparseEdge>(builder);
		int k = (int)k_mean;
		double p = k / (double)graph.getVertices().size();
		generator.generate(graph, p, randomSeed);
//		SpatialGraphMLWriter writer = new SpatialGraphMLWriter();
//		writer.write(graph, "/Users/fearonni/vsp-work/work/socialnets/mcmc/graphk10.graphml");
		/*
		 * convert graph to matrix
		 */
		AdjacencyMatrix<SpatialSparseVertex> y = new AdjacencyMatrix<SpatialSparseVertex>(graph);
		/*
		 * setup ergm terms.
		 */
//		ArrayList<ErgmTerm> terms = new ArrayList<ErgmTerm>();
		
		EdgeCostFunction costFunction = new GravityCostFunction(1.6, 0.0);
		ErgmEdgeCost edgeCost = new ErgmEdgeCost(y, costFunction, totalCost, outputDir + "/thetas.txt");
//		terms.add(edgeCost);
		
		Ergm ergm = new Ergm();
//		ergm.setErgmTerms(terms.toArray(new ErgmTerm[1]));
		ergm.addComponent(edgeCost);
		/*
		 * setup gibbs sampler.
		 */
//		GibbsSampler sampler = new GibbsSampler(randomSeed);
		GibbsSampler sampler = new GibbsEdgeSwitch();
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
