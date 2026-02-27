/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2025 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.contrib.drt.optimizer.insertion.parallel;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.optimizer.insertion.parallel.partitioner.RequestData;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import java.util.*;

/**
 * Handles conflict resolution for parallel DRT request insertion.
 * <p>
 * When multiple requests are assigned to the same vehicle in parallel,
 * conflicts arise. This class resolves these conflicts by selecting the
 * best insertion (lowest time loss) and marking others as conflicts
 * for retry or rejection.
 * <p>
 * The resolution follows these rules:
 * <ul>
 *   <li>For each vehicle, the request with the lowest total time loss is selected</li>
 *   <li>Ties are broken by comparing request IDs lexicographically</li>
 *   <li>All other requests targeting the same vehicle are marked as conflicts</li>
 * </ul>
 *
 * @author Steffen Axer
 */
public class ConflictResolver {

	/**
	 * Comparator for DrtRequest ordering: first by submission time, then by ID string.
	 */
	public static final Comparator<DrtRequest> DRT_REQUEST_COMPARATOR =
		Comparator.comparingDouble(DrtRequest::getSubmissionTime)
			.thenComparing(req -> req.getId().toString());

	private long nConflicting = 0;
	private long nNonConflicting = 0;

	/**
	 * Result of the conflict resolution process.
	 *
	 * @param toBeScheduled Requests that have no conflicts and can be scheduled
	 * @param toBeRejected  Requests that either had conflicts or no solution was found
	 * @param conflictCount Number of conflicts resolved in this consolidation (requests that lost to a better solution)
	 * @param noSolutionCount Number of requests that had no solution found
	 */
	public record ConsolidationResult(List<RequestData> toBeScheduled, Collection<DrtRequest> toBeRejected,
									  int conflictCount, int noSolutionCount) {
	}

	/**
	 * Internal result of the resolve step, separating conflicting from non-conflicting requests.
	 *
	 * @param noConflicts Requests without conflicts (best solution per vehicle)
	 * @param conflicts   Requests that conflict with a better solution for the same vehicle
	 */
	public record ResolvedConflicts(List<RequestData> noConflicts, List<RequestData> conflicts) {
	}

	/**
	 * Consolidates solutions and no-solutions into a final result.
	 * <p>
	 * This method resolves conflicts among the solutions (multiple requests assigned
	 * to the same vehicle) and combines them with requests that had no solution.
	 *
	 * @param solutions   Map from vehicle ID to sorted set of request data (sorted by time loss)
	 * @param noSolutions Set of requests for which no insertion was found
	 * @return ConsolidationResult containing requests to schedule and to reject
	 */
	public ConsolidationResult consolidate(
		Map<Id<DvrpVehicle>, SortedSet<RequestData>> solutions,
		SortedSet<DrtRequest> noSolutions) {

		Set<DrtRequest> allRejection = new TreeSet<>(DRT_REQUEST_COMPARATOR);
		allRejection.addAll(noSolutions);

		ResolvedConflicts resolvedConflicts = resolve(solutions);

		this.nConflicting += resolvedConflicts.conflicts.size();
		this.nNonConflicting += resolvedConflicts.noConflicts.size();

		// Remaining conflicts, add up into allRejection
		allRejection.addAll(
			resolvedConflicts.conflicts.stream()
				.map(RequestData::getDrtRequest)
				.toList()
		);

		return new ConsolidationResult(resolvedConflicts.noConflicts, allRejection,
			resolvedConflicts.conflicts.size(), noSolutions.size());
	}

	/**
	 * Resolves conflicts by selecting the best insertion per vehicle.
	 * <p>
	 * For each vehicle, the SortedSet is ordered by time loss (lowest first).
	 * The first element is the best solution and is added to noConflicts.
	 * All remaining elements are conflicts.
	 *
	 * @param data Map from vehicle ID to sorted set of request data
	 * @return ResolvedConflicts separating best solutions from conflicts
	 */
	ResolvedConflicts resolve(Map<Id<DvrpVehicle>, SortedSet<RequestData>> data) {
		List<RequestData> noConflicts = new ArrayList<>();
		List<RequestData> conflicts = new ArrayList<>();

		for (var requestDataList : data.values()) {
			if (requestDataList.isEmpty()) continue;

			var iterator = requestDataList.iterator();
			var bestSolution = iterator.next();
			noConflicts.add(bestSolution);

			while (iterator.hasNext()) {
				conflicts.add(iterator.next());
			}
		}

		return new ResolvedConflicts(noConflicts, conflicts);
	}

	/**
	 * @return Total number of conflicting requests encountered
	 */
	public long getConflictingCount() {
		return nConflicting;
	}

	/**
	 * @return Total number of non-conflicting requests encountered
	 */
	public long getNonConflictingCount() {
		return nNonConflicting;
	}

	/**
	 * @return Average share of conflicts (conflicting / total), or NaN if no requests processed
	 */
	public double getAverageConflictShare() {
		long total = nConflicting + nNonConflicting;
		return total > 0 ? nConflicting / (double) total : Double.NaN;
	}

	/**
	 * Resets the conflict statistics.
	 */
	public void resetStatistics() {
		this.nConflicting = 0;
		this.nNonConflicting = 0;
	}
}
