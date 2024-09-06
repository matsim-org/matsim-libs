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

import ch.sbb.matsim.routing.pt.raptor.OccupancyData.DepartureData;
import ch.sbb.matsim.routing.pt.raptor.OccupancyData.LineData;
import ch.sbb.matsim.routing.pt.raptor.OccupancyData.PassengerData;
import ch.sbb.matsim.routing.pt.raptor.OccupancyData.RouteData;
import ch.sbb.matsim.routing.pt.raptor.OccupancyData.StopData;
import ch.sbb.matsim.routing.pt.raptor.OccupancyData.VehicleData;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import ch.sbb.matsim.routing.pt.raptor.RaptorInVehicleCostCalculator.RouteSegmentIterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.AgentWaitingForPtEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.scoring.functions.ModeUtilityParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import jakarta.inject.Inject;

/**
 * Collects various data related to public transport during the execution of the plans (=during mobsim).
 * For example, for each departure of a transit vehicle at a stop the latest time an agent can arrive
 * to catch a departure is recorder, as is the vehicle load upon departure at a stop.
 *
 * @author mrieser / Simunto GmbH
 */
public class OccupancyTracker implements PersonDepartureEventHandler, AgentWaitingForPtEventHandler, TransitDriverStartsEventHandler, VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {

	private final static Logger LOG = LogManager.getLogger(OccupancyTracker.class);

	private final OccupancyData data;
	private final Scenario scenario;
	private final Set<Id<Person>> transitDrivers = new HashSet<>();
	private final RaptorInVehicleCostCalculator inVehCostCalculator;
	private final EventsManager events;
	private final boolean useInVehCosts;

	private final static VehicleData DUMMY_VEHDATA = new VehicleData(null, null, null, null);
	private final ScoringParametersForPerson scoringParams;

	@Inject
	public OccupancyTracker(OccupancyData data, Scenario scenario, RaptorInVehicleCostCalculator inVehCostCalculator, EventsManager events, ScoringParametersForPerson scoringParams) {
		this.data = data;
		this.scenario = scenario;
		this.inVehCostCalculator = inVehCostCalculator;
		// the default in vehicle cost calculator can be ignored
		this.useInVehCosts = !inVehCostCalculator.getClass().equals(DefaultRaptorInVehicleCostCalculator.class);
		this.events = events;
		this.scoringParams = scoringParams;
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		PassengerData pd = this.data.paxData.computeIfAbsent(event.getPersonId(), k -> {
			Person person = this.scenario.getPopulation().getPersons().get(k);
			Map<String, ModeUtilityParameters> modeParams = this.scoringParams.getScoringParameters(person).modeParams;
			return new PassengerData(person, modeParams);
		});
		pd.mode = event.getLegMode();
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		this.transitDrivers.add(event.getDriverId());
		// store information about the current service of the transit vehicle
		Vehicle vehicle = this.scenario.getTransitVehicles().getVehicles().get(event.getVehicleId());
		this.data.vehicleData.put(event.getVehicleId(), new VehicleData(vehicle, event.getTransitLineId(), event.getTransitRouteId(), event.getDepartureId()));
		LineData line = this.data.lineData.computeIfAbsent(event.getTransitLineId(), id -> new LineData());
		RouteData route = line.routeData.computeIfAbsent(event.getTransitRouteId(), id -> new RouteData(
				this.scenario.getTransitSchedule().getTransitLines().get(event.getTransitLineId()).getRoutes().get(event.getTransitRouteId())
		));
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
			StopData stop = route.stopData.computeIfAbsent(vehData.stopFacilityId, id -> new StopData());
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
			StopData stop = route.stopData.computeIfAbsent(vehData.stopFacilityId, id -> new StopData());
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

	@Override
	public void reset(int iteration) {
		this.data.reset();
		this.transitDrivers.clear();
	}

	private void calculateInVehicleCosts(double time, PassengerData passengerData, VehicleData vehData) {
		Person person = passengerData.person;

		double depTime = this.data.getRouteData(vehData.lineId, vehData.routeId).stopData.get(passengerData.boardingStopId).depData.get(passengerData.departureId).vehDepTime;
//		double depTime = passengerData.vehBoardingTime;
		double arrTime = time;
		double travelTime = arrTime - depTime;

		ModeUtilityParameters modeParams = passengerData.modeParams.get(passengerData.mode);
		double margUtil_s = modeParams.marginalUtilityOfTraveling_s;
		double defaultScore = travelTime * margUtil_s;

		EffectiveRouteSegmentIterator iter = new EffectiveRouteSegmentIterator(this.data, passengerData, time, vehData);
		double capacityScore = -this.inVehCostCalculator.getInVehicleCost(travelTime, margUtil_s, person, vehData.vehicle, null, iter);

		double scoreDiff = capacityScore - defaultScore;
		if (Double.isNaN(scoreDiff)) {
			LOG.warn("Getting NaN as score: " + passengerData.person.getId() + " " + vehData.lineId + " " + vehData.routeId + " " + Time.writeTime(time) + " " + depTime + " " + travelTime + " " + defaultScore + " " + capacityScore);
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

}
