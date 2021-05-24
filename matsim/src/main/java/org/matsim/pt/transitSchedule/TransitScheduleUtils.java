/* *********************************************************************** *
 * project: org.matsim.*
 * TransitScheduleUtils
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.pt.transitSchedule;

import java.util.Collection;

import org.matsim.core.utils.collections.QuadTree;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * Helper class for commonly used operations on TransitSchedules
 * 
 * @author Thibaut Dubernet, gleich
 *
 */
public final class TransitScheduleUtils {
	// Logic gotten from PopulationUtils, but I am actually a bit unsure about the value of those methods now that
	// attributable is the only way to get attributes...

	public static Object getStopFacilityAttribute(TransitStopFacility facility, String key) {
		return facility.getAttributes().getAttribute( key );
	}

	public static void putStopFacilityAttribute(TransitStopFacility facility, String key, Object value ) {
		facility.getAttributes().putAttribute( key, value ) ;
	}

	public static Object removeStopFacilityAttribute( TransitStopFacility facility, String key ) {
		return facility.getAttributes().removeAttribute( key );
	}

	public static Object getLineAttribute(TransitLine facility, String key) {
		return facility.getAttributes().getAttribute( key );
	}

	public static void putLineAttribute(TransitLine facility, String key, Object value ) {
		facility.getAttributes().putAttribute( key, value ) ;
	}

	public static Object removeLineAttribute( TransitLine facility, String key ) {
		return facility.getAttributes().removeAttribute( key );
	}
	
	public final static QuadTree<TransitStopFacility> createQuadTreeOfTransitStopFacilities(TransitSchedule transitSchedule) {
		return createQuadTreeOfTransitStopFacilities(transitSchedule.getFacilities().values());
	}
	
	public final static QuadTree<TransitStopFacility> createQuadTreeOfTransitStopFacilities(Collection<TransitStopFacility> transitStopFacilities) {
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		for (TransitStopFacility stopFacility : transitStopFacilities) {
			double x = stopFacility.getCoord().getX();
			double y = stopFacility.getCoord().getY();

			if (x < minX)
				minX = x;
			if (y < minY)
				minY = y;
			if (x > maxX)
				maxX = x;
			if (y > maxY)
				maxY = y;
		}
		QuadTree<TransitStopFacility> stopsQT = new QuadTree<>(minX, minY, maxX, maxY);
		for (TransitStopFacility stopFacility : transitStopFacilities) {
			double x = stopFacility.getCoord().getX();
			double y = stopFacility.getCoord().getY();
			stopsQT.put(x, y, stopFacility);
		}
		return stopsQT;
	}
}
