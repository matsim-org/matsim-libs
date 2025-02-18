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
import org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils.TranshipmentHubSchedulerBuilder;
import org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils.TransshipmentHubBuilder;
import org.matsim.freight.logistics.shipment.LspShipment;
import org.matsim.freight.logistics.shipment.LspShipmentPlanElement;
import org.matsim.freight.logistics.shipment.LspShipmentUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/*package-private*/ class ExampleMobsimOfTransportChain {

  private static LSP createInitialLSP(Scenario scenario) {

    Network network = scenario.getNetwork();

    // The Carrier for collection is created
    Id<Carrier> collectionCarrierId = Id.create("CollectionCarrier", Carrier.class);
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

    Carrier collectionCarrier = CarriersUtils.createCarrier(collectionCarrierId);
    collectionCarrier.setCarrierCapabilities(capabilities);

    // The collection adapter i.e. the Resource is created
    LSPResource collectionResource =
        ResourceImplementationUtils.CollectionCarrierResourceBuilder.newInstance(
                collectionCarrier)
            .setCollectionScheduler(
                ResourceImplementationUtils.createDefaultCollectionCarrierScheduler(scenario))
            .setLocationLinkId(collectionLinkId)
            .build();

    // The adapter is now inserted into the corresponding LogisticsSolutionElement of the only
    // LogisticsSolution of the LSP
    LogisticChainElement collectionElement =
        LSPUtils.LogisticChainElementBuilder.newInstance(
                Id.create("CollectionElement", LogisticChainElement.class))
            .setResource(collectionResource)
            .build();

    // The first reloading adapter i.e. the Resource is created
    Id<LSPResource> firstTransshipmentHubId = Id.create("TranshipmentHub1", LSPResource.class);
    Id<Link> firstTransshipmentHub_LinkId = Id.createLinkId("(4 2) (4 3)");
    TransshipmentHubBuilder firstTransshipmentHubBuilder =
        ResourceImplementationUtils.TransshipmentHubBuilder.newInstance(
            firstTransshipmentHubId, firstTransshipmentHub_LinkId, scenario);

    // The scheduler for the first reloading point is created
    TranshipmentHubSchedulerBuilder firstReloadingSchedulerBuilder =
        ResourceImplementationUtils.TranshipmentHubSchedulerBuilder.newInstance();
    firstReloadingSchedulerBuilder.setCapacityNeedFixed(10);
    firstReloadingSchedulerBuilder.setCapacityNeedLinear(1);

    // The scheduler is added to the Resource and the Resource is created
    firstTransshipmentHubBuilder.setTransshipmentHubScheduler(
        firstReloadingSchedulerBuilder.build());
    LSPResource firstTranshipmentHubResource = firstTransshipmentHubBuilder.build();

    // The SolutionElement for the first reloading point is created
    Id<LogisticChainElement> firstHubElementId =
        Id.create("FirstHubElement", LogisticChainElement.class);
    LSPUtils.LogisticChainElementBuilder firstHubElementBuilder =
        LSPUtils.LogisticChainElementBuilder.newInstance(firstHubElementId);
    firstHubElementBuilder.setResource(firstTranshipmentHubResource);
    LogisticChainElement firstHubElement = firstHubElementBuilder.build();

    // The Carrier for the main run Resource is created
    final Id<Carrier> mainRunCarrierId = Id.create("MainRunCarrier", Carrier.class);
    final Id<VehicleType> mainRunVehTypeId = Id.create("MainRunCarrierVehicleType", VehicleType.class);
    final VehicleType mainRunVehType = VehicleUtils.createVehicleType(mainRunVehTypeId, TransportMode.car);
    mainRunVehType.getCapacity().setOther(30);
    mainRunVehType.getCostInformation().setCostsPerMeter(0.0002);
    mainRunVehType.getCostInformation().setCostsPerSecond(0.38);
    mainRunVehType.getCostInformation().setFixedCost(120.);
    mainRunVehType.setMaximumVelocity(50 / 3.6);
    mainRunVehType.setNetworkMode(TransportMode.car);

    Id<Link> fromLinkId = Id.createLinkId("(4 2) (4 3)");
    Id<Vehicle> mainRunVehicleId = Id.createVehicleId("MainRunVehicle");
    CarrierVehicle mainRunCarrierVehicle = CarrierVehicle.newInstance(mainRunVehicleId, fromLinkId, mainRunVehType);

    CarrierCapabilities mainRunCapabilities =
        CarrierCapabilities.Builder.newInstance()
            .addVehicle(mainRunCarrierVehicle)
            .setFleetSize(FleetSize.INFINITE)
            .build();
    Carrier mainRunCarrier = CarriersUtils.createCarrier(mainRunCarrierId);
    mainRunCarrier.setCarrierCapabilities(mainRunCapabilities);

    // The adapter i.e. the main run resource is created
    LSPResource mainRunResource =
        ResourceImplementationUtils.MainRunCarrierResourceBuilder.newInstance(mainRunCarrier)
            .setFromLinkId(Id.createLinkId("(4 2) (4 3)"))
            .setToLinkId(Id.createLinkId("(14 2) (14 3)"))
            .setMainRunCarrierScheduler(ResourceImplementationUtils.createDefaultMainRunCarrierScheduler(scenario))
            .build();

    // The LogisticsSolutionElement for the main run Resource is created
    Id<LogisticChainElement> mainRunElementId =
        Id.create("MainRunElement", LogisticChainElement.class);
    LogisticChainElement mainRunElement =
        LSPUtils.LogisticChainElementBuilder.newInstance(mainRunElementId)
            .setResource(mainRunResource)
            .build();

    // The second reloading adapter i.e. the Resource is created
    Id<LSPResource> secondTransshipmentHubId = Id.create("TranshipmentHub2", LSPResource.class);
    Id<Link> secondTransshipmentHub_LinkId = Id.createLinkId("(14 2) (14 3)");
    TransshipmentHubBuilder secondTransshipmentHubBuilder =
        ResourceImplementationUtils.TransshipmentHubBuilder.newInstance(
            secondTransshipmentHubId, secondTransshipmentHub_LinkId, scenario);

    // The scheduler for the second reloading point is created
    TranshipmentHubSchedulerBuilder secondSchedulerBuilder =
        ResourceImplementationUtils.TranshipmentHubSchedulerBuilder.newInstance();
    secondSchedulerBuilder.setCapacityNeedFixed(10);
    secondSchedulerBuilder.setCapacityNeedLinear(1);

    // The scheduler is added to the Resource and the Resource is created
    secondTransshipmentHubBuilder.setTransshipmentHubScheduler(secondSchedulerBuilder.build());
    LSPResource secondTranshipmentHubResource = secondTransshipmentHubBuilder.build();

    // The adapter is now inserted into the corresponding LogisticsSolutionElement of the only
    // LogisticsSolution of the LSP
    Id<LogisticChainElement> secondHubElementId =
        Id.create("SecondHubElement", LogisticChainElement.class);
    LSPUtils.LogisticChainElementBuilder secondHubElementBuilder =
        LSPUtils.LogisticChainElementBuilder.newInstance(secondHubElementId);
    secondHubElementBuilder.setResource(secondTranshipmentHubResource);
    LogisticChainElement secondHubElement = secondHubElementBuilder.build();

    // The Carrier for distribution is created
    Id<Carrier> distributionCarrierId = Id.create("DistributionCarrier", Carrier.class);
    Id<VehicleType> distributionVehTypeId =
        Id.create("DistributionCarrierVehicleType", VehicleType.class);
    VehicleType distributionVehType = VehicleUtils.createVehicleType(vehicleTypeId, TransportMode.car);
    distributionVehType.getCapacity().setOther(10);
    distributionVehType.getCostInformation().setCostsPerMeter(0.0004);
    distributionVehType.getCostInformation().setCostsPerSecond(0.38);
    distributionVehType.getCostInformation().setFixedCost(49.);
    distributionVehType.setMaximumVelocity(50 / 3.6);

    Id<Link> distributionLinkId = Id.createLinkId("(4 2) (4 3)");
    Id<Vehicle> distributionVehicleId = Id.createVehicleId("DistributionVehicle");
    CarrierVehicle distributionCarrierVehicle =
        CarrierVehicle.newInstance(distributionVehicleId, distributionLinkId, distributionVehType);

    CarrierCapabilities distributionCapabilities = CarrierCapabilities.Builder.newInstance()
            .addVehicle(distributionCarrierVehicle)
            .setFleetSize(FleetSize.INFINITE)
            .build();

    Carrier distributionCarrier = CarriersUtils.createCarrier(distributionCarrierId);
    distributionCarrier.setCarrierCapabilities(distributionCapabilities);

    // The distribution adapter i.e. the Resource is created
    LSPResource distributionResource =
        ResourceImplementationUtils.DistributionCarrierResourceBuilder.newInstance(
                distributionCarrier)
            .setLocationLinkId(distributionLinkId)
            // The scheduler for the Resource is created and added. This is where jsprit comes into
            // play.
            .setDistributionScheduler(
                ResourceImplementationUtils.createDefaultDistributionCarrierScheduler(scenario))
            .build();

    // The adapter is now inserted into the corresponding LogisticsSolutionElement of the only
    // LogisticsSolution of the LSP
    Id<LogisticChainElement> distributionElementId =
        Id.create("DistributionElement", LogisticChainElement.class);
    LSPUtils.LogisticChainElementBuilder distributionBuilder =
        LSPUtils.LogisticChainElementBuilder.newInstance(distributionElementId);
    distributionBuilder.setResource(distributionResource);
    LogisticChainElement distributionElement = distributionBuilder.build();

    // The Order of the logisticsSolutionElements is now specified
    collectionElement.connectWithNextElement(firstHubElement);
    firstHubElement.connectWithNextElement(mainRunElement);
    mainRunElement.connectWithNextElement(secondHubElement);
    secondHubElement.connectWithNextElement(distributionElement);

    // The SolutionElements are now inserted into the only LogisticsSolution of the LSP
    Id<LogisticChain> solutionId = Id.create("SolutionId", LogisticChain.class);
    LSPUtils.LogisticChainBuilder completeSolutionBuilder =
        LSPUtils.LogisticChainBuilder.newInstance(solutionId);
    completeSolutionBuilder.addLogisticChainElement(collectionElement);
    completeSolutionBuilder.addLogisticChainElement(firstHubElement);
    completeSolutionBuilder.addLogisticChainElement(mainRunElement);
    completeSolutionBuilder.addLogisticChainElement(secondHubElement);
    completeSolutionBuilder.addLogisticChainElement(distributionElement);
    LogisticChain completeSolution = completeSolutionBuilder.build();

    // The initial plan of the lsp is generated and the assigner and the solution from above are
    // added
    LSPPlan completePlan = LSPUtils.createLSPPlan();
    InitialShipmentAssigner assigner =
        ResourceImplementationUtils.createSingleLogisticChainShipmentAssigner();
    completePlan.setInitialShipmentAssigner(assigner);
    completePlan.addLogisticChain(completeSolution);

    LSPUtils.LSPBuilder completeLSPBuilder =
        LSPUtils.LSPBuilder.getInstance(Id.create("CollectionLSP", LSP.class));
    completeLSPBuilder.setInitialPlan(completePlan);

    // The exogenous list of Resources for the SolutionScheduler is compiled and the Scheduler is
    // added to the LSPBuilder
    ArrayList<LSPResource> resourcesList = new ArrayList<>();
    resourcesList.add(collectionResource);
    resourcesList.add(firstTranshipmentHubResource);
    resourcesList.add(mainRunResource);
    resourcesList.add(secondTranshipmentHubResource);
    resourcesList.add(distributionResource);
    LogisticChainScheduler simpleScheduler =
        ResourceImplementationUtils.createDefaultSimpleForwardLogisticChainScheduler(resourcesList);
    completeLSPBuilder.setLogisticChainScheduler(simpleScheduler);

    return completeLSPBuilder.build();
  }

  private static Collection<LspShipment> createInitialLSPShipments(Network network) {
    ArrayList<LspShipment> shipmentList = new ArrayList<>();
    ArrayList<Link> linkList = new ArrayList<>(network.getLinks().values());
    Random rand = new Random(1);
    for (int i = 1; i < 6; i++) {
      Id<LspShipment> id = Id.create(i, LspShipment.class);
      LspShipmentUtils.LspShipmentBuilder builder = LspShipmentUtils.LspShipmentBuilder.newInstance(id);
      int capacityDemand = rand.nextInt(10);
      builder.setCapacityDemand(capacityDemand);

      while (true) {
        Collections.shuffle(linkList, rand);
        Link pendingToLink = linkList.getFirst();
        if ((pendingToLink.getFromNode().getCoord().getX() <= 18000
            && pendingToLink.getFromNode().getCoord().getY() <= 4000
            && pendingToLink.getFromNode().getCoord().getX() >= 14000
            && pendingToLink.getToNode().getCoord().getX() <= 18000
            && pendingToLink.getToNode().getCoord().getY() <= 4000
            && pendingToLink.getToNode().getCoord().getX() >= 14000)) {
          builder.setToLinkId(pendingToLink.getId());
          break;
        }
      }

      while (true) {
        Collections.shuffle(linkList, rand);
        Link pendingFromLink = linkList.getFirst();
        if (pendingFromLink.getFromNode().getCoord().getX() <= 4000
            && pendingFromLink.getFromNode().getCoord().getY() <= 4000
            && pendingFromLink.getToNode().getCoord().getX() <= 4000
            && pendingFromLink.getToNode().getCoord().getY() <= 4000) {
          builder.setFromLinkId(pendingFromLink.getId());
          break;
        }
      }

      TimeWindow endTimeWindow = TimeWindow.newInstance(0, (24 * 3600));
      builder.setEndTimeWindow(endTimeWindow);
      TimeWindow startTimeWindow = TimeWindow.newInstance(0, (24 * 3600));
      builder.setStartTimeWindow(startTimeWindow);
      builder.setDeliveryServiceTime(capacityDemand * 60);
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

    Controller controller = ControllerUtils.createController(scenario);
    controller.addOverridingModule(
        new AbstractModule() {
          @Override
          public void install() {
            install(
                new LSPModule()); // this is the better syntax, having everything in one module.
                                  // kai, may'22
          }
        });
    config.controller().setFirstIteration(0);
    config.controller().setLastIteration(0);
    config.controller().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
    config.network().setInputFile(ExamplesUtils.getTestScenarioURL("logistics-2regions") + "2regions-network.xml");
    // The VSP default settings are designed for person transport simulation. After talking to Kai,
    // they will be set to WARN here. Kai MT may'23
    controller
        .getConfig()
        .vspExperimental()
        .setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);
    controller.run();

    for (LspShipment lspShipment : lsp.getLspShipments()) {
      System.out.println("Shipment: " + lspShipment.getId());
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
}
