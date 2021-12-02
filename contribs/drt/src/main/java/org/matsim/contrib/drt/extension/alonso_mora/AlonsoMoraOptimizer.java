package org.matsim.contrib.drt.extension.alonso_mora;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;

import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraAlgorithm;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraRequest;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraRequestFactory;
import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.ScheduleTimingUpdater;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;

/**
 * Entry point for the Alonso-Mora dispatcher in DRT that implements
 * DrtOptimizer, receives requests, and delegates dispatching in each time step.
 */
public class AlonsoMoraOptimizer implements DrtOptimizer {
	private final ScheduleTimingUpdater scheduleTimingUpdater;
	private final Fleet fleet;

	private final AlonsoMoraAlgorithm algorithm;
	private final AlonsoMoraRequestFactory requestFactory;

	private final List<DrtRequest> submittedRequests = new LinkedList<>();
	private final double assignmentInterval;

	private final int maximumGroupRequestSize;

	private final ForkJoinPool forkJoinPool;
	private final LeastCostPathCalculator router;
	private final TravelTime travelTime;

	private final InformationCollector collector;

	private final Queue<AlonsoMoraRequest> prebookingQueue = new PriorityQueue<>((a, b) -> {
		return Double.compare(a.getEarliestPickupTime(), b.getEarliestPickupTime());
	});

	private final double prebookingHorizon;

	public AlonsoMoraOptimizer(AlonsoMoraAlgorithm algorithm, AlonsoMoraRequestFactory requestFactory,
			ScheduleTimingUpdater scheduleTimingUpdater, Fleet fleet, double assignmentInterval,
			int maximumGroupRequestSize, ForkJoinPool forkJoinPool, LeastCostPathCalculator router,
			TravelTime travelTime, double prebookingHorizon, InformationCollector collector) {
		this.algorithm = algorithm;
		this.requestFactory = requestFactory;
		this.assignmentInterval = assignmentInterval;
		this.scheduleTimingUpdater = scheduleTimingUpdater;
		this.fleet = fleet;
		this.maximumGroupRequestSize = maximumGroupRequestSize;
		this.forkJoinPool = forkJoinPool;
		this.router = router;
		this.travelTime = travelTime;
		this.prebookingHorizon = prebookingHorizon;
		this.collector = collector;
	}

	@Override
	public void requestSubmitted(Request request) {
		submittedRequests.add((DrtRequest) request);
	}

	/**
	 * Goes through the submitted individual requests and tries to find those that
	 * should be aggregated to a collective request.
	 */
	private List<List<DrtRequest>> poolRequests(List<DrtRequest> submittedRequests) {
		submittedRequests = new LinkedList<>(submittedRequests);
		List<List<DrtRequest>> allPooledRequests = new LinkedList<>();

		while (submittedRequests.size() > 0) {
			List<DrtRequest> pooledRequests = new LinkedList<>();
			pooledRequests.add(submittedRequests.remove(0));
			DrtRequest mainRequest = pooledRequests.get(0);

			Iterator<DrtRequest> iterator = submittedRequests.iterator();

			while (iterator.hasNext() && pooledRequests.size() < maximumGroupRequestSize) {
				DrtRequest nextRequest = iterator.next();

				if (nextRequest.getFromLink() == mainRequest.getFromLink()) {
					if (nextRequest.getToLink() == mainRequest.getToLink()) {
						if (nextRequest.getEarliestStartTime() == mainRequest.getEarliestStartTime()) {
							pooledRequests.add(nextRequest);
							iterator.remove();
						}
					}
				}
			}

			allPooledRequests.add(pooledRequests);
		}

		return allPooledRequests;
	}

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e) {
		double now = e.getSimulationTime();
		
		if (now % assignmentInterval == 0) {
			List<AlonsoMoraRequest> newRequests = new LinkedList<>();

			List<List<DrtRequest>> pooledRequests = poolRequests(submittedRequests);
			List<VrpPathWithTravelData> paths = new ArrayList<>(Collections.nCopies(pooledRequests.size(), null));

			// Here this direct routing is performed
			forkJoinPool.submit(() -> {
				IntStream.range(0, pooledRequests.size()).parallel().forEach(i -> {
					DrtRequest request = pooledRequests.get(i).get(0);
					paths.set(i, VrpPaths.calcAndCreatePath(request.getFromLink(), request.getToLink(), request.getEarliestStartTime(), router,
							travelTime));
				});
			}).join();

			// Grouped requests
			for (int i = 0; i < pooledRequests.size(); i++) {
				List<DrtRequest> pool = pooledRequests.get(i);
				
				double earliestDepartureTime = pool.get(0).getEarliestStartTime();
				double directArrivalTime = paths.get(i).getTravelTime() + earliestDepartureTime;
				double directRideDistance = VrpPaths.calcDistance(paths.get(i));

				AlonsoMoraRequest request = requestFactory.createRequest(pool, directArrivalTime, earliestDepartureTime,
						directRideDistance);

				if (now >= request.getEarliestPickupTime() - prebookingHorizon) {
					newRequests.add(request);
				} else {
					prebookingQueue.add(request);
				}
			}

			while (prebookingQueue.size() > 0
					&& now >= prebookingQueue.peek().getEarliestPickupTime() - prebookingHorizon) {
				newRequests.add(prebookingQueue.poll());
			}

			submittedRequests.clear();

			for (DvrpVehicle v : fleet.getVehicles().values()) {
				scheduleTimingUpdater.updateTimings(v);
			}

			Optional<AlonsoMoraAlgorithm.Information> information = algorithm.run(newRequests, e.getSimulationTime());

			if (information.isPresent()) {
				collector.addInformation(e.getSimulationTime(), information.get());
			}
		}
	}

	@Override
	public void nextTask(DvrpVehicle vehicle) {
		scheduleTimingUpdater.updateBeforeNextTask(vehicle);
		vehicle.getSchedule().nextTask();
	}
}
