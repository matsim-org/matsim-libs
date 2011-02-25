/* *********************************************************************** *
 * project: org.matsim.*
 * EdgePowerLawDistance.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

import gnu.trove.TIntDoubleHashMap;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.matrix.AdjacencyMatrix;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.math.Discretizer;
import org.matsim.contrib.sna.math.LinearDiscretizer;
import org.matsim.contrib.sna.util.ProgressLogger;

import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.gis.DistanceCalculator;

/**
 * @author illenberger
 *
 */
public class EdgePowerLawDistance implements EdgeProbabilityFunction {
	
	private static final Logger logger = Logger.getLogger(EdgePowerLawDistance.class);

	private AdjacencyMatrix<? extends SpatialVertex> y;
	
	private DistanceCalculator distanceCalculator;
	
	private Discretizer discretizer;
	
//	private double konst;
	
	private TIntDoubleHashMap konsts = new TIntDoubleHashMap();
	
	private final double gamma;
	
	public EdgePowerLawDistance(AdjacencyMatrix<? extends SpatialVertex> y, double gamma, double mExpect) {
		this.y = y;
		this.gamma = gamma;
		discretizer = new LinearDiscretizer(1.0);
		distanceCalculator = new CartesianDistanceCalculator();
//		
//		konst = 1;
//		double sum = 0;
//		for(int i = 0; i < y.getVertexCount(); i++) {
//			for(int j = i+1; j < y.getVertexCount(); j++) {
//				sum += probability(i, j);
//			}
//		}
//		
//		konst = mExpect/sum;
//		konst = 1;
		
		logger.info("Calculating norm constants...");
		ProgressLogger.init(y.getVertexCount(), 1, 5);
		for(int i = 0; i < y.getVertexCount(); i++) {
			double sum = 0;
			for(int j = 0; j < y.getVertexCount(); j++) {
				double d = distanceCalculator.distance(y.getVertex(i).getPoint(), y.getVertex(j).getPoint());
				d = discretizer.index(d);
				d = Math.max(1.0, d);
				sum += Math.pow(d, gamma);
			}
			if(Double.isInfinite(sum))
				logger.warn("Infinity");
			else if(Double.isNaN(sum))
				logger.warn("NaN");
			
			konsts.put(i, 5000/sum);
			ProgressLogger.step();
		}
	}
	
	@Override
	public double probability(int i, int j) {
		double d = distanceCalculator.distance(y.getVertex(i).getPoint(), y.getVertex(j).getPoint());
		d = discretizer.index(d);
		d = Math.max(1.0, d);
		
		return konsts.get(i) * Math.pow(d, gamma);
	}

}
