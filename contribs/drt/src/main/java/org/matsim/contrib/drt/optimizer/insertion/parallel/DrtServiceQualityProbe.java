/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2026 by the members listed in the COPYING,        *
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

import com.google.inject.Provider;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
	import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.common.zones.ZoneSystemUtils;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystem;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.optimizer.insertion.DrtInsertionSearch;
import org.matsim.contrib.drt.optimizer.insertion.InsertionWithDetourData;
import org.matsim.contrib.drt.optimizer.insertion.RequestFleetFilter;
import org.matsim.contrib.drt.optimizer.constraints.DrtRouteConstraints;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.routing.DrtRouteConstraintsCalculator;
import org.matsim.contrib.drt.routing.DrtStopFacility;
import org.matsim.contrib.drt.routing.DrtStopNetwork;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.load.DvrpLoad;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.DvrpLoadFromTrip;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Estimates stop-to-stop DRT service quality from the current optimizer state without scheduling the requests.
 */
class DrtServiceQualityProbe {
	private static final Logger LOG = LogManager.getLogger(DrtServiceQualityProbe.class);
	private static final List<Id<Person>> PASSENGER_IDS = List.of(Id.createPersonId("drt-service-quality-probe"));

	private final MatsimServices matsimServices;
	private final String mode;
	private final List<DrtStopFacility> stops;
	private final List<StopPair> stopPairs;
	private final List<ProbeLocation> zoneLocations;
	private final Network network;
	private final TravelTime travelTime;
	private final LeastCostPathCalculator router;
	private final DrtRouteConstraintsCalculator constraintsCalculator;
	private final DvrpLoad load;
	private final RequestFleetFilter requestFleetFilter;
	private final Provider<DrtInsertionSearch> insertionSearchProvider;
	private final List<Double> probeTimes;
	private final String outputFile;
	private final String delimiter;
	private final DrtParallelInserterParams.ServiceQualityProbeSpatialResolution spatialResolution;
	private final double zoneCellSize;
	private final Person dummyPerson = PopulationUtils.getFactory().createPerson(Id.createPersonId("drt-service-quality-probe"));
	private final Attributes tripAttributes = new AttributesImpl();
	private int nextProbeTimeIndex = 0;
	private BufferedWriter outputWriter;

	private record ProbeLocation(String id, Link link, Coord coord) {
	}

	record StopServiceQualityRecord(double time, Id<DrtStopFacility> originStop, Id<DrtStopFacility> destinationStop,
								double waitTime, double directRideTime, double rideTimeWithDetour, double detourFactor,
								double directRideDistance, double rideDistanceWithDetour, double distanceDetourFactor) {
	}

	private record StopPair(DrtStopFacility origin, DrtStopFacility destination) {
	}

	private record ProbeRequest(DrtRequest request, double directRideDistance) {
	}

	record ZoneServiceQualityRecord(double time, Id<Zone> originZone, double originX, double originY,
									Id<Zone> destinationZone, double destinationX, double destinationY,
									double waitTime, double directRideTime, double rideTimeWithDetour,
									double detourFactor) {
	}

	DrtServiceQualityProbe(MatsimServices matsimServices, String mode, DrtStopNetwork stopNetwork, Network network,
						   TravelTime travelTime, LeastCostPathCalculatorFactory leastCostPathCalculatorFactory,
						   TravelDisutilityFactory travelDisutilityFactory,
						   DrtRouteConstraintsCalculator constraintsCalculator, DvrpLoadFromTrip loadFromTrip,
						   RequestFleetFilter requestFleetFilter, Provider<DrtInsertionSearch> insertionSearchProvider,
						   DrtParallelInserterParams params) {
		this.matsimServices = matsimServices;
		this.mode = mode;
		this.stops = stopNetwork.getDrtStops().values().stream()
			.sorted(Comparator.comparing(stop -> stop.getId().toString()))
			.collect(Collectors.toList());
		this.network = network;
		this.delimiter = matsimServices.getConfig().global().getDefaultDelimiter();
		this.spatialResolution = params.getServiceQualityProbeSpatialResolution();
		this.stopPairs = spatialResolution == DrtParallelInserterParams.ServiceQualityProbeSpatialResolution.STOP_TO_STOP
			? createStopPairs(params.getServiceQualityProbeStopPairInputFiles()) : List.of();
		if (spatialResolution == DrtParallelInserterParams.ServiceQualityProbeSpatialResolution.ZONE_TO_ZONE
			&& !params.getServiceQualityProbeStopPairInputFiles().isBlank()) {
			throw new IllegalArgumentException("serviceQualityProbeStopPairInputFiles can only be used with STOP_TO_STOP probing");
		}
		this.zoneCellSize = params.getServiceQualityProbeZoneCellSize();
		this.zoneLocations = spatialResolution == DrtParallelInserterParams.ServiceQualityProbeSpatialResolution.ZONE_TO_ZONE
			? createZoneLocations()
			: List.of();
		this.travelTime = travelTime;
		this.router = leastCostPathCalculatorFactory.createPathCalculator(network,
			travelDisutilityFactory.createTravelDisutility(travelTime), travelTime);
		this.constraintsCalculator = constraintsCalculator;
		this.load = loadFromTrip.getLoad(dummyPerson, tripAttributes);
		this.requestFleetFilter = requestFleetFilter;
		this.insertionSearchProvider = insertionSearchProvider;
		this.probeTimes = parseProbeTimes(params.getServiceQualityProbeTimes());
		this.outputFile = params.getServiceQualityProbeOutputFile();
	}

	boolean isEnabled() {
		return !probeTimes.isEmpty();
	}

	boolean isDue(double now) {
		return matsimServices.getIterationNumber() == matsimServices.getConfig().controller().getLastIteration()
			&& nextProbeTimeIndex < probeTimes.size()
			&& now >= probeTimes.get(nextProbeTimeIndex);
	}

	void probeIfDue(double now, Map<Id<DvrpVehicle>, VehicleEntry> vehicleEntries) {
		if (matsimServices.getIterationNumber() != matsimServices.getConfig().controller().getLastIteration()) {
			return;
		}
		while (nextProbeTimeIndex < probeTimes.size() && now >= probeTimes.get(nextProbeTimeIndex)) {
			double probeTime = probeTimes.get(nextProbeTimeIndex++);
			probe(probeTime, vehicleEntries);
		}
	}

	void writeOutput() {
		if (outputWriter == null) {
			return;
		}
		try {
			outputWriter.close();
		} catch (IOException e) {
			LOG.error("Failed to write DRT service quality probe output", e);
		} finally {
			outputWriter = null;
		}
	}

	private void probe(double time, Map<Id<DvrpVehicle>, VehicleEntry> vehicleEntries) {
		try {
			ensureOutputWriter();
			if (spatialResolution == DrtParallelInserterParams.ServiceQualityProbeSpatialResolution.ZONE_TO_ZONE) {
				probeZones(time, vehicleEntries);
			} else {
				probeStops(time, vehicleEntries);
			}
			outputWriter.flush();
		} catch (IOException e) {
			throw new RuntimeException("Failed to stream DRT service quality probe output", e);
		}
	}

	private void probeStops(double time, Map<Id<DvrpVehicle>, VehicleEntry> vehicleEntries) throws IOException {
		if (stops.isEmpty()) {
			throw new IllegalStateException("DRT stop-to-stop service quality probes require a non-empty DrtStopNetwork for mode " + mode);
		}
		LOG.info("Estimating DRT service quality for {} stop-to-stop pairs at time {}", stopPairs.size(), time);
		DrtInsertionSearch insertionSearch = insertionSearchProvider.get();
		for (StopPair pair : stopPairs) {
			Estimate estimate = estimate(time, pair.origin.getId().toString(), getLink(pair.origin),
				pair.destination.getId().toString(), getLink(pair.destination), vehicleEntries, insertionSearch);
			writeStopRecord(new StopServiceQualityRecord(time, pair.origin.getId(), pair.destination.getId(),
				estimate.waitTime, estimate.directRideTime, estimate.rideTimeWithDetour, estimate.detourFactor,
				estimate.directRideDistance, estimate.rideDistanceWithDetour, estimate.distanceDetourFactor));
		}
	}

	private void probeZones(double time, Map<Id<DvrpVehicle>, VehicleEntry> vehicleEntries) throws IOException {
		if (zoneLocations.isEmpty()) {
			throw new IllegalStateException("DRT zone-to-zone service quality probes require a non-empty travel-time-matrix zone system for mode " + mode);
		}
		LOG.info("Estimating DRT service quality for {} zone-to-zone pairs at time {}", zoneLocations.size() * (zoneLocations.size() - 1), time);
		DrtInsertionSearch insertionSearch = insertionSearchProvider.get();
		for (ProbeLocation origin : zoneLocations) {
			for (ProbeLocation destination : zoneLocations) {
				if (origin.id.equals(destination.id)) {
					continue;
				}
				Estimate estimate = estimate(time, origin.id, origin.link, destination.id, destination.link,
					vehicleEntries, insertionSearch);
				writeZoneRecord(new ZoneServiceQualityRecord(time, Id.create(origin.id, Zone.class),
					origin.coord.getX(), origin.coord.getY(), Id.create(destination.id, Zone.class),
					destination.coord.getX(), destination.coord.getY(), estimate.waitTime,
					estimate.directRideTime, estimate.rideTimeWithDetour, estimate.detourFactor));
			}
		}
	}

	private Estimate estimate(double time, String originId, Link fromLink, String destinationId, Link toLink,
							  Map<Id<DvrpVehicle>, VehicleEntry> vehicleEntries,
							  DrtInsertionSearch insertionSearch) {
		ProbeRequest probeRequest = createRequest(time, originId, fromLink, destinationId, toLink);
		DrtRequest request = probeRequest.request;
		Collection<VehicleEntry> filteredFleet = requestFleetFilter.filter(request, vehicleEntries, time);
		Optional<InsertionWithDetourData> insertion = insertionSearch.findBestInsertion(request, filteredFleet);
		if (insertion.isEmpty()) {
			return new Estimate(Double.NaN, request.getUnsharedRideTime(), Double.NaN, Double.NaN,
				probeRequest.directRideDistance, Double.NaN, Double.NaN);
		}

		double pickupTime = insertion.get().detourTimeInfo.pickupDetourInfo.requestPickupTime;
		double dropoffTime = insertion.get().detourTimeInfo.dropoffDetourInfo.requestDropoffTime;
		double waitTime = pickupTime - request.getEarliestStartTime();
		double rideTimeWithDetour = dropoffTime - pickupTime;
		double detourFactor = rideTimeWithDetour / request.getUnsharedRideTime();
		double rideDistanceWithDetour = calculateRideDistanceWithDetour(insertion.get(), pickupTime);
		return new Estimate(waitTime, request.getUnsharedRideTime(), rideTimeWithDetour, detourFactor,
			probeRequest.directRideDistance, rideDistanceWithDetour, rideDistanceWithDetour / probeRequest.directRideDistance);
	}

	private record Estimate(double waitTime, double directRideTime, double rideTimeWithDetour, double detourFactor,
			double directRideDistance, double rideDistanceWithDetour, double distanceDetourFactor) {
	}

	private ProbeRequest createRequest(double time, String originId, Link fromLink, String destinationId, Link toLink) {
		VrpPathWithTravelData unsharedPath = VrpPaths.calcAndCreatePath(fromLink, toLink, time, router, travelTime);
		double directRideTime = unsharedPath.getTravelTime();
		double directRideDistance = VrpPaths.calcDistance(unsharedPath);
		DrtRouteConstraints constraints = constraintsCalculator.calculateRouteConstraints(time, fromLink, toLink,
			dummyPerson, tripAttributes, directRideTime, directRideDistance);

		DrtRequest request = DrtRequest.newBuilder()
			.id(Id.create("service-quality-probe-" + time + "-" + originId + "-" + destinationId, Request.class))
			.passengerIds(PASSENGER_IDS)
			.mode(mode)
			.fromLink(fromLink)
			.toLink(toLink)
			.earliestDepartureTime(time)
			.constraints(constraints)
			.submissionTime(time)
			.load(load)
			.unsharedRideTime(directRideTime)
			.build();
		return new ProbeRequest(request, directRideDistance);
	}

	private double calculateRideDistanceWithDetour(InsertionWithDetourData insertionWithDetourData, double departureTime) {
		var insertion = insertionWithDetourData.insertion;
		List<Waypoint> itinerary = new ArrayList<>();
		itinerary.add(insertion.pickup.newWaypoint);
		if (insertion.pickup.index == insertion.dropoff.index) {
			itinerary.add(insertion.dropoff.newWaypoint);
		} else {
			for (int index = insertion.pickup.index + 1; index <= insertion.dropoff.index; index++) {
				itinerary.add(insertion.vehicleEntry.getWaypoint(index));
			}
			itinerary.add(insertion.dropoff.newWaypoint);
		}

		double distance = 0;
		double time = departureTime;
		for (int index = 0; index < itinerary.size() - 1; index++) {
			VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(itinerary.get(index).getLink(), itinerary.get(index + 1).getLink(),
				time, router, travelTime);
			distance += VrpPaths.calcDistance(path);
			time += path.getTravelTime();
		}
		return distance;
	}

	private Link getLink(DrtStopFacility stop) {
		Link link = network.getLinks().get(stop.getLinkId());
		if (link == null) {
			throw new IllegalStateException("Cannot find network link " + stop.getLinkId() + " for DRT stop " + stop.getId());
		}
		return link;
	}

	private List<ProbeLocation> createZoneLocations() {
		ZoneSystem zoneSystem = createProbeZoneSystem();
		Set<Id<Zone>> zoneIdsWithStops = getZoneIdsWithStops(zoneSystem);
		if (!zoneIdsWithStops.isEmpty()) {
			LOG.info("Restricting DRT service quality probe zones to {} zones with DRT stops", zoneIdsWithStops.size());
		}
		return zoneSystem.getZones().values().stream()
			.filter(zone -> zoneIdsWithStops.isEmpty() || zoneIdsWithStops.contains(zone.getId()))
			.sorted(Comparator.comparing(zone -> zone.getId().toString()))
			.map(zone -> createZoneLocation(zoneSystem, zone))
			.flatMap(Optional::stream)
			.toList();
	}

	private List<StopPair> createStopPairs(String inputFiles) {
		if (inputFiles == null || inputFiles.isBlank()) {
			return stops.stream().flatMap(origin -> stops.stream()
				.filter(destination -> !origin.getId().equals(destination.getId()))
				.map(destination -> new StopPair(origin, destination))).toList();
		}
		Map<String, DrtStopFacility> stopsById = stops.stream()
			.collect(Collectors.toMap(stop -> stop.getId().toString(), stop -> stop));
		Map<String, StopPair> uniquePairs = new TreeMap<>();
		for (String inputFile : inputFiles.split(",")) {
			if (!inputFile.trim().isEmpty()) {
				readStopPairs(inputFile.trim(), stopsById, uniquePairs);
			}
		}
		if (uniquePairs.isEmpty()) {
			throw new IllegalArgumentException("No non-identical stop pairs found in serviceQualityProbeStopPairInputFiles");
		}
		return List.copyOf(uniquePairs.values());
	}

	private void readStopPairs(String inputFile, Map<String, DrtStopFacility> stopsById, Map<String, StopPair> uniquePairs) {
		CSVFormat format = CSVFormat.DEFAULT.builder().setDelimiter(delimiter).setHeader().setSkipHeaderRecord(true).build();
		try (CSVParser parser = new CSVParser(IOUtils.getBufferedReader(inputFile), format)) {
			for (CSVRecord record : parser) {
				DrtStopFacility origin = getValidatedStop(record, "accessStopId", "accessLinkId", stopsById, inputFile);
				DrtStopFacility destination = getValidatedStop(record, "egressStopId", "egressLinkId", stopsById, inputFile);
				if (!origin.getId().equals(destination.getId())) {
					uniquePairs.put(origin.getId() + "\u0000" + destination.getId(), new StopPair(origin, destination));
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to read service-quality stop pairs from " + inputFile, e);
		}
	}

	private DrtStopFacility getValidatedStop(CSVRecord record, String stopColumn, String linkColumn,
			Map<String, DrtStopFacility> stopsById, String inputFile) {
		DrtStopFacility stop = stopsById.get(record.get(stopColumn));
		if (stop == null) {
			throw new IllegalArgumentException("Unknown DRT stop '" + record.get(stopColumn) + "' in " + inputFile);
		}
		if (!stop.getLinkId().toString().equals(record.get(linkColumn))) {
			throw new IllegalArgumentException("Link '" + record.get(linkColumn) + "' does not match DRT stop '" + stop.getId() + "' in " + inputFile);
		}
		return stop;
	}

	private ZoneSystem createProbeZoneSystem() {
		if (!Double.isNaN(zoneCellSize)) {
			if (!Double.isFinite(zoneCellSize) || zoneCellSize <= 0) {
				throw new IllegalArgumentException("serviceQualityProbeZoneCellSize must be a positive finite number");
			}
			LOG.info("Creating DRT service quality probe square-grid zone system with cell size {}", zoneCellSize);
			return new SquareGridZoneSystem(network, zoneCellSize, zone -> true);
		}

		LOG.info("Creating DRT service quality probe zone system from DVRP travel-time-matrix zone parameters");
		return ZoneSystemUtils.createZoneSystem(matsimServices.getConfig().getContext(), network,
			DvrpConfigGroup.get(matsimServices.getConfig()).getTravelTimeMatrixParams().getZoneSystemParams(),
			matsimServices.getConfig().global().getCoordinateSystem(), zone -> true);
	}

	private Optional<ProbeLocation> createZoneLocation(ZoneSystem zoneSystem, Zone zone) {
		List<Link> stopLinksInZone = getStopLinksInZone(zoneSystem, zone);
		if (!stopLinksInZone.isEmpty()) {
			return stopLinksInZone.stream()
				.min(Comparator
					.comparingDouble((Link link) -> squaredDistance(link.getCoord(), zone.getCentroid()))
					.thenComparing(link -> link.getId().toString()))
				.map(link -> new ProbeLocation(zone.getId().toString(), link, zone.getCentroid()));
		}

		List<Link> links = zoneSystem.getLinksForZoneId(zone.getId());
		if (links == null || links.isEmpty()) {
			return Optional.empty();
		}
		return links.stream()
			.filter(link -> network.getLinks().containsKey(link.getId()))
			.filter(link -> link.getAllowedModes().contains(mode))
			.min(Comparator
				.comparingDouble((Link link) -> squaredDistance(link.getCoord(), zone.getCentroid()))
			.thenComparing(link -> link.getId().toString()))
			.map(link -> new ProbeLocation(zone.getId().toString(), link, zone.getCentroid()));
	}

	private Set<Id<Zone>> getZoneIdsWithStops(ZoneSystem zoneSystem) {
		return stops.stream()
			.map(stop -> zoneSystem.getZoneForLinkId(stop.getLinkId()))
			.flatMap(Optional::stream)
			.map(Zone::getId)
			.collect(Collectors.toSet());
	}

	private List<Link> getStopLinksInZone(ZoneSystem zoneSystem, Zone zone) {
		return stops.stream()
			.filter(stop -> zoneSystem.getZoneForLinkId(stop.getLinkId())
				.map(stopZone -> stopZone.getId().equals(zone.getId()))
				.orElse(false))
			.map(this::getLink)
			.toList();
	}

	private void ensureOutputWriter() throws IOException {
		if (outputWriter != null) {
			return;
		}
		String filename = matsimServices.getControllerIO().getOutputFilename(outputFile);
		outputWriter = IOUtils.getBufferedWriter(filename);
		if (spatialResolution == DrtParallelInserterParams.ServiceQualityProbeSpatialResolution.ZONE_TO_ZONE) {
			outputWriter.write(String.join(delimiter, "time", "originZone", "originX", "originY", "destinationZone",
				"destinationX", "destinationY", "waitTime", "directRideTime", "rideTimeWithDetour", "detourFactor"));
		} else {
			outputWriter.write(String.join(delimiter, "time", "originStop", "destinationStop", "waitTime", "directRideTime",
			"rideTimeWithDetour", "detourFactor", "directRideDistance", "rideDistanceWithDetour", "distanceDetourFactor"));
		}
		outputWriter.newLine();
	}

	private void writeStopRecord(StopServiceQualityRecord record) throws IOException {
		outputWriter.write(String.join(delimiter,
				Double.toString(record.time),
				record.originStop.toString(),
				record.destinationStop.toString(),
				Double.toString(record.waitTime),
				Double.toString(record.directRideTime),
				Double.toString(record.rideTimeWithDetour),
				Double.toString(record.detourFactor),
				Double.toString(record.directRideDistance),
				Double.toString(record.rideDistanceWithDetour),
				Double.toString(record.distanceDetourFactor)));
		outputWriter.newLine();
	}

	private void writeZoneRecord(ZoneServiceQualityRecord record) throws IOException {
		outputWriter.write(String.join(delimiter,
				Double.toString(record.time),
				record.originZone.toString(),
				Double.toString(record.originX),
				Double.toString(record.originY),
				record.destinationZone.toString(),
				Double.toString(record.destinationX),
				Double.toString(record.destinationY),
				Double.toString(record.waitTime),
				Double.toString(record.directRideTime),
				Double.toString(record.rideTimeWithDetour),
				Double.toString(record.detourFactor)));
		outputWriter.newLine();
	}

	private static double squaredDistance(Coord first, Coord second) {
		double dx = first.getX() - second.getX();
		double dy = first.getY() - second.getY();
		return dx * dx + dy * dy;
	}

	private static List<Double> parseProbeTimes(String probeTimes) {
		if (probeTimes == null || probeTimes.isBlank()) {
			return List.of();
		}
		return Arrays.stream(probeTimes.split(","))
			.map(String::trim)
			.filter(token -> !token.isEmpty())
			.map(Double::parseDouble)
			.sorted()
			.toList();
	}
}
