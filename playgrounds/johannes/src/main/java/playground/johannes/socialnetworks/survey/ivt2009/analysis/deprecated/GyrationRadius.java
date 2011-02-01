/* *********************************************************************** *
 * project: org.matsim.*
 * GyrationRadius.java
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
package playground.johannes.socialnetworks.survey.ivt2009.analysis.deprecated;

import org.matsim.contrib.sna.graph.spatial.SpatialVertex;

/**
 * @author illenberger
 *
 */
public class GyrationRadius {

	public double radiusOfGyration(SpatialVertex vertex) {
		double dsum = 0;
		
		
		double[] cm = centerMass(vertex);
		double xcm = cm[0];
		double ycm = cm[1];
		
		double dx = vertex.getPoint().getX() - xcm;
		double dy = vertex.getPoint().getY() - ycm;
		double d = Math.sqrt(dx*dx + dy*dy);
		
		dsum += d;
		int cnt = 1;
		for(SpatialVertex neighbor : vertex.getNeighbours()) {
			if(neighbor.getPoint() != null) {
			dx = neighbor.getPoint().getX() - xcm;
			dy = neighbor.getPoint().getY() - ycm;
			d = (dx*dx + dy*dy);
			
			dsum += d;
			cnt++;
			}
		}
		
		return Math.sqrt(dsum/(double)cnt);
	}
	
	private double[] centerMass(SpatialVertex vertex) {
		double xsum = vertex.getPoint().getX();
		double ysum = vertex.getPoint().getY();
		double cnt = 1;
		for(SpatialVertex neighbor : vertex.getNeighbours()) {
			if(neighbor.getPoint() != null) {
			xsum += neighbor.getPoint().getX();
			ysum += neighbor.getPoint().getY();
			cnt++;
			}
		}
		
		
		return new double[]{xsum/cnt, ysum/cnt};
	}
}
