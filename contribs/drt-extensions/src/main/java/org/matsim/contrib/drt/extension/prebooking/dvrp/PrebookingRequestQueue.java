package org.matsim.contrib.drt.extension.prebooking.dvrp;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.dvrp.passenger.RequestQueue;

/**
 * @author Michal Maciejewski (michalm)
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public final class PrebookingRequestQueue<R extends PassengerRequest> implements RequestQueue<R> {
	private final PrebookingManager prebookingManager;
	private final RequestQueue<R> delegate;

	private final List<R> schedulableRequests = new LinkedList<>();

	public PrebookingRequestQueue(PrebookingManager prebookingManager, RequestQueue<R> delegate) {
		this.prebookingManager = prebookingManager;
		this.delegate = delegate;
	}

	@Override
	public void updateQueuesOnNextTimeSteps(double currentTime) {
		delegate.updateQueuesOnNextTimeSteps(currentTime);
	}

	@Override
	public void addRequest(R request) {
		if (prebookingManager.isPrebookedRequest(request.getId())) {
			// add to queue in the order of submission
			schedulableRequests.add(request);
		} else {
			delegate.addRequest(request);
		}
	}

	@Override
	public Collection<R> getSchedulableRequests() {
		schedulableRequests.addAll(delegate.getSchedulableRequests());
		delegate.getSchedulableRequests().clear();
		return schedulableRequests;
	}
}
