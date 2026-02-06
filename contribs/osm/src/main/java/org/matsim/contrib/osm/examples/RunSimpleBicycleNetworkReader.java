package org.matsim.contrib.osm.examples;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.contrib.osm.networkReader.OsmBicycleReader;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.contrib.osm.networkReader.SupersonicOsmNetworkReader.Direction;

import java.util.Map;
import java.util.HashSet;
import java.util.Set;

//import org.matsim.core.utils.io.OsmNetworkReader;

public class RunSimpleBicycleNetworkReader {

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
							var modes = new java.util.HashSet<>(link.getAllowedModes());
							modes.remove(org.matsim.api.core.v01.TransportMode.bike);
							link.setAllowedModes(modes);
						}






					// Beispiel: cycleway, cycleway:left/right
					copyTag(link, tags, "cycleway");
					copyTag(link, tags, "cycleway:left");
					copyTag(link, tags, "cycleway:right");
					copyTag(link, tags, "cycleway:both");

					copyTag(link, tags, "highway");
					copyTag(link, tags, "bicycle");

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

		// ---- helpers / short-hands ----
		String highway = v(tags, "highway");
		String bicycle = v(tags, "bicycle");
		String foot = v(tags, "foot");
		String segregated = v(tags, "segregated");

		// direction-specific cycleway tag preference:
		// Forward: right > both > cycleway
		// Reverse: left  > both > cycleway
		String cwDir = direction == Direction.Forward ? v(tags, "cycleway:right") : v(tags, "cycleway:left");
		String cwBoth = v(tags, "cycleway:both");
		String cw = firstNonEmpty(cwDir, cwBoth, v(tags, "cycleway"));

		// lane subtype (exclusive/advisory) direction-specific
		String laneKey = direction == Direction.Forward ? "cycleway:right:lane" : "cycleway:left:lane";
		String cwLaneType = firstNonEmpty(v(tags, laneKey), v(tags, "cycleway:both:lane"));

		// sidepath indicator used in your doc (mostly for highway=cycleway / footway)
		String isSidepath = v(tags, "is_sidepath"); // "yes" / "no" / empty

		// crossing marker (rough; adapt if you have a different signal)
		String highwayCrossing = v(tags, "highway"); // keep same var
		String footway = v(tags, "footway");
		String crossing = v(tags, "crossing");

		// ---- 0) explicit forbiddance ----
		if ("no".equals(bicycle)) return "NONE";

		// ---- 1) bicycle road ----
		if ("yes".equals(v(tags, "bicycle_road"))) {
			// optional variant: bicycle road but vehicle destination/free
			// (depends on local tagging; keep simple)
			if ("destination".equals(v(tags, "motor_vehicle")) || "destination".equals(v(tags, "vehicle"))) {
				return "BICYCLE_ROAD_VEHICLE_DESTINATION";
			}
			return "BICYCLE_ROAD";
		}

		// ---- 2) pedestrian area with bicycle allowed ----
		if ("pedestrian".equals(highway) && isAnyOf(bicycle, "yes", "designated")) {
			return "PEDESTRIAN_AREA_BICYCLE_YES";
		}

		// ---- 3) crossings (optional, but in your enum list) ----
		// Very heuristic: only tag if it is explicitly a crossing.
		if ("crossing".equals(highwayCrossing) || "crossing".equals(footway) || !isEmpty(crossing)) {
			return "CROSSING";
		}

		// ---- 4) highway=cycleway (separate path) ----
		if ("cycleway".equals(highway)) {
			if ("yes".equals(isSidepath)) return "CYCLEWAY_ADJOINING";
			if ("no".equals(isSidepath)) return "CYCLEWAY_ISOLATED";
			return "CYCLEWAY_ADJOINING_OR_ISOLATED";
		}

		// ---- 5) cycleway=* on highway (lane/track/opposite/protected/between_lanes) ----
		// protected / between_lanes are not fully standardized; match common values
		if (!isEmpty(cw)) {

			// opposite* (oneway bicycle contra-flow)
			if (isAnyOf(cw, "opposite", "opposite_lane", "opposite_track")) {
				// In your GH enum list you had no explicit "OPPOSITE", so map to a relevant class:
				// opposite_lane => on-highway lane, opposite_track => separated track
				if ("opposite_track".equals(cw)) return "CYCLEWAY_ADJOINING_OR_ISOLATED";
				return "CYCLEWAY_ON_HIGHWAY_ADVISORY_OR_EXCLUSIVE";
			}

			// track (usually separated)
			if ("track".equals(cw)) {
				// We don't know adjoining vs isolated without is_sidepath; keep the combined class
				return "CYCLEWAY_ADJOINING_OR_ISOLATED";
			}

			// lane
			if (isAnyOf(cw, "lane", "shared_lane")) {

				// protected lane (common tagging patterns)
				// Examples: cycleway:separation=kerb / bollard / buffer / "protected"
				String separation = firstNonEmpty(
					v(tags, "cycleway:separation"),
					direction == Direction.Forward ? v(tags, "cycleway:right:separation") : v(tags, "cycleway:left:separation"),
					v(tags, "cycleway:both:separation")
				);
				if (isAnyOf(separation, "kerb", "bollard", "buffer", "protected")) {
					return "CYCLEWAY_ON_HIGHWAY_PROTECTED";
				}

				// between_lanes (Mittellage)
				if (isAnyOf(
					v(tags, "cycleway:placement"),
					direction == Direction.Forward ? v(tags, "cycleway:right:placement") : v(tags, "cycleway:left:placement"),
					v(tags, "cycleway:both:placement")
					, "middle", "between_lanes")) {
					return "CYCLEWAY_ON_HIGHWAY_BETWEEN_LANES";
				}

				// exclusive/advisory
				if ("exclusive".equals(cwLaneType)) return "CYCLEWAY_ON_HIGHWAY_EXCLUSIVE";
				if ("advisory".equals(cwLaneType)) return "CYCLEWAY_ON_HIGHWAY_ADVISORY";

				return "CYCLEWAY_ON_HIGHWAY_ADVISORY_OR_EXCLUSIVE";
			}
		}

		// ---- 6) path/footway with bicycle/foot designated (shared/segregated) ----
		// Common case: highway=path with foot+bicycle designated
		if ("path".equals(highway) && "designated".equals(foot) && "designated".equals(bicycle)) {

			boolean isSeg = "yes".equals(segregated);
			if (isSeg) {
				// your doc mentions ..._ADJOINING_OR_ISOLATED (if is_sidepath missing)
				if ("yes".equals(isSidepath)) return "FOOT_AND_CYCLEWAY_SEGREGATED_ADJOINING";
				if ("no".equals(isSidepath)) return "FOOT_AND_CYCLEWAY_SEGREGATED_ISOLATED";
				return "FOOT_AND_CYCLEWAY_SEGREGATED_ADJOINING_OR_ISOLATED";
			} else {
				if ("yes".equals(isSidepath)) return "FOOT_AND_CYCLEWAY_SHARED_ADJOINING";
				if ("no".equals(isSidepath)) return "FOOT_AND_CYCLEWAY_SHARED_ISOLATED";
				return "FOOT_AND_CYCLEWAY_SHARED_ADJOINING_OR_ISOLATED";
			}
		}

		// ---- 7) footway with bicycle allowed ----
		if ("footway".equals(highway) && isAnyOf(bicycle, "yes", "designated")) {
			if ("yes".equals(isSidepath)) return "FOOTWAY_BICYCLE_YES_ADJOINING";
			if ("no".equals(isSidepath)) return "FOOTWAY_BICYCLE_YES_ISOLATED";
			// if unknown, keep a conservative fallback
			return "FOOTWAY_BICYCLE_YES_ADJOINING_OR_ISOLATED";
		}

		// ---- 8) bus lane variants (heuristic) ----
		// You can refine this if you know your exact tagging patterns.
		if ("yes".equals(v(tags, "busway")) || isAnyOf(v(tags, "busway:left"), "lane") || isAnyOf(v(tags, "busway:right"), "lane")) {
			// distinguish who is "with" whom using bicycle/bus access tags if present
			if (isAnyOf(bicycle, "yes", "designated")) {
				return "SHARED_BUS_LANE_BUS_WITH_BIKE";
			}
		}
		if (isAnyOf(v(tags, "cycleway:lane"), "share_busway", "shared_busway")) {
			return "SHARED_BUS_LANE_BIKE_WITH_BUS";
		}

		// ---- 9) shared motor vehicle lane (pictograms) ----
		// OSM tagging varies (e.g., cycleway=shared_lane already mapped above)
		if ("shared_lane".equals(cw)) {
			return "SHARED_MOTOR_VEHICLE_LANE";
		}

		// ---- 10) default ----
		// If tags indicate something bike-relevant but we couldn't map precisely:
		if (!isEmpty(cw) || isAnyOf(bicycle, "yes", "designated")) {
			return "NEEDS_CLARIFICATION";
		}

		return "NONE";
	}

	// -------- helpers ----------
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

	private static String firstNonEmpty(String... vals) {
		for (String v : vals) if (v != null && !v.isBlank()) return v;
		return "";
	}




}
