package org.matsim.application.prepare.counts;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.matsim.api.core.v01.network.Link;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public interface MatchingPoint {

	String getDirection();

	String getLinkDirection(Link l);

	Geometry getGeometry();

	default Link getClosestCandidate(Map<Link, Geometry> result) {

		if (result.isEmpty()) return null;
		if (result.size() == 1) return result.keySet().stream().findFirst().get();

		Geometry geometry = this.getGeometry();

		Predicate<Map.Entry<Link, Geometry>> filter = entry -> {
			String dir = this.getLinkDirection(entry.getKey());
			return dir.contains(this.getDirection());
		};

		Map<Link, Double> distances = result.entrySet().stream()
				.filter(filter)
				.collect(Collectors.toMap(Map.Entry::getKey, r -> r.getValue().distance(geometry)));

		Double min = Collections.min(distances.values());

		for (Map.Entry<Link, Double> entry : distances.entrySet()) {
			if(entry.getValue().doubleValue() == min.doubleValue()) {
				return entry.getKey();
			}
		}
		return null;
	}
}
