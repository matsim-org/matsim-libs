package playground.thibautd.analysis.joinabletripsidentifier;

import org.matsim.api.core.v01.network.Network;
import playground.thibautd.analysis.joinabletripsidentifier.DataPloter.DriverTripValidator;
import playground.thibautd.analysis.joinabletripsidentifier.JoinableTrips.JoinableTrip;

/**
 * Wraps a condition validator, and only accepts trips valid for this validator
 * and which obey to a max-distance criterion.
 */
class ShortDriverTripValidator implements DriverTripValidator {
	private final double maxDist;
	private JoinableTrips trips = null;
	private ConditionValidator validator = null;
	private final Network network;

	public ShortDriverTripValidator(
			final Network network,
			final double maxDist) {
		this.network = network;
		this.maxDist = maxDist;
	}

	@Override
	public void setJoinableTrips(final JoinableTrips joinableTrips) {
		trips = joinableTrips;
	}

	public void setValidator( final ConditionValidator validator ) {
		this.validator = validator;
	}

	@Override
	public boolean isValid(final JoinableTrip driverTrip) {
		return validator.isValid( driverTrip ) && trips.getTripRecords().get(
				driverTrip.getTripId() ).getDistance( network ) <= maxDist;
	}

	@Override
	public String getConditionDescription() {
		return "maximum driver trips distance: "+maxDist+"\n"+validator.getTailDescription();
	}
}