package playground.thibautd.analysis.joinabletripsidentifier;

import org.matsim.api.core.v01.network.Network;
import playground.thibautd.analysis.joinabletripsidentifier.DataPloter.TwofoldTripValidator;
import playground.thibautd.analysis.joinabletripsidentifier.JoinableTrips.JoinableTrip;

class ConditionValidator implements TwofoldTripValidator {
	private final AcceptabilityCondition condition;
	// joinable trips corresponding to a walk distance > alphaWalk * bee_fly_trip_distance
	// will be considered invalid (this should be below 1)
	private final double alphaWalk;
	private final Network network;
	private JoinableTrips joinableTrips = null;

	public ConditionValidator(
			final Network network,
			final int distance,
			final int time) {
		this( network , distance , time , Double.POSITIVE_INFINITY );
	}

	public ConditionValidator(
			final Network network,
			final int distance,
			final int time,
			final double alphaWalk) {
		this.condition = new AcceptabilityCondition(distance, time);
		this.alphaWalk = alphaWalk;
		this.network = network;
	}

	@Override
	public void setJoinableTrips(final JoinableTrips joinableTrips) {
		this.joinableTrips = joinableTrips;
	}

	@Override
	public boolean isValid(final JoinableTrip driverTrip) {
		TripInfo tripInfo = driverTrip.getFullfilledConditionsInfo().get(condition);
		boolean conditionVerified = tripInfo != null;

		if (conditionVerified) {
			double walk = tripInfo.getMinPuWalkDistance();
			walk += tripInfo.getMinDoWalkDistance();
			conditionVerified = walk < alphaWalk * joinableTrips.getTripRecords().get( driverTrip.getPassengerTripId() ).getDistance( network );
		}

		return conditionVerified;
	}

	@Override
	public String getConditionDescription() {
		return "all drivers\n"+getTailDescription();
	}

	public String getTailDescription() {
		return "acceptable distance = "+condition.getDistance()+" m"+
			"\nacceptable time = "+(condition.getTime()/60d)+" min"+
			"\nalpha walk = "+alphaWalk;
	}

	@Override
	public String toString() {
		return getConditionDescription();
	}

	@Override
	public Label getFirstCriterion() {
		return new Label(condition.getDistance(), "", "m");
	}

	@Override
	public Label getSecondCriterion() {
		return new Label(condition.getTime()/60d, "", "min");
	}

	@Override
	public boolean equals(final Object object) {
		if ( !(object instanceof ConditionValidator) ) {
			return false;
		}

		AcceptabilityCondition otherCondition = ((ConditionValidator) object).condition;
		return condition.equals(otherCondition);
	}

	@Override
	public int hashCode() {
		return condition.hashCode();
	}
}