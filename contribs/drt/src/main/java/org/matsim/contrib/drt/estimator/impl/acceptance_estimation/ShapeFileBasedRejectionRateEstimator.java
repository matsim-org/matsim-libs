package org.matsim.contrib.drt.estimator.impl.acceptance_estimation;

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

/**
 * The acceptance rate (in other words, the probability of rejection) is depending on the departure location of the trip.
 * The information is to be read from a shape file. Each feature in the shape file should have an attribute called "rej_rate", with
 * rejection rate (probability of rejection) as the value (value range [0,1]).
 * When no value can be found (e.g., location is not covered by any feature, or no attribute "rej_rate" is present), the baseRejectionRate will be used.
 * When multiple rejection rate presents for a location (linkId), then the minimum value will be used.
 */
public class ShapeFileBasedRejectionRateEstimator implements RejectionRateEstimator{
	private final double baseRejectionRate;
	private final static String REJECTION_RATE = "rej_rate";
	private final Map<Id<Link>, Double> probabilityOfRejectionForEachLink = new HashMap<>();

	public ShapeFileBasedRejectionRateEstimator(double baseRejectionRate, Network network, List<SimpleFeature> features) {
		this.baseRejectionRate = baseRejectionRate;
		initializeWaitingTimeMap(network, features);
	}

	private void initializeWaitingTimeMap(Network network, List<SimpleFeature> features) {
		for (Link link : network.getLinks().values()) {
			if (!link.getAllowedModes().contains(TransportMode.car) && !link.getAllowedModes().contains(TransportMode.drt)) {
				continue;
			}
			double minRejectionRate = baseRejectionRate;
			for (SimpleFeature feature : features) {
				Geometry geometry = (Geometry) feature.getDefaultGeometry();
				if (geometry.contains(MGC.coord2Point(link.getToNode().getCoord()))) {
					// The link is located within the zone -> reduce typical waiting time if necessary
					double rejectionRateForZone = (long) feature.getAttribute(REJECTION_RATE);
					if (rejectionRateForZone < minRejectionRate) {
						minRejectionRate = rejectionRateForZone;
					}
				}
			}
			probabilityOfRejectionForEachLink.put(link.getId(), minRejectionRate);
		}
	}

	@Override
	public double getEstimatedProbabilityOfRejection(Id<Link> fromLinkId, Id<Link> toLinkId, OptionalTime departureTime) {
		return probabilityOfRejectionForEachLink.get(fromLinkId);
	}
}
