/* *********************************************************************** *
 * project: org.matsim.*
 * DistanceCalculatorFactory.java
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

import org.geotools.referencing.cs.DefaultCartesianCS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * @author illenberger
 *
 */
public class DistanceCalculatorFactory {

	public static DistanceCalculator createDistanceCalculator(CoordinateReferenceSystem crs) {
		if(crs.getCoordinateSystem() instanceof DefaultCartesianCS) {
			return CartesianDistanceCalculator.getInstance();
		} else {
			return OrthodromicDistanceCalculator.getInstance();
		}
	}
}