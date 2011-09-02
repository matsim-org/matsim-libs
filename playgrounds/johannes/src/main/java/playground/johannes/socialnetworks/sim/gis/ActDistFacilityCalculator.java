/* *********************************************************************** *
 * project: org.matsim.*
 * ActDistFacilityCalculator.java
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
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;

import playground.johannes.socialnetworks.gis.DistanceCalculator;
import playground.johannes.socialnetworks.gis.OrthodromicDistanceCalculator;

import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class ActDistFacilityCalculator implements ActivityDistanceCalculator {

	private final ActivityFacilities facilities;
	
	private final DistanceCalculator calculator;
	
	public ActDistFacilityCalculator(ActivityFacilities factilities) {
		this(factilities, OrthodromicDistanceCalculator.getInstance());
	}
	
	public ActDistFacilityCalculator(ActivityFacilities factilities, DistanceCalculator calculator) {
		this.facilities = factilities;
		this.calculator = calculator;
	}
	
	@Override
	public double distance(Activity origin, Activity destination) {
		ActivityFacility oFac = facilities.getFacilities().get(origin.getFacilityId());
		ActivityFacility dFac = facilities.getFacilities().get(destination.getFacilityId());
		
		Point p1 = MatsimCoordUtils.coordToPoint(oFac.getCoord());
		Point p2 = MatsimCoordUtils.coordToPoint(dFac.getCoord());
		
		return calculator.distance(p1, p2);
	}

}
