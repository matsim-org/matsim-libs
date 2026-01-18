/*
 *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       :  (C) 2024 by the members listed in the COPYING,       *
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
 * ***********************************************************************
 */

package org.matsim.freight.logistics.examples.initialPlans;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.Tour;
import org.matsim.freight.carriers.controller.CarrierScoringFunctionFactory;
import org.matsim.freight.carriers.events.CarrierTourEndEvent;
import org.matsim.freight.carriers.events.CarrierTourStartEvent;
import org.matsim.freight.logistics.analysis.Vehicle2CarrierEventHandler;
import org.matsim.freight.logistics.examples.ExampleConstants;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.*;

/**
 * A carrier scoring function based on events.
 * Please note: The fixed costs are reduced, of service based carriers are driving severeal tours. (Workaround for service-based carriers)
 * <p>
 * Todo: Score toll based on personMoney events
 *
 * @author Kai Martins-Turner (kturner)
 */
class MyEventBasedCarrierScorer implements CarrierScoringFunctionFactory, IterationEndsListener {

	@Inject
	private Network network;

	@Inject
	private Scenario scenario;

	@Inject
	private EventsManager eventsManager;

	private double toll;

	private final Map<Id<Carrier>, Collection<BasicEventHandler>> scoringFunctions = new HashMap<>();

	public ScoringFunction createScoringFunction(Carrier carrier) {

		if (scoringFunctions.containsKey(carrier.getId())) {
			var functions = scoringFunctions.get(carrier.getId());
			var sf = new SumScoringFunction();
			for (var function : functions) {
				sf.addScoringFunction((SumScoringFunction.BasicScoring) function);
			}
			return sf;
		}

		SumScoringFunction sf = new SumScoringFunction();
		sf.addScoringFunction(new EventBasedScoring(carrier.getId()));
		sf.addScoringFunction(new LinkBasedTollScoring(toll, List.of("large50"), carrier.getId()));

		var ebs = new EventBasedScoring(carrier.getId());
		var lbts = new LinkBasedTollScoring(toll, List.of("large50"), carrier.getId());
		var listeners = scoringFunctions.computeIfAbsent(carrier.getId(), k -> new ArrayList<>());
		listeners.add(ebs);
		listeners.add(lbts);
		sf.addScoringFunction(ebs);
		sf.addScoringFunction(lbts);
		eventsManager.addHandler(ebs);
		eventsManager.addHandler(lbts);

		return sf;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		for (var handlers : scoringFunctions.values()) {
			for (var handler : handlers) {
				eventsManager.removeHandler(handler);
			}
		}
		scoringFunctions.clear();
	}

	void setToll(double toll) {
		this.toll = toll;
	}

	/**
	 * Calculate the carrier's score based on Events. Currently, it includes: - fixed costs (using
	 * CarrierTourEndEvent) - time-dependent costs (using FreightTourStart- and -EndEvent) -
	 * distance-dependent costs (using LinkEnterEvent)
	 */
	private class EventBasedScoring implements BasicEventHandler, SumScoringFunction.BasicScoring {

		final Logger log = LogManager.getLogger(EventBasedScoring.class);
		private final Map<VehicleType, Double> vehicleType2TourDuration = new LinkedHashMap<>();
		private final Map<VehicleType, Integer> vehicleType2ScoredFixCosts = new LinkedHashMap<>();
		private final Vehicle2CarrierEventHandler v2c = new Vehicle2CarrierEventHandler();
		private final Map<Id<Tour>, Double> tourStartTime = new LinkedHashMap<>();
		private final Id<Carrier> carrierId;

		private double score;


		public EventBasedScoring(Id<Carrier> carrierId) {
			this.carrierId = carrierId;
		}

		@Override
		public void finish() {
		}

		@Override
		public double getScore() {
			return score;
		}

		@Override
		public void handleEvent(Event event) {
			log.debug(event.toString());
			switch (event) {
				case CarrierTourStartEvent freightTourStartEvent -> handleEvent(freightTourStartEvent);
				case CarrierTourEndEvent freightTourEndEvent -> handleEvent(freightTourEndEvent);
				case LinkEnterEvent linkEnterEvent -> handleEvent(linkEnterEvent);
				default -> {
				}
			}
		}

		private void handleEvent(CarrierTourStartEvent event) {
			if (!carrierId.equals(event.getCarrierId())) return;
			v2c.handleEvent(event);
			// Save time of freight tour start
			tourStartTime.put(event.getTourId(), event.getTime());
		}

		// Fix costs for vehicle usage
		//Todo: reduce this for a fixed-based scoring for each verhicle used, once shipment-based carriers are implemented for all carrierTypes
		//maybe do it stepwise: if (carrierType=distributionCarrier): score each vehicle, else use current workaround
		// Reason: distCarrier is the first, I am implementing the shipment-based approach.
		private void handleEvent(CarrierTourEndEvent event) {
			if (!carrierId.equals(event.getCarrierId())) return;

			v2c.handleEvent(event);

			// Fix costs for vehicle usage
			final VehicleType vehicleType =
				(VehicleUtils.findVehicle(event.getVehicleId(), scenario)).getType();

			double tourDuration = event.getTime() - tourStartTime.get(event.getTourId());
			{ // limit fixed costs of vehicles if vehicles could be reused during shift
				double MAX_SHIFT_DURATION = 8 * 3600;
				if (tourDuration > MAX_SHIFT_DURATION) {
					log.fatal("Duration of tour is longer than max shift defined in scoring fct, caused by event: {}, tourDuration: {}, max shift duration: {}", event, tourDuration, MAX_SHIFT_DURATION, new RuntimeException());
				}

				// sum up tour durations
				if (vehicleType2TourDuration.containsKey(vehicleType)) {
					vehicleType2TourDuration.put(vehicleType, vehicleType2TourDuration.get(vehicleType) + tourDuration);
				} else {
					vehicleType2TourDuration.put(vehicleType, tourDuration);
				}

				// scoring needed? (This is a workaround to reduce number of fix costs for vehicles in scoring, because currently there is no possibility to run vehicles for several tours.)
				final double currentNuOfVehiclesNeeded = Math.ceil(vehicleType2TourDuration.get(vehicleType) / MAX_SHIFT_DURATION);
				//Rechnet mit wie oft schon Fixkosten eingerechnet wurden in Scoring. Solange der Wert kleiner ist als nuOfVehiclesNeeded, werden Fixkosten abgezogen.
				vehicleType2ScoredFixCosts.compute(vehicleType, (key, nuAlreadyScored) -> {
					if (nuAlreadyScored == null || currentNuOfVehiclesNeeded > nuAlreadyScored) {
						log.info("Score fixed costs for vehicle type: {}", vehicleType.getId().toString());
						score -= vehicleType.getCostInformation().getFixedCosts();
						return (nuAlreadyScored == null) ? 1 : nuAlreadyScored + 1;
					}
					return nuAlreadyScored;
				});
			}

			// variable costs per time
			score = score - (tourDuration * vehicleType.getCostInformation().getCostsPerSecond());
		}

		private void handleEvent(LinkEnterEvent event) {
			if (!carrierId.equals(v2c.getCarrierOfVehicle(event.getVehicleId()))) return;
			final double distance = network.getLinks().get(event.getLinkId()).getLength();
			final double costPerMeter = (VehicleUtils.findVehicle(event.getVehicleId(), scenario)).getType().getCostInformation().getCostsPerMeter();
			// variable costs per distance
			score = score - (distance * costPerMeter);
		}
	}

	/**
	 * Calculate some toll for driving on a link This a lazy implementation of a cordon toll. A
	 * vehicle is only tolled once.
	 */
	class LinkBasedTollScoring implements BasicEventHandler, SumScoringFunction.BasicScoring {

		final Logger log = LogManager.getLogger(LinkBasedTollScoring.class);

		private final double toll;
		private final List<String> vehicleTypesToBeTolled;
		private final List<Id<Vehicle>> tolledVehicles = new ArrayList<>();
		private final Vehicle2CarrierEventHandler v2c = new Vehicle2CarrierEventHandler();
		private final Id<Carrier> carrierId;

		private double score;

		public LinkBasedTollScoring(double toll, List<String> vehicleTypesToBeTolled, Id<Carrier> carrierId) {
			this.toll = toll;
			this.vehicleTypesToBeTolled = vehicleTypesToBeTolled;
			this.carrierId = carrierId;
		}

		@Override
		public void finish() {
		}

		@Override
		public double getScore() {
			return score;
		}

		@Override
		public void handleEvent(Event event) {

			switch (event) {
				case CarrierTourStartEvent freightTourStartEvent -> handleEvent(freightTourStartEvent);
				case CarrierTourEndEvent freightTourEndEvent -> handleEvent(freightTourEndEvent);
				case LinkEnterEvent linkEnterEvent -> handleEvent(linkEnterEvent);
				default -> {
				}
			}
		}

		private void handleEvent(CarrierTourStartEvent event) {
			if (!carrierId.equals(event.getCarrierId())) return;
			v2c.handleEvent(event);
		}

		private void handleEvent(CarrierTourEndEvent event) {
			if (!carrierId.equals(event.getCarrierId())) return;
			v2c.handleEvent(event);
		}

		private void handleEvent(LinkEnterEvent event) {
			if (!carrierId.equals(v2c.getCarrierOfVehicle(event.getVehicleId()))) return;

			List<String> tolledLinkList = ExampleConstants.TOLLED_LINK_LIST_GRID;

			final Id<VehicleType> vehicleTypeId =
				(VehicleUtils.findVehicle(event.getVehicleId(), scenario)).getType().getId();

			// toll a vehicle only once.
			if (!tolledVehicles.contains(event.getVehicleId()))
				if (vehicleTypesToBeTolled.contains(vehicleTypeId.toString())) {
					if (tolledLinkList.contains(event.getLinkId().toString())) {
						log.info("Tolling caused by event: {}", event);
						tolledVehicles.add(event.getVehicleId());
						score = score - toll;
					}
				}
		}
	}
}
