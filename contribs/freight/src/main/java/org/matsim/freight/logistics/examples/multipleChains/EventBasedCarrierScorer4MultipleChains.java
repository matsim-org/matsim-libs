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

package org.matsim.freight.logistics.examples.multipleChains;

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
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.*;

/**
 * @author Kai Martins-Turner (kturner)
 * <p>
 * I think the design of this class if fundamentally broken. Please reconcider if you want to use it. janek Jan' 26
 *
 *
 * @deprecated Should be replaced by {@link org.matsim.freight.carriers.usecases.CarrierScorerEventBasedInclToll}. This will need a correct implementation of tolling in the coresponding classes.
 * The very lazy implementation of tolling in this class is not sufficient. And now seems to cause the issues observed in the tests with the different scores.
 */
@Deprecated
class EventBasedCarrierScorer4MultipleChains implements CarrierScoringFunctionFactory, IterationEndsListener {

	// I think these are never used, as the code instantiating this class binds to an instance constructed with no constructor arguments...
	@Inject
	private Network network;
	@Inject
	private Scenario scenario;
	@Inject
	private EventsManager eventsManager;

	private double toll;
	private List<String> tolledVehicleTypes = new ArrayList<>();
	private List<String> tolledLinks = new ArrayList<>();
	private final Map<Id<Carrier>, Collection<BasicEventHandler>> scoringFunctions = new HashMap<>();

	public ScoringFunction createScoringFunction(Carrier carrier) {
		// I don't understand why this factory saves the carrier id. My understanding is that it should create new scoring functions
		// for each carrier it receives.
		if (scoringFunctions.containsKey(carrier.getId())) {
			var functions = scoringFunctions.get(carrier.getId());
			var sf = new SumScoringFunction();
			for (var f : functions) {
				sf.addScoringFunction((SumScoringFunction.BasicScoring) f);
			}
			return sf;
		}

		SumScoringFunction sf = new SumScoringFunction();

		var ebs = new EventBasedScoring(carrier.getId(), network, scenario);
		var lbts = new LinkBasedTollScoring(toll, tolledVehicleTypes, tolledLinks, carrier.getId(), scenario);
		var listeners = scoringFunctions.computeIfAbsent(carrier.getId(), k -> new ArrayList<>());
		listeners.add(ebs);
		listeners.add(lbts);
		sf.addScoringFunction(ebs);
		sf.addScoringFunction(lbts);
		eventsManager.addHandler(ebs);
		eventsManager.addHandler(lbts);
		return sf;
	}

	void setToll(double toll) {
		this.toll = toll;
	}

	void setTolledVehicleTypes(List<String> tolledVehicleTypes) {
		this.tolledVehicleTypes = tolledVehicleTypes;
	}

	void setTolledLinks(List<String> tolledLinks) {
		this.tolledLinks = tolledLinks;
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

	/**
	 * Calculate the carrier's score based on Events. Currently, it includes: - fixed costs (using
	 * CarrierTourEndEvent) - time-dependent costs (using FreightTourStart- and -EndEvent) -
	 * distance-dependent costs (using LinkEnterEvent)
	 * <p>
	 * With the current design, there is one factory for one scoring function, as the inner class is non static.
	 * This is probably not what one wants to do.
	 */
	private static class EventBasedScoring implements SumScoringFunction.BasicScoring, BasicEventHandler {

		final Logger log = LogManager.getLogger(EventBasedScoring.class);
		private final Map<Id<Tour>, Double> tourStartTime = new LinkedHashMap<>();
		private final Vehicle2CarrierEventHandler v2c = new Vehicle2CarrierEventHandler();
		private final Id<Carrier> carrierId;
		private final Network network;
		private final Scenario scenario;

		private double score;

		public EventBasedScoring(Id<Carrier> carrierId, Network network, Scenario scenario) {
			this.carrierId = carrierId;
			this.network = network;
			this.scenario = scenario;
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
				case CarrierTourStartEvent carrierTourStartEvent -> handleEvent(carrierTourStartEvent);
				case CarrierTourEndEvent carrierTourEndEvent -> handleEvent(carrierTourEndEvent);
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

		// scores fix costs for vehicle usage and variable costs per time
		private void handleEvent(CarrierTourEndEvent event) {
			if (!carrierId.equals(event.getCarrierId())) return;

			v2c.handleEvent(event);
			// Fix costs for vehicle usage
			final VehicleType vehicleType =
				(VehicleUtils.findVehicle(event.getVehicleId(), scenario)).getType();

			double tourDuration = event.getTime() - tourStartTime.get(event.getTourId());

			//log.info("Score fixed costs for vehicle type: {}", vehicleType.getId().toString());
			score = score - vehicleType.getCostInformation().getFixedCosts();

			// variable costs per time
			score = score - (tourDuration * vehicleType.getCostInformation().getCostsPerSecond());
		}

		// scores variable costs per distance
		private void handleEvent(LinkEnterEvent event) {
			if (!carrierId.equals(v2c.getCarrierOfVehicle(event.getVehicleId()))) return;

			final double distance = network.getLinks().get(event.getLinkId()).getLength();
			final double costPerMeter =
				(VehicleUtils.findVehicle(event.getVehicleId(), scenario))
					.getType()
					.getCostInformation()
					.getCostsPerMeter();
			// variable costs per distance
			score = score - (distance * costPerMeter);
		}
	}

	/**
	 * Calculate some toll for driving on a link This a lazy implementation of a cordon toll. A
	 * vehicle is only tolled once.
	 */
	static class LinkBasedTollScoring implements SumScoringFunction.BasicScoring, BasicEventHandler {

		final Logger log = LogManager.getLogger(LinkBasedTollScoring.class);

		private final double toll;
		private final List<String> vehicleTypesToBeTolled;
		private double score;
		private final List<String> tolledLinkList;
		private final Vehicle2CarrierEventHandler v2c = new Vehicle2CarrierEventHandler();
		private final Id<Carrier> carrierId;
		private final Scenario scenario;

		public LinkBasedTollScoring(double toll, List<String> vehicleTypeToBeTolled, List<String> tolledLinkListBerlin, Id<Carrier> carrierId, Scenario scenario) {
			this.vehicleTypesToBeTolled = vehicleTypeToBeTolled;
			this.tolledLinkList = tolledLinkListBerlin;
			this.toll = toll;
			this.carrierId = carrierId;
			this.scenario = scenario;
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
				case LinkEnterEvent linkEnterEvent -> handleEvent(linkEnterEvent);
				case CarrierTourStartEvent carrierTourStartEvent -> v2c.handleEvent(carrierTourStartEvent);
				case CarrierTourEndEvent carrierTourEndEvent -> v2c.handleEvent(carrierTourEndEvent);
				default -> {
				}
			}

		}

		private void handleEvent(LinkEnterEvent event) {
			if (!carrierId.equals(v2c.getCarrierOfVehicle(event.getVehicleId()))) return;

			final Id<VehicleType> vehicleTypeId = (VehicleUtils.findVehicle(event.getVehicleId(), scenario)).getType().getId();

			Id<Vehicle> vehicleId = event.getVehicleId();
			if (vehicleTypesToBeTolled.contains(vehicleTypeId.toString())) {
				if (tolledLinkList.contains(event.getLinkId().toString())) {
					Id<Carrier> carrierIdOfVehicle = v2c.getCarrierOfVehicle(vehicleId);
					if (carrierId.equals(carrierIdOfVehicle)) {
						log.info("Tolling caused by event: {}, tollvalue {}", event, toll);
						score = score - toll;
					}
				}
			}
		}
	}
}
