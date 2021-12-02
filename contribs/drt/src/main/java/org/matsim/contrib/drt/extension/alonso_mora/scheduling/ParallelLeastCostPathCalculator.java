package org.matsim.contrib.drt.extension.alonso_mora.scheduling;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

/**
 * A battery of routers that can perform routing tasks in parallel.
 */
public class ParallelLeastCostPathCalculator implements LeastCostPathCalculator, MobsimBeforeCleanupListener {
	private final ExecutorService executor;
	private final BlockingQueue<LeastCostPathCalculator> routers;

	public ParallelLeastCostPathCalculator(int numberOfThreads, LeastCostPathCalculatorFactory factory, Network network,
			TravelDisutility travelDisutility, TravelTime travelTime) {
		this.executor = Executors.newFixedThreadPool(numberOfThreads);
		this.routers = new ArrayBlockingQueue<>(numberOfThreads);

		try {
			for (int i = 0; i < numberOfThreads; i++) {
				routers.put(factory.createPathCalculator(network, travelDisutility, travelTime));
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw new IllegalStateException("Error in parallel routing");
		}
	}

	@Override
	public Path calcLeastCostPath(Node fromNode, Node toNode, double starttime, Person person, Vehicle vehicle) {
		try {
			return submit(fromNode, toNode, starttime, person, vehicle).get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			throw new IllegalStateException("Error in parallel routing");
		}
	}

	public Future<Path> submit(Node fromNode, Node toNode, double starttime, Person person, Vehicle vehicle) {
		return executor.submit(() -> {
			LeastCostPathCalculator router = routers.take();
			Path path = router.calcLeastCostPath(fromNode, toNode, starttime, person, vehicle);

			routers.put(router);
			return path;
		});
	}

	@Override
	public void notifyMobsimBeforeCleanup(@SuppressWarnings("rawtypes") MobsimBeforeCleanupEvent e) {
		executor.shutdownNow();
	}
}
