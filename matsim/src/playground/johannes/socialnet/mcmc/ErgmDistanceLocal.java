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

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.johannes.graph.mcmc.AdjacencyMatrix;
import playground.johannes.graph.mcmc.ErgmTerm;

/**
 * @author illenberger
 *
 */
public class ErgmDistanceLocal extends ErgmTerm {
	
	private static final Logger logger = Logger.getLogger(ErgmDistanceLocal.class);

	private double scale = 0.001;
	
	private double normBinSize = 500;
	
	private double area = 1000 * 1000 * Math.PI;
	
	private TIntObjectHashMap<TDoubleDoubleHashMap> normConstants;
		
	private int totalVertices;
	
	public ErgmDistanceLocal(SNAdjacencyMatrix<?> m) {
		logger.info("Initializing ergm distance term...");
		int n = m.getVertexCount();
		totalVertices = n;
		normConstants = new TIntObjectHashMap<TDoubleDoubleHashMap>();
		
		for(int i = 0; i < n; i++) {
			TDoubleDoubleHashMap norm_i = new TDoubleDoubleHashMap();
			
			Coord c_i = m.getEgo(i).getCoord();
			for(int j = 0; j < n; j++) {
				if(j != i) {
					Coord c_j = m.getEgo(j).getCoord();
					double d = CoordUtils.calcDistance(c_i, c_j);
//					double a = Math.PI * d *d;
					double bin = getBin(d);
					norm_i.adjustOrPutValue(bin, 1, 1);
				}
			}
			
			if(i % 1000 == 0)
				logger.info((i/(float)n * 100) + " %...");
			
			normConstants.put(i, norm_i);
		}
	}
	

	
//	public ErgmDistanceLocal(SNAdjacencyMatrix<?> m) {
//		logger.info("Initializing ergm distance term...");
//		int n = m.getVertexCount();
//		normConstants = new TIntObjectHashMap<TDoubleDoubleHashMap>();
//		
//		for(int i = 0; i < n; i++) {
//			TDoubleDoubleHashMap norm_i = new TDoubleDoubleHashMap();
//			Coord c_i = m.getEgo(i).getCoord();
//			for(int j = 0; j < n; j++) {
//				if(j != i) {
//					Coord c_j = m.getEgo(j).getCoord();
//					double d = CoordUtils.calcDistance(c_i, c_j);
//					double bin = Math.ceil(d / normBinSize);
////					double count = norm_i.get(bin);
////					count++;
//					norm_i.adjustOrPutValue(bin, 1, 1);
//				}
//			}
//			
//			if(i % 1000 == 0)
//				logger.info((i/(float)n * 100) + " %...");
//			
//			normConstants.put(i, norm_i);
//		}
//	}
	
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

	private double getBin(double d) {
		double a = Math.PI * d *d;
		return Math.ceil(a / area);
	}
	
	@Override
	public double changeStatistic(AdjacencyMatrix m, int i, int j, boolean y_ij) {
		Coord c_i = ((SNAdjacencyMatrix<?>)m).getEgo(i).getCoord();
		Coord c_j = ((SNAdjacencyMatrix<?>)m).getEgo(j).getCoord();
		
		double d = CoordUtils.calcDistance(c_i, c_j);
		
		double norm_i = normConstants.get(i).get(getBin(d));
		if(norm_i == 0)
			System.err.println("norm = 0");
		norm_i = Math.max(norm_i, 1.0);
		norm_i = norm_i/(double)totalVertices;
		
		double norm_j = normConstants.get(j).get(getBin(d));
		if(norm_j == 0)
			System.err.println("norm = 0");
		norm_j = Math.max(norm_j, 1.0);
		norm_j = norm_j/(double)totalVertices;
		
		double H_1 = - getTheta() * (Math.log(1 / (scale * d * norm_i)) + Math.log(1 / (scale * d * norm_j)));
	
		
		return H_1;// + H_2;
	}

}
