/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.dziemke.cemdapMatsimCadyts.mmoyo.analysis.stopZoneOccupancyAnalysis;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public final class FacilityUtils{
	final static char POINT = '.';
	
	private FacilityUtils(){} // do not instantiate

	/**
	 * Converts the id of a stop facility into a stop Zone. (it is the same id, just without the point and suffix)
	 */
	public static Id<TransitStopFacility> convertFacilitytoZoneId(Id<TransitStopFacility> facId) {
		String str = getStrUntilPoint(facId.toString());
		return Id.create(str, TransitStopFacility.class);
	}

	static String getStrUntilPoint(String origStr){
		int pointIndex = origStr.indexOf(POINT);
		return origStr.substring(0, pointIndex > -1? pointIndex: origStr.length());
	}
}
