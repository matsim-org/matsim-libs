/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.contrib.emissions;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.api.core.v01.events.HasPersonId;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.Transit;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.PositionEvent;
import org.matsim.vis.snapshotwriters.PositionInfo;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PositionEmissionsModule extends AbstractModule {

	private static final Logger log = LogManager.getLogger(PositionEmissionsModule.class);

	@Inject
	private Config config;

	private static void checkConsistency(Config config) {

		if (config.qsim().getSnapshotPeriod() > 1) {
			throw new RuntimeException("only snapshot periods of 1s are supported.");
		}
		if (!config.controller().getSnapshotFormat().contains(ControllerConfigGroup.SnapshotFormat.positionevents)) {
			throw new RuntimeException("config.controler.snapshotFormat must be set to 'positionevents'");
		}
		if (isNotCorrectSnapshotStyle(config.qsim().getSnapshotStyle())) {
			throw new RuntimeException("I think generating emissions only makes sense if config.qsim.snapshotstyle == queue or == kinematicWaves");
		}
	}

	private static boolean isNotCorrectSnapshotStyle(QSimConfigGroup.SnapshotStyle style) {
		return !(style.equals(QSimConfigGroup.SnapshotStyle.queue) || style.equals(QSimConfigGroup.SnapshotStyle.kinematicWaves));
	}

	@Override
	public void install() {

		checkConsistency(config);
		bind(EmissionCalculator.class);
		bind(EmissionModule.class);
		addEventHandlerBinding().to(Handler.class);
	}

	static class EmissionCalculator {

		@Inject
		private EmissionsConfigGroup emissionsConfigGroup;
		@Inject
		private EmissionModule emissionModule;

		Map<Pollutant, Double> calculateWarmEmissions(Vehicle vehicle, Link link, double distance, double time) {

			var vehicleAttributes = getVehicleAttributes(vehicle);
			var roadType = EmissionUtils.getHbefaRoadType(link);
			return emissionModule.getWarmEmissionAnalysisModule().calculateWarmEmissions(time, roadType, link.getFreespeed(), distance, vehicleAttributes);
		}

		Map<Pollutant, Double> calculateColdEmissions(Vehicle vehicle, double parkingDuration, int distance) {

			// linkid and event time are never used in the underlying code.
			return emissionModule.getColdEmissionAnalysisModule()
					.checkVehicleInfoAndCalculateWColdEmissions(vehicle.getType(), vehicle.getId(), null, -1, parkingDuration, distance);
		}

		private Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> getVehicleAttributes(Vehicle vehicle) {
			// the following block fixes the vehicle type's emission information when using an  old vehicle type format
			// the unit test I found uses an old format, so have it here.
			{
				String hbefaVehicleTypeDescription = EmissionUtils.getHbefaVehicleDescription(vehicle.getType(), emissionsConfigGroup);
				// (this will, importantly, repair the hbefa description in the vehicle type. kai/kai, jan'20)
				Gbl.assertNotNull(hbefaVehicleTypeDescription);
			}
			return EmissionUtils.convertVehicleDescription2VehicleInformationTuple(vehicle.getType());
		}

		private Map<Pollutant, Double> getEmptyColdEmissions() {
			return emissionModule.getColdPollutants().stream().collect(Collectors.toMap(p -> p, p -> 0.0));
		}

		private Map<Pollutant, Double> getEmptyWarmEmissions() {
			return emissionModule.getWarmPollutants().stream().collect(Collectors.toMap(p -> p, p -> 0.0));
		}
	}

	static class Handler implements BasicEventHandler {

		private final Map<Id<Vehicle>, LinkedList<PositionEvent>> trajectories = new HashMap<>();
		private final Map<Id<Vehicle>, VehicleEntersTrafficEvent> vehiclesInTraffic = new HashMap<>();
		private final Map<Id<Vehicle>, VehicleLeavesTrafficEvent> parkedVehicles = new HashMap<>();
		private final Map<Id<Vehicle>, Double> parkingDurations = new HashMap<>();
		private final Set<Id<Vehicle>> vehiclesEmittingColdEmissions = new HashSet<>();

		@Inject
		private EmissionCalculator emissionCalculator;

		@Inject
		private Vehicles vehicles;

		@Inject(optional = true)
		@Transit
		private Vehicles transitVehicles;

		@Inject
		private Network network;

		@Inject
		private EventsManager eventsManager;

		@Override
		public void handleEvent(Event event) {

			var type = event.getEventType();
			switch (type) {

				case PositionEvent.EVENT_TYPE:
					handlePositionEvent((PositionEvent) event);
					break;
				case VehicleEntersTrafficEvent.EVENT_TYPE:
					handleVehicleEntersTrafficEvent((VehicleEntersTrafficEvent) event);
					break;
				case VehicleLeavesTrafficEvent.EVENT_TYPE:
					handleVehicleLeavesTraffic((VehicleLeavesTrafficEvent) event);
					break;
				default:
					// we're not interested in anything else
			}
		}

		private void handleVehicleEntersTrafficEvent(VehicleEntersTrafficEvent event) {
			if (!event.getNetworkMode().equals(TransportMode.car)) return;

			vehiclesInTraffic.put(event.getVehicleId(), event);
			vehiclesEmittingColdEmissions.add(event.getVehicleId());
			var parkingDuration = calculateParkingTime(event.getVehicleId(), event.getTime());
			this.parkingDurations.put(event.getVehicleId(), parkingDuration);
		}

		/**
		 * Calculates parking time AND removes vehicle from parking vehicles
		 */
		private double calculateParkingTime(Id<Vehicle> vehicleId, double startTime) {

			if (parkedVehicles.containsKey(vehicleId)) {
				var stopTime = parkedVehicles.remove(vehicleId).getTime();
				return startTime - stopTime;
			}
			//parking duration is assumed to be at least 12 hours when parking overnight
			return 43200;
		}

		private void handleVehicleLeavesTraffic(VehicleLeavesTrafficEvent event) {
			if (!event.getNetworkMode().equals(TransportMode.car)) return;

			trajectories.remove(event.getVehicleId());
			parkedVehicles.put(event.getVehicleId(), event);
		}

		private void handlePositionEvent(PositionEvent event) {

			if (!event.getState().equals(AgentSnapshotInfo.AgentState.PERSON_DRIVING_CAR))
				return; // only calculate emissions for cars

			if (!vehiclesInTraffic.containsKey(event.getVehicleId()))
				return; // only collect positions if vehicle has entered traffic (if vehicle is wait2link its position is calculated, but it hasn't entered traffic yet.)

			if (trajectories.containsKey(event.getVehicleId())) {
				computeCombinedEmissionEvent(event);
			}

			// remember all the positions. It is important to do it here, so that the current event is not yet in the
			// queue when emissions events are computed
			// the first few positions seem to be a little weird talk about this with kai
			trajectories.computeIfAbsent(event.getVehicleId(), key -> new LinkedList<>()).add(event);
		}

		private Map<Pollutant, Double> computeColdEmissions(PositionEvent event, Vehicle vehicle, double distanceToLastPosition) {

			// we remember the vehicles which are currently emitting cold emissions if not stored here return nothing
			if (!vehiclesEmittingColdEmissions.contains(vehicle.getId()))
				return emissionCalculator.getEmptyColdEmissions();

			double distance = calculateTravelledDistance(event);

			// this model assumes vehicles to emmit cold emissions for the first 2000m of a trip remove a vehicle from
			// the list of emitting vehicles if the current trajectory is longer than 2000m
			if (distance > 2000) {
				vehiclesEmittingColdEmissions.remove(vehicle.getId());
				return emissionCalculator.getEmptyColdEmissions();
			}

			// HBEFA assumes a constantly decreasing amount of cold emissions depending on the distance travelled
			// the underlying emission module simplifies this into two steps. Between 0-1km and 1-2km. We use the same
			// classes here because we don't want to rewrite all the stuff. The cold emission module computes emissions
			// for 1000m. We take these as is and multiply with distanceToLastPosition / 1000. This way we have the fraction
			// of cold emissions for the distance travelled during the last time step janek oct' 2021
			int distanceClass = distance <= 1000 ? 1 : 2;

			var coldEmissionsFor1km = emissionCalculator.calculateColdEmissions(
					vehicle, parkingDurations.get(vehicle.getId()), distanceClass
			);
			return coldEmissionsFor1km.entrySet().stream()
					.collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue() * distanceToLastPosition / 1000));
		}

		private double calculateTravelledDistance(PositionEvent event) {
			// check for cold emissions which should be computed once if distance is > 1000m
			double distance = 0;
			Coord previousCoord = null;
			for (var position : trajectories.get(event.getVehicleId())) {

				if (previousCoord != null) {
					distance += CoordUtils.calcEuclideanDistance(previousCoord, position.getCoord());
				}
				previousCoord = position.getCoord();
			}
			assert previousCoord != null;
			distance += CoordUtils.calcEuclideanDistance(previousCoord, event.getCoord());
			return distance;
		}

		private void computeCombinedEmissionEvent(PositionEvent event) {

			var previousPosition = trajectories.get(event.getVehicleId()).getLast();
			var distanceToLastPosition = CoordUtils.calcEuclideanDistance(event.getCoord(), previousPosition.getCoord());

			if (distanceToLastPosition > 0) {

				var link = network.getLinks().get(event.getLinkId());
				var travelTime = event.getTime() - previousPosition.getTime();
				var speed = distanceToLastPosition / travelTime;

				// don't go faster than light (freespeed)
				// add a rounding error to the compared freespeed. The warm emission module has a tolerance of 1km/h
				if (speed <= link.getFreespeed() + 0.01) {
					var vehicle = getVehicle(event.getVehicleId());

					var coldEmissions = computeColdEmissions(event, vehicle, distanceToLastPosition);
					var warmEmissions = emissionCalculator.calculateWarmEmissions(vehicle, link, distanceToLastPosition, travelTime);
					var combinedEmissions = Stream.concat(coldEmissions.entrySet().stream(), warmEmissions.entrySet().stream())
							.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Double::sum));

					eventsManager.processEvent(new PositionEmissionEvent(event, combinedEmissions, "combined"));

				} else {
					log.warn("speed was too fast: {}m/s Current time: {} prev time: {} current linkId: {} prev linkId: {} agentId: {}", speed, event.getTime(), previousPosition.getTime(), event.getLinkId(), previousPosition.getLinkId(), event.getPersonId());
				}
			} else {
				// if the vehicle hasn't moved, issue an event with 0 emissions. This way there is an event for every timestep
				// we want that for the palm integration. The more appropriate fix would be to not issue an event here
				// but generate the 0 emission positions in the necdf module in the palm project but this is much easier
				// and will do for now
				// janek july'21
				var emissions = emissionCalculator.getEmptyWarmEmissions();
				eventsManager.processEvent(new PositionEmissionEvent(event, emissions, "warm"));
			}
		}

		private Vehicle getVehicle(Id<Vehicle> id) {

			var vehicle = vehicles.getVehicles().get(id);
			if (vehicle == null) {
				vehicle = transitVehicles.getVehicles().get(id);
			}

			return vehicle;
		}
	}

	public static class PositionEmissionEvent extends Event {

		public static final String EVENT_TYPE = "positionEmission";

		private final PositionEvent position;
		private final Map<Pollutant, Double> emissions;
		private final String emissionType;

		public Map<Pollutant, Double> getEmissions() {
			return emissions;
		}

		public PositionEmissionEvent(PositionEvent positionEvent, Map<Pollutant, Double> emissions, String emissionType) {
			super(positionEvent.getTime());
			this.position = positionEvent;
			this.emissions = emissions;
			this.emissionType = emissionType;
		}

		public Id<Link> getLinkId() {
			return position.getLinkId();
		}

		public Id<Vehicle> getVehicleId() {
			return position.getVehicleId();
		}

		public Id<Person> getPersonId() {
			return position.getPersonId();
		}

		public String getEmissionType() {
			return emissionType;
		}

		public Coord getCoord() {
			return position.getCoord();
		}


		@Override
		public Map<String, String> getAttributes() {

			// call super second, so that the event type get overridden
			var attr = position.getAttributes();
			attr.putAll(super.getAttributes());
			attr.put("emissionType", emissionType);

			for (var pollutant : emissions.entrySet()) {
				attr.put(pollutant.getKey().toString(), pollutant.getValue().toString());
			}
			return attr;
		}

		@Override
		public String getEventType() {
			return EVENT_TYPE;
		}

		public static MatsimEventsReader.CustomEventMapper getEventMapper() {
			return event -> {
				var position = new PositionInfo.DirectBuilder()
						.setAgentState(AgentSnapshotInfo.AgentState.valueOf(event.getAttributes().get("state")))
						.setEasting(Double.parseDouble(event.getAttributes().get(Event.ATTRIBUTE_X)))
						.setNorthing(Double.parseDouble(event.getAttributes().get(Event.ATTRIBUTE_Y)))
						.setPersonId(Id.createPersonId(event.getAttributes().get(HasPersonId.ATTRIBUTE_PERSON)))
						.setLinkId(Id.createLinkId(event.getAttributes().get("linkId")))
						.setVehicleId(Id.createVehicleId(event.getAttributes().get("vehicleId")))
						.build();

				var positionEvent = new PositionEvent(event.getTime(), position);
				var emissions = Arrays.stream(Pollutant.values())
						.filter(pollutant -> event.getAttributes().containsKey(pollutant.toString()))
						.map(pollutant -> Tuple.of(pollutant, Double.parseDouble(event.getAttributes().get(pollutant.toString()))))
						.collect(Collectors.toMap(Tuple::getFirst, Tuple::getSecond));
				var type = event.getAttributes().get("emissionType");

				return new PositionEmissionEvent(positionEvent, emissions, type);
			};
		}
	}
}
