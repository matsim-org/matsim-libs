package org.matsim.contrib.taxi.optimizer.rules;

import com.google.common.base.MoreObjects;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.taxi.passenger.TaxiRequest;

public class DriverConfirmation {
	public final TaxiRequest request;
	public final DvrpVehicle vehicle;
	private final VrpPathWithTravelData pathToPickup;

	final double endTime; // DriverConfirmation is due at this moment

	private boolean isComplete = false;
	private boolean isAccepted = false;

	DriverConfirmation(TaxiRequest request, DvrpVehicle vehicle, VrpPathWithTravelData pathToPickup, double endTime) {
		this.request = request;
		this.vehicle = vehicle;
		this.pathToPickup = pathToPickup;
		this.endTime = endTime;
	}

	public boolean isComplete() {
		return isComplete;
	}

	public boolean isAccepted() {
		return isAccepted;
	}

	public void setComplete(boolean isAccepted) {
		this.isComplete = true;
		this.isAccepted = isAccepted;
	}

	public VrpPathWithTravelData getPathToPickup(double now) {
		return pathToPickup.getDepartureTime() < now ? pathToPickup.withDepartureTime(now) : pathToPickup;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("request", request)
				.add("vehicle", vehicle)
				.add("endTime", endTime)
				.add("isComplete", isComplete)
				.add("isAccepted", isAccepted)
				.toString();
	}
}
