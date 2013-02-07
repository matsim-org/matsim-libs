package playground.thibautd.socnetsim.cliques.replanning.modules.jointtripinsertor;

import org.matsim.api.core.v01.Id;

final class ActedUponInformation {
	private final Id driverId, passengerId;
	//private final Activity driverDeparture, driverArrival;
	//private final Activity passengerDeparture, passengerArrival;

	ActedUponInformation(
			final Id driverId,
			//final Activity driverDeparture,
			//final Activity driverArrival,
			final Id passengerId) {
			//final Activity passengerDeparture,
			//final Activity passengerArrival) {
		this.driverId = driverId;
		//this.driverDeparture = driverDeparture;
		//this.driverArrival = driverArrival;
		this.passengerId = passengerId;
		//this.passengerDeparture = passengerDeparture;
		//this.passengerArrival = passengerArrival;
	}

	public Id getDriverId() {
		return driverId;
	}

	public Id getPassengerId() {
		return passengerId;
	}

	//public Activity getDriverDeparture() {
	//	return driverDeparture;
	//}

	//public Activity getDriverArrival() {
	//	return driverArrival;
	//}

	//public Activity getPassengerDeparture() {
	//	return passengerDeparture;
	//}

	//public Activity getPassengerArrival() {
	//	return passengerArrival;
	//}

	@Override
	public String toString() {
		return "[ActedUpon: driver="+driverId+", passenger"+passengerId+"]";
	}
}
