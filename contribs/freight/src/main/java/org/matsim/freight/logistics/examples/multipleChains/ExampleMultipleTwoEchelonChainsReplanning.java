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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.*;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.GenericWorstPlanForRemovalSelector;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.controller.CarrierControllerUtils;
import org.matsim.freight.carriers.controller.CarrierScoringFunctionFactory;
import org.matsim.freight.carriers.controller.CarrierStrategyManager;
import org.matsim.freight.logistics.*;
import org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils;
import org.matsim.freight.logistics.shipment.LspShipment;
import org.matsim.freight.logistics.shipment.LspShipmentUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

final class ExampleMultipleTwoEchelonChainsReplanning {

  static final double HUBCOSTS_FIX = 100;
  private static final Logger log =
      LogManager.getLogger(ExampleMultipleTwoEchelonChainsReplanning.class);
  private static final Id<Link> DEPOT_LINK_ID = Id.createLinkId("i(5,0)");
  private static final Id<Link> HUB_LEFT_LINK_ID = Id.createLinkId("i(1,5)R");
  private static final Id<Link> HUB_RIGHT_LINK_ID = Id.createLinkId("j(9,5)");

  private static final VehicleType VEH_TYPE_LARGE_50 = createVehTypeLarge50();
  private static final VehicleType VEH_TYPE_SMALL_05 = createVehTypeSmall05();

  private static VehicleType createVehTypeLarge50() {
    VehicleType vehicleType = VehicleUtils.createVehicleType(Id.create("large50", VehicleType.class), TransportMode.car);
    vehicleType.getCapacity().setOther(50);
    vehicleType.getCostInformation().setCostsPerMeter(0.01);
    vehicleType.getCostInformation().setCostsPerSecond(0.01);
    vehicleType.getCostInformation().setFixedCost(150.);
    vehicleType.setMaximumVelocity(10);
    vehicleType.setNetworkMode(TransportMode.car);

    return vehicleType;
  }

  private static VehicleType createVehTypeSmall05() {
    VehicleType vehicleType = VehicleUtils.createVehicleType(Id.create("small05", VehicleType.class), TransportMode.car);
    vehicleType.getCapacity().setOther(5);
    vehicleType.getCostInformation().setCostsPerMeter(0.001);
    vehicleType.getCostInformation().setCostsPerSecond(0.005);
    vehicleType.getCostInformation().setFixedCost(25.);
    vehicleType.setMaximumVelocity(10);
    vehicleType.setNetworkMode(TransportMode.car);

    return vehicleType;
  }

  private ExampleMultipleTwoEchelonChainsReplanning() {}

  public static void main(String[] args) {
    log.info("Prepare config");
    Config config = prepareConfig(args);

    log.info("Prepare scenario");
    Scenario scenario = prepareScenario(config);

    log.info("Prepare controller");
    Controller controller = ControllerUtils.createController(scenario);
    controller.addOverridingModule(
        new AbstractModule() {
          @Override
          public void install() {
            install(new LSPModule());
          }
        });

    // @formatter:off
    controller.addOverridingModule(
        new AbstractModule() {
          @Override
          public void install() {
            final EventBasedCarrierScorer4MultipleChains carrierScorer = new EventBasedCarrierScorer4MultipleChains();
            bind(CarrierScoringFunctionFactory.class).toInstance(carrierScorer);
            bind(LSPScorerFactory.class).toInstance(MyLSPScorer::new);
            bind(CarrierStrategyManager.class).toProvider( () -> {
                      CarrierStrategyManager strategyManager = CarrierControllerUtils.createDefaultCarrierStrategyManager();
                      strategyManager.addStrategy( new GenericPlanStrategyImpl<>(new BestPlanSelector<>()), null, 1);
                      return strategyManager;
                    });
            bind(LSPStrategyManager.class).toProvider( () -> {
                      LSPStrategyManager strategyManager = new LSPStrategyManagerImpl();
                      strategyManager.addStrategy( new GenericPlanStrategyImpl<>( new ExpBetaPlanSelector<>(new ScoringConfigGroup())), null, 1);
                      strategyManager.addStrategy( ProximityStrategyFactory.createStrategy(scenario.getNetwork()), null, 1);
                      strategyManager.setMaxPlansPerAgent(5);
                      strategyManager.setPlanSelectorForRemoval( new GenericWorstPlanForRemovalSelector<>());
                      return strategyManager;
                    });
          }
        });
    // @formatter:on

    log.info("Run MATSim");

    // The VSP default settings are designed for person transport simulation. After talking to Kai,
    // they will be set to WARN here. Kai MT may'23
    controller
        .getConfig()
        .vspExperimental()
        .setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);
    controller.run();

    log.info("Done.");
  }

  private static Config prepareConfig(String[] args) {
    Config config = ConfigUtils.createConfig();
    if (args.length != 0) {
      for (String arg : args) {
        log.warn(arg);
      }
      ConfigUtils.applyCommandline(config, args);
    } else {
      config.controller().setOutputDirectory("output/multipleTwoEchelonChainsReplanning");
      config.controller().setLastIteration(8);
    }
    config
        .network()
        .setInputFile(
            String.valueOf(
                IOUtils.extendUrl(
                    ExamplesUtils.getTestScenarioURL("freight-chessboard-9x9"), "grid9x9.xml")));
    config
        .controller()
        .setOverwriteFileSetting(
            OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
    config.controller().setWriteEventsInterval(1);

    FreightCarriersConfigGroup freightConfig =
        ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
    freightConfig.setTimeWindowHandling(FreightCarriersConfigGroup.TimeWindowHandling.ignore);

    return config;
  }

  private static Scenario prepareScenario(Config config) {
    Scenario scenario = ScenarioUtils.loadScenario(config);

    for (Link link : scenario.getNetwork().getLinks().values()) {
      link.setFreespeed(30 / 3.6);
      link.setCapacity(1000);
    }

    log.info("Add LSP to the scenario");
    LSPUtils.addLSPs(scenario, new LSPs(Collections.singletonList(createLSP(scenario))));

    return scenario;
  }

  private static LSP createLSP(Scenario scenario) {
    log.info("create LSP");
    Network network = scenario.getNetwork();

    // A plan with a two hub chains is created
    LSPPlan multipleTwoEchelonChainsPlan;
    {
      LogisticChain hubChainLeft;
      {
        Carrier mainCarrierLeft =
            CarriersUtils.createCarrier(Id.create("mainCarrierLeft", Carrier.class));
        mainCarrierLeft
            .getCarrierCapabilities()
            .setFleetSize(CarrierCapabilities.FleetSize.INFINITE);

        CarriersUtils.addCarrierVehicle(
            mainCarrierLeft,
            CarrierVehicle.newInstance(
                Id.createVehicleId("mainTruck"), DEPOT_LINK_ID, VEH_TYPE_LARGE_50));
        LSPResource mainCarrierResourceLeft =
            ResourceImplementationUtils.MainRunCarrierResourceBuilder.newInstance(mainCarrierLeft)
                .setFromLinkId(DEPOT_LINK_ID)
                .setMainRunCarrierScheduler(
                    ResourceImplementationUtils.createDefaultMainRunCarrierScheduler(scenario))
                .setToLinkId(HUB_LEFT_LINK_ID)
                .setVehicleReturn(ResourceImplementationUtils.VehicleReturn.returnToFromLink)
                .build();

        LogisticChainElement mainCarrierElementLeft =
            LSPUtils.LogisticChainElementBuilder.newInstance(
                    Id.create("mainCarrierElementLeft", LogisticChainElement.class))
                .setResource(mainCarrierResourceLeft)
                .build();

        LSPResourceScheduler hubSchedulerLeft =
            ResourceImplementationUtils.TranshipmentHubSchedulerBuilder.newInstance()
                .setCapacityNeedFixed(10)
                .setCapacityNeedLinear(1)
                .build();

        LSPResource hubResourceLeft =
            ResourceImplementationUtils.TransshipmentHubBuilder.newInstance(
                    Id.create("HubLeft", LSPResource.class), HUB_LEFT_LINK_ID, scenario)
                .setTransshipmentHubScheduler(hubSchedulerLeft)
                .build();
        LSPUtils.setFixedCost(hubResourceLeft, HUBCOSTS_FIX);

        LogisticChainElement hubElementLeft =
            LSPUtils.LogisticChainElementBuilder.newInstance(
                    Id.create("HubElement", LogisticChainElement.class))
                .setResource(hubResourceLeft)
                .build();

        Carrier distributionCarrierLeft =
            CarriersUtils.createCarrier(Id.create("distributionCarrierLeft", Carrier.class));
        distributionCarrierLeft
            .getCarrierCapabilities()
            .setFleetSize(CarrierCapabilities.FleetSize.INFINITE);

        CarriersUtils.addCarrierVehicle(
            distributionCarrierLeft,
            CarrierVehicle.newInstance(
                Id.createVehicleId("distributionTruck"), HUB_LEFT_LINK_ID, VEH_TYPE_SMALL_05));
        LSPResource distributionCarrierResourceLeft =
            ResourceImplementationUtils.DistributionCarrierResourceBuilder.newInstance(
                    distributionCarrierLeft)
                .setDistributionScheduler(
                    ResourceImplementationUtils.createDefaultDistributionCarrierScheduler(scenario))
                .build();

        LogisticChainElement distributionCarrierElementLeft =
            LSPUtils.LogisticChainElementBuilder.newInstance(
                    Id.create("distributionCarrierElementLeft", LogisticChainElement.class))
                .setResource(distributionCarrierResourceLeft)
                .build();

        mainCarrierElementLeft.connectWithNextElement(hubElementLeft);
        hubElementLeft.connectWithNextElement(distributionCarrierElementLeft);

        hubChainLeft =
            LSPUtils.LogisticChainBuilder.newInstance(
                    Id.create("hubChainLeft", LogisticChain.class))
                .addLogisticChainElement(mainCarrierElementLeft)
                .addLogisticChainElement(hubElementLeft)
                .addLogisticChainElement(distributionCarrierElementLeft)
                .build();
      }

      LogisticChain hubChainRight;
      {
        Carrier mainCarrier = CarriersUtils.createCarrier(Id.create("mainCarrier", Carrier.class));
        mainCarrier.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);

        CarriersUtils.addCarrierVehicle(
            mainCarrier,
            CarrierVehicle.newInstance(
                Id.createVehicleId("mainTruck"), DEPOT_LINK_ID, VEH_TYPE_LARGE_50));
        LSPResource mainCarrierResource =
            ResourceImplementationUtils.MainRunCarrierResourceBuilder.newInstance(mainCarrier)
                .setFromLinkId(DEPOT_LINK_ID)
                .setMainRunCarrierScheduler(
                    ResourceImplementationUtils.createDefaultMainRunCarrierScheduler(scenario))
                .setToLinkId(HUB_RIGHT_LINK_ID)
                .setVehicleReturn(ResourceImplementationUtils.VehicleReturn.returnToFromLink)
                .build();

        LogisticChainElement mainCarrierElement =
            LSPUtils.LogisticChainElementBuilder.newInstance(
                    Id.create("mainCarrierElement", LogisticChainElement.class))
                .setResource(mainCarrierResource)
                .build();

        LSPResourceScheduler hubScheduler =
            ResourceImplementationUtils.TranshipmentHubSchedulerBuilder.newInstance()
                .setCapacityNeedFixed(10)
                .setCapacityNeedLinear(1)
                .build();

        LSPResource hubResourceRight =
            ResourceImplementationUtils.TransshipmentHubBuilder.newInstance(
                    Id.create("HubRight", LSPResource.class), HUB_RIGHT_LINK_ID, scenario)
                .setTransshipmentHubScheduler(hubScheduler)
                .build();
        LSPUtils.setFixedCost(hubResourceRight, HUBCOSTS_FIX);

        LogisticChainElement hubElementRight =
            LSPUtils.LogisticChainElementBuilder.newInstance(
                    Id.create("HubElement", LogisticChainElement.class))
                .setResource(hubResourceRight)
                .build();

        Carrier distributionCarrier =
            CarriersUtils.createCarrier(Id.create("distributionCarrier", Carrier.class));
        distributionCarrier
            .getCarrierCapabilities()
            .setFleetSize(CarrierCapabilities.FleetSize.INFINITE);

        CarriersUtils.addCarrierVehicle(
            distributionCarrier,
            CarrierVehicle.newInstance(
                Id.createVehicleId("distributionTruck"), HUB_RIGHT_LINK_ID, VEH_TYPE_SMALL_05));
        LSPResource distributionCarrierResource =
            ResourceImplementationUtils.DistributionCarrierResourceBuilder.newInstance(
                    distributionCarrier)
                .setDistributionScheduler(
                    ResourceImplementationUtils.createDefaultDistributionCarrierScheduler(scenario))
                .build();

        LogisticChainElement distributionCarrierElement =
            LSPUtils.LogisticChainElementBuilder.newInstance(
                    Id.create("distributionCarrierElement", LogisticChainElement.class))
                .setResource(distributionCarrierResource)
                .build();

        mainCarrierElement.connectWithNextElement(hubElementRight);
        hubElementRight.connectWithNextElement(distributionCarrierElement);

        hubChainRight =
            LSPUtils.LogisticChainBuilder.newInstance(
                    Id.create("hubChainRight", LogisticChain.class))
                .addLogisticChainElement(mainCarrierElement)
                .addLogisticChainElement(hubElementRight)
                .addLogisticChainElement(distributionCarrierElement)
                .build();
      }

      multipleTwoEchelonChainsPlan =
          LSPUtils.createLSPPlan()
              .addLogisticChain(hubChainLeft)
              .addLogisticChain(hubChainRight)
              .setInitialShipmentAssigner(MultipleChainsUtils.createRandomLogisticChainShipmentAssigner());
    }

    List<LSPPlan> lspPlans = new ArrayList<>();
    lspPlans.add(multipleTwoEchelonChainsPlan);

    LSP lsp =
        LSPUtils.LSPBuilder.getInstance(Id.create("myLSP", LSP.class))
            .setInitialPlan(multipleTwoEchelonChainsPlan)
            .setLogisticChainScheduler(
                ResourceImplementationUtils.createDefaultSimpleForwardLogisticChainScheduler(
                    createResourcesListFromLSPPlans(lspPlans)))
            .build();

    log.info("create initial LSPShipments");
    log.info("assign the shipments to the LSP");
    for (LspShipment lspShipment : createInitialLSPShipments()) {
      lsp.assignShipmentToLSP(lspShipment);
    }

    log.info("schedule the LSP with the shipments and according to the scheduler of the Resource");
    lsp.scheduleLogisticChains();

    return lsp;
  }

  private static Collection<LspShipment> createInitialLSPShipments() {
    List<LspShipment> shipmentList = new ArrayList<>();
    int capacityDemand = 1;

    for (int i = 1; i <= 10; i++) {
      if (i % 2 != 0) {
        Id<LspShipment> id = Id.create("ShipmentLeft_" + i, LspShipment.class);
        LspShipmentUtils.LspShipmentBuilder builder = LspShipmentUtils.LspShipmentBuilder.newInstance(id);

        builder.setCapacityDemand(capacityDemand);
        builder.setFromLinkId(DEPOT_LINK_ID);
        final Id<Link> shipmentLeftLinkId = Id.createLinkId("i(1,9)R");
        builder.setToLinkId(shipmentLeftLinkId);

        builder.setEndTimeWindow(TimeWindow.newInstance(0, (24 * 3600)));
        builder.setStartTimeWindow(TimeWindow.newInstance(0, (24 * 3600)));
        builder.setDeliveryServiceTime(capacityDemand * 60);

        shipmentList.add(builder.build());
      } else {
        Id<LspShipment> id = Id.create("ShipmentRight_" + i, LspShipment.class);
        LspShipmentUtils.LspShipmentBuilder builder = LspShipmentUtils.LspShipmentBuilder.newInstance(id);

        builder.setCapacityDemand(capacityDemand);
        builder.setFromLinkId(DEPOT_LINK_ID);
        final Id<Link> shipmentRightLinkId = Id.createLinkId("j(9,9)");
        builder.setToLinkId(shipmentRightLinkId);

        builder.setEndTimeWindow(TimeWindow.newInstance(0, (24 * 3600)));
        builder.setStartTimeWindow(TimeWindow.newInstance(0, (24 * 3600)));
        builder.setDeliveryServiceTime(capacityDemand * 60);

        shipmentList.add(builder.build());
      }
    }
    return shipmentList;
  }

  private static List<LSPResource> createResourcesListFromLSPPlans(List<LSPPlan> lspPlans) {
    log.info("Collecting all LSPResources from the LSPPlans");
    List<LSPResource> resourceList = new ArrayList<>();
    for (LSPPlan lspPlan : lspPlans) {
      for (LogisticChain logisticChain : lspPlan.getLogisticChains()) {
        for (LogisticChainElement logisticChainElement : logisticChain.getLogisticChainElements()) {
          resourceList.add(logisticChainElement.getResource());
        }
      }
    }
    return resourceList;
  }
}
