package org.matsim.contrib.drt.extension.alonso_mora;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.optimizer.Request;

/**
 * Simple event handler used to track the requests that are aggregated for the
 * dispatcher.
 */
class RequestAggregationHandler implements AlonsoMoraSubmissionEventHandler {
	private final List<Set<Id<Request>>> requests = new LinkedList<>();

	@Override
	public void handleEvent(AlonsoMoraSubmissionEvent event) {
		requests.add(event.getRequestIds());
	}

	public List<Set<Id<Request>>> consolidate() {
		List<Set<Id<Request>>> returnValue = new ArrayList<>(requests);
		requests.clear();
		return returnValue;
	}
}
