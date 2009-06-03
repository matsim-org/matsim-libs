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
package playground.johannes.socialnet.mcmc;

import gnu.trove.TDoubleDoubleHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.johannes.graph.mcmc.AdjacencyMatrix;
import playground.johannes.graph.mcmc.ErgmTerm;

/**
 * @author illenberger
 *
 */
public class ErgmDistance extends ErgmTerm {
	
	private static final Logger logger = Logger.getLogger(ErgmDistance.class);

	private double scale = 0.001;
	
	private double normBinSize = 1000;
	
	private TDoubleDoubleHashMap normConstants;
	
	public ErgmDistance(SNAdjacencyMatrix<?> m) {
		logger.info("Initializing ergm distance term...");
		int n = m.getVertexCount();
		normConstants = new TDoubleDoubleHashMap();
		
		for(int i = 0; i < n; i++) {
			Coord c_i = m.getEgo(i).getCoordinate();
			for(int j = i+1; j < n; j++) {
					Coord c_j = m.getEgo(j).getCoordinate();
					double d = CoordUtils.calcDistance(c_i, c_j);
					double bin = Math.ceil(d / normBinSize);
					double count = normConstants.get(bin);
					count++;
					normConstants.put(bin, count);
			}
		}
	}
	
	public double getScalingFactor() {
		return scale;
	}

	public void setScalingFactor(double scale) {
		this.scale = scale;
	}

	public double getNormBinSize() {
		return normBinSize;
	}

	public void setNormBinSize(double normBinSize) {
		this.normBinSize = normBinSize;
	}

	@Override
	public double changeStatistic(AdjacencyMatrix m, int i, int j, boolean y_ij) {
		Coord c_i = ((SNAdjacencyMatrix<?>)m).getEgo(i).getCoordinate();
		Coord c_j = ((SNAdjacencyMatrix<?>)m).getEgo(j).getCoordinate();
		
		double d = CoordUtils.calcDistance(c_i, c_j);
		double norm = normConstants.get(Math.ceil(d/normBinSize));
		norm = Math.max(norm, 1.0);
		return - getTheta() * Math.log(1 / (scale * d * norm));
	}

}
