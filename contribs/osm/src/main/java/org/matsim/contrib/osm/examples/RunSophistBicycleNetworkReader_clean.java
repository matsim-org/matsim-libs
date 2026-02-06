package org.matsim.contrib.osm.examples;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.contrib.osm.networkReader.OsmBicycleReader;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.util.List;

public class RunSophistBicycleNetworkReader_clean {

	private static final String inputFile = "C://Users/metz_so/IdeaProjects/data/neukoelln.osm.pbf";
//    private static final String inputFile = "C://Users/metz_so/IdeaProjects/data/berlin-260122.osm.pbf";
	private static final String outputFile = "C://Users/metz_so/IdeaProjects/data/matsim-network_nk_bike_rules_slim.xml.gz";
	//private static final String outputFile = "C://Users/metz_so/IdeaProjects/data/matsim-network_berlin_bike_rules.xml.gz";

	private static final CoordinateTransformation ct =
		TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:25832");

	private static final List<String> TAGS_TO_COPY = List.of(
//		"bicycle",
//		"cycleway",
//		"cycleway:left",
//		"cycleway:right",
//		"cycleway:both",
//		"surface",
//		"smoothness",
//		"oneway",
//		"oneway:bicycle"
	);

	public static void main(String[] args) {

		var classifier = new BicycleInfraClassifier();
		var tagCopy = new TagCopy(TAGS_TO_COPY, "osm:");
		var policy = new BicycleLinkPolicy(classifier, tagCopy);

		Network network = new OsmBicycleReader.Builder()
			.setCoordinateTransformation(ct)
			.setAfterLinkCreated(policy::apply)
			.build()
			.read(inputFile);

		new NetworkWriter(network).write(outputFile);
	}
}





//package org.matsim.contrib.osm.examples;
//import org.matsim.contrib.osm.examples.BicycleInfraClassifier;
//
//
//import org.matsim.api.core.v01.TransportMode;
//import org.matsim.api.core.v01.network.Link;
//import org.matsim.api.core.v01.network.Network;
//import org.matsim.api.core.v01.network.NetworkWriter;
//import org.matsim.contrib.osm.networkReader.OsmBicycleReader;
//import org.matsim.contrib.osm.networkReader.SupersonicOsmNetworkReader.Direction;
//import org.matsim.core.network.NetworkUtils;
//import org.matsim.core.utils.geometry.CoordinateTransformation;
//import org.matsim.core.utils.geometry.transformations.TransformationFactory;
//
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//public class RunSophistBicycleNetworkReader_clean {
//
//	private static final String inputFile = "C://Users/metz_so/IdeaProjects/data/neukoelln.osm.pbf";
//	private static final String outputFile = "C://Users/metz_so/IdeaProjects/data/matsim-network_nk_bike_rules_NEW2.xml.gz";
//
//	private static final CoordinateTransformation coordinateTransformation =
//		TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:25832");
//
//	// Welche OSM-Tags du als Attribute behalten willst
//	private static final List<String> TAGS_TO_COPY = List.of(
//		//"highway",
//		"bicycle",
//		"cycleway",
//		"cycleway:left",
//		"cycleway:right",
//		"cycleway:both",
//		"surface",
//		"smoothness",
//		"oneway",
//		"oneway:bicycle"
//	);
//
//	public static void main(String[] args) {
//
//		BicycleInfraClassifier classifier = new BicycleInfraClassifier();
//
//		TagCopy tagCopy = new TagCopy(TAGS_TO_COPY, "osm:");
//
//		Network network = new OsmBicycleReader.Builder()
//			.setCoordinateTransformation(coordinateTransformation)
//			.setAfterLinkCreated((link, tags, direction) -> {
//
//				// (A) OSM tags in Link-Attribute kopieren
//				//copyTags(link, tags);
//				tagCopy.copy(link, tags);
//
//				// (B) OSM Way-ID (kommt NICHT aus tags, sondern aus ORIGID)
//				Object origWayId = link.getAttributes().getAttribute(NetworkUtils.ORIGID);
//				if (origWayId != null) {
//					link.getAttributes().putAttribute("osm:way_id", origWayId);
//				}
//
//				// (C) bicycle_infra klassifizieren
//				String infra = classifier.classify(tags, direction);
//				link.getAttributes().putAttribute("bicycle_infra", infra);
//
////				// Optional: wenn infra == NONE, bike entfernen (car lassen)
////				if ("NONE".equals(infra)) {
////					removeMode(link, TransportMode.bike);
////				}
//
//				// (D) footway/pedestrian: bike nur wenn bicycle=yes|designated
//				enforceFootwayPedestrianWhitelist(link, tags);
//
//				// (E) harte Sperre bicycle=no
//				if ("no".equals(tags.get("bicycle"))) {
//					removeMode(link, TransportMode.bike);
//
//					// extra reverse-bike link komplett deaktivieren
//					if (link.getId().toString().endsWith("_bike-reverse")) {
//						link.setAllowedModes(Set.of());
//						link.setCapacity(0);
//					}
//					return;
//				}
//
//				// (F) oneway-cleaning: nur für path/cycleway/footway
//				if (isBicycleOnewayRelevant(tags)) {
//
//					// Reverse-Link ("...r") soll nicht für bike gelten
//					if (link.getId().toString().endsWith("r")) {
//						removeMode(link, TransportMode.bike);
//					}
//
//					// extra _bike-reverse link (contraflow) killen
//					if (link.getId().toString().endsWith("_bike-reverse")) {
//						link.setAllowedModes(Set.of());
//						link.setCapacity(0);
//					}
//				}
//			})
//			.build()
//			.read(inputFile);
//
//		new NetworkWriter(network).write(outputFile);
//	}
//
////	private static void copyTags(Link link, Map<String, String> tags) {
////		for (String key : TAGS_TO_COPY) {
////			String v = tags.get(key);
////			if (v != null && !v.isBlank()) {
////				link.getAttributes().putAttribute("osm:" + key, v);
////			}
////		}
////	}
//
////	private static void enforceFootwayPedestrianWhitelist(Link link, Map<String, String> tags) {
////		String highway = tags.get("highway");
////		if (!("footway".equals(highway) || "pedestrian".equals(highway))) return;
////
////		String bicycle = tags.get("bicycle");
////		boolean bicycleOk = "yes".equals(bicycle) || "designated".equals(bicycle);
////		if (!bicycleOk) {
////			removeMode(link, TransportMode.bike);
////		}
////	}
////
////	private static void removeMode(Link link, String mode) {
////		HashSet<String> modes = new HashSet<>(link.getAllowedModes());
////		modes.remove(mode);
////		link.setAllowedModes(modes);
////	}
//
////	/**
////	 * Oneway-Policy: relevant nur für path/cycleway/footway.
////	 * If oneway:bicycle=yes => bike nur OSM-Richtung
////	 * If oneway=yes and oneway:bicycle!=no => bike nur OSM-Richtung
////	 */
////	private static boolean isBicycleOnewayRelevant(Map<String, String> tags) {
////		String h = tags.get("highway");
////		boolean isRelevant = "path".equals(h) || "cycleway".equals(h) || "footway".equals(h);
////		if (!isRelevant) return false;
////
////		if ("yes".equals(tags.get("oneway:bicycle"))) return true;
////
////		if ("yes".equals(tags.get("oneway"))) {
////			return !"no".equals(tags.get("oneway:bicycle"));
////		}
////		return false;
////	}
//}
