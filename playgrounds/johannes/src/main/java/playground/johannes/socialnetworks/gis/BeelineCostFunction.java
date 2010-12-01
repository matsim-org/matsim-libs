/* *********************************************************************** *
 * project: org.matsim.*
 * BeelineCostFunction.java
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

import org.matsim.contrib.sna.math.Discretizer;
import org.matsim.contrib.sna.math.LinearDiscretizer;


import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class BeelineCostFunction implements SpatialCostFunction {

	private DistanceCalculator calculator = new OrthodromicDistanceCalculator();
	
	private Discretizer discretizer = new LinearDiscretizer(1000.0);
	
	public void setDistanceCalculator(DistanceCalculator calculator) {
		this.calculator = calculator;
	}
	
	@Override
	public double costs(Point p1, Point p2) {
		return discretizer.index(calculator.distance(p1, p2));
	}

}
