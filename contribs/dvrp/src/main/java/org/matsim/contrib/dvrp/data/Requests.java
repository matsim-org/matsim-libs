package org.matsim.contrib.dvrp.data;

import java.util.Comparator;

import com.google.common.base.Predicate;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Iterables;

/**
 * @author michalm
 */
public class Requests {
	public static final Comparator<Request> T0_COMPARATOR = (Request r1, Request r2) -> Double
			.compare(r1.getEarliestStartTime(), r2.getEarliestStartTime());

	public static final Comparator<Request> T1_COMPARATOR = (Request r1, Request r2) -> Double
			.compare(r1.getLatestStartTime(), r2.getLatestStartTime());

	public static final Comparator<Request> SUBMISSION_TIME_COMPARATOR = (Request r1, Request r2) -> Double
			.compare(r1.getSubmissionTime(), r2.getSubmissionTime());

	// necessary for instance when TreeSet is used to store requests
	// (TreeSet uses comparisons instead of Object.equals(Object))
	public static final Comparator<Request> ABSOLUTE_COMPARATOR = (Request r1, Request r2) -> ComparisonChain.start()
			.compare(r1.getEarliestStartTime(), r2.getEarliestStartTime())//
			.compare(r1.getLatestStartTime(), r2.getLatestStartTime())//
			.compare(r1.getSubmissionTime(), r2.getSubmissionTime())//
			.compare(r1.getId(), r2.getId()).result();

	public static final boolean isUrgent(Request request, double now) {
		return request.getEarliestStartTime() <= now;
	}

	public static int countRequests(Iterable<? extends Request> requests, Predicate<Request> predicate) {
		return Iterables.size(Iterables.filter(requests, predicate));
	}
}
