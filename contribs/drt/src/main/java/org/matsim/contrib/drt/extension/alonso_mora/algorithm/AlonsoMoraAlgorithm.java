package org.matsim.contrib.drt.extension.alonso_mora.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.alonso_mora.AlonsoMoraConfigGroup;
import org.matsim.contrib.drt.extension.alonso_mora.AlonsoMoraSubmissionEvent;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraStop.StopType;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.assignment.AssignmentSolver;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.assignment.AssignmentSolver.Solution;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.function.AlonsoMoraFunction;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.function.AlonsoMoraFunction.Result;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.function.RouteTracker;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.graphs.DefaultRequestGraph;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.graphs.DefaultVehicleGraph;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.graphs.RequestGraph;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.graphs.VehicleGraph;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.relocation.RelocationSolver;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.relocation.RelocationSolver.Relocation;
import org.matsim.contrib.drt.extension.alonso_mora.scheduling.AlonsoMoraScheduler;
import org.matsim.contrib.drt.extension.alonso_mora.travel_time.TravelTimeEstimator;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DefaultDrtStopTask;
import org.matsim.contrib.drt.scheduler.EmptyVehicleRelocator;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.schedule.Task.TaskStatus;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.core.api.experimental.events.EventsManager;

import com.google.common.base.Verify;

/**
 * This class performs all the request and vehicle management and bookkeeping
 * tasks to run the fleet control strategy described in
 *
 * Alonso-Mora, J., Samaranayake, S., Wallar, A., Frazzoli, E., Rus, D., 2017.
 * On-demand high-capacity ride-sharing via dynamic trip-vehicle assignment.
 * Proc Natl Acad Sci USA 114, 462–467. https://doi.org/10.1073/pnas.1611675114
 *
 */
public class AlonsoMoraAlgorithm {
	private final Logger logger = Logger.getLogger(AlonsoMoraAlgorithm.class);

	private RequestGraph requestGraph;
	private final AssignmentSolver assignmentSolver;
	private final RelocationSolver rebalancingSolver;
	private final AlonsoMoraScheduler scheduler;
	private final AlonsoMoraFunction function;
	private final ForkJoinPool forkJoinPool;

	private final EventsManager eventsManager;
	private final String mode;

	private final List<AlonsoMoraVehicle> vehicles;
	private final Map<AlonsoMoraVehicle, VehicleGraph> vehicleGraphs = new TreeMap<>();

	private final Set<AlonsoMoraRequest> queuedRequests = new TreeSet<>();
	private final Set<AlonsoMoraRequest> assignedRequests = new TreeSet<>();
	private final Set<AlonsoMoraRequest> onboardRequests = new TreeSet<>();
	private final Set<AlonsoMoraVehicle> relocatingVehicles = new TreeSet<>();

	private int numberOfServedRequests = 0;
	private int numberOfRejectedRequests = 0;

	private final int maximumOccupancy;

	private final TravelTimeEstimator travelTimeEstimator;
	private final double stopDuration;

	private final AlgorithmSettings settings;

	public AlonsoMoraAlgorithm(Fleet fleet, AssignmentSolver assignmentSolver, RelocationSolver rebalancingSolver,
			AlonsoMoraFunction function, AlonsoMoraScheduler scheduler, EventsManager eventsManager, String mode,
			AlonsoMoraVehicleFactory vehicleFactory, ForkJoinPool forkJoinPool, TravelTimeEstimator travelTimeEstimator,
			double stopDuration, AlgorithmSettings settings) {
		this.assignmentSolver = assignmentSolver;
		this.rebalancingSolver = rebalancingSolver;
		this.scheduler = scheduler;
		this.eventsManager = eventsManager;
		this.mode = mode;
		this.function = function;
		this.forkJoinPool = forkJoinPool;
		this.travelTimeEstimator = travelTimeEstimator;
		this.stopDuration = stopDuration;
		this.settings = settings;

		// Create vehicle wrappers
		vehicles = new ArrayList<>(fleet.getVehicles().size());

		for (DvrpVehicle vehicle : fleet.getVehicles().values()) {
			vehicles.add(vehicleFactory.createVehicle(vehicle));
		}

		Collections.sort(vehicles, (a, b) -> a.getVehicle().getId().compareTo(b.getVehicle().getId()));

		maximumOccupancy = vehicles.stream().mapToInt(v -> v.getVehicle().getCapacity()).max().orElse(0);
		updateRequestGraph(Double.NEGATIVE_INFINITY, new Information(maximumOccupancy));
	}

	/**
	 * The main purpose of this method is to check whether vehicles are (still)
	 * relocating. Later on, this may define whether the vehicle is available for
	 * assignment or a different relocation tasks
	 */
	private void updateVehiclesBeforeAssignment() {
		relocatingVehicles.clear();

		/*
		 * We do not work with a simple flag here, but we examine the vehicle schedule.
		 * This way the detecting is robust for outside manipulations of the schedule,
		 * for instance, when we use the existing relocation algorithms for DRT.
		 *
		 * There are edge cases where a vehicle is currently relocating, but it then
		 * gets a pickup assigned. However, the assignment rate is so high, that the
		 * vehicle is still finishing the last link of the relocation trip when the next
		 * iteration of the algorithm starts. In that case we will have a relocation
		 * drive task, followed by other tasks. Such a case is not a relocating vehicle!
		 */

		for (AlonsoMoraVehicle vehicle : vehicles) {
			Schedule schedule = vehicle.getVehicle().getSchedule();

			if (schedule.getStatus().equals(ScheduleStatus.STARTED)) {
				if (schedule.getTaskCount() > 1) {
					Task lastTask = Schedules.getLastTask(schedule);
					Task previousTask = Schedules.getNextToLastTask(schedule);

					if (lastTask instanceof StayTask && previousTask instanceof DriveTask) {
						DriveTask driveTask = (DriveTask) previousTask;

						if (EmptyVehicleRelocator.RELOCATE_VEHICLE_TASK_TYPE.equals(driveTask.getTaskType())) {
							relocatingVehicles.add(vehicle);
						}
					}
				}
			}
		}
	}

	/**
	 * Requests can either be "queued", "assigned", or "onboard", after, they exit
	 * the system. The dispatcher performs bookkeeping with a list for each state.
	 * As entering/exiting vehicles in MATSim does not need to be determinstic,
	 * here, we detect whether agents enter/exit vehicles on the fly before the
	 * dispatching step.
	 *
	 * Furthermore, the vehicles are informed whether passengers have entered or
	 * exited.
	 */
	private void updateRequestsBeforeAssignment(Collection<AlonsoMoraRequest> newRequests, double now) {
		Iterator<AlonsoMoraRequest> iterator;

		/*
		 * First, we check all assigned requests. If the associated pickup task is
		 * ongoing or finished, the request is denoted as "on board". This means that
		 * the request cannot be assigned to a new vehicle anymore.
		 */

		iterator = assignedRequests.iterator();
		while (iterator.hasNext()) {
			AlonsoMoraRequest request = iterator.next();
			TaskStatus status = request.getPickupTask().getStatus();

			if (status.equals(TaskStatus.STARTED) || status.equals(TaskStatus.PERFORMED)) {
				iterator.remove();
				onboardRequests.add(request);
				request.getVehicle().addOnboardRequest(request);
			}
		}

		/*
		 * Second, we check on-board requests. If the drop-off activity has finished or
		 * is ongoing, we consider the request as dropped off.
		 */

		iterator = onboardRequests.iterator();
		while (iterator.hasNext()) {
			AlonsoMoraRequest request = iterator.next();
			TaskStatus status = request.getDropoffTask().getStatus();

			if (status.equals(TaskStatus.STARTED) || status.equals(TaskStatus.PERFORMED)) {
				iterator.remove();
				numberOfServedRequests += request.getSize();
				request.getVehicle().removeOnboardRequest(request);
			}
		}

		/*
		 * Third, requests that remain too long in the queue, are rejected.
		 */

		iterator = queuedRequests.iterator();

		while (iterator.hasNext()) {
			AlonsoMoraRequest request = iterator.next();

			if (now > request.getLatestAssignmentTime()) {
				iterator.remove();

				for (DrtRequest drtRequest : request.getDrtRequests()) {
					eventsManager.processEvent(new PassengerRequestRejectedEvent(now, mode, drtRequest.getId(),
							drtRequest.getPassengerId(), "queue time exeeded"));
				}

				numberOfRejectedRequests++;
			}
		}

		/*
		 * Finally, we have completely new requests, which are added to the queue. We
		 * create events to notify submission.s
		 */
		queuedRequests.addAll(newRequests);

		for (AlonsoMoraRequest request : newRequests) {
			eventsManager.processEvent(new AlonsoMoraSubmissionEvent(now,
					request.getDrtRequests().stream().map(r -> r.getId()).collect(Collectors.toSet())));
		}
	}

	/**
	 * This method updates the request graph which determines which requests could
	 * potentially be on a shared route.
	 */
	private void updateRequestGraph(double now, Information information) {
		information.requestGraphStartTime = System.nanoTime();

		// Create request graph
		requestGraph = new DefaultRequestGraph(function, forkJoinPool);

		for (AlonsoMoraRequest request : queuedRequests) {
			requestGraph.addRequest(request, now);
		}

		for (AlonsoMoraRequest request : assignedRequests) {
			requestGraph.addRequest(request, now);
		}

		information.requestGraphEndTime = System.nanoTime();
		information.requestGraphSize = requestGraph.getSize();
	}

	/**
	 * This method updates the trip-vehicle graphs for all eligible vehicles.
	 */
	void updateVehicleGraphs(double now, Information information) {
		// Clean all graphs
		vehicleGraphs.clear();

		information.vehicleGraphsStartTime = System.nanoTime();

		// Create a graph per vehicle that is eligible
		for (AlonsoMoraVehicle vehicle : vehicles) {
			if (settings.useBindingRelocations && relocatingVehicles.contains(vehicle)) {
				continue; // Don't create a graph for relocating vehicles if desired
			}

			vehicleGraphs.put(vehicle, new DefaultVehicleGraph(function, requestGraph, vehicle));
		}

		// Update timings along the current route in order to calculate delays
		forkJoinPool.submit(() -> {
			vehicleGraphs.keySet().parallelStream() //
					.filter(v -> v.getRoute().size() > 0) //
					.filter(v -> !relocatingVehicles.contains(v)) //
					.forEach(v -> {
						LinkTimePair diversion = v.getNextDiversion(now);

						// We need to remove stops that are already over
						List<AlonsoMoraStop> updatedRoute = new LinkedList<>();

						for (AlonsoMoraStop stop : v.getRoute()) {
							if (stop.getType().equals(StopType.Pickup)) {
								if (stop.getRequest().isPickedUp()) {
									continue;
								}
							}

							if (stop.getType().equals(StopType.Dropoff)) {
								if (stop.getRequest().isDroppedOff()) {
									continue;
								}
							}

							updatedRoute.add(stop);
						}

						v.setRoute(updatedRoute);

						if (updatedRoute.size() > 0) {
							RouteTracker congestionTracker = new RouteTracker(travelTimeEstimator, stopDuration, 0,
									diversion.time, Optional.of(diversion.link));
							congestionTracker.setDrivingState(v);
							congestionTracker.update(v.getRoute());
						}
					});
		}).join();

		/*
		 * Pre-filter for request-vehicle assignments as described in III.C in
		 * Supplementary Material. The Trip-Request-Vehicle graph can get quite large,
		 * so the authors propose to limit the graph building procedure to only the top
		 * 30 candidate vehicles per request. Below, we perform a graph building *per
		 * vehicle* so the only thing we can do is to find those top N vehicles per
		 * request already in advance as we cannot do this in parallel otherwise. This
		 * is an optional feature to reduce the size of the trip-vehicle graph.
		 */

		final Map<AlonsoMoraVehicle, Map<AlonsoMoraRequest, AlonsoMoraFunction.Result>> topMatchings;

		if (settings.candidateVehiclesPerRequest > 0) {
			topMatchings = forkJoinPool.submit(() -> {
				return Stream.concat(queuedRequests.parallelStream(), assignedRequests.parallelStream()) //
						.flatMap(request -> {
							return vehicleGraphs.keySet().parallelStream() //
									.filter(vehicle -> vehicle.getVehicle().getSchedule().getStatus()
											.equals(ScheduleStatus.STARTED)) //
									.map(vehicle -> {
										return Pair.of(Pair.of(request, vehicle),
												function.calculateRoute(Arrays.asList(request), vehicle, now));
									})//
									.filter(result -> result.getRight().isPresent()) //
									.map(pair -> Pair.of(pair.getLeft(), pair.getRight().get())) //
									.sorted((a, b) -> {
										return Double.compare(a.getRight().getCost(), b.getRight().getCost());
									}) //
									.limit(settings.candidateVehiclesPerRequest);
						}) //
						.collect(Collectors.groupingBy(p -> p.getLeft().getRight(),
								Collectors.toMap(p -> p.getLeft().getLeft(), p -> p.getRight())));
			}).join();
		} else {
			topMatchings = null;
		}

		// Fill the graphs
		forkJoinPool.submit(() -> {
			vehicleGraphs.entrySet().parallelStream() //
					.filter(item -> item.getKey().getVehicle().getSchedule().getStatus().equals(ScheduleStatus.STARTED)) //
					.forEach(item -> {
						VehicleGraph vehicleGraph = item.getValue();

						if (topMatchings == null) {
							for (AlonsoMoraRequest request : queuedRequests) {
								vehicleGraph.addRequest(request, now);
							}

							for (AlonsoMoraRequest request : assignedRequests) {
								vehicleGraph.addRequest(request, now);
							}
						} else {
							Map<AlonsoMoraRequest, AlonsoMoraFunction.Result> vehicleMatchings = topMatchings
									.get(item.getKey());

							if (vehicleMatchings != null) {
								for (Map.Entry<AlonsoMoraRequest, AlonsoMoraFunction.Result> matching : vehicleMatchings
										.entrySet()) {
									vehicleGraph.addRequest(matching.getKey(), now, matching.getValue());
								}
							}
						}

						if (settings.preserveVehicleAssignments) {
							// Optionally, make sure that the currently assigned route is always included
							// (even though the matching may not be available anymore as it does not exist
							// any longer in the request graph).

							vehicleGraph.preserveVehicleAssignment(now);
						}
					});
		}).join();

		information.vehicleGraphsEndTime = System.nanoTime();
		information.vehicleGraphSize = vehicleGraphs.values().stream().mapToInt(g -> g.getSize()).sum();
	}

	/*
	 * This is the main core of the assignment where we run the matching algorithm.
	 */
	private void performAssignment(double now, Information information) {
		// Run the assignment solver

		information.assignmentStartTime = System.nanoTime();
		Solution solution = assignmentSolver.solve(vehicleGraphs.values().stream().flatMap(v -> v.stream()));
		information.assignmentEndTime = System.nanoTime();
		information.solutionStatus = solution.status;

		processAssignedRequests(solution, now, information);
		processAssignedVehicles(solution, now, information);
	}

	/**
	 * This process updates the state of the requests that have or have not been
	 * assigned.
	 */
	private void processAssignedRequests(Solution solution, double now, Information information) {
		Set<AlonsoMoraRequest> newAssignedRequests = new HashSet<>();

		/*
		 * First, handle the timing of all the assigned requests and update their
		 * internal state.
		 */

		// Iterate over the selected trips
		for (AlonsoMoraTrip trip : solution.trips) {
			// Find information for each request along the sequence
			for (AlonsoMoraRequest request : trip.getRequests()) {
				Verify.verify(newAssignedRequests.add(request), "Request is assigned twice!");

				// Set the vehicle
				request.setVehicle(trip.getVehicle());

				double expectedPickupTime = Double.NaN;
				double expectedDropoffTime = Double.NaN;

				for (AlonsoMoraStop stop : trip.getResult().getStops()) {
					Verify.verify(Double.isFinite(stop.getTime()));

					if (stop.getRequest() == request) {
						// This stop covers the selected request

						if (stop.getType().equals(StopType.Pickup)) {
							// We're looking at the pickup stop

							expectedPickupTime = stop.getTime();

							if (settings.usePlannedPickupTime) {
								// We have a waiting time slack, i.e. we move the pickup constraint to the
								// promised value from the assignment plus a small slack, which, by default, is
								// zero. So in the usual case, we require that the request be picked up at the
								// time that we promise at the first assignment.

								// ... but adding the slack cannot exceed the initial pickup requirement
								double plannedPickupTime = expectedPickupTime + settings.plannedPickupTimeSlack;
								plannedPickupTime = Math.min(plannedPickupTime, request.getLatestPickupTime());

								request.setPlannedPickupTime(plannedPickupTime);
							}

							if (request.isAssigned() && !request.getVehicle().equals(trip.getVehicle())) {
								// Just for statistics: We track whether a request is assigned to a new vehicle
								information.numberOfReassignments++;
							}
						} else if (stop.getType().equals(StopType.Dropoff)) {
							// We're looking at the dropoff stop
							expectedDropoffTime = stop.getTime();
						}
					}
				}

				/* For each DRT request, we create a scheduling event */
				for (DrtRequest drtRequest : request.getDrtRequests()) {
					eventsManager.processEvent(new PassengerRequestScheduledEvent(now, mode, drtRequest.getId(),
							drtRequest.getPassengerId(), trip.getVehicle().getVehicle().getId(), expectedPickupTime,
							expectedDropoffTime));
				}
			}
		}

		/*
		 * Second, do the bookkeeping for the request lists. (1) Here, we find requests
		 * that were previously assigned, but are not anymore. They go back to the
		 * queue.
		 */

		Set<AlonsoMoraRequest> unassignedRequests = new HashSet<>(assignedRequests);
		unassignedRequests.removeAll(newAssignedRequests);

		queuedRequests.addAll(unassignedRequests);
		assignedRequests.removeAll(unassignedRequests);

		// Clean the state of those requests (
		unassignedRequests.forEach(AlonsoMoraRequest::unassign);

		/*
		 * (2) Here we handle the newly assigned requests.
		 */

		assignedRequests.addAll(newAssignedRequests);
		queuedRequests.removeAll(newAssignedRequests);
	}

	/**
	 * This method processes the assigned vehicles. Two major things need to be
	 * done: The route information needs to be transferred to the internal state of
	 * the vehicle. And based on the route sequence, a new DVRP task sequence needs
	 * to be created. Furthermore, vehicles need to be handled that were not
	 * assigned any requests in the current iteration.
	 */
	private void processAssignedVehicles(Solution solution, double now, Information information) {
		Set<AlonsoMoraVehicle> newAssignedVehicles = new HashSet<>();

		// Check that assignments are unique
		for (AlonsoMoraTrip trip : solution.trips) {
			Verify.verify(newAssignedVehicles.add(trip.getVehicle()), "Vehicle is assigned twice!");
		}

		/*
		 * First, scheduling is done for vehicles that have received a stop sequence.
		 */

		forkJoinPool.submit(() -> {
			solution.trips.parallelStream().forEach(trip -> {
				// Set the route to the vehicle state and perform DVRP scheduling
				trip.getVehicle().setRoute(trip.getResult().getStops());
				scheduler.schedule(trip.getVehicle(), now);
			});
		}).join();

		// And those vehicles are definitely not relocating anymore now.
		relocatingVehicles.removeAll(newAssignedVehicles);

		/*
		 * Second, scheduling is done for all vehicles that have not received a
		 * sequence. Those may be vehicles that are currently relocating or dropping off
		 * some on-board customers without taking new ones. The previous category is not
		 * touched by the assignment, so we let them relocate. The second category must
		 * be rescheduled as previously they might have had future pickups in their
		 * schedules.
		 */

		Set<AlonsoMoraVehicle> unassignedVehicles = new HashSet<>(vehicles);
		unassignedVehicles.removeAll(newAssignedVehicles);
		unassignedVehicles.removeAll(relocatingVehicles);

		forkJoinPool.submit(() -> {
			unassignedVehicles.parallelStream() //
					.filter(v -> v.getVehicle().getSchedule().getStatus().equals(ScheduleStatus.STARTED)) //
					.forEach(vehicle -> {
						// Perform a scheduling without any new pickups
						Optional<Result> result = function.calculateRoute(Collections.emptySet(), vehicle, now);
						Verify.verify(result.isPresent());

						// As above, set the route and find DVRP schedule
						vehicle.setRoute(result.get().getStops());
						scheduler.schedule(vehicle, now);
					});
		}).join();

		if (settings.relocationInterval > 0 && now % settings.relocationInterval == 0) {
			performRelocation(solution, now, information);
		}
	}

	/**
	 * This method performs the relocation for all not assigned vehicles
	 */
	private void performRelocation(Solution solution, double now, Information information) {
		List<Relocation> trips = new LinkedList<>();

		/*
		 * First, we find destinations by looking at all the unassigned requests
		 */

		Collection<Link> destinations = queuedRequests.stream().map(r -> r.getPickupLink())
				.collect(Collectors.toList());

		/*
		 * Second, we find all viable vehicles. If relocations are not binding,
		 * currently relocating vehicles are not viable. On top, vehicles that have just
		 * been assigned are not viable, and also vehicles which have on board
		 * customers.
		 */

		List<AlonsoMoraVehicle> relocatableVehicles = new ArrayList<>(vehicles);
		relocatableVehicles
				.removeAll(solution.trips.stream().map(trip -> trip.getVehicle()).collect(Collectors.toSet()));
		relocatableVehicles.removeIf(v -> !v.getVehicle().getSchedule().getStatus().equals(ScheduleStatus.STARTED));
		relocatableVehicles.removeIf(v -> v.getOnboardRequests().size() > 0);

		if (settings.useBindingRelocations) {
			relocatableVehicles.removeAll(relocatingVehicles);
		}

		for (AlonsoMoraVehicle vehicle : relocatableVehicles) {
			for (Link destination : destinations) {
				Optional<Double> arrivalTime = function.checkRelocation(vehicle, destination, now);

				if (arrivalTime.isPresent()) {
					Verify.verify(arrivalTime.get() >= now);
					trips.add(new Relocation(vehicle, destination, arrivalTime.get() - now));
				}
			}
		}

		/*
		 * Third, perform relocation by (1) setting the vehicle route, and (2)
		 * performing the DVRP scheduling
		 */

		information.relocationStartTime = System.nanoTime();
		Collection<Relocation> relocations = rebalancingSolver.solve(trips);
		information.relocationEndTime = System.nanoTime();

		for (Relocation trip : relocations) {
			trip.vehicle.setRoute(
					Collections.singletonList(new AlonsoMoraStop(StopType.Relocation, trip.destination, null)));
			scheduler.schedule(trip.vehicle, now);

			information.numberOfRelocations++;
		}
	}

	public Optional<Information> run(List<AlonsoMoraRequest> newRequests, double now) {
		Optional<Information> information = Optional.empty();

		// Update request states
		updateRequestsBeforeAssignment(newRequests, now);

		if (newRequests.size() > 0 || settings.allowBareReassignment) {
			/*
			 * If traffic conditions are deterministic, vehicles only need to be reassigned
			 * if new requests come in that might make new configurations more favorable.
			 * However, if travel times are non-deterministic, traffic conditions can change
			 * and we might still consider reassigning vehicles based on the current state
			 * of the system.
			 */

			information = Optional.of(new Information(maximumOccupancy));
			generateOccupancyInformation(now, information.get());

			// Update vehicle states
			updateVehiclesBeforeAssignment();

			// Update graphs
			updateRequestGraph(now, information.get());
			updateVehicleGraphs(now, information.get());

			// Perform assignment and relocation
			performAssignment(now, information.get());
		}

		// Print logging information
		if (now % settings.loggingInterval == 0) {
			printInformation();
		}

		return information;
	}

	/*
	 * Logs running information of the algorithm
	 */
	private void printInformation() {
		int numberOfQueuedRequests = queuedRequests.stream().mapToInt(r -> r.getSize()).sum();
		int numberOfAssignedRequests = assignedRequests.stream().mapToInt(r -> r.getSize()).sum();
		int numberOfOnboardRequests = onboardRequests.stream().mapToInt(r -> r.getSize()).sum();

		int numberOfAssignedVehicles = (int) vehicles.stream().filter(v -> {
			if (v.getOnboardRequests().size() > 0) {
				return true;
			}

			Schedule schedule = v.getVehicle().getSchedule();

			if (schedule.getStatus().equals(ScheduleStatus.STARTED)) {
				for (int k = schedule.getCurrentTask().getTaskIdx(); k < schedule.getTaskCount(); k++) {
					Task task = schedule.getTasks().get(k);

					if (DefaultDrtStopTask.TYPE.equals(task.getTaskType())) {
						// It can happen that we're dropping of customers (dropoff task has started),
						// but we have already scheduled a relocation task.
						return !relocatingVehicles.contains(v);
					}
				}
			}

			return false;
		}).count();

		int numberOfRebalancingVehicles = relocatingVehicles.size();
		int numberOfIdleVehicles = vehicles.size() - numberOfAssignedVehicles - numberOfRebalancingVehicles;

		logger.info(String.format("Graphs: %d RR, %d RTV", requestGraph.getSize(),
				vehicleGraphs.values().stream().mapToInt(graph -> graph.getSize()).sum()));

		logger.info(String.format("Requests: Q(%d) A(%d) O(%d) S(%d) R(%d), Vehicles: I(%d), A(%d), R(%d)",
				numberOfQueuedRequests, numberOfAssignedRequests, numberOfOnboardRequests, numberOfServedRequests,
				numberOfRejectedRequests, numberOfIdleVehicles, numberOfAssignedVehicles, numberOfRebalancingVehicles));
	}

	private void generateOccupancyInformation(double now, Information information) {
		for (AlonsoMoraVehicle vehicle : vehicles) {
			Set<AlonsoMoraRequest> requests = vehicle.getOnboardRequests();

			int requestCount = (int) requests.stream() //
					.count();

			int passengerCount = requests.stream() //
					.mapToInt(r -> r.getSize()).sum();

			information.occupiedVehiclesByRequests.set(requestCount,
					information.occupiedVehiclesByRequests.get(requestCount) + 1);
			information.occupiedVehiclesByPassengers.set(passengerCount,
					information.occupiedVehiclesByPassengers.get(passengerCount) + 1);
		}
	}

	static public class AlgorithmSettings {
		final boolean useBindingRelocations;
		final boolean preserveVehicleAssignments;
		final boolean usePlannedPickupTime;
		final double plannedPickupTimeSlack;
		final double relocationInterval;
		final boolean allowBareReassignment;
		final double loggingInterval;
		final int candidateVehiclesPerRequest;

		public AlgorithmSettings(AlonsoMoraConfigGroup config) {
			this.useBindingRelocations = config.getUseBindingRelocations();
			this.preserveVehicleAssignments = config.getCongestionMitigationParameters()
					.getPreserveVehicleAssignments();
			this.usePlannedPickupTime = config.getUsePlannedPickupTime();
			this.plannedPickupTimeSlack = config.getPlannedPickupTimeSlack();
			this.relocationInterval = config.getRelocationInterval();
			this.allowBareReassignment = config.getCongestionMitigationParameters().getAllowBareReassignment();
			this.loggingInterval = config.getLoggingInterval();
			this.candidateVehiclesPerRequest = config.getCandidateVehiclesPerRequest();
		}
	}

	static public class Information {
		Information(int maximumOccupancy) {
			occupiedVehiclesByPassengers = new ArrayList<>(Collections.nCopies(maximumOccupancy + 1, 0));
			occupiedVehiclesByRequests = new ArrayList<>(Collections.nCopies(maximumOccupancy + 1, 0));
		}

		public long assignmentStartTime;
		public long assignmentEndTime;

		public long relocationStartTime;
		public long relocationEndTime;

		public long requestGraphStartTime;
		public long requestGraphEndTime;
		public int requestGraphSize;

		public long vehicleGraphsStartTime;
		public long vehicleGraphsEndTime;
		public int vehicleGraphSize;

		public final List<Integer> occupiedVehiclesByPassengers;
		public final List<Integer> occupiedVehiclesByRequests;

		public int numberOfRelocations = 0;
		public int numberOfReassignments = 0;

		public Solution.Status solutionStatus;
	}
}
