/* *********************************************************************** *
 * project: org.matsim.*
 * LatticeGenerator.java
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

import org.matsim.core.utils.geometry.CoordImpl;

import playground.johannes.socialnetworks.graph.mcmc.Ergm;
import playground.johannes.socialnetworks.graph.mcmc.ErgmDensity;
import playground.johannes.socialnetworks.graph.mcmc.ErgmTerm;
import playground.johannes.socialnetworks.graph.mcmc.GibbsSampler;
import playground.johannes.socialnetworks.graph.spatial.SpatialAdjacencyMatrix;
import playground.johannes.socialnetworks.graph.spatial.SpatialGraph;
import playground.johannes.socialnetworks.graph.spatial.SpatialGraphFactory;
import playground.johannes.socialnetworks.graph.spatial.SpatialGrid;

/**
 * @author illenberger
 *
 */
public class LatticeGenerator {

	public SpatialGraph generate(int width, int height, int gap) {
		SpatialGraphFactory factory = new SpatialGraphFactory();
		SpatialGraph graph = factory.createGraph();
		
		double x_center = width/2.0;
		double y_center = height/2.0;
		
		int step = gap + 1;
		for(int i = 1; i <= width; i += step) {
			for(int k = 1; k <= width; k += step) {
				double dx = Math.abs(x_center - i);
				double dy = Math.abs(y_center - k);
				double d = Math.sqrt(dx*dx + dy*dy);
				if(d <= width/2.0)
					factory.addVertex(graph, new CoordImpl(i, k));
			}
		}
		
		return graph;
	}
	
	public static void main(String args[]) {
		int width = 100;
		SpatialGraph graph = new LatticeGenerator().generate(width, width, 0);
		
		SpatialGrid<Double> grid = new SpatialGrid<Double>(0, 0, width, width, 1);
		for(int i = 0; i < grid.getNumRows(); i++) {
			for(int k = 0; k < grid.getNumCols(i); k++) {
				grid.setValue(i, k, 1.0);
			}
		}
		
		SpatialAdjacencyMatrix matrix = new SpatialAdjacencyMatrix(graph);
		/*
		 * Setup ergm terms.
		 */
		Ergm ergm = new Ergm();
		ErgmTerm[] terms = new ErgmTerm[2];
		terms[0] = new ErgmDensity();
		terms[0].setTheta(0);
		
		ErgmGravity gravity = new ErgmGravity(matrix, 1);
		gravity.setTheta(1);
//		gravity.setDescretization(descretization);
		terms[1] = gravity;
		
//		ErgmPrefAttach attach = new ErgmPrefAttach();
//		attach.setTheta(0.1);
//		terms[2] = attach;
		
		ergm.setErgmTerms(terms);
		/*
		 * Setup gibbs sampler.
		 */
		GibbsSampler sampler = new GibbsSampler();
		sampler.setInterval(1000000);
		
//		Handler handler = new Handler(outputDir, densityGrid);
//		handler.setSampleSize(sampleSize);
//		handler.setSampleInterval(sampleInterval);
		DumpHandler handler = new DumpHandler("/Users/fearonni/vsp-work/work/socialnets/mcmc/output/", grid);
		handler.setBurnin((long)2E8);
		handler.setDumpInterval((long)2E7);
		handler.setLogInterval((long)1E6);
		
//		logger.info(String.format("Starting gibbs sampler. Burnin time: %1$s iterations.", burnin));
		sampler.sample(matrix, ergm, handler);
//		logger.info("Gibbs sampler terminated.");
		
		
	}
}
