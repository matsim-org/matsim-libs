/* *********************************************************************** *
 * project: org.matsim.*
 * DigicoreNodeCoordinateTransformer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.southafrica.freight.digicore.algorithms.complexNetwork;

import java.util.Map;

import org.apache.commons.collections15.Transformer;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacility;

public class DigicoreNodeCoordinateTransformer implements Transformer<Id<ActivityFacility>, String> {
	private Map<Id<ActivityFacility>, Coord> map;
	
	public DigicoreNodeCoordinateTransformer(Map<Id<ActivityFacility>, Coord> coordinateMap) {
		this.map = coordinateMap;
	}

	@Override
	public String transform(Id<ActivityFacility> facilityId) {
		return String.format("[%.2f ; %.2f]", map.get(facilityId).getX(), map.get(facilityId).getY());
	}


}

