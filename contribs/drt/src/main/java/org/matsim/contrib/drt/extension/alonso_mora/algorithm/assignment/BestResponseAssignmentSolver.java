package org.matsim.contrib.drt.extension.alonso_mora.algorithm.assignment;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraRequest;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraTrip;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.assignment.AssignmentSolver.Solution.Status;

/**
 * Performs the assignment in a best-response fashion. Iteratively, the
 * algorithm goes over all trips of all the vehicle that have not been assigned
 * yet. The trip with the lowest cost is then assigned to the respective
 * vehicle, which does not take part in the next iteration. No external solver
 * or library is necessary to use this solver.
 * 
 * @author sebhoerl
 */
public class BestResponseAssignmentSolver implements AssignmentSolver {
	static public final String TYPE = "BestResponse";
	
	static private class TripComparator implements Comparator<AlonsoMoraTrip> {
		@Override
		public int compare(AlonsoMoraTrip a, AlonsoMoraTrip b) {
			return Double.compare(a.getResult().getCost(), b.getResult().getCost());
		}
	}

	@Override
	public Solution solve(Stream<AlonsoMoraTrip> candidates) {
		List<AlonsoMoraTrip> candidateList = candidates.collect(Collectors.toList());
		Collections.sort(candidateList, new TripComparator());

		List<AlonsoMoraTrip> result = new LinkedList<>();

		while (candidateList.size() > 0) {
			AlonsoMoraTrip trip = candidateList.remove(0);
			result.add(trip);

			Iterator<AlonsoMoraTrip> iterator = candidateList.iterator();

			while (iterator.hasNext()) {
				AlonsoMoraTrip nonChosenTrip = iterator.next();
				boolean removeNonChosenTrip = false;

				if (nonChosenTrip.getVehicle() == trip.getVehicle()) {
					removeNonChosenTrip = true;
				}

				for (AlonsoMoraRequest request : trip.getRequests()) {
					if (nonChosenTrip.getRequests().contains(request)) {
						removeNonChosenTrip = true;
						break;
					}
				}

				if (removeNonChosenTrip) {
					iterator.remove();
				}
			}
		}

		return new Solution(Status.OPTIMAL, result);
	}
}
