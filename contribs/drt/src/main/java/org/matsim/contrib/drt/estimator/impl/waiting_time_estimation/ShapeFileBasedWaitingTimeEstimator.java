package org.matsim.contrib.drt.estimator.impl.waiting_time_estimation;

import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.misc.OptionalTime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShapeFileBasedWaitingTimeEstimator implements WaitingTimeEstimator {
	/**
	 * Typical waiting time. Due to the length limit of the attribute name, we have to use some abbreviate
	 */
	private final static String TYPICAL_WAITING_TIME_NAME = "typ_wt";
	private final Map<Id<Link>, Double> typicalWaitingTimeForEachLink = new HashMap<>();
	/**
	 * The default typical waiting time. This value will be used for links that are not covered by any
	 * waiting time zone. The baseTypicalWaitingTime is usually larger than the typical waiting time
	 * in any waiting time zone.
	 */
	private final double baseTypicalWaitingTime;

	public ShapeFileBasedWaitingTimeEstimator(Network network, List<SimpleFeature> features) {
		baseTypicalWaitingTime = 1800;
		initializeWaitingTimeMap(network, features);
	}

	public ShapeFileBasedWaitingTimeEstimator(Network network, List<SimpleFeature> features, double baseTypicalWaitingTime) {
		this.baseTypicalWaitingTime = baseTypicalWaitingTime;
		initializeWaitingTimeMap(network, features);
	}

	private void initializeWaitingTimeMap(Network network, List<SimpleFeature> features) {
		for (Link link : network.getLinks().values()) {
			if (!link.getAllowedModes().contains(TransportMode.car) && !link.getAllowedModes().contains(TransportMode.drt)) {
				continue;
			}
			double minTypicalWaitingTime = baseTypicalWaitingTime;
			for (SimpleFeature feature : features) {
				Geometry geometry = (Geometry) feature.getDefaultGeometry();
				if (geometry.contains(MGC.coord2Point(link.getToNode().getCoord()))) {
					// The link is located within the zone -> reduce typical waiting time if necessary
					double typicalWaitingTimeForCurrentZone = (long) feature.getAttribute(TYPICAL_WAITING_TIME_NAME);
					if (typicalWaitingTimeForCurrentZone < minTypicalWaitingTime) {
						minTypicalWaitingTime = typicalWaitingTimeForCurrentZone;
					}
				}
			}
			typicalWaitingTimeForEachLink.put(link.getId(), minTypicalWaitingTime);
		}
	}

	@Override
	public double estimateWaitTime(Id<Link> fromLinkId, Id<Link> toLinkId, OptionalTime departureTime) {
		return typicalWaitingTimeForEachLink.get(fromLinkId);
	}
}
