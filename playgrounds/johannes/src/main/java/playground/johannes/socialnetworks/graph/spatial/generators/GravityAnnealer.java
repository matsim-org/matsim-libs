/* *********************************************************************** *
 * project: org.matsim.*
 * GravityBasedAnnealer.java
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
package playground.johannes.socialnetworks.graph.spatial.generators;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.xml.sax.SAXException;

import playground.johannes.socialnetworks.graph.mcmc.Ergm;
import playground.johannes.socialnetworks.graph.mcmc.ErgmDensity;
import playground.johannes.socialnetworks.graph.mcmc.ErgmTerm;
import playground.johannes.socialnetworks.graph.mcmc.ErgmTriangles;
import playground.johannes.socialnetworks.graph.mcmc.GibbsEdgeSwitch;
import playground.johannes.socialnetworks.graph.mcmc.GibbsEdgeFlip;
import playground.johannes.socialnetworks.graph.spatial.SpatialAdjacencyMatrix;
import playground.johannes.socialnetworks.graph.spatial.SpatialSparseGraph;
import playground.johannes.socialnetworks.graph.spatial.SpatialGrid;
import playground.johannes.socialnetworks.graph.spatial.io.SpatialGraphMLReader;

/**
 * @author illenberger
 *
 */
public class GravityAnnealer {

	private static final Logger logger = Logger.getLogger(GravityAnnealer.class);
	
	private static final String MODULE_NAME = "gravityGenerator";
	/**
	 * @param args
	 * @throws IOException 
	 * @throws ParserConfigurationException 
	 * @throws SAXException 
	 */
	public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
		Config config = new Config();
		MatsimConfigReader creader = new MatsimConfigReader(config);
		creader.parse(args[0]);
		
		SpatialGraphMLReader reader = new SpatialGraphMLReader();
		SpatialSparseGraph graph = reader.readGraph(config.findParam(MODULE_NAME, "graphfile"));
		
		GravityAnnealer generator = new GravityAnnealer();
		generator.thetaDensity = Double.parseDouble(config.getParam(MODULE_NAME, "theta_density"));
		generator.thetaTriangle = Double.parseDouble(config.getParam(MODULE_NAME, "theta_triangle"));
		generator.burnin = (long)Double.parseDouble(config.getParam(MODULE_NAME, "burnin"));
//		generator.sampleSize = Integer.parseInt(config.getParam(MODULE_NAME, "samplesize"));
		generator.logInterval = (long)Double.parseDouble(config.getParam(MODULE_NAME, "loginterval"));
		generator.sampleInterval = (long)Double.parseDouble(config.getParam(MODULE_NAME, "sampleinterval"));
		generator.outputDir = config.getParam(MODULE_NAME, "output");
		generator.descretization = Double.parseDouble(config.getParam(MODULE_NAME, "descretization"));
		
		String gridFile = config.findParam(MODULE_NAME, "densityGrid");
		SpatialGrid<Double> densityGrid = null;
		if(gridFile != null)
			densityGrid = SpatialGrid.readFromFile(gridFile);
		
		new File(generator.outputDir).mkdirs();
		generator.generate(graph, densityGrid);
	
	}
	
	private long burnin;
	
//	private int sampleSize;
	
	private long sampleInterval;
	
	private long logInterval;
	
	private String outputDir;
	
	private double thetaDensity;
	
	private final double thetaGravity = 1;
	
	private double thetaTriangle;
	
	private double descretization = 1000.0;
	
	public void generate(SpatialSparseGraph graph, SpatialGrid<Double> densityGrid) {
		SpatialAdjacencyMatrix matrix = new SpatialAdjacencyMatrix(graph);
		/*
		 * Setup ergm terms.
		 */
		Ergm ergm = new Ergm();
		ErgmTerm[] terms = new ErgmTerm[3];
		terms[0] = new ErgmDensity();
		terms[0].setTheta(thetaDensity);
		
		ErgmGravity gravity = new ErgmGravity(matrix, descretization, true, true, thetaGravity); //FIXME
		gravity.setTheta(thetaGravity);
		gravity.setDescretization(descretization);
		terms[1] = gravity;
		
		ErgmTriangles triangles = new ErgmTriangles();
		triangles.setTheta(thetaTriangle);
		terms[2] = triangles;
		
		
		ergm.setErgmTerms(terms);
		/*
		 * Setup gibbs sampler.
		 */
//		GibbsEdgeSwitcher sampler = new GibbsEdgeSwitcher();
		GibbsEdgeSwitch sampler = new GibbsEdgeSwitch();
		sampler.setInterval(1000000);
		
		DumpHandler handler = new DumpHandler(outputDir, null, null);
		handler.setBurnin(burnin);
		handler.setDumpInterval(sampleInterval);
		handler.setLogInterval(logInterval);
		
		logger.info(String.format("Starting gibbs sampler. Burnin time: %1$s iterations.", burnin));
		sampler.sample(matrix, ergm, handler);
		logger.info("Gibbs sampler terminated.");
		logger.info(handler.toString());
	}

}
