package org.matsim.contrib.drt.schedule;

import com.google.common.base.MoreObjects;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.dvrp.fleet.DvrpLoad;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.schedule.CapacityChangeTask;

public class DefaultDrtStopTaskWithVehicleCapacityChange extends DefaultDrtStopTask implements DrtStopTaskWithVehicleCapacityChange {

	private final DvrpLoad newVehicleCapacity;

	public DefaultDrtStopTaskWithVehicleCapacityChange(double beginTime, double endTime, Link link, DvrpLoad newVehicleCapacity) {
		super(beginTime, endTime, link);
		this.newVehicleCapacity = newVehicleCapacity;
	}

	@Override
	public DvrpLoad getNewVehicleCapacity() {
		return this.newVehicleCapacity;
	}

	@Override
	public void addDropoffRequest(AcceptedDrtRequest request) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addPickupRequest(AcceptedDrtRequest request) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("dropoffRequests", newVehicleCapacity)
			.add("super", super.toString())
			.toString();
	}

	@Override
	public void removePickupRequest(Id<Request> requestId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeDropoffRequest(Id<Request> requestId) {
		throw new UnsupportedOperationException();
	}
}
