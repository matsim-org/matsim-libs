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

/*
  *********************************************************************** *
  * project: org.matsim.*
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  * copyright       :  (C) 2022 by the members listed in the COPYING,       *
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

package org.matsim.freight.logistics.examples.requirementsChecking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.CarrierCapabilities.FleetSize;
import org.matsim.freight.logistics.*;
import org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils;
import org.matsim.freight.logistics.shipment.LspShipment;
import org.matsim.freight.logistics.shipment.LspShipmentUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

class ExampleCheckRequirementsOfAssigner {

  static final String ATTRIBUTE_COLOR = "color";

  private static LSP createLSPWithProperties(Scenario scenario) {

    final Network network = scenario.getNetwork();

    // Create red LogisticsSolution which has the corresponding info
    final Id<Carrier> redCarrierId = Id.create("RedCarrier", Carrier.class);
    final Id<VehicleType> collectionVehTypeId = Id.create("RedCarrierVehicleType", VehicleType.class);
    final VehicleType collectionVehType = VehicleUtils.createVehicleType(collectionVehTypeId, TransportMode.car);
    collectionVehType.getCapacity().setOther(10);
    collectionVehType.getCostInformation().setCostsPerMeter(0.0004);
    collectionVehType.getCostInformation().setCostsPerSecond(0.38);
    collectionVehType.getCostInformation().setFixedCost(49.);
    collectionVehType.setMaximumVelocity(50 / 3.6);
    collectionVehType.setNetworkMode(TransportMode.car);

    Id<Link> collectionLinkId = Id.createLinkId("(4 2) (4 3)");
    Id<Vehicle> redVehicleId = Id.createVehicleId("RedVehicle");
    CarrierVehicle redVehicle =
        CarrierVehicle.newInstance(redVehicleId, collectionLinkId, collectionVehType);

    CarrierCapabilities redCapabilities =
        CarrierCapabilities.Builder.newInstance()
            .addVehicle(redVehicle)
            .setFleetSize(FleetSize.INFINITE)
            .build();
    Carrier redCarrier = CarriersUtils.createCarrier(redCarrierId);
    redCarrier.setCarrierCapabilities(redCapabilities);

    LSPResource redResource =
        ResourceImplementationUtils.CollectionCarrierResourceBuilder.newInstance(redCarrier)
            .setCollectionScheduler(
                ResourceImplementationUtils.createDefaultCollectionCarrierScheduler(scenario))
            .setLocationLinkId(collectionLinkId)
            .build();

    Id<LogisticChainElement> redElementId = Id.create("RedElement", LogisticChainElement.class);
    LogisticChainElement redElement =
        LSPUtils.LogisticChainElementBuilder.newInstance(redElementId)
            .setResource(redResource)
            .build();

    Id<LogisticChain> redSolutionId = Id.create("RedSolution", LogisticChain.class);
    LSPUtils.LogisticChainBuilder redSolutionBuilder =
        LSPUtils.LogisticChainBuilder.newInstance(redSolutionId);
    redSolutionBuilder.addLogisticChainElement(redElement);
    LogisticChain redSolution = redSolutionBuilder.build();

    // Add info that shows the world the color of the solution
    redSolution.getAttributes().putAttribute(ATTRIBUTE_COLOR, RedRequirement.RED);

    // Create blue LogisticsSolution which has the corresponding info
    Id<Carrier> blueCarrierId = Id.create("BlueCarrier", Carrier.class);
    Id<Vehicle> blueVehicleId = Id.createVehicleId("BlueVehicle");
    CarrierVehicle blueVehicle =
        CarrierVehicle.newInstance(blueVehicleId, collectionLinkId, collectionVehType);

    CarrierCapabilities blueCapabilities =
        CarrierCapabilities.Builder.newInstance()
            .addVehicle(blueVehicle)
            .setFleetSize(FleetSize.INFINITE)
            .build();
    Carrier blueCarrier = CarriersUtils.createCarrier(blueCarrierId);
    blueCarrier.setCarrierCapabilities(blueCapabilities);

    LSPResource blueResource =
        ResourceImplementationUtils.CollectionCarrierResourceBuilder.newInstance(blueCarrier)
            .setCollectionScheduler(
                ResourceImplementationUtils.createDefaultCollectionCarrierScheduler(scenario))
            .setLocationLinkId(collectionLinkId)
            .build();

    Id<LogisticChainElement> blueElementId = Id.create("BlueCElement", LogisticChainElement.class);
    LogisticChainElement blueElement =
        LSPUtils.LogisticChainElementBuilder.newInstance(blueElementId)
            .setResource(blueResource)
            .build();

    LogisticChain blueSolution =
        LSPUtils.LogisticChainBuilder.newInstance(Id.create("BlueSolution", LogisticChain.class))
            .addLogisticChainElement(blueElement)
            .build();

    // Add info that shows the world the color of the solution
    blueSolution.getAttributes().putAttribute(ATTRIBUTE_COLOR, BlueRequirement.BLUE);

    // Create the initial plan, add assigner that checks requirements of the shipments when
    // assigning and add both solutions (red and blue) to the
    // plan.
    LSPPlan plan =
        LSPUtils.createLSPPlan()
            .setInitialShipmentAssigner(new RequirementsAssigner())
            .addLogisticChain(redSolution)
            .addLogisticChain(blueSolution);

    ArrayList<LSPResource> resourcesList = new ArrayList<>();
    resourcesList.add(redResource);
    resourcesList.add(blueResource);

    return LSPUtils.LSPBuilder.getInstance(Id.create("CollectionLSP", LSP.class))
        .setInitialPlan(plan)
        .setLogisticChainScheduler(
            ResourceImplementationUtils.createDefaultSimpleForwardLogisticChainScheduler(
                resourcesList))
        .build();
  }

  public static Collection<LspShipment> createShipmentsWithRequirements(Network network) {
    // Create ten shipments with either a red or blue requirement, i.e. that they only can be
    // transported in a solution with the matching color
    ArrayList<LspShipment> shipmentList = new ArrayList<>();
    ArrayList<Link> linkList = new ArrayList<>(network.getLinks().values());

    Random rand = new Random(1);

    for (int i = 1; i < 11; i++) {
      Id<LspShipment> id = Id.create(i, LspShipment.class);
      LspShipmentUtils.LspShipmentBuilder builder = LspShipmentUtils.LspShipmentBuilder.newInstance(id);
      int capacityDemand = rand.nextInt(10);
      builder.setCapacityDemand(capacityDemand);

      while (true) {
        Collections.shuffle(linkList);
        Link pendingFromLink = linkList.getFirst();
        if (pendingFromLink.getFromNode().getCoord().getX() <= 4000
            && pendingFromLink.getFromNode().getCoord().getY() <= 4000
            && pendingFromLink.getToNode().getCoord().getX() <= 4000
            && pendingFromLink.getToNode().getCoord().getY() <= 4000) {
          builder.setFromLinkId(pendingFromLink.getId());
          break;
        }
      }

      builder.setToLinkId(Id.createLinkId("(4 2) (4 3)"));
      TimeWindow endTimeWindow = TimeWindow.newInstance(0, (24 * 3600));
      builder.setEndTimeWindow(endTimeWindow);
      TimeWindow startTimeWindow = TimeWindow.newInstance(0, (24 * 3600));
      builder.setStartTimeWindow(startTimeWindow);
      builder.setDeliveryServiceTime(capacityDemand * 60);
      boolean blue = rand.nextBoolean();
      if (blue) {
        builder.addRequirement(new BlueRequirement());
      } else {
        builder.addRequirement(new RedRequirement());
      }

      shipmentList.add(builder.build());
    }

    return shipmentList;
  }

  public static void main(String[] args) {

    // Set up required MATSim classes
    Config config = new Config();
    config.addCoreModules();
    Scenario scenario = ScenarioUtils.createScenario(config);
    new MatsimNetworkReader(scenario.getNetwork())
        .readFile(ExamplesUtils.getTestScenarioURL("logistics-2regions") + "2regions-network.xml");
    Network network = scenario.getNetwork();

    // Create LSP and lspShipments
    LSP lsp = createLSPWithProperties(scenario);
    Collection<LspShipment> lspShipments = createShipmentsWithRequirements(network);

    // assign the lspShipments to the LSP
    for (LspShipment lspShipment : lspShipments) {
      lsp.assignShipmentToLSP(lspShipment);
    }

    for (LogisticChain logisticChain : lsp.getSelectedPlan().getLogisticChains()) {
      if (logisticChain.getId().toString().equals("RedSolution")) {
        for (Id<LspShipment> lspShipmentId : logisticChain.getLspShipmentIds()) {
          LspShipment lspShipment = LSPUtils.findLspShipment(lsp, lspShipmentId);
            if (lspShipment != null && !(lspShipment.getRequirements().iterator().next() instanceof RedRequirement)) {
                break;
            }
        }
        System.out.println("All lspShipments in " + logisticChain.getId() + " are red");
      }
      if (logisticChain.getId().toString().equals("BlueSolution")) {
        for (Id<LspShipment> lspShipmentId : logisticChain.getLspShipmentIds()) {
          LspShipment shipment = LSPUtils.findLspShipment(lsp, lspShipmentId);
            if (shipment != null && !(shipment.getRequirements().iterator().next() instanceof BlueRequirement)) {
                break;
            }
        }
        System.out.println("All lspShipments in " + logisticChain.getId() + " are blue");
      }
    }
  }
}
