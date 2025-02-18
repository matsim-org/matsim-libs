package org.matsim.contrib.drt.schedule;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.dvrp.load.DvrpLoad;
import org.matsim.contrib.dvrp.optimizer.Request;

import com.google.common.base.MoreObjects;

/**
 * Represents a {@link DefaultDrtStopTask} where no person is picked-up or
 * dropped-off and the vehicle's capacity is changed (by implementing the
 * {@link DrtCapacityChangeTask} interface).
 * Calling the methods below will throw an
 * {@link UnsupportedOperationException}:
 * - {@link DefaultDrtCapacityChangeTask#addPickupRequest(AcceptedDrtRequest)}
 * - {@link DefaultDrtCapacityChangeTask#addDropoffRequest(AcceptedDrtRequest)}
 * - {@link DefaultDrtCapacityChangeTask#removePickupRequest(Id)}
 * - {@link DefaultDrtCapacityChangeTask#removeDropoffRequest(Id)}
 * 
 * @author Tarek Chouaki (tkchouaki), IRT SystemX
 */
public class DefaultDrtCapacityChangeTask extends DefaultDrtStopTask implements DrtCapacityChangeTask {

	private final DvrpLoad changedCapacity;

	public DefaultDrtCapacityChangeTask(double beginTime, double endTime, Link link, DvrpLoad changedCapacity) {
		super(beginTime, endTime, link);
		this.changedCapacity = changedCapacity;
	}

	@Override
	public DvrpLoad getChangedCapacity() {
		return this.changedCapacity;
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
