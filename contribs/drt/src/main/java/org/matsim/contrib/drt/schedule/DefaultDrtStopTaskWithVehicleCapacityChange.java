package org.matsim.contrib.drt.schedule;

import com.google.common.base.MoreObjects;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.dvrp.fleet.dvrp_load.DvrpLoad;
import org.matsim.contrib.dvrp.optimizer.Request;

/**
 * Represents a {@link DefaultDrtStopTask} where no person is picked-up or dropped-off and the vehicle's capacity is changed (by implementing the {@link DrtStopTaskWithVehicleCapacityChange} interface).
 * Calling the methods below will throw an {@link UnsupportedOperationException}:
 * - {@link DefaultDrtStopTaskWithVehicleCapacityChange#addPickupRequest(AcceptedDrtRequest)}
 * - {@link DefaultDrtStopTaskWithVehicleCapacityChange#addDropoffRequest(AcceptedDrtRequest)}
 * - {@link DefaultDrtStopTaskWithVehicleCapacityChange#removePickupRequest(Id)}
 * - {@link DefaultDrtStopTaskWithVehicleCapacityChange#removeDropoffRequest(Id)}
 * @author Tarek Chouaki (tkchouaki)
 */
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
