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

package org.matsim.freight.logistics.examples.mobsimExamples;

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
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controller;
import org.matsim.core.controler.ControllerUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.CarrierCapabilities.FleetSize;
import org.matsim.freight.logistics.*;
import org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils;
import org.matsim.freight.logistics.shipment.LspShipment;
import org.matsim.freight.logistics.shipment.LspShipmentPlanElement;
import org.matsim.freight.logistics.shipment.LspShipmentUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/*package-private*/ class ExampleMobsimOfSimpleLSP {

  public static void main(String[] args) {
    // Set up required MATSim classes
    Config config = new Config();
    config.addCoreModules();

    if (args.length != 0) {
      ConfigUtils.applyCommandline(config, args);
    }

    FreightCarriersConfigGroup freightConfig =
        ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
    freightConfig.setTimeWindowHandling(FreightCarriersConfigGroup.TimeWindowHandling.ignore);

    Scenario scenario = ScenarioUtils.createScenario(config);
    new MatsimNetworkReader(scenario.getNetwork())
        .readFile(ExamplesUtils.getTestScenarioURL("logistics-2regions") + "2regions-network.xml");

    // Create LSP and lspShipments
    LSP lsp = createInitialLSP(scenario);
    Collection<LspShipment> lspShipments = createInitialLSPShipments(scenario.getNetwork());

    // assign the lspShipments to the LSP
    for (LspShipment lspShipment : lspShipments) {
      lsp.assignShipmentToLSP(lspShipment);
    }

    // schedule the LSP with the lspShipments and according to the scheduler of the Resource
    lsp.scheduleLogisticChains();

    // set up simulation controller and LSPModule
    ArrayList<LSP> lspList = new ArrayList<>();
    lspList.add(lsp);
    LSPs lsps = new LSPs(lspList);
    LSPUtils.addLSPs(scenario, lsps);

    config.controller().setFirstIteration(0);
    config.controller().setLastIteration(0);
    config.controller().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);

    Controller controller = ControllerUtils.createController(scenario);
    controller.addOverridingModule(
        new AbstractModule() {
          @Override
          public void install() {
            install(new LSPModule());
          }
        });
    // The VSP default settings are designed for person transport simulation. After talking to Kai,
    // they will be set to WARN here. Kai MT may'23
    controller
        .getConfig()
        .vspExperimental()
        .setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);
    controller.run();

    for (LspShipment lspShipment : lsp.getLspShipments()) {
      System.out.println("LspShipment: " + lspShipment.getId());
      ArrayList<LspShipmentPlanElement> scheduleElements =
          new ArrayList<>(
              LspShipmentUtils.getOrCreateShipmentPlan(lsp.getSelectedPlan(), lspShipment.getId())
                  .getPlanElements()
                  .values());
      scheduleElements.sort(LspShipmentUtils.createShipmentPlanElementComparator());
      ArrayList<LspShipmentPlanElement> logElements =
          new ArrayList<>(lspShipment.getShipmentLog().getPlanElements().values());
      logElements.sort(LspShipmentUtils.createShipmentPlanElementComparator());

      for (int i = 0;
          i
              < LspShipmentUtils.getOrCreateShipmentPlan(lsp.getSelectedPlan(), lspShipment.getId())
                  .getPlanElements()
                  .size();
          i++) {
        System.out.println(
            "Scheduled: "
                + scheduleElements.get(i).getLogisticChainElement().getId()
                + "  "
                + scheduleElements.get(i).getResourceId()
                + "  "
                + scheduleElements.get(i).getElementType()
                + " Start: "
                + scheduleElements.get(i).getStartTime()
                + " End: "
                + scheduleElements.get(i).getEndTime());
      }
      System.out.println();
      for (int i = 0; i < lspShipment.getShipmentLog().getPlanElements().size(); i++) {
        System.out.println(
            "Logged: "
                + logElements.get(i).getLogisticChainElement().getId()
                + "  "
                + logElements.get(i).getResourceId()
                + "  "
                + logElements.get(i).getElementType()
                + " Start: "
                + logElements.get(i).getStartTime()
                + " End: "
                + logElements.get(i).getEndTime());
      }
      System.out.println();
    }
  }

  private static LSP createInitialLSP(Scenario scenario) {

    // The Carrier for the resource of the sole LogisticsSolutionElement of the LSP is created
    Id<Carrier> carrierId = Id.create("CollectionCarrier", Carrier.class);
    Id<VehicleType> vehicleTypeId = Id.create("CollectionCarrierVehicleType", VehicleType.class);
    VehicleType collectionVehType = VehicleUtils.createVehicleType(vehicleTypeId, TransportMode.car);
    collectionVehType.getCapacity().setOther(10);
    collectionVehType.getCostInformation().setCostsPerMeter(0.0004);
    collectionVehType.getCostInformation().setCostsPerSecond(0.38);
    collectionVehType.getCostInformation().setFixedCost(49.);
    collectionVehType.setMaximumVelocity(50 / 3.6);

    Id<Link> collectionLinkId = Id.createLinkId("(4 2) (4 3)");
    Id<Vehicle> vollectionVehicleId = Id.createVehicleId("CollectionVehicle");
    CarrierVehicle carrierVehicle =
        CarrierVehicle.newInstance(vollectionVehicleId, collectionLinkId, collectionVehType);

    CarrierCapabilities capabilities = CarrierCapabilities.Builder.newInstance()
            .addVehicle(carrierVehicle)
            .setFleetSize(FleetSize.INFINITE)
            .build();

    Carrier carrier = CarriersUtils.createCarrier(carrierId);
    carrier.setCarrierCapabilities(capabilities);

    // The Resource i.e. the Resource is created
    LSPResource collectionResource =
        ResourceImplementationUtils.CollectionCarrierResourceBuilder.newInstance(carrier)
            .setCollectionScheduler(
                ResourceImplementationUtils.createDefaultCollectionCarrierScheduler(scenario))
            .setLocationLinkId(collectionLinkId)
            .build();

    // The adapter is now inserted into the only LogisticsSolutionElement of the only
    // LogisticsSolution of the LSP
    LogisticChainElement collectionElement =
        LSPUtils.LogisticChainElementBuilder.newInstance(
                Id.create("CollectionElement", LogisticChainElement.class))
            .setResource(collectionResource)
            .build();

    // The LogisticsSolutionElement is now inserted into the only LogisticsSolution of the LSP
    LogisticChain collectionSolution =
        LSPUtils.LogisticChainBuilder.newInstance(
                Id.create("CollectionSolution", LogisticChain.class))
            .addLogisticChainElement(collectionElement)
            .build();

    // The initial plan of the lsp is generated and the assigner and the solution from above are
    // added
    LSPPlan collectionPlan = LSPUtils.createLSPPlan();
    InitialShipmentAssigner assigner =
        ResourceImplementationUtils.createSingleLogisticChainShipmentAssigner();
    collectionPlan.setInitialShipmentAssigner(assigner);
    collectionPlan.addLogisticChain(collectionSolution);

    LSPUtils.LSPBuilder collectionLSPBuilder =
        LSPUtils.LSPBuilder.getInstance(Id.create("CollectionLSP", LSP.class));
    collectionLSPBuilder.setInitialPlan(collectionPlan);

    // The exogenous list of Resources for the SolutionScheduler is compiled and the Scheduler is
    // added to the LSPBuilder
    ArrayList<LSPResource> resourcesList = new ArrayList<>();
    resourcesList.add(collectionResource);
    LogisticChainScheduler simpleScheduler =
        ResourceImplementationUtils.createDefaultSimpleForwardLogisticChainScheduler(resourcesList);
    collectionLSPBuilder.setLogisticChainScheduler(simpleScheduler);

    return collectionLSPBuilder.build();
  }

  private static Collection<LspShipment> createInitialLSPShipments(Network network) {
    ArrayList<LspShipment> shipmentList = new ArrayList<>();
    ArrayList<Link> linkList = new ArrayList<>(network.getLinks().values());

    // Create five LSPShipments that are located in the left half of the network.
    for (int i = 1; i < 6; i++) {
      Id<LspShipment> id = Id.create(i, LspShipment.class);
      LspShipmentUtils.LspShipmentBuilder builder = LspShipmentUtils.LspShipmentBuilder.newInstance(id);
      Random random = new Random(1);
      int capacityDemand = random.nextInt(4);
      builder.setCapacityDemand(capacityDemand);

      while (true) {
        Collections.shuffle(linkList, random);
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
      shipmentList.add(builder.build());
    }
    return shipmentList;
  }
}
