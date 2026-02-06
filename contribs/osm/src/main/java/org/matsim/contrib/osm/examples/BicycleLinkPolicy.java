package org.matsim.contrib.osm.examples;


import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.osm.networkReader.SupersonicOsmNetworkReader.Direction;
import org.matsim.core.network.NetworkUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class BicycleLinkPolicy {

	private final BicycleInfraClassifier classifier;
	private final TagCopy tagCopy;

	public BicycleLinkPolicy(BicycleInfraClassifier classifier, TagCopy tagCopy) {
		this.classifier = classifier;
		this.tagCopy = tagCopy;
	}

	public void apply(Link link, Map<String,String> tags, Direction direction) {

		// 0) optional: OSM tags kopieren (nur wenn du das wirklich brauchst)
		tagCopy.copy(link, tags);

		// 1) bicycle_infra
		String infra = classifier.classify(tags, direction);
		link.getAttributes().putAttribute("bicycle_infra", infra);

//		// 2) infra NONE -> bike raus
//		if ("NONE".equals(infra)) {
//			removeMode(link, TransportMode.bike);
//		}

		// 3) Regeln für footway/pedestrian: bike nur wenn yes/designated
		enforceFootwayPedestrianWhitelist(link, tags);

		// 4) bicycle=no -> bike raus + ggf. _bike-reverse deaktivieren
		if ("no".equals(tags.get("bicycle"))) {
			removeMode(link, TransportMode.bike);
			if (link.getId().toString().endsWith("_bike-reverse")) {
				link.setAllowedModes(Set.of());
				link.setCapacity(0);
			}
			return;
		}

		// 5) oneway-cleaning (nur path/cycleway/footway)
		if (isBicycleOnewayRelevant(tags)) {
			if (link.getId().toString().endsWith("r")) {
				removeMode(link, TransportMode.bike);
			}
			if (link.getId().toString().endsWith("_bike-reverse")) {
				link.setAllowedModes(Set.of());
				link.setCapacity(0);
			}
		}

//		// 6) OSM Way-ID robust rausgeben (falls du sie brauchst)
//		// Reader schreibt ORIGID (original way id)
//		Object origId = link.getAttributes().getAttribute(NetworkUtils.ORIGID);
//		if (origId != null) link.getAttributes().putAttribute("osm:way_id", origId);
	}

	private static void enforceFootwayPedestrianWhitelist(Link link, Map<String,String> tags) {
		String h = tags.get("highway");
		if (!("footway".equals(h) || "pedestrian".equals(h))) return;

		String bicycle = tags.get("bicycle");
		boolean ok = "yes".equals(bicycle) || "designated".equals(bicycle);
		if (!ok) removeMode(link, TransportMode.bike);
	}

	private static void removeMode(Link link, String mode) {
		var modes = new HashSet<>(link.getAllowedModes());
		modes.remove(mode);
		link.setAllowedModes(modes);
	}

	private static boolean isBicycleOnewayRelevant(Map<String,String> tags) {
		String h = tags.get("highway");
		boolean relevant = "path".equals(h) || "cycleway".equals(h) || "footway".equals(h);
		if (!relevant) return false;

		if ("yes".equals(tags.get("oneway:bicycle"))) return true;
		if ("yes".equals(tags.get("oneway"))) {
			return !"no".equals(tags.get("oneway:bicycle"));
		}
		return false;
	}
}
