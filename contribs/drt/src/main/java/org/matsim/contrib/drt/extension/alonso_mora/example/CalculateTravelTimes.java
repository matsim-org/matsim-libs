package org.matsim.contrib.drt.extension.alonso_mora.example;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

public class CalculateTravelTimes {
	private final static Logger logger = Logger.getLogger(CalculateTravelTimes.class);

	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("population-path", "network-path", "output-path") //
				.allowOptions("hour", "sampling-rate", "initial-speed") //
				.build();

		Optional<Integer> hour = cmd.getOption("hour").map(Integer::parseInt);
		double initialSpeed = cmd.getOption("initial-speed").map(Double::parseDouble).orElse(20.0 / 3.6);

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(cmd.getOptionStrict("network-path"));
		new PopulationReader(scenario).readFile(cmd.getOptionStrict("population-path"));

		/* Prepare node indices */
		List<Node> nodes = new ArrayList<>(scenario.getNetwork().getNodes().values());
		List<Integer> nodeWeights = new ArrayList<>(Collections.nCopies(nodes.size(), 0));

		int maximumIndex = nodes.stream().mapToInt(node -> node.getId().index()).max().getAsInt();
		List<Integer> id2index = new ArrayList<>(Collections.nCopies(maximumIndex + 1, -1));

		for (int index = 0; index < nodes.size(); index++) {
			id2index.set(nodes.get(index).getId().index(), index);
		}

		/* 1) Equivalent trip reduction */
		logger.info("1) Equivalent trip reduction");

		List<Trip> trips = new LinkedList<>();
		int numberOfTrips = 0;

		Random random = new Random(0);
		double samplingRate = cmd.getOption("sampling-rate").map(Double::parseDouble).orElse(1.0);

		{
			int numberOfNodes = scenario.getNetwork().getNodes().size();

			double[][] meanTravelTimes = new double[numberOfNodes][numberOfNodes];
			int[][] counts = new int[numberOfNodes][numberOfNodes];

			for (Person person : scenario.getPopulation().getPersons().values()) {
				if (random.nextDouble() > samplingRate) {
					continue;
				}

				Plan plan = person.getSelectedPlan();

				Activity originActivity = (Activity) plan.getPlanElements().get(0);
				Leg leg = (Leg) plan.getPlanElements().get(1);
				Activity destinationActivity = (Activity) plan.getPlanElements().get(2);

				Link originLink = scenario.getNetwork().getLinks().get(originActivity.getLinkId());
				Link destinationLink = scenario.getNetwork().getLinks().get(destinationActivity.getLinkId());

				Node originNode = originLink.getToNode();
				Node destinationNode = destinationLink.getFromNode();

				int originIndex = id2index.get(originNode.getId().index());
				int destinationIndex = id2index.get(destinationNode.getId().index());

				double departureTime = originActivity.getEndTime().seconds();

				if (hour.isEmpty() || ((int) (departureTime / 3600.0)) % 24 == hour.get()) {
					counts[originIndex][destinationIndex] += 1;
					meanTravelTimes[originIndex][destinationIndex] += leg.getTravelTime().seconds();

					numberOfTrips++;
				}

				if (((int) (departureTime / 3600.0)) % 24 == 0) {
					nodeWeights.set(originIndex, nodeWeights.get(originIndex) + 1);
				}
			}

			for (int originIndex = 0; originIndex < numberOfNodes; originIndex++) {
				for (int destinationIndex = 0; destinationIndex < numberOfNodes; destinationIndex++) {
					if (counts[originIndex][destinationIndex] > 0) {
						trips.add(new Trip(originIndex, destinationIndex,
								meanTravelTimes[originIndex][destinationIndex] /= (double) counts[originIndex][destinationIndex],
								counts[originIndex][destinationIndex]));
					}
				}
			}
		}

		logger.info("   Trips were reduced from " + numberOfTrips + " to " + trips.size());

		/* 2) First trip filtering */
		logger.info("2) First trip filtering");

		{
			Iterator<Trip> iterator = trips.iterator();
			int initialNumber = trips.size();

			while (iterator.hasNext()) {
				Trip trip = iterator.next();

				if (trip.originIndex == trip.destinationIndex) {
					iterator.remove();
					continue;
				}

				if (trip.meanTravelTime < 2.0 * 60.0) {
					iterator.remove();
					continue;
				}

				if (trip.meanTravelTime > 3600.0) {
					iterator.remove();
					continue;
				}
			}

			int finalNumber = trips.size();
			logger.info(String.format("   Filtered %d/%d (%.2f%%) of trips", initialNumber - finalNumber, initialNumber,
					100 * (initialNumber - finalNumber) / (double) initialNumber));
		}

		/* 3) Initial route computation */
		logger.info("3) Initial route computation");

		LeastCostPathCalculatorFactory routerFactory = new DijkstraFactory();

		int numberOfThreads = Runtime.getRuntime().availableProcessors();
		ForkJoinPool pool = new ForkJoinPool(numberOfThreads);

		for (Link link : scenario.getNetwork().getLinks().values()) {
			link.setFreespeed(initialSpeed);
		}

		performRouting(pool, routerFactory, scenario.getNetwork(), nodes, trips);

		/* 4) Second trip filtering */
		logger.info("4) Second trip filtering");

		{
			Iterator<Trip> iterator = trips.iterator();
			int initialNumber = trips.size();

			while (iterator.hasNext()) {
				Trip trip = iterator.next();

				double meanSpeed = trip.route.stream().mapToDouble(Link::getLength).sum() / trip.meanTravelTime;

				if (meanSpeed < 0.5) {
					iterator.remove();
					continue;
				}

				if (meanSpeed > 30) {
					iterator.remove();
					continue;
				}
			}

			int finalNumber = trips.size();
			logger.info(String.format("   Filtered %d/%d (%.2f%%) of trips", initialNumber - finalNumber, finalNumber,
					100.0 * (initialNumber - finalNumber) / (double) initialNumber));
		}

		/* 5) Iterative steps */
		logger.info("5) Iterative steps");

		boolean again = true;
		int mainIteration = 0;
		TravelTime travelTime = new FreeSpeedTravelTime();

		while (again) {
			again = false;
			mainIteration++;

			performRouting(pool, routerFactory, scenario.getNetwork(), nodes, trips);

			Set<Link> relevantLinks = new HashSet<>();
			trips.forEach(trip -> relevantLinks.addAll(trip.route));

			double relativeError = trips.stream().mapToDouble(trip -> {
				return Math.abs(trip.estimatedTravelTime - trip.meanTravelTime) / trip.meanTravelTime;
			}).average().getAsDouble();

			logger.info("   Main iteration " + mainIteration + " with rel. error " + relativeError);

			logger.info("      Calculating offsets");

			AtomicInteger currentCount = new AtomicInteger(0);
			AtomicLong lastTime = new AtomicLong(System.nanoTime());

			pool.submit(() -> {
				relevantLinks.stream().parallel().forEach(link -> {
					Collection<Trip> tripsWithLink = trips.stream().filter(t -> t.route.contains(link))
							.collect(Collectors.toSet());

					double offset = tripsWithLink.stream().mapToDouble(t -> {
						return (t.estimatedTravelTime - t.meanTravelTime) * t.elements;
					}).sum();

					link.getAttributes().putAttribute("offset", offset);
					currentCount.incrementAndGet();

					if (lastTime.get() + 1e10 < System.nanoTime()) {
						lastTime.set(System.nanoTime());
						logger.info("         " + currentCount.get() + "/" + trips.size());
					}
				});
			}).join();

			double k = 1.2;
			int innerIteration = 0;

			while (true) {
				innerIteration++;
				logger.info("      Inner iteration " + innerIteration + " (k=" + k + ")");

				for (Link link : relevantLinks) {
					double offset = (Double) link.getAttributes().getAttribute("offset");

					if (offset < 0.0) {
						link.setFreespeed(link.getFreespeed() / k);
					} else {
						link.setFreespeed(link.getFreespeed() * k);
					}
				}

				for (Trip trip : trips) {
					double updatedTime = trip.route.stream()
							.mapToDouble(link -> travelTime.getLinkTravelTime(link, 0.0, null, null)).sum();
					trip.updatedEstimatedTravelTime = updatedTime;
				}

				double newRelativeError = trips.stream().mapToDouble(trip -> {
					return Math.abs(trip.updatedEstimatedTravelTime - trip.meanTravelTime) / trip.meanTravelTime;
				}).average().getAsDouble();

				logger.info("         Relative error " + newRelativeError + " (comp. " + relativeError + ")");

				if (newRelativeError < relativeError) {
					for (Trip trip : trips) {
						trip.estimatedTravelTime = trip.updatedEstimatedTravelTime;
					}

					relativeError = newRelativeError;
					again = true;
					break;
				} else {
					// Revert (not covered in pseudo code)
					
					if (true) {
						for (Link link : relevantLinks) {
							double offset = (Double) link.getAttributes().getAttribute("offset");

							if (offset < 0.0) {
								link.setFreespeed(link.getFreespeed() * k);
							} else {
								link.setFreespeed(link.getFreespeed() / k);
							}
						}
					}

					k = 1 + (k - 1) * 0.75;

					if (k < 1.0001) {
						break;
					}
				}
			}
		}

		/* 6) Computation of estimated travel time for remaining streets */
		logger.info("6) Computation of estimated travel time for remaining streets");

		{
			Set<Link> estimatedLinks = new HashSet<>();
			trips.forEach(trip -> estimatedLinks.addAll(trip.route));

			logger.info("   Estimated links: " + estimatedLinks.size());

			List<Link> additionalLinks = new LinkedList<>(scenario.getNetwork().getLinks().values());
			additionalLinks.removeAll(estimatedLinks);

			logger.info("   Additional links: " + additionalLinks.size());

			additionalLinks.forEach(l -> l.setFreespeed(-1.0));

			while (additionalLinks.size() > 0) {
				additionalLinks.sort((a, b) -> {
					return -Integer.compare(getEstimatedIntersectionLinks(a, estimatedLinks).size(),
							getEstimatedIntersectionLinks(b, estimatedLinks).size());
				});

				Link link = additionalLinks.get(0);
				double speed = getEstimatedIntersectionLinks(link, estimatedLinks).stream()
						.mapToDouble(Link::getFreespeed).average().getAsDouble();
				link.setFreespeed(speed);

				estimatedLinks.add(link);
				additionalLinks.remove(0);
			}
		}

		/* 7) Travel time estimation */

		// Will be done in the simulation, here we have now a realistic speed for every
		// link in the network.

		/* Write node weights */

		for (int u = 0; u < nodes.size(); u++) {
			nodes.get(u).getAttributes().putAttribute("weight", nodeWeights.get(u));
		}

		new NetworkWriter(scenario.getNetwork()).write(cmd.getOptionStrict("output-path"));
	}

	private static void performRouting(ForkJoinPool pool, LeastCostPathCalculatorFactory routerFactory, Network network,
			List<Node> nodes, List<Trip> trips) {
		TravelTime travelTime = new FreeSpeedTravelTime();
		ArrayBlockingQueue<LeastCostPathCalculator> queue = new ArrayBlockingQueue<>(pool.getParallelism());

		for (int i = 0; i < pool.getParallelism(); i++) {
			queue.add(routerFactory.createPathCalculator(network, new OnlyTimeDependentTravelDisutility(travelTime),
					travelTime));
		}

		AtomicInteger currentCount = new AtomicInteger(0);
		AtomicLong lastTime = new AtomicLong(System.nanoTime());

		pool.submit(() -> {
			logger.info("         -> Routing");

			trips.stream().parallel().forEach(trip -> {
				try {
					LeastCostPathCalculator router;

					router = queue.take();

					Node originNode = nodes.get(trip.originIndex);
					Node destinationNode = nodes.get(trip.destinationIndex);

					Path path = router.calcLeastCostPath(originNode, destinationNode, 0.0, null, null);

					trip.route = path.links;
					trip.estimatedTravelTime = path.links.stream()
							.mapToDouble(link -> travelTime.getLinkTravelTime(link, 0.0, null, null)).sum();

					queue.add(router);
					currentCount.incrementAndGet();

					if (lastTime.get() + 1e10 < System.nanoTime()) {
						lastTime.set(System.nanoTime());
						logger.info("            " + currentCount.get() + "/" + trips.size());
					}
				} catch (InterruptedException e) {
				}
			});
		}).join();
	}

	private static Set<Link> getEstimatedIntersectionLinks(Link link, Set<Link> estimatedLinks) {
		Set<Link> intersectionLinks = new HashSet<>();

		intersectionLinks.addAll(link.getFromNode().getInLinks().values());
		intersectionLinks.addAll(link.getFromNode().getOutLinks().values());
		intersectionLinks.addAll(link.getToNode().getInLinks().values());
		intersectionLinks.addAll(link.getToNode().getOutLinks().values());

		intersectionLinks.remove(link);

		intersectionLinks.retainAll(estimatedLinks);

		return intersectionLinks;
	}

	static class Trip {
		final int originIndex;
		final int destinationIndex;
		final double meanTravelTime;
		final int elements;

		List<Link> route;
		double estimatedTravelTime;
		double updatedEstimatedTravelTime;

		Trip(int originIndex, int destinationIndex, double meanTravelTime, int elements) {
			this.originIndex = originIndex;
			this.destinationIndex = destinationIndex;
			this.meanTravelTime = meanTravelTime;
			this.elements = elements;
		}
	}
}
