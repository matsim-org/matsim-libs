package org.matsim.contrib.drt.extension.alonso_mora.algorithm.assignment;

import java.util.Collection;
import java.util.stream.Stream;

import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraTrip;

/**
 * Solves the assignment problem given a list of potential vehicle trips.
 * 
 * @author sebhoerl
 */
public interface AssignmentSolver {
	Solution solve(Stream<AlonsoMoraTrip> candidates);

	static public class Solution {
		public final Status status;
		public final Collection<AlonsoMoraTrip> trips;

		public enum Status {
			OPTIMAL, FEASIBLE, FAILURE
		}

		public Solution(Status status, Collection<AlonsoMoraTrip> trips) {
			this.status = status;
			this.trips = trips;
		}
	}
}
