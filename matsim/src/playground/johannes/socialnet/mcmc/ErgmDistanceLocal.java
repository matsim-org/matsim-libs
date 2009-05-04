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
import gnu.trove.TIntObjectHashMap;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.johannes.graph.mcmc.AdjacencyMatrix;
import playground.johannes.graph.mcmc.ErgmTerm;
import playground.johannes.socialnet.Ego;

/**
 * @author illenberger
 *
 */
public class ErgmDistanceLocal extends ErgmTerm {
	
	private static final Logger logger = Logger.getLogger(ErgmDistanceLocal.class);

	private double scale = 0.001;
	
	private double normBinSize = 1000;
	
//	private TDoubleDoubleHashMap normConstants;
	
	private TIntObjectHashMap<TDoubleDoubleHashMap> normConstants;
	
	public ErgmDistanceLocal(SNAdjacencyMatrix<?> m) {
		logger.info("Initializing ergm distance term...");
		int n = m.getVertexCount();
		normConstants = new TIntObjectHashMap<TDoubleDoubleHashMap>();
		
		for(int i = 0; i < n; i++) {
			TDoubleDoubleHashMap norm_i = new TDoubleDoubleHashMap();
			Coord c_i = m.getEgo(i).getCoord();
			for(int j = 0; j < n; j++) {
				if(j != i) {
					Coord c_j = m.getEgo(j).getCoord();
					double d = CoordUtils.calcDistance(c_i, c_j);
					double bin = Math.ceil(d / normBinSize);
//					double count = norm_i.get(bin);
//					count++;
					norm_i.adjustOrPutValue(bin, 1, 1);
				}
			}
				
			normConstants.put(i, norm_i);
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
		Coord c_i = ((SNAdjacencyMatrix<?>)m).getEgo(i).getCoord();
		Coord c_j = ((SNAdjacencyMatrix<?>)m).getEgo(j).getCoord();
		
		double d = CoordUtils.calcDistance(c_i, c_j);
		
		double norm_i = normConstants.get(i).get(Math.ceil(d/normBinSize));
		if(norm_i == 0)
			System.err.println("norm = 0");
		norm_i = Math.max(norm_i, 1.0);
		
		double norm_j = normConstants.get(j).get(Math.ceil(d/normBinSize));
		if(norm_j == 0)
			System.err.println("norm = 0");
		norm_j = Math.max(norm_j, 1.0);
		
		double H_1 = - getTheta() * (Math.log(1 / (scale * d * norm_i)) + Math.log(1 / (scale * d * norm_j)));
		
//		double sum_d = 0;
//		for(int u = 0; u < m.getNeighbours(i).size(); u++) {
//			Coord c_u = ((SNAdjacencyMatrix<?>)m).getEgo(m.getNeighbours(i).get(u)).getCoord();
//			double d_iu = CoordUtils.calcDistance(c_i, c_u);
//			sum_d = d_iu;
//		}
//		double d_i_mean = sum_d/(double)m.getNeighbours(i).size();
//		
//		sum_d = 0;
//		for(int u = 0; u < m.getNeighbours(j).size(); u++) {
//			Coord c_u = ((SNAdjacencyMatrix<?>)m).getEgo(m.getNeighbours(j).get(u)).getCoord();
//			double d_ju = CoordUtils.calcDistance(c_j, c_u);
//			sum_d = d_ju;
//		}
//		double d_j_mean = sum_d/(double)m.getNeighbours(j).size();
//		
//		double H_2 = 100*Math.abs(d_i_mean - d_j_mean);
		
		return H_1;// + H_2;
	}

}
