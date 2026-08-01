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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;

/**
 * Enumeration of frequently used labels related to bicycles.
 *
 * @author dziemke
 */
public final class BicycleUtils {
	public static final String SURFACE = "surface";
	public static final String SMOOTHNESS = "smoothness";
	public static final String CYCLEWAY = "cycleway";
	static final String WAY_TYPE = "type";
	/*package*/ static final String BICYCLE_INFRASTRUCTURE_SPEED_FACTOR = "bicycleInfrastructureSpeedFactor";

	// ---- attributes written by the network tools in org.matsim.contrib.bicycle.network ----
	// Their keys are snake_case, following what network-from-sumo already writes on the same
	// networks (allowed_speed, restricted_lanes).

	/** Cycling infrastructure category, holding a {@code BicycleInfraCategory} name. */
	public static final String BICYCLE_INFRA = "bicycle_infra";

	/**
	 * Set to {@code true} on links whose OSM ways were merged into one edge but classify
	 * differently, so their {@link #BICYCLE_INFRA} fell back to {@code NEEDS_CLARIFICATION}.
	 */
	public static final String BICYCLE_INFRA_MIXED = "bicycle_infra_mixed";

	/** Mean elevation over the link in m; written for inspection, not consumed by the simulation. */
	public static final String AVERAGE_ELEVATION = "average_elevation";

	/** Signed end-to-end gradient as a ratio, e.g. {@code +0.03} for 3 % uphill. */
	public static final String GRADIENT = "gradient";

	/** Steepest gradient on any sub-segment, as a ratio. */
	public static final String MAX_GRADIENT = "max_gradient";

	/** Cumulative meters climbed along the link. */
	public static final String ELEVATION_GAIN = "elevation_gain";

	/** Cumulative meters descended along the link, as a positive number. */
	public static final String ELEVATION_LOSS = "elevation_loss";

	/**
	 * Prefix under which the network-building tools in {@code org.matsim.contrib.bicycle.network}
	 * store raw OSM tag values on links, e.g. {@code osm:surface}.
	 */
	public static final String OSM_PREFIX = "osm:";

	private BicycleUtils() {
		// Don't allow to create instances of this class
	}
	public static String getCyclewaytype( Link link ){
		return getStringAttribute( link, CYCLEWAY );
	}

	public static String getSurface( Link link ){
		return getStringAttribute( link, SURFACE );
	}

	/** The cycling infrastructure category name, or {@code null} on an unclassified link. */
	public static String getBicycleInfra( Link link ){
		return (String) link.getAttributes().getAttribute( BICYCLE_INFRA );
	}

	// The elevation getters return null when the link carries no metrics — no DEM was
	// supplied, the DEM had no data there, or the link sat outside the bicycle area.

	public static Double getAverageElevation( Link link ){
		return (Double) link.getAttributes().getAttribute( AVERAGE_ELEVATION );
	}

	public static Double getGradient( Link link ){
		return (Double) link.getAttributes().getAttribute( GRADIENT );
	}

	public static Double getMaxGradient( Link link ){
		return (Double) link.getAttributes().getAttribute( MAX_GRADIENT );
	}

	public static Double getElevationGain( Link link ){
		return (Double) link.getAttributes().getAttribute( ELEVATION_GAIN );
	}

	public static Double getElevationLoss( Link link ){
		return (Double) link.getAttributes().getAttribute( ELEVATION_LOSS );
	}

	/**
	 * Reads a link attribute that may sit under its plain OSM key (as {@code OsmBicycleReader}
	 * writes it) or under the {@code osm:} prefix (as the network tools in
	 * {@code org.matsim.contrib.bicycle.network} write it), so networks from either source
	 * score the same. The plain key wins; the tools move the attribute rather than copy it,
	 * so a network never carries both.
	 */
	private static String getStringAttribute( Link link, String key ){
		Object value = link.getAttributes().getAttribute( key );
		if ( value == null ){
			value = link.getAttributes().getAttribute( OSM_PREFIX + key );
		}
		return (String) value;
	}

	// ===
	public static void setSmoothness( Link link, String smoothness ){
		link.getAttributes().putAttribute( SMOOTHNESS, smoothness );
	}

	public static void setBicycleInfrastructureFactor( Link link, double factor ){
		link.getAttributes().putAttribute( BICYCLE_INFRASTRUCTURE_SPEED_FACTOR, factor );
	}
}
