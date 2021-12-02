package org.matsim.contrib.drt.extension.alonso_mora;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.contrib.dvrp.optimizer.Request;

/**
 * For computational performance, this implementation of the algorithm by
 * Alonso-Mora et al. aggregates requests with the same origin and destination
 * into group requests. This event is published whenever an aggregated request
 * arrives at the dispatcher to allow for a post-processing analysis of
 * aggregated requests.
 * 
 * @author sebhoerl
 */
public class AlonsoMoraSubmissionEvent extends Event {
	static public final String EVENT_TYPE = "alonso mora submission";

	static public final String REQUEST_IDS = "requestIds";

	private final Set<Id<Request>> requestIds;

	public AlonsoMoraSubmissionEvent(double time, Set<Id<Request>> requestIds) {
		super(time);
		this.requestIds = requestIds;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public Set<Id<Request>> getRequestIds() {
		return requestIds;
	}

	public Map<String, String> getAttributes() {
		Map<String, String> attributes = super.getAttributes();
		attributes.put(REQUEST_IDS, requestIds.stream().map(id -> id.toString()).collect(Collectors.joining(",")));
		return attributes;
	}

	static public AlonsoMoraSubmissionEvent convert(Event event) {
		return new AlonsoMoraSubmissionEvent(event.getTime(),
				Arrays.asList(((String) event.getAttributes().get(REQUEST_IDS)).split(",")).stream()
						.map(id -> Id.create(id, Request.class)).collect(Collectors.toSet()));
	}
}
