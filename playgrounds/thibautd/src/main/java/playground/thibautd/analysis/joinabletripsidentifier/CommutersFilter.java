package playground.thibautd.analysis.joinabletripsidentifier;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import playground.thibautd.analysis.joinabletripsidentifier.DataPloter.PassengerFilter;
import playground.thibautd.analysis.joinabletripsidentifier.JoinableTrips.TripRecord;

import java.util.ArrayList;
import java.util.List;

class CommutersFilter implements PassengerFilter {
	private static final String WORK_REGEXP = "w.*";
	private static final String HOME_REGEXP = "h.*";

	private final Network network;
	private final double minDistance;
	private final double maxDistance;

	/**
	 * @param network the network to use to compute distance information
	 * @param minTravelDistance the minimal trip length for a trip to be included
	 * in the list of trips to treat. Negative or 0-valued means no lower bound
	 * @param maxTravelDistance the maximal trip length for a trip to be included
	 * in the list of trips to treat. Negative or 0-valued means no upper bound
	 */
	public CommutersFilter(
			final Network network,
			final double minTravelDistance,
			final double maxTravelDistance) {
		this.network = network;
		this.minDistance = minTravelDistance;
		this.maxDistance = maxTravelDistance;
	}

	@Override
	public List<TripRecord> filterRecords(final JoinableTrips trips) {
		List<TripRecord> filtered = new ArrayList<TripRecord>();

		for (TripRecord record : trips.getTripRecords().values()) {
			// only add commuters
			if ( (record.getOriginActivityType().matches(HOME_REGEXP) &&
					 record.getDestinationActivityType().matches(WORK_REGEXP)) ||
					(record.getDestinationActivityType().matches(HOME_REGEXP) &&
					 record.getOriginActivityType().matches(WORK_REGEXP)) ) {
				// check for distance
				if (( (minDistance <= 0) || (minDistance <= record.getDistance(network)) ) &&
				 ( (maxDistance <= 0) || (maxDistance >= record.getDistance(network)) )) {
					// check for mode (pt simulation makes results difficult to interpret otherwise)
					if ( record.getMode().equals( TransportMode.car ) ) {
						filtered.add(record);
					 }
				}
			}
		}

		return filtered;
	}

	@Override
	public String getConditionDescription() {
		return "commuter passengers only"+
			(minDistance > 0 ?
				 ", trips longer than "+minDistance+"m" :
				 "")+
			(maxDistance > 0 ?
				 ", trips shorter than "+maxDistance+"m" :
				 "");
	}

	@Override
	public String toString() {
		return getConditionDescription();
	}
}