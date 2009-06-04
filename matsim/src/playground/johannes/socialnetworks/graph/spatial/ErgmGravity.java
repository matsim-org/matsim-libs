/* *********************************************************************** *
 * project: org.matsim.*
 * ErgmDistance.java
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
package playground.johannes.socialnetworks.graph.spatial;

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TDoubleDoubleIterator;
import gnu.trove.TIntObjectHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.johannes.socialnetworks.graph.mcmc.AdjacencyMatrix;
import playground.johannes.socialnetworks.graph.mcmc.ErgmTerm;

/**
 * @author illenberger
 *
 */
public class ErgmGravity extends ErgmTerm {
	
	private static final Logger logger = Logger.getLogger(ErgmGravity.class);

	private double normBinSize = 1000.0;
	
//	private double scale = 1000.0;
	
	private TIntObjectHashMap<TDoubleDoubleHashMap> normConstants;
		
	public ErgmGravity(SpatialAdjacencyMatrix m) {
		logger.info("Initializing ERGM gravity term...");
		int n = m.getVertexCount();
		normConstants = new TIntObjectHashMap<TDoubleDoubleHashMap>();
		
		for(int i = 0; i < n; i++) {
			TDoubleDoubleHashMap norm_i = new TDoubleDoubleHashMap();
			
			Coord c_i = m.getVertex(i).getCoordinate();
			for(int j = 0; j < n; j++) {
				if(j != i) {
					Coord c_j = m.getVertex(j).getCoordinate();
					double d = CoordUtils.calcDistance(c_i, c_j);
					double bin = getBin(d);
					norm_i.adjustOrPutValue(bin, 1, 1);
				}
			}
			
			TDoubleDoubleIterator it = norm_i.iterator();
			for(int k = 0; k < norm_i.size(); k++) {
				it.advance();
				double a = 2 * Math.PI * it.key() - Math.PI;
				double count = it.value();
				it.setValue(count/a);
			}
			
			if(i % 1000 == 0)
				logger.info((i/(float)n * 100) + " %...");
			
			normConstants.put(i, norm_i);
		}
	}
	
//	public double getScalingFactor() {
//		return scale;
//	}
//
//	public void setScalingFactor(double scale) {
//		this.scale = scale;
//	}

	public double getNormBinSize() {
		return normBinSize;
	}

	public void setNormBinSize(double normBinSize) {
		this.normBinSize = normBinSize;
	}

	private double getBin(double d) {
//		double a = Math.PI * d *d;
//		return Math.ceil(a / area);
		return Math.ceil(d/normBinSize);
	}
	
	@Override
	public double changeStatistic(AdjacencyMatrix m, int i, int j, boolean y_ij) {
		Coord c_i = ((SpatialAdjacencyMatrix)m).getVertex(i).getCoordinate();
		Coord c_j = ((SpatialAdjacencyMatrix)m).getVertex(j).getCoordinate();
		
		double d = getBin(CoordUtils.calcDistance(c_i, c_j));
		
		
		double norm_i = normConstants.get(i).get(d);
		if(norm_i == 0)
			throw new IllegalArgumentException("Norm must no be zero!");
		
		return - getTheta() * (Math.log(1 / (d * d * norm_i)));
	}

}
