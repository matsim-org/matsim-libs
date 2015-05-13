package playground.thibautd.socnetsim.jointtrips.replanning.modules;

import org.matsim.api.core.v01.Id;

final class ActedUponInformation {
	private final Id driverId, passengerId;

	ActedUponInformation(
			final Id driverId,
			final Id passengerId) {
		this.driverId = driverId;
		this.passengerId = passengerId;
	}

	public Id getDriverId() {
		return driverId;
	}

	public Id getPassengerId() {
		return passengerId;
	}

	@Override
	public String toString() {
		return "[ActedUpon: driver="+driverId+", passenger"+passengerId+"]";
	}
}
