/* *********************************************************************** *
 * project: org.matsim.*
 * GravityBasedGenerator.java
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.xml.sax.SAXException;

import playground.johannes.socialnetworks.graph.mcmc.Ergm;
import playground.johannes.socialnetworks.graph.mcmc.ErgmDensity;
import playground.johannes.socialnetworks.graph.mcmc.ErgmTerm;
import playground.johannes.socialnetworks.graph.mcmc.ErgmTriangles;
import playground.johannes.socialnetworks.graph.mcmc.GibbsEdgeFlip;
import playground.johannes.socialnetworks.graph.mcmc.GibbsSampler;
import playground.johannes.socialnetworks.graph.spatial.SpatialAdjacencyMatrix;
import playground.johannes.socialnetworks.graph.spatial.SpatialSparseGraph;
import playground.johannes.socialnetworks.graph.spatial.io.Population2SpatialGraph;
import playground.johannes.socialnetworks.graph.spatial.io.SpatialGraphMLReader;
import playground.johannes.socialnetworks.spatial.TravelTimeMatrix;
import playground.johannes.socialnetworks.spatial.Zone;
import playground.johannes.socialnetworks.spatial.ZoneLayer;
import playground.johannes.socialnetworks.spatial.ZoneLayerDouble;

/**
 * @author illenberger
 *
 */
public class GravityGenerator {

	private static final Logger logger = Logger.getLogger(GravityGenerator.class);
	
	private static final String MODULE_NAME = "gravityGenerator";
	
	public static void main(String[] args) throws FileNotFoundException, IOException, SAXException, ParserConfigurationException {
		Config config = new Config();
		MatsimConfigReader creader = new MatsimConfigReader(config);
		creader.parse(args[0]);
		
		SpatialSparseGraph graph = null;
		String graphFile = config.findParam(MODULE_NAME, "graphfile");
		
		if(graphFile == null) {
			Population2SpatialGraph reader = new Population2SpatialGraph();
			graph = reader.read(config.findParam("plans", "inputPlansFile"));
		} else {
			SpatialGraphMLReader reader = new SpatialGraphMLReader();
			graph = reader.readGraph(graphFile);
		}

		GravityGenerator generator = new GravityGenerator();
		generator.randomSeed = Long.parseLong(config.getParam("global", "randomSeed"));
		
		generator.thetaDensity = Double.parseDouble(config.getParam(MODULE_NAME, "theta_density"));
		generator.thetaTriangle = Double.parseDouble(config.getParam(MODULE_NAME, "theta_triangle"));
		generator.k_mean = Double.parseDouble(config.getParam(MODULE_NAME, "meanDegree"));
		generator.burnin = (long)Double.parseDouble(config.getParam(MODULE_NAME, "burnin"));

		generator.logInterval = (long)Double.parseDouble(config.getParam(MODULE_NAME, "loginterval"));
		generator.sampleInterval = (long)Double.parseDouble(config.getParam(MODULE_NAME, "sampleinterval"));
		generator.outputDir = config.getParam(MODULE_NAME, "output");
		generator.descretization = Double.parseDouble(config.getParam(MODULE_NAME, "descretization"));
		
		generator.reweightBoundaries = Boolean.parseBoolean(config.getParam(MODULE_NAME, "boundaries"));
		generator.reweightDensity = Boolean.parseBoolean(config.getParam(MODULE_NAME, "popdensity"));
		
//		String gridFile = config.findParam(MODULE_NAME, "densityGrid");
//		SpatialGrid<Double> densityGrid = null;
//		if(gridFile != null)
//			densityGrid = SpatialGrid.readFromFile(gridFile);
		String zonesFile = config.findParam(MODULE_NAME, "zonesFile");
		String densityFile = config.findParam(MODULE_NAME, "densityFile");
		String ttmatrixFile = config.findParam(MODULE_NAME, "ttmatrixFile");
		
		ZoneLayerDouble zones = null;
		ZoneLayer layer = null;
		TravelTimeMatrix ttmatrix = null;
		if(zonesFile != null && densityFile != null) {
			layer = ZoneLayer.createFromShapeFile(zonesFile);
			zones = ZoneLayerDouble.createFromFile(new HashSet<Zone>(layer.getZones()), densityFile);
			
			ttmatrix = TravelTimeMatrix.createFromFile(new HashSet<Zone>(layer.getZones()), ttmatrixFile);
		}
	
		generator.ttmatrix = ttmatrix;
		
		new File(generator.outputDir).mkdirs();
		
		String type = config.getParam(MODULE_NAME, "randomWalk");
		generator.generate(graph, zones, RandomWalkType.valueOf(type));

	}

	private long burnin;
	
	private long sampleInterval;
	
	private long logInterval;
	
	private String outputDir;
	
	private double thetaDensity;
	
	private double thetaTriangle;
	
	private double k_mean;
	
	private double descretization = 1000.0;
	
	private long randomSeed;
	
	private boolean reweightDensity = true;
	
	private boolean reweightBoundaries = true;
	
	private TravelTimeMatrix ttmatrix;
	
	public void generate(SpatialSparseGraph graph, ZoneLayerDouble zones, RandomWalkType type) {
		/*
		 * convert graph to matrix
		 */
		SpatialAdjacencyMatrix matrix = new SpatialAdjacencyMatrix(graph);
		/*
		 * setup ergm terms.
		 */
		
		ArrayList<ErgmTerm> terms = new ArrayList<ErgmTerm>();
		ErgmTerm density = new ErgmDensity();
		density.setTheta(thetaDensity);
		terms.add(density);
		
		ErgmGravity gravity = new ErgmGravity(matrix, descretization, reweightBoundaries, reweightDensity, k_mean); //FIXME
//		gravity.setTheta(k_mean);
//		gravity.setReweightBoundaries(reweightBoundaries);
//		gravity.setReweightDensity(reweightDensity);
		terms.add(gravity);
		
		if (type == RandomWalkType.flip) {
			ErgmTriangles triangle = new ErgmTriangles();
			triangle.setTheta(thetaTriangle);
			terms.add(triangle);
		}
		
		Ergm ergm = new Ergm();
		ergm.setErgmTerms(terms.toArray(new ErgmTerm[1]));
		/*
		 * setup gibbs sampler.
		 */
		GibbsSampler sampler = null;
		if(type == RandomWalkType.insert)
			sampler = new GibbsSampler(randomSeed);
		else if(type == RandomWalkType.flip)
			sampler = new GibbsEdgeFlip(randomSeed);
		
		sampler.setInterval(1000000);
		
		DumpHandler handler = new DumpHandler(outputDir, zones, ttmatrix);
		handler.setBurnin(burnin);
		handler.setDumpInterval(sampleInterval);
		handler.setLogInterval(logInterval);
		
		logger.info(String.format("Starting gibbs sampler. Burnin time: %1$s iterations.", burnin));
		sampler.sample(matrix, ergm, handler);
		logger.info("Gibbs sampler terminated.");
		
	}
	
	public static enum RandomWalkType {
		insert, flip
	}
}
