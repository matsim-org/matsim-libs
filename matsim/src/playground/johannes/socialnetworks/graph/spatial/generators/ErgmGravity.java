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
package playground.johannes.socialnetworks.graph.spatial.generators;

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TDoubleDoubleIterator;
import gnu.trove.TIntObjectHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.johannes.socialnetworks.graph.mcmc.AdjacencyMatrix;
import playground.johannes.socialnetworks.graph.mcmc.ErgmTerm;
import playground.johannes.socialnetworks.graph.spatial.SpatialAdjacencyMatrix;

/**
 * @author illenberger
 *
 */
public class ErgmGravity extends ErgmTerm {
	
	private static final Logger logger = Logger.getLogger(ErgmGravity.class);

	private double descretization;
	
	private TIntObjectHashMap<TDoubleDoubleHashMap> normConstants;
	
	public ErgmGravity(SpatialAdjacencyMatrix m, double descretization) {
		this.descretization = descretization;
		logger.info("Initializing ERGM gravity term...");
		int n = m.getVertexCount();
		normConstants = new TIntObjectHashMap<TDoubleDoubleHashMap>();
		
		double xmin = Double.MAX_VALUE;
		double ymin = Double.MAX_VALUE;
		double xmax = - Double.MAX_VALUE;
		double ymax = - Double.MAX_VALUE;
		for(int i = 0; i < n; i++) {
			Coord c_i = m.getVertex(i).getCoordinate();
			xmin = Math.min(xmin, c_i.getX());
			ymin = Math.min(ymin, c_i.getY());
			xmax = Math.max(xmax, c_i.getX());
			ymax = Math.max(ymax, c_i.getY());
		}
		
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
				double r = it.key();
				
				double dx1 = (c_i.getX() - xmin) / descretization;
				double dx2 = (xmax - c_i.getX()) / descretization;
				double dy1 = (c_i.getY() - ymin) / descretization;
				double dy2 = (ymax - c_i.getY()) / descretization;

				double b = calculateCircleSegment(dx2, dy2, r);
				b += calculateCircleSegment(dy1, dx2, r);
				b += calculateCircleSegment(dx1, dy1, r);
				b += calculateCircleSegment(dy2, dx1, r);

				if (Double.isNaN(b)) {
					throw new IllegalArgumentException();

				}
				double a = b * r - b / 2.0;				
				
				double count = it.value();
				if(count == 0) {
					count = 1;
					logger.warn(String.format("No sample in d=%1$s.", it.key()));
				}
				it.setValue(count/a);
			}
			
			if(i % 1000 == 0)
				logger.info((i/(float)n * 100) + " %...");
			
			normConstants.put(i, norm_i);
		}
	}
	
	public double calculateCircleSegment(double dx, double dy, double r) {
		if(dx >= r && dy >= r) {
			return Math.PI/2.0;
		} else if (Math.sqrt(dx * dx + dy *dy) <= r){
			return 0.0;
		} else {
			double alpha1 = 0;
			if(dx < r)
				alpha1 = Math.acos(dx/r);
			
			double alpha2 = 0;
			if(dy < r)
				alpha2 = Math.asin(dy/r);
			
			double alpha = Math.abs(alpha2 - alpha1);
			return Math.PI * alpha / 180.0;
		}
	}
	
	public double getDescretization() {
		return descretization;
	}

	public void setDescretization(double descretization) {
		this.descretization = descretization;
	}

	private double getBin(double d) {
		return Math.ceil(d/descretization);
	}
	
	@Override
	public double changeStatistic(AdjacencyMatrix m, int i, int j, boolean y_ij) {
		Coord c_i = ((SpatialAdjacencyMatrix)m).getVertex(i).getCoordinate();
		Coord c_j = ((SpatialAdjacencyMatrix)m).getVertex(j).getCoordinate();
		
		double d = getBin(CoordUtils.calcDistance(c_i, c_j));
		
		
		double norm_i = normConstants.get(i).get(d);
		if(norm_i == 0) {
			throw new IllegalArgumentException(String.format("Norm must no be zero! i=%1$s, d=%2$s", i, d));
		}
		
		return - getTheta() * (Math.log(1 / (d * d * norm_i)));
	}

}
