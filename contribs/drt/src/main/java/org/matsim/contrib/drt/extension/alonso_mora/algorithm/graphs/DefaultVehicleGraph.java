package org.matsim.contrib.drt.extension.alonso_mora.algorithm.graphs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraRequest;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraStop.StopType;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraTrip;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraVehicle;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.function.AlonsoMoraFunction;

import com.google.common.base.Verify;

/**
 * Default implementation for the trip-vehicle graph which creates all possible
 * trips (routes) for the requests and one vehicle.
 * 
 * @author sebhoerl
 */
public class DefaultVehicleGraph implements VehicleGraph {
	private final RequestGraph requestGraph;
	private final AlonsoMoraFunction function;

	private final AlonsoMoraVehicle vehicle;

	private final List<List<AlonsoMoraTrip>> trips = new ArrayList<>();
	private final Set<AlonsoMoraRequest> requests = new HashSet<>();

	public DefaultVehicleGraph(AlonsoMoraFunction function, RequestGraph requestGraph, AlonsoMoraVehicle vehicle) {
		this.vehicle = vehicle;
		this.requestGraph = requestGraph;
		this.function = function;

		ensureTripListSize(vehicle.getVehicle().getCapacity() * 2);
	}

	private void ensureTripListSize(int numberOfPassengers) {
		while (trips.size() < numberOfPassengers) {
			trips.add(new LinkedList<>());
		}
	}

	@Override
	public void addRequest(AlonsoMoraRequest request, double now) {
		Optional<AlonsoMoraFunction.Result> unpooledResult = function.calculateRoute(Arrays.asList(request), vehicle,
				now);

		if (unpooledResult.isPresent()) {
			addRequest(request, now, unpooledResult.get());
		}
	}

	@Override
	public void addRequest(AlonsoMoraRequest request, double now, AlonsoMoraFunction.Result unpooledResult) {
		Verify.verify(requests.add(request), "Request is already registered");

		trips.get(0).add(new AlonsoMoraTrip(vehicle, Arrays.asList(request), unpooledResult));
		List<AlonsoMoraTrip> currentLevelTrips = new LinkedList<>();

		for (AlonsoMoraRequest pairableRequest : requestGraph.getShareableRequests(request)) {
			if (requests.contains(pairableRequest)) {
				Optional<AlonsoMoraFunction.Result> pairedResult = function
						.calculateRoute(Arrays.asList(request, pairableRequest), vehicle, now);

				if (pairedResult.isPresent()) {
					AlonsoMoraTrip trip = new AlonsoMoraTrip(vehicle, Arrays.asList(request, pairableRequest),
							pairedResult.get());
					trips.get(1).add(trip);
					currentLevelTrips.add(trip);
				}
			}
		}

		if (currentLevelTrips.size() > 0) {
			constructTrips(currentLevelTrips, 2, now);
		}
	}

	@Override
	public void removeRequest(AlonsoMoraRequest request) {
		Verify.verify(requests.remove(request), "Request is not registered");

		for (int i = 0; i < trips.size(); i++) {
			Iterator<AlonsoMoraTrip> tripIterator = trips.get(i).iterator();

			while (tripIterator.hasNext()) {
				AlonsoMoraTrip trip = tripIterator.next();

				if (trip.getRequests().contains(request)) {
					tripIterator.remove();
				}
			}
		}
	}

	private void constructTrips(List<AlonsoMoraTrip> previousLevelTrips, int level, double now) {
		ensureTripListSize(level + 1);
		Collections.sort(previousLevelTrips);
		List<AlonsoMoraTrip> currentLevelTrips = new ArrayList<>();

		for (int i = 0; i < previousLevelTrips.size(); i++) {
			for (int j = i + 1; j < previousLevelTrips.size(); j++) {
				AlonsoMoraTrip firstTrip = previousLevelTrips.get(i);
				AlonsoMoraTrip secondTrip = previousLevelTrips.get(j);

				Set<AlonsoMoraRequest> requests = new HashSet<>();
				requests.addAll(firstTrip.getRequests());
				requests.addAll(secondTrip.getRequests());

				if (requests.size() == firstTrip.getRequests().size() + 1) {
					// One more requests on a trip

					boolean allSubtripsExist = true;
					List<AlonsoMoraRequest> requestList = new ArrayList<>(requests);
					Collections.sort(requestList);

					boolean alreadyExists = false;

					for (int k = 0; k < currentLevelTrips.size() && !alreadyExists; k++) {
						if (checkSame(requestList, currentLevelTrips.get(k).getRequests())) {
							alreadyExists = true;
							break;
						}
					}

					if (!alreadyExists) {
						for (int k = 0; k < requests.size() && allSubtripsExist; k++) {
							List<AlonsoMoraRequest> reducedList = new ArrayList<>(requestList);
							reducedList.remove(k);

							boolean foundSubtrip = false;

							for (AlonsoMoraTrip trip : trips.get(level - 1)) {
								if (checkSame(reducedList, trip.getRequests())) {
									foundSubtrip = true;
									break;
								}
							}

							if (!foundSubtrip) {
								allSubtripsExist = false;
							}
						}

						if (allSubtripsExist) {
							Optional<AlonsoMoraFunction.Result> result = function.calculateRoute(requestList, vehicle,
									now);

							if (result.isPresent()) {
								AlonsoMoraTrip trip = new AlonsoMoraTrip(vehicle, requestList, result.get());
								trips.get(level).add(trip);
								currentLevelTrips.add(trip);
							}
						}
					}
				}
			}
		}

		if (currentLevelTrips.size() > 0) {
			constructTrips(currentLevelTrips, level + 1, now);
		}
	}

	/**
	 * This assumes that the lists are already ordered!
	 */
	private boolean checkSame(List<AlonsoMoraRequest> first, List<AlonsoMoraRequest> second) {
		for (int u = 0; u < first.size(); u++) {
			if (first.get(u) != second.get(u)) {
				return false;
			}
		}

		return true;
	}

	@Override
	public void preserveVehicleAssignment(double now) {
		List<AlonsoMoraRequest> assignedRequests = vehicle.getRoute().stream() //
				.filter(s -> s.getType().equals(StopType.Pickup)) //
				.map(s -> s.getRequest()) //
				.filter(r -> !r.isPickedUp()) //
				.sorted() //
				.collect(Collectors.toList());

		Collections.sort(assignedRequests);

		if (assignedRequests.size() > 0) {
			for (AlonsoMoraTrip candidate : trips.get(assignedRequests.size() - 1)) {
				if (checkSame(candidate.getRequests(), assignedRequests)) {
					return; // Sequence is already included in the optimization set.
				}
			}

			// Sequence does not exist anymore, so add it manually if feasible.
			Optional<AlonsoMoraFunction.Result> result = function.calculateRoute(assignedRequests, vehicle, now);

			if (result.isPresent()) {
				AlonsoMoraTrip trip = new AlonsoMoraTrip(vehicle, assignedRequests, result.get());
				trips.get(assignedRequests.size() - 1).add(trip);
			}
		}
	}

	@Override
	public Stream<AlonsoMoraTrip> stream() {
		return trips.stream().flatMap(list -> list.stream());
	}

	@Override
	public int getSize() {
		return trips.stream().mapToInt(t -> t.size()).sum();
	}
}
