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

import gnu.trove.TIntArrayList;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntDoubleIterator;

import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.sna.graph.matrix.AdjacencyMatrix;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.johannes.socialnetworks.graph.mcmc.ErgmTerm;
import playground.johannes.socialnetworks.graph.spatial.SpatialAdjacencyMatrix;

/**
 * @author illenberger
 *
 */
public class ErgmTotalDistance extends ErgmTerm {
	
//	private static final Logger logger = Logger.getLogger(ErgmTotalDistance.class);

//	private final double mu;
	
//	private final double sigma;
	
	private final double descretization;
	
//	private boolean linear = false;
	
//	private final double beta;
	
	private final TIntDoubleHashMap c_i;
	
	private long evalErrs = 0;
	
	public ErgmTotalDistance(SpatialAdjacencyMatrix y, double totalDistance, boolean adjustCosts, double descretization) {
		this.descretization = descretization;
		
		if(adjustCosts)
			c_i = calcCi(y, totalDistance);
		else {
			c_i = new TIntDoubleHashMap();
			for(int i = 0; i < y.getVertexCount(); i++)
				c_i.put(i, totalDistance);
		}
	}
	
	private TIntDoubleHashMap calcCi(SpatialAdjacencyMatrix y, double budget) {
		int n = y.getVertexCount();
		double totalBudget = budget * n;
		TIntDoubleHashMap costSums = new TIntDoubleHashMap();
		double totalCosts = 0;
		for(int i = 0; i < n; i++) {
			double sum_i = 0;
			for(int j = 0; j < n; j++) {
				if(i != j) {
					sum_i += descretize(calcDistance(y, i, j));
				}
			}
			costSums.put(i, sum_i);
			totalCosts += sum_i;
		}
		
		double konst = totalBudget/totalCosts;
		TIntDoubleHashMap cost_i = new TIntDoubleHashMap();
		TIntDoubleIterator it = costSums.iterator();
		for(int i = 0; i < costSums.size(); i++) {
			it.advance();
			cost_i.put(it.key(), it.value() * konst * budget);
		}
		
		return cost_i;
	}
	
	@Override
	public double changeStatistic(AdjacencyMatrix y, int i, int j, boolean yIj) {
		double sum_i = 0;
		TIntArrayList u_i = y.getNeighbours(i);
		for(int k = 0; k < u_i.size(); k++) {
			int u = u_i.get(k);
			sum_i += distance2Cost(descretize(calcDistance((SpatialAdjacencyMatrix) y, i, u)));
		}
		
		double sum_j = 0;
		TIntArrayList u_j = y.getNeighbours(j);
		for(int k = 0; k < u_j.size(); k++) {
			int u = u_j.get(k);
			sum_j += distance2Cost(descretize(calcDistance((SpatialAdjacencyMatrix) y, j, u)));
		}
		
		double d = distance2Cost(descretize((calcDistance((SpatialAdjacencyMatrix) y, i, j))));
		
		double D_i_plus = 0;
		double D_i_minus = 0;
		double D_j_plus = 0;
		double D_j_minus = 0;
		
		if(yIj) {
			D_i_plus = sum_i;
			D_i_minus = sum_i - d;
			D_j_plus = sum_j;
			D_j_minus = sum_j - d;
		} else {
			D_i_plus = sum_i + d;
			D_i_minus = sum_i;
			D_j_plus = sum_j + d;
			D_j_minus = sum_j;
		}
		
		double mu = c_i.get(i);
		double sigma = mu/10.0;
		
		double r = 1/evalGauss(D_i_minus, mu, sigma);
		r *= 1/evalGauss(D_j_minus, mu, sigma);
		r *= evalGauss(D_i_plus, mu, sigma);
		r *= evalGauss(D_j_plus, mu, sigma);
		
		
		
		double r2 = - Math.log(r);
		if(Double.isNaN(r2)) {
			evalErrs++;
			if(evalErrs % 1000 == 0)
				System.err.println(evalErrs + " steps that could not be evaluated!");
			
			return Double.POSITIVE_INFINITY;
		}
		return  r2;
	}
	
	private double calcDistance(SpatialAdjacencyMatrix y, int i, int j) {
		Coord c_i = y.getVertex(i).getCoordinate();
		Coord c_j = y.getVertex(j).getCoordinate();
		
		return CoordUtils.calcDistance(c_i, c_j);
	}

	private double evalGauss(double x, double mu, double sigma) {
		return 1/(sigma * Math.sqrt(2*Math.PI)) * Math.exp(-0.5 * Math.pow((x-mu)/sigma, 2));
	}
	
	private double distance2Cost(double d) {
			return Math.log(d);
	}
	
	private double descretize(double d) {
		d = Math.ceil(d/descretization);
		d = Math.max(1, d);
		return d;
	}
}
