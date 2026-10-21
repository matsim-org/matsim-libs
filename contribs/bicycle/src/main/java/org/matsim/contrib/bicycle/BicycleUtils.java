/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.contrib.bicycle;

import org.matsim.api.core.v01.network.Link;

/**
 * Enumeration of frequently used labels related to bicycles.
 *
 * @author dziemke
 */
public final class BicycleUtils {
	public static final String AVERAGE_ELEVATION = "averageElevation";
	public static final String SURFACE = "surface";
	public static final String SMOOTHNESS = "smoothness";
	//public static final String BICYCLE_INFRA = "bicycle_infra";
	//public static final String CYCLEWAY = "cycleway";
	static final String WAY_TYPE = "type";
	/*package*/ static final String BICYCLE_INFRASTRUCTURE_SPEED_FACTOR = "bicycleInfrastructureSpeedFactor";

	/**
	 * Prefix under which newer network-building tools store raw OSM tag values on links,
	 * e.g. {@code osm:surface} instead of {@code surface}.
	 */
	public static final String OSM_PREFIX = "osm:";

	public enum BicycleInfraAttribute {
		cycleway, bicycle_infra
		// Namen klein, damit existierende XMLs unverändert lesbar bleiben
	}

	private BicycleUtils() {
		// Don't allow to create instances of this class
	}


	//	public static String getBicycleInfrastructureValue(Link link, String bicycleInfraAttribute) {
//		// TODO Simon: use enum instead of string, more stable -> for cycleway vs bicycle_infra or the categories itself CYCLEWAY_ISOLATED ...
//		Object value = link.getAttributes().getAttribute(bicycleInfraAttribute);
//		return value == null ? null : value.toString();
//	}
	public static String getBicycleInfrastructureValue(Link link, BicycleInfraAttribute attr) {
		if (attr == BicycleInfraAttribute.cycleway) {
			// a raw OSM tag, so it may also sit under the osm: prefix; bicycle_infra is derived and never does
			return getCyclewaytype(link);
		}
		Object value = link.getAttributes().getAttribute(attr.name());
		return value == null ? null : value.toString();
	}


	// legacy use; "cycleway" is deprecated
	public static String getCyclewaytype(Link link) {
		return getStringAttribute(link, BicycleInfraAttribute.cycleway.name());
	}

	public static String getSurface(Link link) {
		return getStringAttribute(link, SURFACE);
	}

	public static String getSmoothness(Link link) {
		return getStringAttribute(link, SMOOTHNESS);
	}

	/**
	 * Reads a link attribute that may sit under its plain OSM key (as {@code OsmBicycleReader}
	 * writes it) or under the {@link #OSM_PREFIX} (as newer network-building tools write it),
	 * so networks from either source score the same. The plain key wins.
	 */
	private static String getStringAttribute(Link link, String key) {
		Object value = link.getAttributes().getAttribute(key);
		if (value == null) {
			value = link.getAttributes().getAttribute(OSM_PREFIX + key);
		}
		return (String) value;
	}

	// ===
	public static void setSmoothness(Link link, String smoothness) {
		link.getAttributes().putAttribute(SMOOTHNESS, smoothness);
	}

	public static void setBicycleInfrastructureFactor(Link link, double factor) {
		link.getAttributes().putAttribute(BICYCLE_INFRASTRUCTURE_SPEED_FACTOR, factor);
	}
}
