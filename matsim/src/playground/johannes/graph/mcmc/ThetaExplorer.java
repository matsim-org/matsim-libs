/* *********************************************************************** *
 * project: org.matsim.*
 * ThetaExplorer.java
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
package playground.johannes.graph.mcmc;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import playground.johannes.graph.Graph;
import playground.johannes.graph.GraphStatistics;
import playground.johannes.graph.PlainGraph;
import playground.johannes.graph.SparseEdge;
import playground.johannes.graph.SparseVertex;
import playground.johannes.graph.generators.ErdosRenyiGenerator;
import playground.johannes.graph.generators.PlainGraphFactory;

/**
 * @author illenberger
 *
 */
public class ThetaExplorer {

	private static final double theta_min = -1;
	
	private static final double theta_max = 1;
	
	private static final double theta_step = 0.1;
	
	private static final int burninTime = (int)1E8;
	
	private static final int N = 1000;
	
	public static void main(String[] args) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(args[0]));
		writer.write("theta_density\ttheta_twostars\ttheta_triangles\tm\t<k>\tc\tsigma_2");
		writer.newLine();
		
		ErdosRenyiGenerator<PlainGraph, SparseVertex, SparseEdge> generator = new ErdosRenyiGenerator<PlainGraph, SparseVertex, SparseEdge>(new PlainGraphFactory());
		PlainGraph graph = generator.generate(N, 0.01, 0);
		
		dump(graph);
		
		GibbsSampler sampler = new GibbsSampler(0);
		
		Ergm ergm = new Ergm();
		
		ErgmTerm[] terms = new ErgmTerm[3];
		terms[0] = new ErgmDensity();
		terms[1] = new ErgmTwoStars();
		terms[2] = new ErgmTriangles();
		
		ergm.setErgmTerms(terms);
		
		for(double theta_density = theta_min; theta_density <= theta_max; theta_density += theta_step) {
			for(double theta_twostars = theta_min; theta_twostars <= theta_max; theta_twostars += theta_step) {
				for(double theta_triangles = theta_min; theta_triangles <= theta_max; theta_triangles += theta_step) {
					terms[0].setTheta(theta_density);
					terms[1].setTheta(theta_twostars);
					terms[2].setTheta(theta_triangles);
					
					AdjacencyMatrix m = new AdjacencyMatrix(graph);
					
					System.out.println(String.format("Simulation with theta_density=%1$s, theta_twostars=%2$s, theta_triangles=%3$s", theta_density, theta_twostars, theta_triangles));
					
					sampler.sample(m, ergm, burninTime);
					
					Graph g = m.getGraph(new PlainGraphFactory());
					dump(g);
					
					int edges = g.getEdges().size();
					double k = GraphStatistics.getDegreeStatistics(g).getMean();
					double c = GraphStatistics.getClusteringStatistics(g).getMean();
					int sigma_2 = GraphStatistics.getNumTwoStars(g);
					
					writer.write(String.valueOf((float)theta_density));
					writer.write("\t");
					writer.write(String.valueOf((float)theta_twostars));
					writer.write("\t");
					writer.write(String.valueOf((float)theta_triangles));
					writer.write("\t");
					writer.write(String.valueOf(edges));
					writer.write("\t");
					writer.write(String.valueOf((float)k));
					writer.write("\t");
					writer.write(String.valueOf((float)c));
					writer.write("\t");
					writer.write(String.valueOf(sigma_2));
					writer.newLine();
					writer.flush();
				}
			}
		}
		
		
		
	}

	private static void dump(Graph g) {
		System.out.println(String.format("m=%1$s, <k>=%2$s, c=%3$s, 2-stars=%4$s",
				g.getEdges().size(),
				GraphStatistics.getDegreeStatistics(g).getMean(),
				GraphStatistics.getClusteringStatistics(g).getMean(),
				GraphStatistics.getNumTwoStars(g)));
	}
}
