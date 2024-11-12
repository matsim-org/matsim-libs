package org.matsim.contrib.drt.schedule;

import com.google.common.base.MoreObjects;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleLoad;
import org.matsim.contrib.dvrp.optimizer.Request;

public class DefaultDrtStopTaskWithVehicleCapacityChange extends DefaultDrtStopTask implements CapacityChangeTask {

	private final DvrpVehicleLoad newVehicleCapacity;
	private final DvrpVehicleLoad previousVehicleCapacity;

	public DefaultDrtStopTaskWithVehicleCapacityChange(double beginTime, double endTime, Link link, DvrpVehicleLoad previousVehicleCapacity, DvrpVehicleLoad newVehicleCapacity) {
		super(beginTime, endTime, link);
		this.previousVehicleCapacity = previousVehicleCapacity;
		this.newVehicleCapacity = newVehicleCapacity;
	}

	@Override
	public DvrpVehicleLoad getNewVehicleCapacity() {
		return this.newVehicleCapacity;
	}

	@Override
	public DvrpVehicleLoad getPreviousVehicleCapacity() {
		return this.previousVehicleCapacity;
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
