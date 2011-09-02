/* *********************************************************************** *
 * project: org.matsim.*
 * ActDistCoordCalculator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.sim.gis;

import org.matsim.api.core.v01.population.Activity;

import playground.johannes.socialnetworks.gis.DistanceCalculator;
import playground.johannes.socialnetworks.gis.OrthodromicDistanceCalculator;

/**
 * @author illenberger
 *
 */
public class ActDistCoordCalculator implements ActivityDistanceCalculator {

	private DistanceCalculator calculator;
	
	public ActDistCoordCalculator() {
		this(OrthodromicDistanceCalculator.getInstance());
	}
	
	public ActDistCoordCalculator(DistanceCalculator calculator) {
		this.calculator = calculator;
	}
	
	@Override
	public double distance(Activity origin, Activity destination) {
		return calculator.distance(MatsimCoordUtils.coordToPoint(origin.getCoord()), MatsimCoordUtils.coordToPoint(destination.getCoord()));
	}

}
