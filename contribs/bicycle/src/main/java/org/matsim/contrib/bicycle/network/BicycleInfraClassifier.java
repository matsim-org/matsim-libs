/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.contrib.bicycle.network;

import org.matsim.contrib.osm.networkReader.SupersonicOsmNetworkReader.Direction;

import java.util.Map;

import static org.matsim.contrib.bicycle.network.BicycleOsmTags.*;

public final class BicycleInfraClassifier {

	/**
	 * Main entry: classify OSM tags into a bicycle infrastructure category.
	 * Precedence: first match wins (mirrors your GraphHopper parser order).
	 */
	public String classify(Map<String, String> tags, Direction direction) {

		// 1. Protected bike lane (must be on top)
		if (isCyclewayOnHighwayProtected(tags)) {
			return "CYCLEWAY_ON_HIGHWAY_PROTECTED";
		}

		// 2. Cycleway link
		if (isCyclewayLink(tags)) {
			return "CYCLEWAY_LINK";
		}

		// 3. Crossing
		if (isCrossing(tags)) {
			return "CROSSING";
		}

		// 4. Bicycle road (incl. vehicle destination)
		String bicycleRoadType = getBicycleRoadType(tags);
		if (bicycleRoadType != null) {
			return bicycleRoadType;
		}

		// 5. Bus lane categories (directional)
		String busLaneType = getSharedBusLaneTypeForDirection(tags, direction);
		if (busLaneType != null) {
			return busLaneType;
		}

		// 6. pedestrian area bicycle yes/designated
		if (isPedestrianAreaBicycleYes(tags)) {
			return "PEDESTRIAN_AREA_BICYCLE_YES";
		}

		// 7. shared motor vehicle lane (directional)
		if (isSharedMotorVehicleLaneForDirection(tags, direction)) {
			return "SHARED_MOTOR_VEHICLE_LANE";
		}

		// 8. between lanes (Angstweiche)
		if (isCyclewayOnHighwayBetweenLanes(tags)) {
			return "CYCLEWAY_ON_HIGHWAY_BETWEEN_LANES";
		}

		// 9a advisory lane (directional)
		if (hasCyclewayOnHighwayAdvisoryForDirection(tags, direction)) {
			return "CYCLEWAY_ON_HIGHWAY_ADVISORY";
		}

		// 9b exclusive lane (directional)
		if (hasCyclewayOnHighwayExclusiveForDirection(tags, direction)) {
			return "CYCLEWAY_ON_HIGHWAY_EXCLUSIVE";
		}

		// 9c lane but unspecified advisory/exclusive (directional)
		if (hasCyclewayOnHighwayAdvisoryOrExclusiveForDirection(tags, direction)) {
			return "CYCLEWAY_ON_HIGHWAY_ADVISORY_OR_EXCLUSIVE";
		}

		// 10. separated cycleways (directional)
		String cyclewayType = getCyclewayTypeForDirection(tags, direction);
		if (cyclewayType != null) {
			return cyclewayType;
		}

		// 11. foot+cycleway combined (shared/segregated)
		String footAndCyclewayType = getFootAndCyclewayType(tags);
		if (footAndCyclewayType != null) {
			return footAndCyclewayType;
		}

		// 12. footway bicycle yes (directional)
		String footwayBicycleYes = getFootwayBicycleYesTypeForDirection(tags, direction);
		if (footwayBicycleYes != null) {
			return footwayBicycleYes;
		}

		// 13. needs clarification
		if (needsClarification(tags)) {
			return "NEEDS_CLARIFICATION";
		}

		return "NONE";
	}

	// -------------------------------------------------------------------------
	// Bicycle road
	// -------------------------------------------------------------------------

	private static String getBicycleRoadType(Map<String, String> t) {
		boolean isBicycleRoad = YES.equals(v(t, BICYCLE_ROAD));

		String trafficSign = v(t, TRAFFIC_SIGN);
		if (!isBicycleRoad && !isEmpty(trafficSign) && trafficSign.contains("DE:244")) {
			isBicycleRoad = true;
		}
		if (!isBicycleRoad) return null;

		// vehicle destination / frei
		if (!isEmpty(trafficSign) && trafficSign.contains("1020-30")) {
			return "BICYCLE_ROAD_VEHICLE_DESTINATION";
		}
		if (!isEmpty(trafficSign) && (trafficSign.contains("Kraftfahrzeuge-frei")
			|| trafficSign.contains("Kfz-Verkehr frei")
			|| trafficSign.contains("KFZ frei"))) {
			return "BICYCLE_ROAD_VEHICLE_DESTINATION";
		}

		String vehicle = v(t, VEHICLE);
		String motorVehicle = v(t, MOTOR_VEHICLE);
		if (isAnyOf(vehicle, "destination", YES) || isAnyOf(motorVehicle, "destination", YES)) {
			return "BICYCLE_ROAD_VEHICLE_DESTINATION";
		}

		return "BICYCLE_ROAD";
	}

	// -------------------------------------------------------------------------
	// Pedestrian area bicycle yes
	// -------------------------------------------------------------------------

	private static boolean isPedestrianAreaBicycleYes(Map<String, String> t) {
		if (!HW_PEDESTRIAN.equals(v(t, HIGHWAY))) return false;
		String bicycle = v(t, BICYCLE);
		return isAnyOf(bicycle, YES, DESIGNATED);
	}

	// -------------------------------------------------------------------------
	// Cycleway link / crossing
	// -------------------------------------------------------------------------

	private static boolean isCyclewayLink(Map<String, String> t) {
		return HW_CYCLEWAY.equals(v(t, HIGHWAY)) && "link".equals(v(t, CYCLEWAY));
	}

	private static boolean isCrossing(Map<String, String> t) {
		String highway = v(t, HIGHWAY);
		String bicycle = v(t, BICYCLE);

		// highway=cycleway + cycleway=crossing
		if (HW_CYCLEWAY.equals(highway) && CW_CROSSING.equals(v(t, CYCLEWAY))) return true;

		// highway=path + path=crossing + bicycle=yes/designated
		if (HW_PATH.equals(highway) && CW_CROSSING.equals(v(t, "path"))
			&& isAnyOf(bicycle, YES, DESIGNATED)) return true;

		// highway=footway + footway=crossing + bicycle=yes/designated
		if (HW_FOOTWAY.equals(highway) && CW_CROSSING.equals(v(t, "footway"))
			&& isAnyOf(bicycle, YES, DESIGNATED)) return true;

		return false;
	}

	// -------------------------------------------------------------------------
	// Bus lanes (directional)
	// -------------------------------------------------------------------------

	private static String getSharedBusLaneTypeForDirection(Map<String, String> t, Direction dir) {
		boolean forward = dir == Direction.Forward;

		String highway = v(t, HIGHWAY);
		String cycleway = v(t, CYCLEWAY);
		String trafficSign = v(t, TRAFFIC_SIGN);

		// highway=cycleway cases (keep core GH ones)
		if (HW_CYCLEWAY.equals(highway)) {
			if (isAnyOf(cycleway, CW_SHARE_BUSWAY, "opposite_share_busway")) {
				return "SHARED_BUS_LANE_BUS_WITH_BIKE";
			}
			if (!isEmpty(trafficSign) && trafficSign.startsWith("DE:245")) {
				if (trafficSign.contains("1022-10") || trafficSign.contains("1022-14")) {
					return "SHARED_BUS_LANE_BUS_WITH_BIKE";
				}
			}
			if (CW_SHARE_BUSWAY.equals(v(t, "lane"))) {
				return "SHARED_BUS_LANE_BIKE_WITH_BUS";
			}
			if (!isEmpty(trafficSign) && trafficSign.startsWith("DE:237")) {
				if (trafficSign.contains("1024-14") || trafficSign.contains("1026-32")) {
					return "SHARED_BUS_LANE_BIKE_WITH_BUS";
				}
			}
		}

		// directional: cycleway:right/left OR busway:right/left
		String cwSide = forward ? v(t, CYCLEWAY_RIGHT) : v(t, CYCLEWAY_LEFT);
		String bwSide = forward ? v(t, BUSWAY_RIGHT) : v(t, BUSWAY_LEFT);

		if (CW_SHARE_BUSWAY.equals(cwSide) || CW_SHARE_BUSWAY.equals(bwSide)) {
			return "SHARED_BUS_LANE_BUS_WITH_BIKE";
		}

		// both sides
		if (CW_SHARE_BUSWAY.equals(v(t, CYCLEWAY_BOTH)) || CW_SHARE_BUSWAY.equals(v(t, BUSWAY_BOTH))) {
			return "SHARED_BUS_LANE_BUS_WITH_BIKE";
		}

		// generic
		if (isAnyOf(cycleway, CW_SHARE_BUSWAY, "opposite_share_busway")) {
			return "SHARED_BUS_LANE_BUS_WITH_BIKE";
		}

		return null;
	}

	// -------------------------------------------------------------------------
	// Shared lane (directional)
	// -------------------------------------------------------------------------

	private static boolean isSharedMotorVehicleLaneForDirection(Map<String, String> t, Direction dir) {
		boolean forward = dir == Direction.Forward;

		String highway = v(t, HIGHWAY);
		String cycleway = v(t, CYCLEWAY);

		// highway=cycleway + cycleway=shared_lane
		if (HW_CYCLEWAY.equals(highway) && CW_SHARED_LANE.equals(cycleway)) return true;

		// side tags
		String cwSide = forward ? v(t, CYCLEWAY_RIGHT) : v(t, CYCLEWAY_LEFT);
		if (CW_SHARED_LANE.equals(cwSide)) return true;

		// both / generic
		if (CW_SHARED_LANE.equals(v(t, CYCLEWAY_BOTH))) return true;
		return CW_SHARED_LANE.equals(cycleway);
	}

	// -------------------------------------------------------------------------
	// Between lanes (Angstweiche)
	// -------------------------------------------------------------------------

	private static boolean isCyclewayOnHighwayBetweenLanes(Map<String, String> t) {
		String cyclewayLanes = v(t, CYCLEWAY_LANES);
		String bicycleLanes = v(t, BICYCLE_LANES);
		return (!isEmpty(cyclewayLanes) && cyclewayLanes.contains("|lane|"))
			|| (!isEmpty(bicycleLanes) && bicycleLanes.contains("|designated|"));
	}

	// -------------------------------------------------------------------------
	// On-highway lane parsing (advisory/exclusive/unspecified) - directional
	// -------------------------------------------------------------------------

	private static boolean isCyclewayOnHighwayAdvisoryOrExclusiveBase(Map<String, String> t) {

		boolean hasLane =
			isAnyOf(v(t, CYCLEWAY), CW_LANE, CW_OPPOSITE_LANE) ||
				isAnyOf(v(t, CYCLEWAY_RIGHT), CW_LANE, CW_OPPOSITE_LANE) ||
				isAnyOf(v(t, CYCLEWAY_LEFT), CW_LANE, CW_OPPOSITE_LANE) ||
				isAnyOf(v(t, CYCLEWAY_BOTH), CW_LANE, CW_OPPOSITE_LANE);

		if (!hasLane) return false;

		// filter "between lanes only" edge-case like GH
		if (isCyclewayOnHighwayBetweenLanes(t)) {
			String cyclewayLanes = v(t, CYCLEWAY_LANES);
			String bicycleLanes = v(t, BICYCLE_LANES);
			if (!isEmpty(cyclewayLanes) && cyclewayLanes.contains("|lane|") && !cyclewayLanes.endsWith("|lane")) return false;
			if (!isEmpty(bicycleLanes) && bicycleLanes.contains("|designated|") && !bicycleLanes.endsWith("|designated")) return false;
		}
		return true;
	}

	private static boolean hasCyclewayOnHighwayAdvisoryForDirection(Map<String, String> t, Direction dir) {
		if (!isCyclewayOnHighwayAdvisoryOrExclusiveBase(t)) return false;
		boolean forward = dir == Direction.Forward;

		boolean rightBidirectional = NO.equals(v(t, CYCLEWAY_RIGHT_ONEWAY));
		boolean leftBidirectional = NO.equals(v(t, CYCLEWAY_LEFT_ONEWAY));

		boolean checkRight = forward || rightBidirectional;
		boolean checkLeft = !forward || leftBidirectional;

		if (checkRight && CW_LANE_ADVISORY.equals(v(t, CYCLEWAY_RIGHT_LANE))) return true;
		if (checkLeft && CW_LANE_ADVISORY.equals(v(t, CYCLEWAY_LEFT_LANE))) return true;

		return CW_LANE_ADVISORY.equals(v(t, CYCLEWAY_BOTH_LANE)) || CW_LANE_ADVISORY.equals(v(t, CYCLEWAY_LANE));
	}

	private static boolean hasCyclewayOnHighwayExclusiveForDirection(Map<String, String> t, Direction dir) {
		if (!isCyclewayOnHighwayAdvisoryOrExclusiveBase(t)) return false;
		boolean forward = dir == Direction.Forward;

		boolean rightBidirectional = NO.equals(v(t, CYCLEWAY_RIGHT_ONEWAY));
		boolean leftBidirectional = NO.equals(v(t, CYCLEWAY_LEFT_ONEWAY));

		boolean checkRight = forward || rightBidirectional;
		boolean checkLeft = !forward || leftBidirectional;

		if (checkRight && CW_LANE_EXCLUSIVE.equals(v(t, CYCLEWAY_RIGHT_LANE))) return true;
		if (checkLeft && CW_LANE_EXCLUSIVE.equals(v(t, CYCLEWAY_LEFT_LANE))) return true;

		return CW_LANE_EXCLUSIVE.equals(v(t, CYCLEWAY_BOTH_LANE)) || CW_LANE_EXCLUSIVE.equals(v(t, CYCLEWAY_LANE));
	}

	private static boolean hasCyclewayOnHighwayAdvisoryOrExclusiveForDirection(Map<String, String> t, Direction dir) {
		boolean forward = dir == Direction.Forward;

		boolean rightBidirectional = NO.equals(v(t, CYCLEWAY_RIGHT_ONEWAY));
		boolean leftBidirectional = NO.equals(v(t, CYCLEWAY_LEFT_ONEWAY));

		boolean checkRight = forward || rightBidirectional;
		boolean checkLeft = !forward || leftBidirectional;

		if (checkRight && isAnyOf(v(t, CYCLEWAY_RIGHT), CW_LANE, CW_OPPOSITE_LANE)) return true;
		if (checkLeft && isAnyOf(v(t, CYCLEWAY_LEFT), CW_LANE, CW_OPPOSITE_LANE)) return true;

		return isAnyOf(v(t, CYCLEWAY_BOTH), CW_LANE, CW_OPPOSITE_LANE)
			|| isAnyOf(v(t, CYCLEWAY), CW_LANE, CW_OPPOSITE_LANE);
	}

	// -------------------------------------------------------------------------
	// Separated cycleways (directional)
	// -------------------------------------------------------------------------

	private static String getCyclewayTypeForDirection(Map<String, String> t, Direction dir) {
		boolean forward = dir == Direction.Forward;

		String highway = v(t, HIGHWAY);
		String cycleway = v(t, CYCLEWAY);
		String trafficSign = v(t, TRAFFIC_SIGN);
		String isSidepath = v(t, IS_SIDEPATH);

		// GUARD: cycleway=lane is on-highway, not separated
		if (CW_LANE.equals(cycleway)) return null;

		boolean isCycleway = false;

		// highway=cycleway + is_sidepath (whole way)
		if (HW_CYCLEWAY.equals(highway) && !isEmpty(isSidepath)) isCycleway = true;

		// highway=cycleway + cycleway=track/opposite_track
		if (HW_CYCLEWAY.equals(highway) && isAnyOf(cycleway, CW_TRACK, "opposite_track")) isCycleway = true;

		// directional side tags track
		if (forward) {
			if (CW_TRACK.equals(v(t, CYCLEWAY_RIGHT)) || CW_TRACK.equals(v(t, CYCLEWAY_BOTH))) isCycleway = true;
		} else {
			if (CW_TRACK.equals(v(t, CYCLEWAY_LEFT)) || CW_TRACK.equals(v(t, CYCLEWAY_BOTH))) isCycleway = true;
		}

		// generic cycleway=track
		if (CW_TRACK.equals(cycleway)) isCycleway = true;

		// traffic sign DE:237 whitelist
		if (!isEmpty(trafficSign) && trafficSign.contains("DE:237")) {
			if (isAnyOf(highway, HW_LIVING_STREET, HW_PEDESTRIAN, HW_SERVICE, HW_TRACK, HW_BRIDLEWAY, HW_PATH, HW_FOOTWAY, HW_CYCLEWAY)) {
				isCycleway = true;
			}
		}

		if (!isCycleway) return null;

		if (YES.equals(isSidepath)) return "CYCLEWAY_ADJOINING";
		if (NO.equals(isSidepath)) return "CYCLEWAY_ISOLATED";
		return "CYCLEWAY_ADJOINING_OR_ISOLATED";
	}

	// -------------------------------------------------------------------------
	// Foot+cycle combined (shared/segregated) - mostly non-directional like GH
	// -------------------------------------------------------------------------

	private static String getFootAndCyclewayType(Map<String, String> t) {
		String highway = v(t, HIGHWAY);
		String trafficSign = v(t, TRAFFIC_SIGN);
		String segregated = v(t, SEGREGATED);
		String foot = v(t, FOOT);
		String bicycle = v(t, BICYCLE);
		String cycleway = v(t, CYCLEWAY);

		// Special: highway=cycleway + cycleway=track + segregated/signs
		if (HW_CYCLEWAY.equals(highway) && CW_TRACK.equals(cycleway)) {
			String isSidepath = v(t, IS_SIDEPATH);
			if (NO.equals(segregated) || (!isEmpty(trafficSign) && trafficSign.contains("240"))) {
				return sidepathCategory(isSidepath, "FOOT_AND_CYCLEWAY_SHARED");
			}
			if (YES.equals(segregated) || (!isEmpty(trafficSign) && trafficSign.contains("241"))) {
				return sidepathCategory(isSidepath, "FOOT_AND_CYCLEWAY_SEGREGATED");
			}
		}

		// only on cycleway-like highways
		if (!isAnyOf(highway, HW_CYCLEWAY, HW_PATH, HW_FOOTWAY, HW_SERVICE, HW_TRACK)) return null;

		boolean footAllowed = isAnyOf(foot, DESIGNATED, YES);
		boolean bicycleAllowed = isAnyOf(bicycle, DESIGNATED, YES);
		if (!footAllowed || !bicycleAllowed) return null;

		String isSidepath = v(t, IS_SIDEPATH);

		// segregated=yes or sign 241
		if (YES.equals(segregated) || (!isEmpty(trafficSign) && trafficSign.contains("241"))) {
			if (isAnyOf(highway, HW_CYCLEWAY, HW_PATH, HW_FOOTWAY)) {
				return sidepathCategory(isSidepath, "FOOT_AND_CYCLEWAY_SEGREGATED");
			}
			// minimal edge case from GH: traffic_mode:right=foot (no separation)
			if (HW_CYCLEWAY.equals(highway) && FOOT.equals(v(t, TRAFFIC_MODE_RIGHT))) {
				String separationRight = v(t, SEPARATION_RIGHT);
				boolean separationOk = isEmpty(separationRight) || NO.equals(separationRight);
				if (separationOk) return sidepathCategory(isSidepath, "FOOT_AND_CYCLEWAY_SEGREGATED");
			}
		}

		// shared: segregated=no or sign 240
		if (NO.equals(segregated) || (!isEmpty(trafficSign) && trafficSign.contains("240"))) {
			if (isAnyOf(highway, HW_CYCLEWAY, HW_PATH, HW_FOOTWAY, HW_SERVICE, HW_TRACK)) {
				return sidepathCategory(isSidepath, "FOOT_AND_CYCLEWAY_SHARED");
			}
		}

		return null;
	}

	// -------------------------------------------------------------------------
	// Footway bicycle yes (directional for sidewalk:* tags)
	// -------------------------------------------------------------------------

	private static String getFootwayBicycleYesTypeForDirection(Map<String, String> t, Direction dir) {
		boolean forward = dir == Direction.Forward;
		String highway = v(t, HIGHWAY);

		// Case 1: sidewalk:*:bicycle=yes (directional)
		if (forward && YES.equals(v(t, SIDEWALK_RIGHT_BICYCLE))) return "FOOTWAY_BICYCLE_YES_ADJOINING";
		if (!forward && YES.equals(v(t, SIDEWALK_LEFT_BICYCLE))) return "FOOTWAY_BICYCLE_YES_ADJOINING";
		if (YES.equals(v(t, SIDEWALK_BOTH_BICYCLE))) return "FOOTWAY_BICYCLE_YES_ADJOINING";

		// Case 2: separate geometry highway=footway/path
		if (!isAnyOf(highway, HW_FOOTWAY, HW_PATH)) return null;

		String bicycle = v(t, BICYCLE);
		String trafficSign = v(t, TRAFFIC_SIGN);

		boolean hasBicycleAccess = YES.equals(bicycle) || (!isEmpty(trafficSign) && trafficSign.contains("1022-10"));
		if (!hasBicycleAccess) return null;

		// mtb:scale filter
		String mtbScale = v(t, MTB_SCALE);
		if (!isEmpty(mtbScale)) {
			String cleaned = mtbScale.replaceAll("[\\+\\-\\s]", "");
			try {
				int scale = Integer.parseInt(cleaned);
				if (scale > 1) return null;
				if (isEmpty(trafficSign) && isEmpty(v(t, IS_SIDEPATH))) return null;
			} catch (NumberFormatException ignored) {
			}
		}

		String isSidepath = v(t, IS_SIDEPATH);
		return sidepathCategory(isSidepath, "FOOTWAY_BICYCLE_YES");
	}

	// -------------------------------------------------------------------------
	// Protected bike lane (PBL)
	// -------------------------------------------------------------------------

	private static boolean isCyclewayOnHighwayProtected(Map<String, String> t) {
		// Must be sidepath
		if (!YES.equals(v(t, IS_SIDEPATH))) return false;

		// physical separation left
		if (isPhysicalSeparation(v(t, SEPARATION_LEFT))) return true;

		// parking counts as separation
		if ("parking".equals(v(t, TRAFFIC_MODE_LEFT))) return true;

		// contraflow check: traffic_mode:right=motor_vehicle AND physical separation right
		if (MOTOR_VEHICLE.equals(v(t, TRAFFIC_MODE_RIGHT)) && isPhysicalSeparation(v(t, SEPARATION_RIGHT))) return true;

		return false;
	}

	private static boolean isPhysicalSeparation(String sep) {
		return isAnyOf(sep,
			"bollard", "flex_post", "vertical_panel", "studs", "bump", "planter",
			"fence", "jersey_barrier", "guard_rail");
	}

	// -------------------------------------------------------------------------
	// Needs clarification
	// -------------------------------------------------------------------------

	private static boolean needsClarification(Map<String, String> t) {
		String highway = v(t, HIGHWAY);
		String bicycle = v(t, BICYCLE);
		String cycleway = v(t, CYCLEWAY);
		String foot = v(t, FOOT);

		// already categorized: between lanes
		if (isCyclewayOnHighwayBetweenLanes(t)) return false;

		// cycleway=shared is not infra
		if ("shared".equals(cycleway)) return false;

		// highway=cycleway without more details
		if (HW_CYCLEWAY.equals(highway)) return true;

		// path + bicycle=designated without more info
		if (HW_PATH.equals(highway) && DESIGNATED.equals(bicycle)) {

			// exclude MTB-ish
			if (!isEmpty(v(t, MTB_SCALE)) || YES.equals(v(t, "mtb"))) return false;

			String surface = v(t, SURFACE);
			if (isAnyOf(surface, "ground", "dirt", "fine_gravel", "gravel", "pebblestone", "earth")) return false;

			// foot=no on a path is relevant
			if (NO.equals(foot)) return true;

			return true;
		}

		// footway + bicycle=designated
		if (HW_FOOTWAY.equals(highway) && DESIGNATED.equals(bicycle)) return true;

		return false;
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	private static String v(Map<String, String> tags, String key) {
		String val = tags.get(key);
		return val == null ? "" : val.trim();
	}

	private static boolean isEmpty(String s) {
		return s == null || s.isBlank();
	}

	private static boolean isAnyOf(String val, String... opts) {
		if (val == null) return false;
		for (String o : opts) if (o.equals(val)) return true;
		return false;
	}

	private static String sidepathCategory(String isSidepath, String prefix) {
		if (YES.equals(isSidepath)) return prefix + "_ADJOINING";
		if (NO.equals(isSidepath)) return prefix + "_ISOLATED";
		return prefix + "_ADJOINING_OR_ISOLATED";
	}
}

