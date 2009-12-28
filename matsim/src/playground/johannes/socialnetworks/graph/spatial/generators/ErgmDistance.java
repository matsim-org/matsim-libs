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


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.johannes.socialnetworks.graph.mcmc.AdjacencyMatrix;
import playground.johannes.socialnetworks.graph.mcmc.ErgmTerm;
import playground.johannes.socialnetworks.graph.spatial.SpatialAdjacencyMatrix;

/**
 * @author illenberger
 *
 */
public class ErgmDistance extends ErgmTerm {

	private static final Logger logger = Logger.getLogger(ErgmDistance.class);
	
	private final double gamma;
	
	private final double z;
	
	public ErgmDistance(SpatialAdjacencyMatrix y, double gamma, double k_mean) {
		this.gamma = gamma;
		z = 1;//calcZ(y, k_mean);
	}
	
	@Override
	public double changeStatistic(AdjacencyMatrix y, int i, int j, boolean yIj) {
		double P = z * probability(calcDistance((SpatialAdjacencyMatrix) y, i, j));
		return - Math.log(P);
	}
	
	private double probability(double d_ij) {
		d_ij = Math.ceil(d_ij/1000.0);
		
		d_ij = Math.max(d_ij, 1);
		
		return Math.pow(d_ij, gamma);
	}
	
	private double calcDistance(SpatialAdjacencyMatrix y, int i, int j) {
		Coord c_i = y.getVertex(i).getCoordinate();
		Coord c_j = y.getVertex(j).getCoordinate();
		
		return CoordUtils.calcDistance(c_i, c_j);
	}
	
	private double calcZ(SpatialAdjacencyMatrix y, double k_mean) {
		logger.info("Calculating normalization constant...");
		int n = y.getVertexCount();
		double sum = 0;
		for(int i = 0; i < n; i++) {
			for(int j = (i+1); j < n; j++) {
				sum += probability(calcDistance(y, i, j));
			}
		}
		
		return n * k_mean / sum * 0.5;
	}

}
