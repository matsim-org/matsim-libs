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
	private final List<StopServiceQualityRecord> stopRecords = new ArrayList<>();
	private final List<ZoneServiceQualityRecord> zoneRecords = new ArrayList<>();

	private record ProbeLocation(String id, Link link, Coord coord) {
	}

	record StopServiceQualityRecord(double time, Id<DrtStopFacility> originStop, Id<DrtStopFacility> destinationStop,
								double waitTime, double directRideTime, double rideTimeWithDetour, double detourFactor) {
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
		this.spatialResolution = params.getServiceQualityProbeSpatialResolution();
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
		this.delimiter = matsimServices.getConfig().global().getDefaultDelimiter();
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
		if (stopRecords.isEmpty() && zoneRecords.isEmpty()) {
			return;
		}
		String filename = matsimServices.getControllerIO().getOutputFilename(outputFile);
		try (BufferedWriter writer = IOUtils.getBufferedWriter(filename)) {
			if (spatialResolution == DrtParallelInserterParams.ServiceQualityProbeSpatialResolution.ZONE_TO_ZONE) {
				writeZoneRecords(writer);
			} else {
				writeStopRecords(writer);
			}
		} catch (IOException e) {
			LOG.error("Failed to write DRT service quality probe output", e);
		}
	}

	private void probe(double time, Map<Id<DvrpVehicle>, VehicleEntry> vehicleEntries) {
		if (spatialResolution == DrtParallelInserterParams.ServiceQualityProbeSpatialResolution.ZONE_TO_ZONE) {
			probeZones(time, vehicleEntries);
		} else {
			probeStops(time, vehicleEntries);
		}
	}

	private void probeStops(double time, Map<Id<DvrpVehicle>, VehicleEntry> vehicleEntries) {
		if (stops.isEmpty()) {
			throw new IllegalStateException("DRT stop-to-stop service quality probes require a non-empty DrtStopNetwork for mode " + mode);
		}
		LOG.info("Estimating DRT service quality for {} stop-to-stop pairs at time {}", stops.size() * (stops.size() - 1), time);
		DrtInsertionSearch insertionSearch = insertionSearchProvider.get();
		for (DrtStopFacility origin : stops) {
			for (DrtStopFacility destination : stops) {
				if (origin.getId().equals(destination.getId())) {
					continue;
				}
				Estimate estimate = estimate(time, origin.getId().toString(), getLink(origin),
					destination.getId().toString(), getLink(destination), vehicleEntries, insertionSearch);
				stopRecords.add(new StopServiceQualityRecord(time, origin.getId(), destination.getId(),
					estimate.waitTime, estimate.directRideTime, estimate.rideTimeWithDetour, estimate.detourFactor));
			}
		}
	}

	private void probeZones(double time, Map<Id<DvrpVehicle>, VehicleEntry> vehicleEntries) {
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
				zoneRecords.add(new ZoneServiceQualityRecord(time, Id.create(origin.id, Zone.class),
					origin.coord.getX(), origin.coord.getY(), Id.create(destination.id, Zone.class),
					destination.coord.getX(), destination.coord.getY(), estimate.waitTime,
					estimate.directRideTime, estimate.rideTimeWithDetour, estimate.detourFactor));
			}
		}
	}

	private Estimate estimate(double time, String originId, Link fromLink, String destinationId, Link toLink,
							  Map<Id<DvrpVehicle>, VehicleEntry> vehicleEntries,
							  DrtInsertionSearch insertionSearch) {
		DrtRequest request = createRequest(time, originId, fromLink, destinationId, toLink);
		Collection<VehicleEntry> filteredFleet = requestFleetFilter.filter(request, vehicleEntries, time);
		Optional<InsertionWithDetourData> insertion = insertionSearch.findBestInsertion(request, filteredFleet);
		if (insertion.isEmpty()) {
			return new Estimate(Double.NaN, request.getUnsharedRideTime(), Double.NaN, Double.NaN);
		}

		double pickupTime = insertion.get().detourTimeInfo.pickupDetourInfo.requestPickupTime;
		double dropoffTime = insertion.get().detourTimeInfo.dropoffDetourInfo.requestDropoffTime;
		double waitTime = pickupTime - request.getEarliestStartTime();
		double rideTimeWithDetour = dropoffTime - pickupTime;
		double detourFactor = rideTimeWithDetour / request.getUnsharedRideTime();
		return new Estimate(waitTime, request.getUnsharedRideTime(), rideTimeWithDetour, detourFactor);
	}

	private record Estimate(double waitTime, double directRideTime, double rideTimeWithDetour, double detourFactor) {
	}

	private DrtRequest createRequest(double time, String originId, Link fromLink, String destinationId, Link toLink) {
		VrpPathWithTravelData unsharedPath = VrpPaths.calcAndCreatePath(fromLink, toLink, time, router, travelTime);
		double directRideTime = unsharedPath.getTravelTime();
		double directRideDistance = VrpPaths.calcDistance(unsharedPath);
		DrtRouteConstraints constraints = constraintsCalculator.calculateRouteConstraints(time, fromLink, toLink,
			dummyPerson, tripAttributes, directRideTime, directRideDistance);

		return DrtRequest.newBuilder()
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

	private void writeStopRecords(BufferedWriter writer) throws IOException {
		writer.write(String.join(delimiter, "time", "originStop", "destinationStop", "waitTime", "directRideTime",
			"rideTimeWithDetour", "detourFactor"));
		writer.newLine();
		for (StopServiceQualityRecord record : stopRecords) {
			writer.write(String.join(delimiter,
				Double.toString(record.time),
				record.originStop.toString(),
				record.destinationStop.toString(),
				Double.toString(record.waitTime),
				Double.toString(record.directRideTime),
				Double.toString(record.rideTimeWithDetour),
				Double.toString(record.detourFactor)));
			writer.newLine();
		}
	}

	private void writeZoneRecords(BufferedWriter writer) throws IOException {
		writer.write(String.join(delimiter, "time", "originZone", "originX", "originY", "destinationZone",
			"destinationX", "destinationY", "waitTime", "directRideTime", "rideTimeWithDetour", "detourFactor"));
		writer.newLine();
		for (ZoneServiceQualityRecord record : zoneRecords) {
			writer.write(String.join(delimiter,
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
			writer.newLine();
		}
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
