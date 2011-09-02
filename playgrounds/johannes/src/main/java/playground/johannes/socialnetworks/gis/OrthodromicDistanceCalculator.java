/* *********************************************************************** *
 * project: org.matsim.*
 * OrthodromicDistanceCalculator.java
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
package playground.johannes.socialnetworks.gis;

import org.geotools.geometry.jts.JTS;
import org.matsim.contrib.sna.gis.CRSUtils;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 * 
 */
public class OrthodromicDistanceCalculator implements DistanceCalculator {

	private static OrthodromicDistanceCalculator instance;
	
	public static OrthodromicDistanceCalculator getInstance() {
		if(instance == null)
			instance = new OrthodromicDistanceCalculator();
		return instance;
	}

	@Override
	public double distance(Point p1, Point p2) {
		if (p1.getSRID() == p2.getSRID()) {
			if (p1.getSRID() == 0) {
				return CartesianDistanceCalculator.getInstance().distance(p1, p2);
			} else {
				try {
					return JTS.orthodromicDistance(p1.getCoordinate(), p2.getCoordinate(),
							CRSUtils.getCRS(p1.getSRID()));
				} catch (TransformException e) {
					e.printStackTrace();
					return Double.NaN;
				}
			}
		} else {
			throw new RuntimeException("Incompatible coordinate reference systems.");
		}
	}

}
