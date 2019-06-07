package org.matsim.contrib.dvrp.passenger;

import java.util.Comparator;

import org.matsim.contrib.dvrp.optimizer.Request;

/**
 * @author michalm
 */
public class PassengerRequests {
	public static final Comparator<PassengerRequest> EARLIEST_START_TIME_COMPARATOR = Comparator
			.comparing(PassengerRequest::getEarliestStartTime);

	// necessary for instance when TreeSet is used to store requests
	// (TreeSet uses comparisons instead of Object.equals(Object))
	public static final Comparator<PassengerRequest> ABSOLUTE_COMPARATOR = Comparator
			.comparing(PassengerRequest::getEarliestStartTime)//
			.thenComparing(PassengerRequest::getLatestStartTime)//
			.thenComparing(Request::getSubmissionTime)//
			.thenComparing(Request::getId);

	public static final boolean isUrgent(PassengerRequest request, double now) {
		return request.getEarliestStartTime() <= now;
	}
}
