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
import gnu.trove.TIntDoubleHashMap;
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
	
	private TIntObjectHashMap<TDoubleDoubleHashMap> z_id;
	
	private TIntDoubleHashMap z_i;
	
	private double d_max;
	
	private double z;
	
	public ErgmGravity(SpatialAdjacencyMatrix m, double descretization) {
		this.descretization = descretization;

		logger.info("Initializing ERGM gravity term...");
		
		z_i = new TIntDoubleHashMap();
		z_id = new TIntObjectHashMap<TDoubleDoubleHashMap>();
		
		int n = m.getVertexCount();
		/*
		 * get the system boundaries
		 */
		logger.info("Determining system boundaries...");
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
		/*
		 * calculate the normalization constant for the distance distribution 
		 */
		logger.info("Calculating normalization constant z_id...");
		for(int i = 0; i < n; i++) {
			TDoubleDoubleHashMap z_d = new TDoubleDoubleHashMap();
			Coord c_i = m.getVertex(i).getCoordinate();
			/*
			 * count the number of vertices for each possible distance
			 */
			for(int j = 0; j < n; j++) {
				if(j != i) {
					Coord c_j = m.getVertex(j).getCoordinate();
					double d = CoordUtils.calcDistance(c_i, c_j);
					double bin = descretize(d);
					z_d.adjustOrPutValue(bin, 1, 1);
				}
			}
			/*
			 * divide the z_d by the circle segment that lays within the system boundaries
			 */
			TDoubleDoubleIterator it = z_d.iterator();
			for(int k = 0; k < z_d.size(); k++) {
				it.advance();
				double r = it.key();
				double count = it.value();
				/*
				 * determine the distances to the system boundaries
				 */
				double dx1 = descretize(c_i.getX() - xmin);//(c_i.getX() - xmin) / descretization;
				double dx2 = descretize(xmax - c_i.getX());//(xmax - c_i.getX()) / descretization;
				double dy1 = descretize(c_i.getY() - ymin);//(c_i.getY() - ymin) / descretization;
				double dy2 = descretize(ymax - c_i.getY());//(ymax - c_i.getY()) / descretization;
				/*
				 * calculate the circle segment for each quadrant
				 */
				double b = calculateCircleSegment(dx2, dy2, r);
				b += calculateCircleSegment(dy1, dx2, r);
				b += calculateCircleSegment(dx1, dy1, r);
				b += calculateCircleSegment(dy2, dx1, r);
				/*
				 * do some checks
				 */
				if (Double.isNaN(b)) {
					throw new IllegalArgumentException("b must not be NaN!");
				} else if(Double.isInfinite(b)) {
					throw new IllegalArgumentException("b must not be infinity!");
				} else if(b == 0) {
					b = 0.001; //TODO: Check this!
				}
				/*
				 * set the new value for z_d
				 */
				it.setValue(count);
			}
			
			if(i % 1000 == 0)
				logger.info((i/(float)n * 100) + " %...");
			
			z_id.put(i, z_d);
		}
		/*
		 * determine normalization constant z_i
		 */
		logger.info("Determining normalization constant z_i...");
		double dx = (xmax - xmin)/2.0;
		double dy = (ymax - ymin)/2.0;
		d_max = descretize(Math.sqrt(dx*dx + dy*dy));
		
		double sum = 0;
		for(int d = 0; d < d_max; d++) {
			sum += 1/(double)(1 + d);
		}
		z = 1/sum;
//		for(int i = 0; i < n; i++) {
//			Coord c_i = m.getVertex(i).getCoordinate();
//			double sum = 0;
//			for(int j = 0; j < n; j++) {
//				if(j != i) {
//					Coord c_j = m.getVertex(j).getCoordinate();
//					double d = descretize(CoordUtils.calcDistance(c_i, c_j));
//					sum += calculateProba(d, i);//1/((1+d) * (z_id.get(i).get(d) ));
//				}
//			}
//			
//			z_i.put(i, 1/sum);
////			z_i.put(i, 1);
//		}
	}
	
	private double calculateCircleSegment(double dx, double dy, double r) {
		if(dx >= r && dy >= r) {
			return Math.PI/2.0;
		} else if (Math.sqrt(dx * dx + dy *dy) <= r){
			return 0.0;
		} else {
			double alpha1 = 0;
			if(dx < r)
				alpha1 = Math.acos(dx/r);
			
			double alpha2 = Math.PI/2.0;
			if(dy < r)
				alpha2 = Math.asin(dy/r);
			
			return Math.abs(alpha2 - alpha1);
		}
	}
	
	public double getDescretization() {
		return descretization;
	}

	public void setDescretization(double descretization) {
		this.descretization = descretization;
	}

	private double descretize(double d) {
//		if(d == 0)
//			return 1;
//		else
			return Math.ceil(d/descretization);
	}
	
	private double calculateProba(double d, int i) {
		double z_d = z_id.get(i).get(d);
		
		if(Double.isNaN(z_d))
			throw new IllegalArgumentException("z_d must not be NaN!");
		else if(Double.isInfinite(z_d))
			throw new IllegalArgumentException("z_d must not be infinity!");
		else if(z_d == 0)
			throw new IllegalArgumentException("z_d must not be zero!");
		
		if(d > d_max)
			return 0;
		else
			return 1 / ( (1 + d) * z_d );
	}
	
	@Override
	public double changeStatistic(AdjacencyMatrix m, int i, int j, boolean y_ij) {
		Coord c_i = ((SpatialAdjacencyMatrix)m).getVertex(i).getCoordinate();
		Coord c_j = ((SpatialAdjacencyMatrix)m).getVertex(j).getCoordinate();
		
		double d = descretize(CoordUtils.calcDistance(c_i, c_j));
		
//		double z = z_i.get(i);
//		
//		if(Double.isNaN(z))
//			throw new IllegalArgumentException("z must not be NaN!");
//		else if(Double.isInfinite(z))
//			throw new IllegalArgumentException("z must not be infinity!");
//		else if(z == 0)
//			throw new IllegalArgumentException("z must not be zero!");
		
		double p = calculateProba(d, i);
		
		return - Math.log(1 / ( 1 / (p * z) - 1 ) );
	}

}
