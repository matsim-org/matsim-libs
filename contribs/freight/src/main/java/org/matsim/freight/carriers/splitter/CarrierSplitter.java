/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C)  by the members listed in the COPYING,            *
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

package org.matsim.freight.carriers.splitter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.freight.carriers.*;
import org.matsim.utils.objectattributes.attributable.AttributesUtils;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Splits carriers into smaller carriers by spatially clustering their shipments or services.
 * <p>
 * This splitter currently supports carriers with infinite fleets only. Finite fleets need an explicit vehicle allocation
 * policy and are therefore rejected until this behavior is implemented and tested.
 */
public final class CarrierSplitter {

	private static final Logger log = LogManager.getLogger(CarrierSplitter.class);
	//TODO vielelicht kann man die Jobs per Split carrier ja anhand des Widerstandes ermitteln
	private CarrierSplitter() {
	}

	public enum ClusteringStrategy {
		// TODO Add METIS clustering once the external solver dependency is injectable and covered by portable tests.
		RANDOM, GREEDY, SINGLE_LINK, CENTROIDS
	}

	/**
	 * This setting is intentionally mandatory for shipment clustering: {@link CarrierShipment}s have two spatially
	 * relevant locations, pickup and delivery. Choosing the wrong one can fundamentally change the split result,
	 * e.g. waste collection should usually be clustered by pickup locations, while distribution from one depot to many
	 * customers often needs delivery locations. Use {@link #MIDPOINT} only when both ends of the shipment should
	 * influence the spatial split.
	 */
	public enum ShipmentClusteringLocation {
		/** Cluster shipments by their pickup link coordinate. */
		PICKUP,
		/** Cluster shipments by their delivery link coordinate. */
		DELIVERY,
		/** Cluster shipments by the midpoint between pickup and delivery link coordinates. */
		MIDPOINT
	}

	private record Edge<T>(T a, T b, double distance) {
	}

	/**
	 * Splits each carrier in the scenario into smaller carriers by clustering the carrier's jobs.
	 * <p>
	 * A carrier is expected to contain either shipments or services. Shipment carriers require an explicit
	 * {@code shipmentClusterLocation}; service carriers ignore this setting because services have only one service
	 * location.
	 * <p>
	 * The split is deterministic for identical inputs: carriers, vehicles and jobs are processed in ID order, and
	 * distance ties are resolved by job IDs.
	 *
	 * @param scenario MATSim scenario containing the network and carriers to split.
	 * @param clusterStrategy clustering strategy used to build the smaller carrier groups.
	 * @param shipmentClusterLocation location representation for shipment clustering; required for carriers with shipments.
	 * @param maxJobsPerCarrier target upper bound for jobs assigned to each new carrier.
	 */
	public static void splitCarriers(Scenario scenario, ClusteringStrategy clusterStrategy,
			ShipmentClusteringLocation shipmentClusterLocation, int maxJobsPerCarrier) {
		if (maxJobsPerCarrier <= 0) {
			throw new IllegalArgumentException("maxJobsPerCarrier must be larger than zero.");
		}

		Network network = scenario.getNetwork();
		Carriers carriers = CarriersUtils.getCarriers(scenario);
		Carriers newCarriers = new Carriers();
		boolean splitPerformed = false;

		for (Carrier singleCarrier : sortedCarriers(carriers)) {
			if (!singleCarrier.getPlans().isEmpty()) {
				newCarriers.addCarrier(singleCarrier);
				continue;
			}

			String carrierName = singleCarrier.getId().toString();
			int jspritIterations = CarriersUtils.getJspritIterations(singleCarrier);
			if (jspritIterations <= 0) {
				throw new IllegalArgumentException("Carrier " + singleCarrier.getId() + " has invalid jsprit iterations: " + jspritIterations);
			}

			int numberOfCarriers = estimateNumberOfCarriers(maxJobsPerCarrier, singleCarrier);
			if (numberOfCarriers <= 1) {
				newCarriers.addCarrier(singleCarrier);
				continue;
			}
			splitPerformed = true;
			verifyInfiniteFleet(singleCarrier);

			Function<CarrierShipment, Coord> shipmentCoordGetter = getCarrierShipmentCoordFunction(
					shipmentClusterLocation, singleCarrier, network);
			Function<CarrierService, Coord> serviceCoordGetter = service -> network.getLinks().get(service.getServiceLinkId()).getCoord();

			List<List<CarrierShipment>> shipmentClusters;
			List<List<CarrierService>> serviceClusters;
			switch (clusterStrategy) {
				case RANDOM -> {
					shipmentClusters = findRandomClusters(singleCarrier.getShipments().values(), numberOfCarriers,
							maxJobsPerCarrier, CarrierSplitter::shipmentId);
					serviceClusters = findRandomClusters(singleCarrier.getServices().values(), numberOfCarriers,
							maxJobsPerCarrier, CarrierSplitter::serviceId);
				}
				case GREEDY -> {
					CarrierVehicle carrierVehicle = getGreedyDepotReferenceVehicle(singleCarrier);
					shipmentClusters = findGreedyClusters(singleCarrier.getShipments().values(), network, numberOfCarriers,
							carrierVehicle, maxJobsPerCarrier, shipmentCoordGetter,
							CarrierSplitter::shipmentId, (shipment, seedName) -> shipment.getAttributes().putAttribute("seed", seedName));
					serviceClusters = findGreedyClusters(singleCarrier.getServices().values(), network, numberOfCarriers,
							carrierVehicle, maxJobsPerCarrier, serviceCoordGetter,
							CarrierSplitter::serviceId, (service, seedName) -> service.getAttributes().putAttribute("seed", seedName));
				}
				case SINGLE_LINK -> {
					shipmentClusters = findSingleLinkClusters(singleCarrier.getShipments().values(), numberOfCarriers,
							maxJobsPerCarrier, shipmentCoordGetter, CarrierSplitter::shipmentId);
					serviceClusters = findSingleLinkClusters(singleCarrier.getServices().values(), numberOfCarriers,
							maxJobsPerCarrier, serviceCoordGetter, CarrierSplitter::serviceId);
				}
				case CENTROIDS -> {
					shipmentClusters = findCentroidsClusters(singleCarrier.getShipments().values(), numberOfCarriers,
							maxJobsPerCarrier, shipmentCoordGetter, CarrierSplitter::shipmentId);
					serviceClusters = findCentroidsClusters(singleCarrier.getServices().values(), numberOfCarriers,
							maxJobsPerCarrier, serviceCoordGetter, CarrierSplitter::serviceId);
				}
				default -> throw new IllegalArgumentException("Unsupported clustering strategy: " + clusterStrategy);
			}

			int numberOfSplitCarriers = Math.max(shipmentClusters.size(), serviceClusters.size());
			ensureClusterCount(shipmentClusters, numberOfSplitCarriers);
			ensureClusterCount(serviceClusters, numberOfSplitCarriers);

			for (int i = 0; i < numberOfSplitCarriers; i++) {
				if (shipmentClusters.get(i).isEmpty() && serviceClusters.get(i).isEmpty())
					continue;
				Carrier newCarrier = createSingleCarrier(singleCarrier, jspritIterations, i + 1);
				newCarriers.addCarrier(newCarrier);

				for (CarrierShipment shipment : shipmentClusters.get(i))
					CarriersUtils.addShipment(newCarrier, shipment);
				for (CarrierService service : serviceClusters.get(i))
					CarriersUtils.addService(newCarrier, service);
				log.info("New Carrier: {}: {} Shipments, {} Services", newCarrier.getId().toString(),
						shipmentClusters.get(i).size(), serviceClusters.get(i).size());
			}
		}

		carriers.getCarriers().clear();
		for (Carrier singleCarrier : newCarriers.getCarriers().values()) {
			carriers.addCarrier(singleCarrier);
		}
		if (splitPerformed) {
			CarrierSplitPlotWriter.writeCarrierSplitPlot(scenario, clusterStrategy, shipmentClusterLocation);
		}
	}

	/**
	 * Verifies that the carrier uses an infinite fleet, which is the only fleet mode currently supported by this split.
	 *
	 * @param carrier carrier whose capabilities are checked.
	 * @throws UnsupportedOperationException if the carrier uses a finite fleet.
	 */
	private static void verifyInfiniteFleet(Carrier carrier) {
		if (carrier.getCarrierCapabilities().getFleetSize() == CarrierCapabilities.FleetSize.FINITE) {
			throw new UnsupportedOperationException("Splitting carriers with finite fleets is not implemented or tested yet.");
		}
	}

	private static List<Carrier> sortedCarriers(Carriers carriers) {
		return carriers.getCarriers().values().stream()
				.sorted(Comparator.comparing(carrier -> carrier.getId().toString()))
				.toList();
	}

	/**
	 * Returns the depot reference vehicle used by greedy clustering.
	 * <p>
	 * Greedy clustering starts its seed selection from one depot coordinate. If a carrier has multiple vehicles, this
	 * coordinate is only well-defined when all vehicles share the same start link. After that validation, the vehicle
	 * with the smallest ID is returned to keep the split deterministic.
	 *
	 * @param carrier carrier whose vehicles are checked.
	 * @return vehicle with the smallest ID, used as depot reference for greedy clustering.
	 * @throws IllegalArgumentException if the carrier has no vehicle or if its vehicles use different start links.
	 */
	private static CarrierVehicle getGreedyDepotReferenceVehicle(Carrier carrier) {
		List<CarrierVehicle> vehicles = carrier.getCarrierCapabilities().getCarrierVehicles().values().stream()
				.sorted(Comparator.comparing(vehicle -> vehicle.getId().toString()))
				.toList();
		if (vehicles.isEmpty()) {
			throw new IllegalArgumentException("Carrier " + carrier.getId() + " has no carrier vehicle.");
		}

		Id<Link> startLinkId = vehicles.getFirst().getLinkId();
		boolean hasDifferentStartLink = vehicles.stream()
				.anyMatch(vehicle -> !Objects.equals(startLinkId, vehicle.getLinkId()));
		if (hasDifferentStartLink) {
			throw new IllegalArgumentException("Greedy clustering requires all vehicles of carrier " + carrier.getId()
					+ " to have the same start link.");
		}
		return vehicles.getFirst();
	}

	private static String shipmentId(CarrierShipment shipment) {
		return shipment.getId().toString();
	}

	private static String serviceId(CarrierService service) {
		return service.getId().toString();
	}

	private static <T> List<T> sortedDemands(Collection<T> demands, Function<T, String> idGetter) {
		return new ArrayList<>(demands.stream()
				.sorted(Comparator.comparing(idGetter))
				.toList());
	}

	private static <T> Comparator<Edge<T>> edgeComparator(Function<T, String> idGetter) {
		return Comparator.<Edge<T>>comparingDouble(Edge::distance)
				.thenComparing(edge -> idGetter.apply(edge.a()))
				.thenComparing(edge -> idGetter.apply(edge.b()));
	}

	/**
	 * Creates the shipment coordinate function for a carrier.
	 *
	 * @param shipmentClusterLocation chosen shipment location representation; must be non-null for shipment carriers.
	 * @param singleCarrier carrier whose demand type is checked.
	 * @param network network used to resolve shipment link coordinates.
	 * @return coordinate lookup function for shipments of the carrier.
	 */
	private static Function<CarrierShipment, Coord> getCarrierShipmentCoordFunction(
			ShipmentClusteringLocation shipmentClusterLocation, Carrier singleCarrier, Network network) {
		boolean hasShipments = !singleCarrier.getShipments().isEmpty();

		Function<CarrierShipment, Coord> shipmentCoordGetter = shipment -> {
			throw new IllegalStateException("Shipment coordinates are only available for carriers with shipments.");
		};
		if (hasShipments) {
			if (shipmentClusterLocation == null) {
				throw new IllegalArgumentException("shipmentClusterLocation must be specified explicitly when shipments are present.");
			}
			shipmentCoordGetter = createShipmentCoordGetter(network, shipmentClusterLocation);
		}
		return shipmentCoordGetter;
	}

	/**
	 * Builds the coordinate lookup used to represent each shipment during clustering.
	 *
	 * @param network network used to resolve pickup and delivery link coordinates.
	 * @param shipmentClusterLocation shipment location representation selected by the caller.
	 * @return coordinate lookup function for a shipment.
	 */
	private static Function<CarrierShipment, Coord> createShipmentCoordGetter(Network network,
			ShipmentClusteringLocation shipmentClusterLocation) {
		return switch (shipmentClusterLocation) {
			case PICKUP -> shipment -> network.getLinks().get(shipment.getPickupLinkId()).getCoord();
			case DELIVERY -> shipment -> network.getLinks().get(shipment.getDeliveryLinkId()).getCoord();
			case MIDPOINT -> shipment -> {
				Coord pickupCoord = network.getLinks().get(shipment.getPickupLinkId()).getCoord();
				Coord deliveryCoord = network.getLinks().get(shipment.getDeliveryLinkId()).getCoord();
				return new Coord((pickupCoord.getX() + deliveryCoord.getX()) / 2,
						(pickupCoord.getY() + deliveryCoord.getY()) / 2);
			};
		};
	}

	/**
	 * Randomly distributes demands over the requested number of clusters while respecting the max cluster size.
	 *
	 * @param demands shipment or service jobs to cluster.
	 * @param numberOfCarriers number of clusters to create.
	 * @param maxJobsPerCarrier maximum number of jobs per cluster.
	 * @param <T> demand type, either {@link CarrierShipment} or {@link CarrierService}.
	 * @return clustered demands.
	 */
	private static <T> List<List<T>> findRandomClusters(Collection<T> demands, int numberOfCarriers,
			int maxJobsPerCarrier, Function<T, String> idGetter) {
		List<List<T>> clusters = new ArrayList<>();
		for (int i = 0; i < numberOfCarriers; i++) {
			clusters.add(new ArrayList<>());
		}
		Random randomSeed = new Random(1);
		for (T demand : sortedDemands(demands, idGetter)) {
			boolean hasBeenAssigned = false;
			while (!hasBeenAssigned) {
				int coinFlip = randomSeed.nextInt(numberOfCarriers);
				if (clusters.get(coinFlip).size() < maxJobsPerCarrier) {
					clusters.get(coinFlip).add(demand);
					hasBeenAssigned = true;
				}
			}
		}
		return clusters;
	}

	/**
	 * Pads a cluster list with empty clusters until it matches the number of carrier slots.
	 *
	 * @param clusters clusters to pad.
	 * @param numberOfCarriers required number of clusters.
	 * @param <T> demand type stored in the clusters.
	 */
	private static <T> void ensureClusterCount(List<List<T>> clusters, int numberOfCarriers) {
		while (clusters.size() < numberOfCarriers) {
			clusters.add(new ArrayList<>());
		}
	}

	/**
	 * Builds spatial clusters by repeatedly choosing a seed far from existing seeds and assigning nearby demands.
	 *
	 * @param demands shipment or service jobs to cluster.
	 * @param network network used to resolve the depot coordinate from the carrier vehicle.
	 * @param numberOfCarriers number of clusters to create.
	 * @param carrierVehicle vehicle whose start link is used as the depot reference.
	 * @param maxJobsPerCarrier maximum number of jobs per cluster.
	 * @param coordGetter coordinate representation for each demand.
	 * @param seedMarker callback used to mark selected seeds for debug visualization.
	 * @param <T> demand type, either {@link CarrierShipment} or {@link CarrierService}.
	 * @return clustered demands.
	 */
	private static <T> List<List<T>> findGreedyClusters(Collection<T> demands, Network network, int numberOfCarriers,
			CarrierVehicle carrierVehicle, int maxJobsPerCarrier, Function<T, Coord> coordGetter,
			Function<T, String> idGetter, BiConsumer<T, String> seedMarker) {
		List<List<T>> clusters = new ArrayList<>();
		List<Coord> seedCoords = new ArrayList<>();
		List<T> unassignedDemands = sortedDemands(demands, idGetter);

		Map<T, Coord> coords = new HashMap<>();
		for (T demand : unassignedDemands) {
			coords.put(demand, coordGetter.apply(demand));
		}

		Coord depotCoord = network.getLinks().get(carrierVehicle.getLinkId()).getCoord();
		for (int i = 0; i < numberOfCarriers; i++) {
			clusters.add(new ArrayList<>());
		}
		for (int i = 0; i < numberOfCarriers && !unassignedDemands.isEmpty(); i++) {
			double maxDistance = 0;
			T seed = null;

			for (T demand : unassignedDemands) {
				double distance = 0;
				if (seedCoords.isEmpty()) {
					distance = NetworkUtils.getEuclideanDistance(depotCoord, coords.get(demand));
				} else {
					for (Coord coord : seedCoords) {
						distance += NetworkUtils.getEuclideanDistance(coord, coords.get(demand));
					}
				}
				if (distance > maxDistance) {
					maxDistance = distance;
					seed = demand;
				}
			}

			Coord seedCoord = coords.get(seed);
			log.info("Seed {} found at Coord {}", i + 1, seedCoord);
			seedCoords.add(seedCoord);
			seedMarker.accept(seed, "seed" + (i + 1));

			List<Edge<T>> edges = new ArrayList<>();
			for (T toDemand : unassignedDemands) {
				double dist = NetworkUtils.getEuclideanDistance(seedCoord, coords.get(toDemand));
				edges.add(new Edge<>(seed, toDemand, dist));
			}
			edges.sort(edgeComparator(idGetter));

			int remainingDemands = unassignedDemands.size();
			int clusterTargetSize = maxJobsPerCarrier;
			if (remainingDemands < maxJobsPerCarrier * 1.3) {
				clusterTargetSize = remainingDemands;
			}
			for (int counter = 0; clusters.get(i).size() < clusterTargetSize && counter < remainingDemands; counter++) {
				T demandToBeClustered = edges.get(counter).b;
				clusters.get(i).add(demandToBeClustered);
				unassignedDemands.remove(demandToBeClustered);
			}
		}
		return clusters;
	}

	/**
	 * Builds agglomerative single-link clusters from pairwise Euclidean distances between demand coordinates.
	 *
	 * @param demands shipment or service jobs to cluster.
	 * @param numberOfCarriers target number of clusters.
	 * @param maxJobsPerCarrier maximum number of jobs per cluster.
	 * @param coordGetter coordinate representation for each demand.
	 * @param <T> demand type, either {@link CarrierShipment} or {@link CarrierService}.
	 * @return clustered demands.
	 */
	private static <T> List<List<T>> findSingleLinkClusters(Collection<T> demands, int numberOfCarriers,
			int maxJobsPerCarrier, Function<T, Coord> coordGetter, Function<T, String> idGetter) {
		List<List<T>> clusters = new ArrayList<>();
		List<T> demandList = sortedDemands(demands, idGetter);
		int numberOfDemands = demandList.size();

		Map<T, Coord> coords = new HashMap<>();
		for (T demand : demandList) {
			coords.put(demand, coordGetter.apply(demand));
			clusters.add(new ArrayList<>(List.of(demand)));
		}

		List<Edge<T>> edges = new ArrayList<>();
		for (int i = 0; i < numberOfDemands; i++) {
			for (int j = i + 1; j < numberOfDemands; j++) {
				T a = demandList.get(i);
				T b = demandList.get(j);
				double dist = NetworkUtils.getEuclideanDistance(coords.get(a), coords.get(b));
				edges.add(new Edge<>(a, b, dist));
			}
		}
		edges.sort(edgeComparator(idGetter));

		for (Edge<T> edge : edges) {
			T a = edge.a();
			T b = edge.b();
			int aIndex = getClusterIndex(a, clusters);
			int bIndex = getClusterIndex(b, clusters);
			if (clusters.get(aIndex).size() + clusters.get(bIndex).size() > maxJobsPerCarrier) {
				if (clusters.size() > numberOfCarriers * 1.5) {
					continue;
				} else if (clusters.get(aIndex).size() + clusters.get(bIndex).size() > maxJobsPerCarrier * 1.3) {
					continue;
				}
			}
			if (aIndex != bIndex) {
				if (aIndex < bIndex) {
					clusters.get(aIndex).addAll(clusters.get(bIndex));
					clusters.remove(bIndex);
				} else {
					clusters.get(bIndex).addAll(clusters.get(aIndex));
					clusters.remove(aIndex);
				}
				boolean noSmallClusters = checkForSmallClusters(clusters, maxJobsPerCarrier);
				if ((clusters.size() == numberOfCarriers) && noSmallClusters) {
					break;
				}
			}
		}

		return clusters;
	}

	/**
	 * Builds agglomerative clusters by repeatedly merging the two closest cluster centroids.
	 *
	 * @param demands shipment or service jobs to cluster.
	 * @param numberOfCarriers target number of clusters.
	 * @param maxJobsPerCarrier maximum number of jobs per cluster.
	 * @param coordGetter coordinate representation for each demand.
	 * @param <T> demand type, either {@link CarrierShipment} or {@link CarrierService}.
	 * @return clustered demands.
	 */
	private static <T> List<List<T>> findCentroidsClusters(Collection<T> demands, int numberOfCarriers,
			int maxJobsPerCarrier, Function<T, Coord> coordGetter, Function<T, String> idGetter) {
		List<T> demandList = sortedDemands(demands, idGetter);
		List<List<T>> clusters = new ArrayList<>();

		Map<T, Coord> coords = new HashMap<>();
		for (T demand : demandList) {
			coords.put(demand, coordGetter.apply(demand));
			clusters.add(new ArrayList<>(List.of(demand)));
		}

		while (clusters.size() > numberOfCarriers) {
			double minDistance = Double.MAX_VALUE;
			int aIndex = -1;
			int bIndex = -1;

			for (int i = 0; i < clusters.size(); i++) {
				Coord centroidA = computeCentroid(coords, clusters.get(i));
				for (int j = i + 1; j < clusters.size(); j++) {
					if (clusters.get(i).size() + clusters.get(j).size() > maxJobsPerCarrier) {
						continue;
					}
					Coord centroidB = computeCentroid(coords, clusters.get(j));
					double distanceApart = NetworkUtils.getEuclideanDistance(centroidA, centroidB);
					if (distanceApart < minDistance) {
						minDistance = distanceApart;
						aIndex = i;
						bIndex = j;
					}
				}
			}

			if (aIndex < bIndex) {
				clusters.get(aIndex).addAll(clusters.get(bIndex));
				clusters.remove(bIndex);
			} else if (aIndex > bIndex) {
				clusters.get(bIndex).addAll(clusters.get(aIndex));
				clusters.remove(aIndex);
			} else {
				log.info("No further merges found!");
				break;
			}
		}

		return clusters;
	}

	private static <T> boolean checkForSmallClusters(List<List<T>> clusters, int maxJobsPerCarrier) {
		for (List<T> cluster : clusters) {
			if (cluster.size() < maxJobsPerCarrier * 0.3) {
				return false;
			}
		}
		return true;
	}

	private static <T> int getClusterIndex(T a, List<List<T>> clusters) {
		for (int i = 0; i < clusters.size(); i++) {
			for (int j = 0; j < clusters.get(i).size(); j++) {
				if (a == clusters.get(i).get(j)) {
					return i;
				}
			}
		}
		return 0;
	}

	private static <T> Coord computeCentroid(Map<T, Coord> coords, List<T> cluster) {
		double sumX = 0.0;
		double sumY = 0.0;
		for (T demand : cluster) {
			Coord c = coords.get(demand);
			sumX += c.getX();
			sumY += c.getY();
		}
		int size = cluster.size();
		return new Coord(sumX / size, sumY / size);
	}

	private static int estimateNumberOfCarriers(int maxJobsPerCarrier, Carrier carrier) {
		int noOfJobs = carrier.getShipments().size() + carrier.getServices().size();
		int noOfCarriers = (int) Math.ceil((double) noOfJobs / maxJobsPerCarrier);
		if (noOfCarriers > 1) {
			log.info("NO OF JOBS: {} / MAX JOBS PER CARRIER: {}", noOfJobs, maxJobsPerCarrier);
			log.info("NO OF CARRIERS: {}", noOfCarriers);
		}
		return noOfCarriers;
	}

	private static Carrier createSingleCarrier(Carrier originalCarrier, int jspritIterations, int carrierNumber) {
		String carrierName = originalCarrier.getId().toString();
		Carrier newCarrier = CarriersUtils.createCarrier(Id.create(carrierName + "_split_" + carrierNumber, Carrier.class));
		AttributesUtils.copyTo(originalCarrier.getAttributes(), newCarrier.getAttributes());
		CarriersUtils.setJspritIterations(newCarrier, jspritIterations);
		CarrierCapabilities.Builder capabilitiesBuilder = CarrierCapabilities.Builder.newInstance()
				.setFleetSize(CarrierCapabilities.FleetSize.INFINITE);
		List<CarrierVehicle> carrierVehicles = originalCarrier.getCarrierCapabilities().getCarrierVehicles().values().stream()
				.sorted(Comparator.comparing(vehicle -> vehicle.getId().toString()))
				.toList();
		for (int vehicleIndex = 0; vehicleIndex < carrierVehicles.size(); vehicleIndex++) {
			capabilitiesBuilder.addVehicle(createSplitCarrierVehicle(newCarrier.getId(), carrierVehicles.get(vehicleIndex),
					vehicleIndex + 1));
		}
		CarrierCapabilities carrierCapabilities = capabilitiesBuilder.build();
		newCarrier.setCarrierCapabilities(carrierCapabilities);
		return newCarrier;
	}

	private static CarrierVehicle createSplitCarrierVehicle(Id<Carrier> splitCarrierId, CarrierVehicle originalVehicle,
			int vehicleNumber) {
		Id<Vehicle> splitVehicleId = Id.createVehicleId(splitCarrierId + "_" + vehicleNumber);
		CarrierVehicle splitVehicle = CarrierVehicle.Builder.newInstance(splitVehicleId, originalVehicle.getLinkId(),
						originalVehicle.getType())
				.setEarliestStart(originalVehicle.getEarliestStartTime())
				.setLatestEnd(originalVehicle.getLatestEndTime())
				.build();
		AttributesUtils.copyTo(originalVehicle.getAttributes(), splitVehicle.getAttributes());
		return splitVehicle;
	}
}
