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
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.SumScoringFunction.ArbitraryEventScoring;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.Tour;
import org.matsim.freight.carriers.controler.CarrierScoringFunctionFactory;
import org.matsim.freight.carriers.events.CarrierServiceEndEvent;
import org.matsim.freight.carriers.events.CarrierServiceStartEvent;
import org.matsim.freight.carriers.events.CarrierTourEndEvent;
import org.matsim.freight.carriers.events.CarrierTourStartEvent;
import org.matsim.freight.logistics.analysis.Driver2VehicleEventHandler;
import org.matsim.freight.logistics.analysis.Vehicle2CarrierEventHandler;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 * @author Kai Martins-Turner (kturner)
 */
class EventBasedCarrierScorer4MultipleChainsInclToll implements CarrierScoringFunctionFactory {

  @Inject private Network network;
  @Inject private Scenario scenario;

  private Id<Carrier> carrierId;

  public ScoringFunction createScoringFunction(Carrier carrier) {
    this.carrierId = carrier.getId();
    SumScoringFunction sf = new SumScoringFunction();
    sf.addScoringFunction(new EventBasedScoring());
//    sf.addScoringFunction(new FindOtherEventsForDebuggingOnly_NO_Scoring());
//    sf.addScoringFunction(new TollMoneyScoringForDebuggingOnly_NO_Scoring());
    return sf;
  }


  /**
   * Calculate the carrier's score based on Events. Currently, it includes: - fixed costs (using
   * CarrierTourEndEvent) - time-dependent costs (using FreightTourStart- and -EndEvent) -
   * distance-dependent costs (using LinkEnterEvent)
   * tolls (using PersonMoneyEvent)
   */
  private class EventBasedScoring implements ArbitraryEventScoring {

    final Logger log = LogManager.getLogger(EventBasedScoring.class);
    private final Map<Id<Tour>, Double> tourStartTime = new LinkedHashMap<>();
    private final Driver2VehicleEventHandler d2v = new Driver2VehicleEventHandler();
    private final Vehicle2CarrierEventHandler v2c = new Vehicle2CarrierEventHandler();
    private double score;

    public EventBasedScoring() {
      super();
    }

    @Override
    public void finish() {}

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
        case PersonMoneyEvent personMoneyEvent -> handleEvent(personMoneyEvent);     //FIXME: Aus irgendwelchen Gründen kommen hier keine PersonMoneyEvents an...
        case VehicleEntersTrafficEvent vehicleEntersTrafficEvent -> d2v.handleEvent(vehicleEntersTrafficEvent);
        case VehicleLeavesTrafficEvent vehicleLeavesTrafficEvent -> d2v.handleEvent(vehicleLeavesTrafficEvent);
        default -> {}
      }
    }

    private void handleEvent(CarrierTourStartEvent event) {
      v2c.handleEvent(event);
      // Save time of freight tour start
      tourStartTime.put(event.getTourId(), event.getTime());
    }

    // scores fix costs for vehicle usage and variable costs per time
    private void handleEvent(CarrierTourEndEvent event) {
      v2c.handleEvent(event);
      // Fix costs for vehicle usage
      final VehicleType vehicleType =
              (VehicleUtils.findVehicle(event.getVehicleId(), scenario)).getType();

      double tourDuration = event.getTime() - tourStartTime.get(event.getTourId());

      log.info("Score fixed costs for vehicle type: {}", vehicleType.getId().toString());
      score = score - vehicleType.getCostInformation().getFixedCosts();

      // variable costs per time
      score = score - (tourDuration * vehicleType.getCostInformation().getCostsPerSecond());
    }

    // scores variable costs per distance
    private void handleEvent(LinkEnterEvent event) {
      final double distance = network.getLinks().get(event.getLinkId()).getLength();
      final double costPerMeter =
              (VehicleUtils.findVehicle(event.getVehicleId(), scenario))
                      .getType()
                      .getCostInformation()
                      .getCostsPerMeter();
      // variable costs per distance
      score = score - (distance * costPerMeter);
    }

    private final List<Id<Person>> tolledPersons = new ArrayList<>();

    // scores tolls for vehicles driving on tolled links
    private void handleEvent(PersonMoneyEvent event) {
      double tollValue = 0;

      if (event.getPurpose().equals("toll")) {
        Id<Vehicle> vehicleId = d2v.getVehicleOfDriver(event.getPersonId());
        if (vehicleId != null) {
          Id<Carrier> carrierIdOfVehicle = v2c.getCarrierOfVehicle(vehicleId);
          if (carrierId.equals(carrierIdOfVehicle)) {
//           toll a person only once.
            if (!tolledPersons.contains(event.getPersonId())) {
              log.info("Tolling caused by event: {}", event);
              tolledPersons.add(event.getPersonId());
              tollValue = event.getAmount();
            }
          }
          score = score - tollValue;
        }
      }
    }

  }

/**
 * Versuche nur mit dem Debugger die PersonMoneyEvents zu entdecken, die eigentlich da sein sollten.
 */
private class FindOtherEventsForDebuggingOnly_NO_Scoring implements ArbitraryEventScoring {

  final Logger log = LogManager.getLogger(FindOtherEventsForDebuggingOnly_NO_Scoring.class);


  public FindOtherEventsForDebuggingOnly_NO_Scoring() {
    super();
  }

  @Override
  public void finish() {}

  @Override
  public double getScore() {
    return Double.MIN_VALUE;
  }

  @Override
  public void handleEvent(Event event) {
    log.debug(event.toString());

    switch (event) {
      case CarrierTourStartEvent carrierTourStartEvent -> {}
//        case CarrierTourEndEvent carrierTourEndEvent -> {}
      case CarrierServiceStartEvent carrierServiceStartEvent -> {}
      case CarrierServiceEndEvent carrierServiceEndEvent -> {}
      case LinkEnterEvent linkEnterEvent ->{}
      case LinkLeaveEvent linkLeaveEvent ->{}
//        case PersonMoneyEvent personMoneyEvent -> handleEvent(personMoneyEvent);
      default -> {handleOtherEvent(event);}
    }
  }

  private void handleOtherEvent(Event event) {
    log.info("Found another event: {}", event.toString());
  }

}


/**
 * Versuche nur mit dem Debugger die Einträge aus den PersonMoneyEvents zu entdecken, die eigentlich da sein sollten.
 */
private static class TollMoneyScoringForDebuggingOnly_NO_Scoring implements SumScoringFunction.MoneyScoring, PersonMoneyEventHandler {

  private double score = 0.0;
  @Override
  public void addMoney(double amount) {
    // TODO Auto-generated method stub
  }

  @Override
  public void finish() {    }

  @Override
  public double getScore() {
    return this.score;
  }

  @Override
  public void handleEvent(PersonMoneyEvent event) {
    // Todo: implement
  }
}

}
