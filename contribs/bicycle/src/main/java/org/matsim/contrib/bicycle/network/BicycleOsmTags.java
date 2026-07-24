/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 *                                                                         *
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
package org.matsim.contrib.bicycle.network;

/**
 * Bicycle-specific OSM tag keys and values used by {@link BicycleInfraClassifier}
 * and {@link BicycleLinkPolicy}.
 *
 * <p>Mirrors the style of {@code org.matsim.contrib.osm.networkReader.OsmTags}.
 * Anything that is already defined there (e.g. {@code highway}, {@code bicycle},
 * {@code surface}) is intentionally re-declared here as well, so that the
 * bicycle package has a single, self-contained source of truth and does not
 * have to import keys piecemeal from another contrib.
 *
 * <p>Scope: only keys/values that appear at least twice in the bicycle network
 * code, or that are part of a directional tag family (e.g. {@code cycleway:right}
 * and {@code cycleway:left} are kept as a pair even if one of them happens to
 * appear only once today). One-off, highly specific strings — German traffic
 * sign codes such as {@code DE:244} or {@code 1022-10} and free-text fragments
 * like {@code "Kfz-Verkehr frei"} — stay inline at their use site, where the
 * surrounding {@code .contains(...)} check makes the intent clearer than a
 * constant name would.
 *
 * @author smetzler
 */
public final class BicycleOsmTags {

	private BicycleOsmTags() {
	}

	// ------------------------------------------------------------------------
	// Generic keys
	// ------------------------------------------------------------------------

	public static final String HIGHWAY = "highway";
	public static final String BICYCLE = "bicycle";
	public static final String FOOT = "foot";
	public static final String VEHICLE = "vehicle";
	public static final String MOTOR_VEHICLE = "motor_vehicle";
	public static final String ACCESS = "access";
	public static final String ONEWAY = "oneway";
	public static final String ONEWAY_BICYCLE = "oneway:bicycle";

	// ------------------------------------------------------------------------
	// Highway values
	// ------------------------------------------------------------------------

	public static final String HW_CYCLEWAY = "cycleway";
	public static final String HW_FOOTWAY = "footway";
	public static final String HW_PATH = "path";
	public static final String HW_PEDESTRIAN = "pedestrian";
	public static final String HW_SERVICE = "service";
	public static final String HW_TRACK = "track";
	public static final String HW_LIVING_STREET = "living_street";
	public static final String HW_BRIDLEWAY = "bridleway";

	// ------------------------------------------------------------------------
	// Cycleway keys (generic + directional family)
	// ------------------------------------------------------------------------

	public static final String CYCLEWAY = "cycleway";
	public static final String CYCLEWAY_RIGHT = "cycleway:right";
	public static final String CYCLEWAY_LEFT = "cycleway:left";
	public static final String CYCLEWAY_BOTH = "cycleway:both";

	public static final String CYCLEWAY_LANE = "cycleway:lane";
	public static final String CYCLEWAY_RIGHT_LANE = "cycleway:right:lane";
	public static final String CYCLEWAY_LEFT_LANE = "cycleway:left:lane";
	public static final String CYCLEWAY_BOTH_LANE = "cycleway:both:lane";

	public static final String CYCLEWAY_RIGHT_ONEWAY = "cycleway:right:oneway";
	public static final String CYCLEWAY_LEFT_ONEWAY = "cycleway:left:oneway";

	public static final String CYCLEWAY_LANES = "cycleway:lanes";
	public static final String BICYCLE_LANES = "bicycle:lanes";

	// ------------------------------------------------------------------------
	// Cycleway values
	// ------------------------------------------------------------------------

	public static final String CW_LANE = "lane";
	public static final String CW_TRACK = "track";
	public static final String CW_OPPOSITE_LANE = "opposite_lane";
	public static final String CW_SHARED_LANE = "shared_lane";
	public static final String CW_SHARE_BUSWAY = "share_busway";
	public static final String CW_CROSSING = "crossing";

	// Lane subvalues (cycleway:*:lane = ...)
	public static final String CW_LANE_ADVISORY = "advisory";
	public static final String CW_LANE_EXCLUSIVE = "exclusive";

	// ------------------------------------------------------------------------
	// Universal access values
	// ------------------------------------------------------------------------

	public static final String YES = "yes";
	public static final String NO = "no";
	public static final String DESIGNATED = "designated";
	public static final String PRIVATE = "private";
	public static final String CUSTOMER = "customer";

	// ------------------------------------------------------------------------
	// Busway directional family
	// ------------------------------------------------------------------------

	public static final String BUSWAY_RIGHT = "busway:right";
	public static final String BUSWAY_LEFT = "busway:left";
	public static final String BUSWAY_BOTH = "busway:both";

	// ------------------------------------------------------------------------
	// Sidewalk-bicycle directional family
	// ------------------------------------------------------------------------

	public static final String SIDEWALK_RIGHT_BICYCLE = "sidewalk:right:bicycle";
	public static final String SIDEWALK_LEFT_BICYCLE = "sidewalk:left:bicycle";
	public static final String SIDEWALK_BOTH_BICYCLE = "sidewalk:both:bicycle";

	// ------------------------------------------------------------------------
	// Sidepath / separation / traffic mode
	// ------------------------------------------------------------------------

	public static final String IS_SIDEPATH = "is_sidepath";
	public static final String SEGREGATED = "segregated";

	public static final String SEPARATION_LEFT = "separation:left";
	public static final String SEPARATION_RIGHT = "separation:right";

	public static final String TRAFFIC_MODE_LEFT = "traffic_mode:left";
	public static final String TRAFFIC_MODE_RIGHT = "traffic_mode:right";

	// ------------------------------------------------------------------------
	// Misc
	// ------------------------------------------------------------------------

	public static final String TRAFFIC_SIGN = "traffic_sign";
	public static final String BICYCLE_ROAD = "bicycle_road";
	public static final String MTB_SCALE = "mtb:scale";
	public static final String SURFACE = "surface";
	public static final String SMOOTHNESS = "smoothness";
}
