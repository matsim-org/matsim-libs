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
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * Helper class for commonly used operations on TransitSchedules
 *
 * @author Thibaut Dubernet, gleich
 *
 */
public final class TransitScheduleUtils {

	public final static String ACCESSTIME_ATTRIBUTE = "accessTime";
	public final static String EGRESSTIME_ATTRIBUTE = "egressTime";
	private TransitScheduleUtils() {
	}

	public static double getStopAccessTime(TransitStopFacility stopFacility){
		Object accessTime = stopFacility.getAttributes().getAttribute(ACCESSTIME_ATTRIBUTE);
		return accessTime!=null?(double) accessTime:0.0;
	}

	public static void setStopAccessTime(TransitStopFacility stopFacility, double stopAccessTime){
		stopFacility.getAttributes().putAttribute(ACCESSTIME_ATTRIBUTE,stopAccessTime);
	}

	public static double getStopEgressTime(TransitStopFacility stopFacility){
		Object egressTime = stopFacility.getAttributes().getAttribute(EGRESSTIME_ATTRIBUTE);
		return egressTime!=null?(double) egressTime:0.0;
	}
	public static void setStopEgressTime(TransitStopFacility stopFacility, double stopEgressTime){
		stopFacility.getAttributes().putAttribute(EGRESSTIME_ATTRIBUTE,stopEgressTime);
	}

	public static void setSymmetricStopAccessEgressTime(TransitStopFacility stopFacility, double stopAccessEgressTime){
		setStopAccessTime(stopFacility,stopAccessEgressTime);
		setStopEgressTime(stopFacility,stopAccessEgressTime);
	}

	public static QuadTree<TransitStopFacility> createQuadTreeOfTransitStopFacilities(TransitSchedule transitSchedule) {
		return createQuadTreeOfTransitStopFacilities(transitSchedule.getFacilities().values());
	}

	public static QuadTree<TransitStopFacility> createQuadTreeOfTransitStopFacilities(Collection<TransitStopFacility> transitStopFacilities) {
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
