package org.matsim.contrib.dvrp.passenger;

import java.util.Comparator;

import com.google.common.collect.ComparisonChain;

/**
 * @author michalm
 */
public class PassengerRequests {
	public static final Comparator<PassengerRequest> EARLIEST_START_TIME_COMPARATOR = (PassengerRequest r1,
			PassengerRequest r2) -> Double.compare(r1.getEarliestStartTime(), r2.getEarliestStartTime());

	// necessary for instance when TreeSet is used to store requests
	// (TreeSet uses comparisons instead of Object.equals(Object))
	public static final Comparator<PassengerRequest> ABSOLUTE_COMPARATOR = (PassengerRequest r1,
			PassengerRequest r2) -> ComparisonChain.start()
					.compare(r1.getEarliestStartTime(), r2.getEarliestStartTime())//
					.compare(r1.getLatestStartTime(), r2.getLatestStartTime())//
					.compare(r1.getSubmissionTime(), r2.getSubmissionTime())//
					.compare(r1.getId(), r2.getId()).result();

	public static final boolean isUrgent(PassengerRequest request, double now) {
		return request.getEarliestStartTime() <= now;
	}
}
