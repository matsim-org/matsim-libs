package org.matsim.contrib.osm.examples;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.contrib.osm.networkReader.OsmBicycleReader;
import org.matsim.contrib.osm.networkReader.SupersonicOsmNetworkReader.Direction;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

//import org.matsim.core.utils.io.OsmNetworkReader;

public class RunSophistBicycleNetworkReader {

//	private static final String inputFile = "C://Users/metz_so/IdeaProjects/data/sample_illmensee.osm";
//	private static final String inputFile = "C://Users/metz_so/IdeaProjects/data/berlin-260122.osm.pbf";
	private static final String inputFile = "C://Users/metz_so/IdeaProjects/data/neukoelln.osm.pbf";
	private static final String outputFile = "C://Users/metz_so/IdeaProjects/data/matsim-network_nk_bic-no.xml.gz";

	private static final CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:25832");

	public static void main(String[] args) {

		//Network network = new SupersonicOsmNetworkReader.Builder()
		Network network = new OsmBicycleReader.Builder()
				.setCoordinateTransformation(coordinateTransformation)
				.setAfterLinkCreated((link, tags, direction) -> {

						String infra = computeBicycleInfra(tags, direction);
						link.getAttributes().putAttribute("bicycle_infra", infra);

						// optional: wenn infra == NONE, bike entfernen (aber car lassen)
						if ("NONE".equals(infra)) {
							var modes = new HashSet<>(link.getAllowedModes());
							modes.remove(TransportMode.bike);
							link.setAllowedModes(modes);
						}

						// Oneway cleaning:
					if (isBicycleOnewayRelevant(tags) && link.getId().toString().endsWith("r")) {
						// r = Reverse-Link
						removeBikeMode(link);
					}

						// optional: auch den extra _bike-reverse Link killen
					if (isBicycleOnewayRelevant(tags) && link.getId().toString().endsWith("_bike-reverse")) {
						link.setAllowedModes(Set.of());
						link.setCapacity(0);
					}








					// Beispiel: cycleway, cycleway:left/right
					copyTag(link, tags, "cycleway");
					copyTag(link, tags, "cycleway:left");
					copyTag(link, tags, "cycleway:right");
					copyTag(link, tags, "cycleway:both");

					copyTag(link, tags, "highway");
					copyTag(link, tags, "bicycle");

					copyTag(link, tags, "oneway");
					copyTag(link, tags, "oneway:bicycle");

					///  wie bekommt man die raus??
					copyTag(link, tags, "way_id");

					// 2) Fahrrad-Regeln anwenden
					applyBikeRules(link, tags);

					// Optional: Richtung als Attribut
					// link.getAttributes().putAttribute("osm:dir", direction.name());
				})
				.build()
				.read(inputFile);

		new NetworkWriter(network).write(outputFile);
	}

	private static void copyTag(Link link, Map<String, String> tags, String key) {
		String v = tags.get(key);
		if (v != null && !v.isBlank()) {
			link.getAttributes().putAttribute("osm:" + key, v);
		}
	}



	private static void applyBikeRules(Link link, Map<String, String> tags) {
		String highway = tags.get("highway");          // OSM highway
		String bicycle = tags.get("bicycle");          // OSM bicycle (kann null sein)

		// Grundregel: bicycle=no -> bike raus, aber andere Modi bleiben
		if ("no".equals(bicycle)) {
			removeBikeMode(link);
			// optional: falls es ein reiner Bike-Reverse-Link ist, komplett deaktivieren
			if (link.getId().toString().endsWith("_bike-reverse")) {
				link.setAllowedModes(Set.of());
				link.setCapacity(0);
			}
			return;
		}

		// Deine Logik:
		// (type IN ('footway','pedestrian') AND bicycle IN ('designated','yes')) OR (type NOT IN (...))
		boolean isFootwayOrPedestrian = "footway".equals(highway) || "pedestrian".equals(highway);

		if (isFootwayOrPedestrian) {
			boolean bicycleOk = "yes".equals(bicycle) || "designated".equals(bicycle);
			if (!bicycleOk) {
				removeBikeMode(link);

				// falls OsmBicycleReader einen extra reverse-bike-link erzeugt hat: komplett deaktivieren
				if (link.getId().toString().endsWith("_bike-reverse")) {
					link.setAllowedModes(Set.of());
					link.setCapacity(0);
				}
			}
		}
	}

	private static void removeBikeMode(Link link) {
		var modes = new HashSet<>(link.getAllowedModes());
		modes.remove(TransportMode.bike);
		link.setAllowedModes(modes);
	}



	private static String computeBicycleInfra(Map<String, String> tags, Direction direction) {

		// 1. Protected Bike Lane (muss ganz oben stehen)
		if (isCyclewayOnHighwayProtected(tags)) {
			return "CYCLEWAY_ON_HIGHWAY_PROTECTED";
		}

		// 2. Cycleway-Link
		if (isCyclewayLink(tags)) {
			return "CYCLEWAY_LINK";
		}

		// 3. Crossing
		if (isCrossing(tags)) {
			return "CROSSING";
		}

		// 4. Bicycle road (inkl. vehicle destination)
		String bicycleRoadType = getBicycleRoadType(tags);
		if (bicycleRoadType != null) {
			return bicycleRoadType;
		}

		// 5. Bus lane categories (directional)
		String busLaneType = getSharedBusLaneTypeForDirection(tags, direction);
		if (busLaneType != null) {
			return busLaneType;
		}

		// 6. pedestrian + bicycle yes/designated
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

		// 9a advisory (directional)
		if (hasCyclewayOnHighwayAdvisoryForDirection(tags, direction)) {
			return "CYCLEWAY_ON_HIGHWAY_ADVISORY";
		}

		// 9b exclusive (directional)
		if (hasCyclewayOnHighwayExclusiveForDirection(tags, direction)) {
			return "CYCLEWAY_ON_HIGHWAY_EXCLUSIVE";
		}

		// 9c advisory or exclusive (directional)
		if (hasCyclewayOnHighwayAdvisoryOrExclusiveForDirection(tags, direction)) {
			return "CYCLEWAY_ON_HIGHWAY_ADVISORY_OR_EXCLUSIVE";
		}

		// 10. separated cycleways (directional)
		String cyclewayType = getCyclewayTypeForDirection(tags, direction);
		if (cyclewayType != null) {
			return cyclewayType;
		}

		// 11. foot + bicycle designated/yes + (segregated) -> shared/segregated foot&cycleway
		String footAndCycleway = getFootAndCyclewayType(tags);
		if (footAndCycleway != null) {
			return footAndCycleway;
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

	private static String getBicycleRoadType(Map<String,String> t) {
		boolean isBicycleRoad = "yes".equals(v(t,"bicycle_road"));

		String trafficSign = v(t,"traffic_sign");
		if (!isBicycleRoad && !isEmpty(trafficSign) && trafficSign.contains("DE:244")) {
			isBicycleRoad = true;
		}
		if (!isBicycleRoad) return null;

		// destination / frei
		if (!isEmpty(trafficSign) && trafficSign.contains("1020-30")) {
			return "BICYCLE_ROAD_VEHICLE_DESTINATION";
		}
		if (!isEmpty(trafficSign) && (trafficSign.contains("Kraftfahrzeuge-frei")
			|| trafficSign.contains("Kfz-Verkehr frei")
			|| trafficSign.contains("KFZ frei"))) {
			return "BICYCLE_ROAD_VEHICLE_DESTINATION";
		}

		String vehicle = v(t,"vehicle");
		String motorVehicle = v(t,"motor_vehicle");
		if (isAnyOf(vehicle, "destination", "yes") || isAnyOf(motorVehicle, "destination", "yes")) {
			return "BICYCLE_ROAD_VEHICLE_DESTINATION";
		}

		return "BICYCLE_ROAD";
	}

	private static boolean isPedestrianAreaBicycleYes(Map<String,String> t) {
		if (!"pedestrian".equals(v(t,"highway"))) return false;
		String bicycle = v(t,"bicycle");
		return isAnyOf(bicycle, "yes", "designated");
	}

	private static boolean isCyclewayLink(Map<String,String> t) {
		return "cycleway".equals(v(t,"highway")) && "link".equals(v(t,"cycleway"));
	}

	private static boolean isCrossing(Map<String,String> t) {
		String highway = v(t,"highway");
		String bicycle = v(t,"bicycle");

		if ("cycleway".equals(highway) && "crossing".equals(v(t,"cycleway"))) return true;

		if ("path".equals(highway) && "crossing".equals(v(t,"path"))
			&& isAnyOf(bicycle, "yes", "designated")) return true;

		if ("footway".equals(highway) && "crossing".equals(v(t,"footway"))
			&& isAnyOf(bicycle, "yes", "designated")) return true;

		return false;
	}

	private static String getSharedBusLaneTypeForDirection(Map<String,String> t, Direction dir) {
		boolean forward = dir == Direction.Forward;

		String highway = v(t,"highway");
		String cycleway = v(t,"cycleway");
		String trafficSign = v(t,"traffic_sign");

		// GH had more cases for highway=cycleway; keep key ones
		if ("cycleway".equals(highway)) {
			if (isAnyOf(cycleway, "share_busway", "opposite_share_busway")) {
				return "SHARED_BUS_LANE_BUS_WITH_BIKE";
			}
			if (!isEmpty(trafficSign) && trafficSign.startsWith("DE:245")) {
				if (trafficSign.contains("1022-10") || trafficSign.contains("1022-14")) {
					return "SHARED_BUS_LANE_BUS_WITH_BIKE";
				}
			}
			if ("share_busway".equals(v(t,"lane"))) {
				return "SHARED_BUS_LANE_BIKE_WITH_BUS";
			}
			if (!isEmpty(trafficSign) && trafficSign.startsWith("DE:237")) {
				if (trafficSign.contains("1024-14") || trafficSign.contains("1026-32")) {
					return "SHARED_BUS_LANE_BIKE_WITH_BUS";
				}
			}
		}

		// direction-specific: cycleway:right/left/both or busway:right/left/both
		String cwSide = forward ? v(t,"cycleway:right") : v(t,"cycleway:left");
		String bwSide = forward ? v(t,"busway:right") : v(t,"busway:left");

		if ("share_busway".equals(cwSide) || "share_busway".equals(bwSide)) {
			return "SHARED_BUS_LANE_BUS_WITH_BIKE";
		}
		if ("share_busway".equals(v(t,"cycleway:both")) || "share_busway".equals(v(t,"busway:both"))) {
			return "SHARED_BUS_LANE_BUS_WITH_BIKE";
		}
		if (isAnyOf(cycleway, "share_busway", "opposite_share_busway")) {
			return "SHARED_BUS_LANE_BUS_WITH_BIKE";
		}
		return null;
	}

	private static boolean isSharedMotorVehicleLaneForDirection(Map<String,String> t, Direction dir) {
		boolean forward = dir == Direction.Forward;

		String highway = v(t,"highway");
		String cycleway = v(t,"cycleway");

		if ("cycleway".equals(highway) && "shared_lane".equals(cycleway)) return true;

		String cwSide = forward ? v(t,"cycleway:right") : v(t,"cycleway:left");
		if ("shared_lane".equals(cwSide)) return true;

		if ("shared_lane".equals(v(t,"cycleway:both"))) return true;

		return "shared_lane".equals(cycleway);
	}

	private static boolean isCyclewayOnHighwayBetweenLanes(Map<String,String> t) {
		String cyclewayLanes = v(t,"cycleway:lanes");
		String bicycleLanes = v(t,"bicycle:lanes");
		return (!isEmpty(cyclewayLanes) && cyclewayLanes.contains("|lane|"))
			|| (!isEmpty(bicycleLanes) && bicycleLanes.contains("|designated|"));
	}

	// Base check: any lane present but excluding "between_lanes only" edge-case like GH did
	private static boolean isCyclewayOnHighwayAdvisoryOrExclusive(Map<String,String> t) {
		boolean hasLane =
			isAnyOf(v(t,"cycleway"), "lane", "opposite_lane") ||
				isAnyOf(v(t,"cycleway:right"), "lane", "opposite_lane") ||
				isAnyOf(v(t,"cycleway:left"), "lane", "opposite_lane") ||
				isAnyOf(v(t,"cycleway:both"), "lane", "opposite_lane");

		if (!hasLane) return false;

		// GH "Angstweiche" filtering
		if (isCyclewayOnHighwayBetweenLanes(t)) {
			String cyclewayLanes = v(t,"cycleway:lanes");
			String bicycleLanes = v(t,"bicycle:lanes");
			if (!isEmpty(cyclewayLanes) && cyclewayLanes.contains("|lane|") && !cyclewayLanes.endsWith("|lane")) return false;
			if (!isEmpty(bicycleLanes) && bicycleLanes.contains("|designated|") && !bicycleLanes.endsWith("|designated")) return false;
		}
		return true;
	}

	private static boolean hasCyclewayOnHighwayAdvisoryForDirection(Map<String,String> t, Direction dir) {
		if (!isCyclewayOnHighwayAdvisoryOrExclusive(t)) return false;
		boolean forward = dir == Direction.Forward;

		boolean rightBidirectional = "no".equals(v(t,"cycleway:right:oneway"));
		boolean leftBidirectional  = "no".equals(v(t,"cycleway:left:oneway"));

		boolean checkRight = forward || rightBidirectional;
		boolean checkLeft  = !forward || leftBidirectional;

		if (checkRight && "advisory".equals(v(t,"cycleway:right:lane"))) return true;
		if (checkLeft  && "advisory".equals(v(t,"cycleway:left:lane"))) return true;

		return "advisory".equals(v(t,"cycleway:both:lane")) || "advisory".equals(v(t,"cycleway:lane"));
	}

	private static boolean hasCyclewayOnHighwayExclusiveForDirection(Map<String,String> t, Direction dir) {
		if (!isCyclewayOnHighwayAdvisoryOrExclusive(t)) return false;
		boolean forward = dir == Direction.Forward;

		boolean rightBidirectional = "no".equals(v(t,"cycleway:right:oneway"));
		boolean leftBidirectional  = "no".equals(v(t,"cycleway:left:oneway"));

		boolean checkRight = forward || rightBidirectional;
		boolean checkLeft  = !forward || leftBidirectional;

		if (checkRight && "exclusive".equals(v(t,"cycleway:right:lane"))) return true;
		if (checkLeft  && "exclusive".equals(v(t,"cycleway:left:lane"))) return true;

		return "exclusive".equals(v(t,"cycleway:both:lane")) || "exclusive".equals(v(t,"cycleway:lane"));
	}

	private static boolean hasCyclewayOnHighwayAdvisoryOrExclusiveForDirection(Map<String,String> t, Direction dir) {
		boolean forward = dir == Direction.Forward;

		boolean rightBidirectional = "no".equals(v(t,"cycleway:right:oneway"));
		boolean leftBidirectional  = "no".equals(v(t,"cycleway:left:oneway"));

		boolean checkRight = forward || rightBidirectional;
		boolean checkLeft  = !forward || leftBidirectional;

		if (checkRight && isAnyOf(v(t,"cycleway:right"), "lane", "opposite_lane")) return true;
		if (checkLeft  && isAnyOf(v(t,"cycleway:left"), "lane", "opposite_lane")) return true;

		return isAnyOf(v(t,"cycleway:both"), "lane", "opposite_lane")
			|| isAnyOf(v(t,"cycleway"), "lane", "opposite_lane");
	}

	private static String getCyclewayTypeForDirection(Map<String,String> t, Direction dir) {
		boolean forward = dir == Direction.Forward;

		String highway = v(t,"highway");
		String cycleway = v(t,"cycleway");
		String trafficSign = v(t,"traffic_sign");
		String isSidepath = v(t,"is_sidepath");

		// GUARD: cycleway=lane ist on-highway
		if ("lane".equals(cycleway)) return null;

		boolean isCycleway = false;

		// highway=cycleway + is_sidepath (whole way)
		if ("cycleway".equals(highway) && !isEmpty(isSidepath)) isCycleway = true;

		// highway=cycleway + cycleway=track/opposite_track
		if ("cycleway".equals(highway) && isAnyOf(cycleway, "track", "opposite_track")) isCycleway = true;

		// directional side tags = track
		if (forward) {
			if ("track".equals(v(t,"cycleway:right")) || "track".equals(v(t,"cycleway:both"))) isCycleway = true;
		} else {
			if ("track".equals(v(t,"cycleway:left")) || "track".equals(v(t,"cycleway:both"))) isCycleway = true;
		}

		// generic cycleway=track
		if ("track".equals(cycleway)) isCycleway = true;

		// DE:237 whitelist
		if (!isEmpty(trafficSign) && trafficSign.contains("DE:237")) {
			if (isAnyOf(highway, "living_street", "pedestrian", "service", "track", "bridleway", "path", "footway", "cycleway")) {
				isCycleway = true;
			}
		}

		if (!isCycleway) return null;

		if ("yes".equals(isSidepath)) return "CYCLEWAY_ADJOINING";
		if ("no".equals(isSidepath))  return "CYCLEWAY_ISOLATED";
		return "CYCLEWAY_ADJOINING_OR_ISOLATED";
	}

	private static String getFootAndCyclewayType(Map<String,String> t) {
		String highway = v(t,"highway");
		String trafficSign = v(t,"traffic_sign");
		String segregated = v(t,"segregated");
		String foot = v(t,"foot");
		String bicycle = v(t,"bicycle");
		String cycleway = v(t,"cycleway");

		// Special case: highway=cycleway + cycleway=track + segregated/traffic_sign 240/241
		if ("cycleway".equals(highway) && "track".equals(cycleway)) {
			String isSidepath = v(t,"is_sidepath");
			if ("no".equals(segregated) || (!isEmpty(trafficSign) && trafficSign.contains("240"))) {
				return sidepathCategory(isSidepath, "FOOT_AND_CYCLEWAY_SHARED");
			}
			if ("yes".equals(segregated) || (!isEmpty(trafficSign) && trafficSign.contains("241"))) {
				return sidepathCategory(isSidepath, "FOOT_AND_CYCLEWAY_SEGREGATED");
			}
		}

		// only on cycleway-like highways
		if (!isAnyOf(highway, "cycleway", "path", "footway", "service", "track")) return null;

		boolean footAllowed = isAnyOf(foot, "designated", "yes");
		boolean bicycleAllowed = isAnyOf(bicycle, "designated", "yes");
		if (!footAllowed || !bicycleAllowed) return null;

		String isSidepath = v(t,"is_sidepath");

		// segregated=yes or sign 241
		if ("yes".equals(segregated) || (!isEmpty(trafficSign) && trafficSign.contains("241"))) {
			if (isAnyOf(highway, "cycleway", "path", "footway")) {
				return sidepathCategory(isSidepath, "FOOT_AND_CYCLEWAY_SEGREGATED");
			}
			// edge case traffic_mode:right=foot etc. (keep minimal)
			if ("cycleway".equals(highway) && "foot".equals(v(t,"traffic_mode:right"))) {
				String separationRight = v(t,"separation:right");
				boolean separationOk = isEmpty(separationRight) || "no".equals(separationRight);
				if (separationOk) return sidepathCategory(isSidepath, "FOOT_AND_CYCLEWAY_SEGREGATED");
			}
		}

		// shared: segregated=no or sign 240
		if ("no".equals(segregated) || (!isEmpty(trafficSign) && trafficSign.contains("240"))) {
			if (isAnyOf(highway, "cycleway", "path", "footway", "service", "track")) {
				return sidepathCategory(isSidepath, "FOOT_AND_CYCLEWAY_SHARED");
			}
		}
		return null;
	}

	private static String getFootwayBicycleYesTypeForDirection(Map<String,String> t, Direction dir) {
		boolean forward = dir == Direction.Forward;
		String highway = v(t,"highway");

		// Case 1: sidewalk:*:bicycle=yes (street way, directional)
		if (forward && "yes".equals(v(t,"sidewalk:right:bicycle"))) return "FOOTWAY_BICYCLE_YES_ADJOINING";
		if (!forward && "yes".equals(v(t,"sidewalk:left:bicycle"))) return "FOOTWAY_BICYCLE_YES_ADJOINING";
		if ("yes".equals(v(t,"sidewalk:both:bicycle"))) return "FOOTWAY_BICYCLE_YES_ADJOINING";

		// Case 2: separate geometry highway=footway/path
		if (!isAnyOf(highway, "footway", "path")) return null;

		String bicycle = v(t,"bicycle");
		String trafficSign = v(t,"traffic_sign");

		boolean hasBicycleAccess = "yes".equals(bicycle) || (!isEmpty(trafficSign) && trafficSign.contains("1022-10"));
		if (!hasBicycleAccess) return null;

		// mtb:scale filter like GH
		String mtbScale = v(t,"mtb:scale");
		if (!isEmpty(mtbScale)) {
			String cleaned = mtbScale.replaceAll("[\\+\\-\\s]", "");
			try {
				int scale = Integer.parseInt(cleaned);
				if (scale > 1) return null;
				if (isEmpty(trafficSign) && isEmpty(v(t,"is_sidepath"))) return null;
			} catch (NumberFormatException ignored) {}
		}

		String isSidepath = v(t,"is_sidepath");
		return sidepathCategory(isSidepath, "FOOTWAY_BICYCLE_YES");
	}

	private static boolean isCyclewayOnHighwayProtected(Map<String,String> t) {
		// Must be sidepath
		if (!"yes".equals(v(t,"is_sidepath"))) return false;

		// separation:left physical
		if (isPhysicalSeparation(v(t,"separation:left"))) return true;

		// traffic_mode:left=parking
		if ("parking".equals(v(t,"traffic_mode:left"))) return true;

		// contra-flow check: traffic_mode:right=motor_vehicle and physical separation:right
		if ("motor_vehicle".equals(v(t,"traffic_mode:right")) && isPhysicalSeparation(v(t,"separation:right"))) return true;

		return false;
	}

	private static boolean isPhysicalSeparation(String sep) {
		return isAnyOf(sep,
			"bollard", "flex_post", "vertical_panel", "studs", "bump", "planter",
			"fence", "jersey_barrier", "guard_rail");
	}

	private static boolean needsClarification(Map<String,String> t) {
		String highway = v(t,"highway");
		String bicycle = v(t,"bicycle");
		String cycleway = v(t,"cycleway");
		String foot = v(t,"foot");

		// already categorized: between_lanes
		if (isCyclewayOnHighwayBetweenLanes(t)) return false;

		// cycleway=shared ignored
		if ("shared".equals(cycleway)) return false;

		if ("cycleway".equals(highway)) return true;

		if ("path".equals(highway) && "designated".equals(bicycle)) {
			if (!isEmpty(v(t,"mtb:scale")) || "yes".equals(v(t,"mtb"))) return false;

			String surface = v(t,"surface");
			if (isAnyOf(surface, "ground", "dirt", "fine_gravel", "gravel", "pebblestone", "earth")) return false;

			if ("no".equals(foot)) return true;
			return true;
		}

		if ("footway".equals(highway) && "designated".equals(bicycle)) return true;

		return false;
	}



	///  HELPER
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
		if ("yes".equals(isSidepath)) return prefix + "_ADJOINING";
		if ("no".equals(isSidepath))  return prefix + "_ISOLATED";
		return prefix + "_ADJOINING_OR_ISOLATED";
	}

	// Oneway cleaning:
	private static boolean isBicycleOnewayRelevant(Map<String,String> tags) {
		String h = tags.get("highway");
		boolean isRelevant = "path".equals(h) || "cycleway".equals(h) || "footway".equals(h);
		if (!isRelevant) return false;

		if ("yes".equals(tags.get("oneway:bicycle"))) return true;

		if ("yes".equals(tags.get("oneway"))) {
			if ("no".equals(tags.get("oneway:bicycle"))) return false;
			return true;
		}
		return false;
	}



}
