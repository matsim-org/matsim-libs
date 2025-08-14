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
import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.*;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.SumScoringFunction.ArbitraryEventScoring;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.Tour;
import org.matsim.freight.carriers.controller.CarrierScoringFunctionFactory;
import org.matsim.freight.carriers.events.CarrierTourEndEvent;
import org.matsim.freight.carriers.events.CarrierTourStartEvent;
import org.matsim.freight.logistics.analysis.Driver2VehicleEventHandler;
import org.matsim.freight.logistics.analysis.Vehicle2CarrierEventHandler;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 * This is a scoring function for carriers that uses events to calculate the score.
 * This includes also tolls for vehicles driving on tolled links based on the person money event(s).
 * <p></p>
 * Currently, it includes: -
 *  - fixed costs (using CarrierTourEndEvent)
 *  - time-dependent costs (using FreightTourStart- and -EndEvent)
 *  - distance-dependent costs (using LinkEnterEvent)
 * 	- tolls (using PersonMoneyEvent)
 *
 * 	@todo Maybe move this up to the carrier package and make it public?
 * 	Can this be the new default scoring function for carriers?
 * 	Discuss with RE/KN and write tests around it.
 *  @author Kai Martins-Turner (kturner)
 */
class EventBasedCarrierScorer4MultipleChainsInclToll implements CarrierScoringFunctionFactory {

	@Inject private Scenario scenario;

	public ScoringFunction createScoringFunction(Carrier carrier) {
		SumScoringFunction sf = new SumScoringFunction();
		sf.addScoringFunction(new EventBasedScoring(scenario, carrier.getId()) );
		return sf;
	}


	/**
	 * Calculate the carrier's score based on Events. Currently, it includes: - fixed costs (using
	 * CarrierTourEndEvent) - time-dependent costs (using FreightTourStart- and -EndEvent) -
	 * distance-dependent costs (using LinkEnterEvent)
	 * tolls (using PersonMoneyEvent)
	 */
	private static class EventBasedScoring implements ArbitraryEventScoring {

		final Logger log = LogManager.getLogger(EventBasedScoring.class);
		private final Map<Id<Tour>, Double> tourStartTime = new LinkedHashMap<>();
		private final Driver2VehicleEventHandler d2v = new Driver2VehicleEventHandler();
		private final Vehicle2CarrierEventHandler v2c = new Vehicle2CarrierEventHandler();
		private final Id<Carrier> carrierId;
		private double score;
		private final Scenario scenario;

		public EventBasedScoring(Scenario scenario, Id<Carrier> carrierId ) {
			super();
			this.scenario = scenario;
			this.carrierId = carrierId;
			log.debug("Begin scoring of Carrier: {}", carrierId);
		}

		@Override
		public void finish() {}

		@Override
		public double getScore() {
			log.debug("End scoring of Carrier: {}", carrierId);
			return score;
		}

		@Override
		public void handleEvent(Event event) {
			log.debug(event.toString());
			switch (event) {
				case CarrierTourStartEvent carrierTourStartEvent -> handleEvent(carrierTourStartEvent);
				case CarrierTourEndEvent carrierTourEndEvent -> handleEvent(carrierTourEndEvent);
				case LinkEnterEvent linkEnterEvent -> handleEvent(linkEnterEvent);
				case PersonMoneyEvent personMoneyEvent -> handleEvent(personMoneyEvent);
				case VehicleEntersTrafficEvent vehicleEntersTrafficEvent -> d2v.handleEvent(vehicleEntersTrafficEvent);
				case VehicleLeavesTrafficEvent vehicleLeavesTrafficEvent -> d2v.handleEvent(vehicleLeavesTrafficEvent);
				default -> {}
			}
		}

		private void handleEvent(CarrierTourStartEvent event) {
			v2c.handleEvent(event);
			tourStartTime.put(event.getTourId(), event.getTime()); // Save time of freight tour start for later use
		}

		// scores fix costs for vehicle usage and variable costs per time
		private void handleEvent(CarrierTourEndEvent event) {
			v2c.handleEvent(event);
			final VehicleType vehicleType = (VehicleUtils.findVehicle(event.getVehicleId(), scenario)).getType();

			// Fix costs for vehicle usage
			log.debug("Score fixed costs for vehicle type: {}", vehicleType.getId().toString());
			score = score - vehicleType.getCostInformation().getFixedCosts();

			// variable costs per time
			double tourDuration = event.getTime() - tourStartTime.get(event.getTourId());
			score = score - (tourDuration * vehicleType.getCostInformation().getCostsPerSecond());
		}

		// scores variable costs per distance
		private void handleEvent(LinkEnterEvent event) {
			final double distance = scenario.getNetwork().getLinks().get(event.getLinkId()).getLength();
			final double costPerMeter =
				(VehicleUtils.findVehicle(event.getVehicleId(), scenario))
					.getType()
					.getCostInformation()
					.getCostsPerMeter();

			score = score - (distance * costPerMeter); // variable costs per distance
		}


		// scores tolls for vehicles driving on tolled links
		private void handleEvent(PersonMoneyEvent event) {
			log.debug("Scoring Carrier: {}", carrierId);
			log.debug("Event : {}", event.toString());

			if (event.getPurpose().equals("toll")) {
				Id<Vehicle> vehicleId = d2v.getVehicleOfDriver(event.getPersonId());
				if (vehicleId != null) {
					Id<Carrier> carrierIdOfVehicle = v2c.getCarrierOfVehicle(vehicleId);
					if (carrierId.equals(carrierIdOfVehicle)) {
						log.debug("Tolling caused by event: {}, toll value {}", event, event.getAmount());
						score = score + event.getAmount();
					}
				}
			}
		}
	}

}
