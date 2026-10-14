/* *********************************************************************** *
 * project: org.matsim.* 												   *
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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
package ch.sbb.matsim.routing.pt.raptor;

import ch.sbb.matsim.routing.pt.raptor.OccupancyData.*;
import ch.sbb.matsim.routing.pt.raptor.RaptorInVehicleCostCalculator.RouteSegmentIterator;
import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.MobsimMessageCollector;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.messages.ComputeNode;
import org.matsim.api.core.v01.network.NetworkPartition;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.AgentWaitingForPtEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.events.MobsimScopeEventHandler;
import org.matsim.core.mobsim.dsim.DSimComponentsMessageProcessor;
import org.matsim.core.mobsim.dsim.DistributedMobsimVehicle;
import org.matsim.core.mobsim.dsim.NotifyVehiclePartitionTransfer;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.qsim.NoopMessageCollector;
import org.matsim.core.mobsim.qsim.components.QSimComponent;
import org.matsim.core.scoring.functions.ModeUtilityParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.Vehicle;

import java.util.*;

/**
 * Collects various data related to public transport during the execution of the plans (=during mobsim). For example, for each departure of a transit
 * vehicle at a stop the latest time an agent can arrive to catch a departure is recorder, as is the vehicle load upon departure at a stop.
 * <p>
 * The tracker collects into its own data and publishes it to the shared {@link OccupancyData} used by the router after the mobsim. In a distributed
 * simulation, there is one tracker per partition. The state of transit vehicles and their passengers moves with the vehicle when it changes
 * partitions, and the trackers exchange their data at the end of the mobsim, so that each tracker publishes the complete data.
 *
 * @author mrieser / Simunto GmbH
 */
@DistributedEventHandler(value = DistributedMode.PARTITION, processing = ProcessingMode.DIRECT)
public class OccupancyTracker implements PersonDepartureEventHandler, AgentWaitingForPtEventHandler, TransitDriverStartsEventHandler,
	VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler,
	AggregatingEventHandler<OccupancyTracker.OccupancySyncMessage>, MobsimScopeEventHandler, NotifyVehiclePartitionTransfer,
	DSimComponentsMessageProcessor, QSimComponent {

	public static final String COMPONENT_NAME = "OccupancyTracker";

	private final static Logger LOG = LogManager.getLogger(OccupancyTracker.class);

	private final OccupancyData data = new OccupancyData();
	private final OccupancyData sharedData;
	private final Scenario scenario;
	private final Set<Id<Person>> transitDrivers = new HashSet<>();
	private final RaptorInVehicleCostCalculator inVehCostCalculator;
	private final EventsManager events;
	private final boolean useInVehCosts;

	private final static VehicleData DUMMY_VEHDATA = new VehicleData(null, null, null, null);
	private final ScoringParametersForPerson scoringParams;
	private final NetworkPartition partition;
	private final ComputeNode computeNode;
	private final MobsimMessageCollector messageCollector;

	/**
	 * Creates a tracker for a simulation with a single partition, which publishes to the given occupancy data.
	 */
	public OccupancyTracker(OccupancyData sharedData, Scenario scenario, RaptorInVehicleCostCalculator inVehCostCalculator, EventsManager events,
		ScoringParametersForPerson scoringParams) {
		this(sharedData, scenario, inVehCostCalculator, events, scoringParams, NetworkPartition.SINGLE_INSTANCE, ComputeNode.SINGLE_INSTANCE,
			new NoopMessageCollector());
	}

	/**
	 * Creates a tracker for the given partition, which publishes to the given occupancy data.
	 */
	@Inject
	public OccupancyTracker(OccupancyData sharedData, Scenario scenario, RaptorInVehicleCostCalculator inVehCostCalculator, EventsManager events,
		ScoringParametersForPerson scoringParams, NetworkPartition partition, ComputeNode computeNode, MobsimMessageCollector messageCollector) {
		this.sharedData = sharedData;
		this.scenario = scenario;
		this.inVehCostCalculator = inVehCostCalculator;
		// the default in vehicle cost calculator can be ignored
		this.useInVehCosts = !inVehCostCalculator.getClass().equals(DefaultRaptorInVehicleCostCalculator.class);
		this.events = events;
		this.scoringParams = scoringParams;
		this.partition = partition;
		this.computeNode = computeNode;
		this.messageCollector = messageCollector;
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		getOrCreatePassengerData(event.getPersonId()).mode = event.getLegMode();
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		this.transitDrivers.add(event.getDriverId());
		// store information about the current service of the transit vehicle
		Vehicle vehicle = this.scenario.getTransitVehicles().getVehicles().get(event.getVehicleId());
		this.data.vehicleData.put(event.getVehicleId(),
			new VehicleData(vehicle, event.getTransitLineId(), event.getTransitRouteId(), event.getDepartureId()));
		getOrCreateRouteData(event.getTransitLineId(), event.getTransitRouteId());
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		// store at what stop the transit vehicle currently is
		this.data.vehicleData.getOrDefault(event.getVehicleId(), DUMMY_VEHDATA).stopFacilityId = event.getFacilityId();
	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		// if nobody entered, store the departure time as latest time
		VehicleData vehData = this.data.vehicleData.get(event.getVehicleId());
		if (vehData != null) {
			LineData line = this.data.lineData.get(vehData.lineId);
			RouteData route = line.routeData.get(vehData.routeId);
			StopData stop = route.stopData.computeIfAbsent(vehData.stopFacilityId, _ -> new StopData());
			DepartureData dep = stop.getOrCreate(vehData.departureId);
			dep.vehDepTime = event.getTime();
			dep.paxCountAtDeparture = vehData.currentPaxCount;
		}
	}

	@Override
	public void handleEvent(AgentWaitingForPtEvent event) {
		PassengerData pd = this.data.paxData.get(event.getPersonId());
		pd.waitingStartTime = event.getTime();
		pd.boardingStopId = event.waitingAtStopId;
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		VehicleData vehData = this.data.vehicleData.get(event.getVehicleId());
		if (vehData != null && !this.transitDrivers.contains(event.getPersonId())) {
			vehData.currentPaxCount++;
			PassengerData passengerData = this.data.paxData.get(event.getPersonId());
			double waitStart = passengerData.waitingStartTime;
			LineData line = this.data.lineData.get(vehData.lineId);
			RouteData route = line.routeData.get(vehData.routeId);
			StopData stop = route.stopData.computeIfAbsent(vehData.stopFacilityId, _ -> new StopData());
			stop.getOrCreate(vehData.departureId).addWaitingPerson(waitStart);
			passengerData.vehBoardingTime = event.getTime();
			passengerData.departureId = vehData.departureId;
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		VehicleData vehData = this.data.vehicleData.get(event.getVehicleId());
		if (vehData != null && !this.transitDrivers.contains(event.getPersonId())) {
			vehData.currentPaxCount--;
			if (this.useInVehCosts) {
				calculateInVehicleCosts(event.getTime(), this.data.paxData.get(event.getPersonId()), vehData);
			}
		}
	}

	/**
	 * Sends the state of a transit vehicle, its passengers and the data of the stops the vehicle has already served to the partition the vehicle
	 * moves to. The stop data is needed to compute the in-vehicle costs of the passengers when they alight.
	 */
	@Override
	public void onVehicleLeavesPartition(DistributedMobsimVehicle vehicle, int toPartition) {
		VehicleData vehData = this.data.vehicleData.remove(vehicle.getId());
		if (vehData == null) {
			return;
		}

		List<PassengerEntry> passengers = new ArrayList<>();
		for (PassengerAgent passenger : vehicle.getPassengers()) {
			PassengerData pd = this.data.paxData.remove(passenger.getId());
			if (pd != null) {
				passengers.add(
					new PassengerEntry(passenger.getId(), pd.mode, pd.waitingStartTime, pd.vehBoardingTime, pd.boardingStopId, pd.departureId));
			}
		}

		List<DepartureEntry> servedStops = new ArrayList<>();
		RouteData route = this.data.getRouteData(vehData.lineId, vehData.routeId);
		for (Map.Entry<Id<TransitStopFacility>, StopData> e : route.stopData.entrySet()) {
			DepartureData dep = e.getValue().depData.get(vehData.departureId);
			if (dep != null) {
				servedStops.add(DepartureEntry.of(vehData.lineId, vehData.routeId, e.getKey(), dep));
			}
		}

		this.messageCollector.collect(new VehicleTransferMessage(vehicle.getId(), vehicle.getDriver().getId(), vehData.lineId, vehData.routeId,
			vehData.departureId, vehData.currentPaxCount, passengers, servedStops), toPartition);
	}

	@Override
	public void onVehicleEntersPartition(DistributedMobsimVehicle vehicle) {
	}

	@Override
	public Map<Class<? extends Message>, MessageHandler> getMessageHandlers() {
		return Map.of(VehicleTransferMessage.class, this::handleVehicleTransferMessages);
	}

	private void handleVehicleTransferMessages(List<Message> messages, double now) {
		for (Message m : messages) {
			VehicleTransferMessage msg = (VehicleTransferMessage) m;
			Vehicle vehicle = this.scenario.getTransitVehicles().getVehicles().get(msg.vehicleId());
			VehicleData vehData = new VehicleData(vehicle, msg.lineId(), msg.routeId(), msg.departureId());
			vehData.currentPaxCount = msg.currentPaxCount();
			this.data.vehicleData.put(msg.vehicleId(), vehData);
			this.transitDrivers.add(msg.driverId());
			getOrCreateRouteData(msg.lineId(), msg.routeId());

			for (PassengerEntry p : msg.passengers()) {
				PassengerData pd = getOrCreatePassengerData(p.personId());
				pd.mode = p.mode();
				pd.waitingStartTime = p.waitingStartTime();
				pd.vehBoardingTime = p.vehBoardingTime();
				pd.boardingStopId = p.boardingStopId();
				pd.departureId = p.departureId();
			}
			msg.servedStops().forEach(this::merge);
		}
	}

	/**
	 * Returns all departure data collected by this tracker.
	 */
	@Override
	public OccupancySyncMessage send() {
		List<DepartureEntry> departures = new ArrayList<>();
		for (Map.Entry<Id<TransitLine>, LineData> line : this.data.lineData.entrySet()) {
			for (Map.Entry<Id<TransitRoute>, RouteData> route : line.getValue().routeData.entrySet()) {
				for (Map.Entry<Id<TransitStopFacility>, StopData> stop : route.getValue().stopData.entrySet()) {
					for (DepartureData dep : stop.getValue().depList) {
						departures.add(DepartureEntry.of(line.getKey(), route.getKey(), stop.getKey(), dep));
					}
				}
			}
		}
		return new OccupancySyncMessage(departures);
	}

	/**
	 * Adds the departure data of other trackers. Merging is idempotent, so receiving the own message is harmless.
	 */
	@Override
	public void receive(List<OccupancySyncMessage> messages) {
		for (OccupancySyncMessage msg : messages) {
			msg.departures().forEach(this::merge);
		}
	}

	/**
	 * Exchanges data only once, after the mobsim, as the router uses the data only in the next iteration.
	 */
	@Override
	public double getSyncInterval() {
		return Double.POSITIVE_INFINITY;
	}

	/**
	 * Publishes the collected data to the shared occupancy data. All trackers of a compute node hold the same data after the final exchange, so only
	 * the tracker of the node's first partition publishes.
	 */
	@Override
	public void cleanupAfterMobsim(int iteration) {
		if (this.partition.getIndex() == this.computeNode.getParts().getInt(0)) {
			this.sharedData.replaceLineData(this.data.lineData);
		}
	}

	private PassengerData getOrCreatePassengerData(Id<Person> personId) {
		return this.data.paxData.computeIfAbsent(personId, k -> {
			Person person = this.scenario.getPopulation().getPersons().get(k);
			Map<String, ModeUtilityParameters> modeParams = this.scoringParams.getScoringParameters(person).modeParams;
			return new PassengerData(person, modeParams);
		});
	}

	private RouteData getOrCreateRouteData(Id<TransitLine> lineId, Id<TransitRoute> routeId) {
		LineData line = this.data.lineData.computeIfAbsent(lineId, _ -> new LineData());
		return line.routeData.computeIfAbsent(routeId, _ -> new RouteData(
			this.scenario.getTransitSchedule().getTransitLines().get(lineId).getRoutes().get(routeId)
		));
	}

	/**
	 * Adds the data of a departure at a stop, unless this tracker has data of the same or a later visit of that stop. Routes may serve a stop more
	 * than once, and the data of the latest visit wins, as it does with a single tracker.
	 */
	private void merge(DepartureEntry entry) {
		StopData stop = getOrCreateRouteData(entry.lineId(), entry.routeId()).stopData.computeIfAbsent(entry.stopId(), _ -> new StopData());
		DepartureData existing = stop.depData.get(entry.departureId());
		if (existing != null && existing.vehDepTime >= entry.vehDepTime()) {
			return;
		}
		stop.sorted = false;
		DepartureData dep = stop.getOrCreate(entry.departureId());
		dep.vehDepTime = entry.vehDepTime();
		dep.paxCountAtDeparture = entry.paxCountAtDeparture();
		dep.earliestWaitStart = entry.earliestWaitStart();
		dep.latestWaitStart = entry.latestWaitStart();
	}

	private void calculateInVehicleCosts(double time, PassengerData passengerData, VehicleData vehData) {
		Person person = passengerData.person;

		double depTime = this.data.getRouteData(vehData.lineId, vehData.routeId).stopData.get(passengerData.boardingStopId).depData.get(
			passengerData.departureId).vehDepTime;
		double travelTime = time - depTime;

		ModeUtilityParameters modeParams = passengerData.modeParams.get(passengerData.mode);
		double margUtil_s = modeParams.marginalUtilityOfTraveling_s;
		double defaultScore = travelTime * margUtil_s;

		EffectiveRouteSegmentIterator iter = new EffectiveRouteSegmentIterator(this.data, passengerData, time, vehData);
		double capacityScore = -this.inVehCostCalculator.getInVehicleCost(travelTime, margUtil_s, person, vehData.vehicle, null, iter);

		double scoreDiff = capacityScore - defaultScore;
		if (Double.isNaN(scoreDiff)) {
			LOG.warn(
				"Getting NaN as score: " + passengerData.person.getId() + " " + vehData.lineId + " " + vehData.routeId + " " + Time.writeTime(time)
					+ " " + depTime + " " + travelTime + " " + defaultScore + " " + capacityScore);
		} else {
			this.events.processEvent(new PersonScoreEvent(time, person.getId(), scoreDiff, "pt-occupancy"));
		}
	}

	public static class EffectiveRouteSegmentIterator implements RouteSegmentIterator {

		final PassengerData passengerData;
		final VehicleData vehData;
		final RouteData routeData;
		final double alightingTime;
		DepartureData currentDepData = null;
		DepartureData nextFromStopDepData = null;
		DepartureData nextToStopDepData = null;
		Iterator<TransitRouteStop> stopIter;
		boolean hasNext = false;
		TransitRouteStop nextToStop = null;
		TransitRouteStop nextFromStop = null;

		public EffectiveRouteSegmentIterator(OccupancyData data, PassengerData passengerData, double alightingTime, VehicleData vehData) {
			this.passengerData = passengerData;
			this.vehData = vehData;
			this.alightingTime = alightingTime;
			this.routeData = data.getRouteData(vehData.lineId, vehData.routeId);
			TransitRoute ptRoute = this.routeData.route;
			this.stopIter = ptRoute.getStops().iterator();
			next();
		}

		@Override
		public boolean hasNext() {
			return this.hasNext;
		}

		@Override
		public void next() {
			this.currentDepData = this.nextFromStopDepData;
			boolean searchStart = !this.hasNext;
			this.hasNext = false;
			this.nextFromStopDepData = this.nextToStopDepData;
			Id<TransitStopFacility> boardingStopId = this.passengerData.boardingStopId;
			Id<TransitStopFacility> alightingStopId = this.vehData.stopFacilityId;
			while (this.stopIter.hasNext()) {
				this.nextToStop = this.stopIter.next();
				Id<TransitStopFacility> stopId = this.nextFromStop != null ? this.nextFromStop.getStopFacility().getId() : null;
				if (searchStart && this.nextFromStop != null && stopId.equals(boardingStopId)) {
					this.nextFromStopDepData = this.routeData.stopData.get(stopId).depData.get(this.passengerData.departureId);
					this.hasNext = true;
					break;
				}
				if (!searchStart) {
					this.hasNext = !this.nextFromStop.getStopFacility().getId().equals(alightingStopId);
					break;
				}
				this.nextFromStop = this.nextToStop;
			}
			if (this.hasNext) {
				StopData stopData = this.routeData.stopData.get(this.nextToStop.getStopFacility().getId());
				this.nextToStopDepData = stopData == null ? null : stopData.depData.get(this.passengerData.departureId);
				if (this.nextToStopDepData == null) {
					// this can happen if it is on the first service in the morning, and an agent leaves at this stop, where no vehicle yet has departed
					this.nextToStopDepData = new DepartureData(this.passengerData.departureId);
					this.nextToStopDepData.vehDepTime = this.alightingTime;
				}
				this.nextFromStop = this.nextToStop;
			}
		}

		@Override
		public double getInVehicleTime() {
			return this.nextFromStopDepData.vehDepTime - this.currentDepData.vehDepTime;
		}

		@Override
		public double getPassengerCount() {
			return this.currentDepData.paxCountAtDeparture;
		}

		@Override
		public double getTimeOfDay() {
			return this.currentDepData.vehDepTime;
		}
	}

	/**
	 * The data of one departure at one stop.
	 */
	public record DepartureEntry(Id<TransitLine> lineId, Id<TransitRoute> routeId, Id<TransitStopFacility> stopId, Id<Departure> departureId,
		double vehDepTime, int paxCountAtDeparture, double earliestWaitStart, double latestWaitStart) {

		static DepartureEntry of(Id<TransitLine> lineId, Id<TransitRoute> routeId, Id<TransitStopFacility> stopId, DepartureData dep) {
			return new DepartureEntry(lineId, routeId, stopId, dep.departureId, dep.vehDepTime, dep.paxCountAtDeparture,
				dep.earliestWaitStart, dep.latestWaitStart);
		}
	}

	/**
	 * The state of a passenger riding in a transit vehicle.
	 */
	public record PassengerEntry(Id<Person> personId, String mode, double waitingStartTime, double vehBoardingTime,
		Id<TransitStopFacility> boardingStopId, Id<Departure> departureId) {
	}

	/**
	 * The state of a transit vehicle and its passengers, sent when the vehicle moves to another partition.
	 */
	public record VehicleTransferMessage(Id<Vehicle> vehicleId, Id<Person> driverId, Id<TransitLine> lineId, Id<TransitRoute> routeId,
		Id<Departure> departureId, int currentPaxCount, List<PassengerEntry> passengers,
		List<DepartureEntry> servedStops) implements Message {
	}

	/**
	 * All departure data collected by one tracker, exchanged between trackers after the mobsim.
	 */
	public record OccupancySyncMessage(List<DepartureEntry> departures) implements Message {
	}

}
