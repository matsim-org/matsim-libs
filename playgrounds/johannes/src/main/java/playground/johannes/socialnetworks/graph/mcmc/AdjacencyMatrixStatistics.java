/* *********************************************************************** *
 * project: org.matsim.*
 * AdjacencyMatrixStatistics.java
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
package playground.johannes.socialnetworks.graph.mcmc;

import org.matsim.contrib.sna.graph.matrix.AdjacencyMatrix;

/**
 * @author illenberger
 *
 */
public class AdjacencyMatrixStatistics {

	public static double getDensity(AdjacencyMatrix y) {
		int N = y.getVertexCount();
		return 2 * y.countEdges()/ (double)(N * (N - 1));
	}
	
	public static double getMeanDegree(AdjacencyMatrix y) {
		int N = y.getVertexCount();
		int sum = 0;
		for(int i = 0; i < N; i++) {
			sum += y.getNeighbours(i).size();
		}
		
		return sum/(double)N;
	}
	
	public static double getLocalClusteringCoefficient(AdjacencyMatrix y) {
		int N = y.getVertexCount();
		double sum = 0;
		for(int i = 0; i < N; i++) {
			int k = y.getNeighbours(i).size();
			if(k > 1)
				sum += 2 * y.countTriangles(i) / (double)(k * (k-1));
		}
		return sum / (double)N;
	}
	
	public static double getGlobalClusteringCoefficient(AdjacencyMatrix y) {
		int N = y.getVertexCount();
		int triangles = 0;
		int tripples = 0;
		for(int i = 0; i < N; i++) {
			triangles += y.countTriangles(i);
			tripples += y.countTriples(i);
		}
		return triangles/(double)tripples;
	}
}
