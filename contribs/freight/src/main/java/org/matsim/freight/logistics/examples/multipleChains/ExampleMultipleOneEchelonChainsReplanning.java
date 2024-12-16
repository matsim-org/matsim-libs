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

final class ExampleMultipleOneEchelonChainsReplanning {

  private static final Logger log =
      LogManager.getLogger(ExampleMultipleOneEchelonChainsReplanning.class);

  private static final Id<Link> DEPOT_LINK_ID = Id.createLinkId("i(5,0)");

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

  private ExampleMultipleOneEchelonChainsReplanning() {}

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

    controller.addOverridingModule(
        new AbstractModule() {
          @Override
          public void install() {
            final EventBasedCarrierScorer4MultipleChains carrierScorer =
                new EventBasedCarrierScorer4MultipleChains();
            bind(CarrierScoringFunctionFactory.class).toInstance(carrierScorer);
            bind(LSPScorerFactory.class).toInstance(MyLSPScorer::new);
            bind(CarrierStrategyManager.class)
                .toProvider(
                    () -> {
                      CarrierStrategyManager strategyManager =
                          CarrierControllerUtils.createDefaultCarrierStrategyManager();
                      strategyManager.addStrategy(
                          new GenericPlanStrategyImpl<>(new BestPlanSelector<>()), null, 1);
                      return strategyManager;
                    });
            bind(LSPStrategyManager.class)
                .toProvider(
                    () -> {
                      LSPStrategyManager strategyManager = new LSPStrategyManagerImpl();
                      strategyManager.addStrategy(
                          new GenericPlanStrategyImpl<>(
                              new ExpBetaPlanSelector<>(new ScoringConfigGroup())),
                          null,
                          1);
                        strategyManager.addStrategy(
                          RandomShiftingStrategyFactory.createStrategy(), null, 1);
                      //
                      //	strategyManager.addStrategy(ProximityStrategyFactory.createStrategy(scenario.getNetwork()), null, 1);
                      strategyManager.setMaxPlansPerAgent(5);
                      strategyManager.setPlanSelectorForRemoval(
                          new GenericWorstPlanForRemovalSelector<>());
                      return strategyManager;
                    });
          }
        });

    // TODO: Innovation switch not working
    config.replanning().setFractionOfIterationsToDisableInnovation(0.8);

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
      config.controller().setOutputDirectory("output/multipleOneEchelonChainsReplanning");
      config.controller().setLastIteration(10);
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

    // A plan with one logistic chain, containing a single carrier is created
    LSPPlan singleOneEchelonChainPlan;
    {
      Carrier singleCarrier =
          CarriersUtils.createCarrier(Id.create("singleCarrier", Carrier.class));
      singleCarrier.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);

      CarriersUtils.addCarrierVehicle(
          singleCarrier,
          CarrierVehicle.newInstance(
              Id.createVehicleId("veh_large"), DEPOT_LINK_ID, VEH_TYPE_LARGE_50));
      LSPResource singleCarrierResource =
          ResourceImplementationUtils.DistributionCarrierResourceBuilder.newInstance(
                  singleCarrier)
              .setDistributionScheduler(
                  ResourceImplementationUtils.createDefaultDistributionCarrierScheduler(scenario))
              .build();

      LogisticChainElement singleCarrierElement =
          LSPUtils.LogisticChainElementBuilder.newInstance(
                  Id.create("singleCarrierElement", LogisticChainElement.class))
              .setResource(singleCarrierResource)
              .build();

      LogisticChain singleChain =
          LSPUtils.LogisticChainBuilder.newInstance(Id.create("singleChain", LogisticChain.class))
              .addLogisticChainElement(singleCarrierElement)
              .build();

      final InitialShipmentAssigner singleSolutionShipmentAssigner =
          MultipleChainsUtils.createPrimaryLogisticChainShipmentAssigner();
      singleOneEchelonChainPlan =
          LSPUtils.createLSPPlan()
              .addLogisticChain(singleChain)
              .setInitialShipmentAssigner(singleSolutionShipmentAssigner);

      singleOneEchelonChainPlan.setType(
          MultipleChainsUtils.LspPlanTypes.SINGLE_ONE_ECHELON_CHAIN.toString());
    }

    // A plan with two different logistic chains on the left and right, with respective carriers is
    // created
    LSPPlan multipleOneEchelonChainsPlan;
    {
      LogisticChainElement leftCarrierElement;
      {
        Carrier carrierLeft = CarriersUtils.createCarrier(Id.create("carrierLeft", Carrier.class));
        carrierLeft.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);

        CarriersUtils.addCarrierVehicle(
            carrierLeft,
            CarrierVehicle.newInstance(
                Id.createVehicleId("veh_small"), DEPOT_LINK_ID, VEH_TYPE_SMALL_05));
        LSPResource carrierLeftResource =
            ResourceImplementationUtils.DistributionCarrierResourceBuilder.newInstance(
                    carrierLeft)
                .setDistributionScheduler(
                    ResourceImplementationUtils.createDefaultDistributionCarrierScheduler(scenario))
                .build();

        leftCarrierElement =
            LSPUtils.LogisticChainElementBuilder.newInstance(
                    Id.create("leftCarrierElement", LogisticChainElement.class))
                .setResource(carrierLeftResource)
                .build();
      }

      LogisticChainElement rightCarrierElement;
      {
        Carrier carrierRight =
            CarriersUtils.createCarrier(Id.create("carrierRight", Carrier.class));
        carrierRight.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);

        CarriersUtils.addCarrierVehicle(
            carrierRight,
            CarrierVehicle.newInstance(
                Id.createVehicleId("veh_small"), DEPOT_LINK_ID, VEH_TYPE_SMALL_05));
        LSPResource carrierRightResource =
            ResourceImplementationUtils.DistributionCarrierResourceBuilder.newInstance(
                    carrierRight)
                .setDistributionScheduler(
                    ResourceImplementationUtils.createDefaultDistributionCarrierScheduler(scenario))
                .build();

        rightCarrierElement =
            LSPUtils.LogisticChainElementBuilder.newInstance(
                    Id.create("rightCarrierElement", LogisticChainElement.class))
                .setResource(carrierRightResource)
                .build();
      }

      LogisticChain leftChain =
          LSPUtils.LogisticChainBuilder.newInstance(Id.create("leftChain", LogisticChain.class))
              .addLogisticChainElement(leftCarrierElement)
              .build();

      LogisticChain rightChain =
          LSPUtils.LogisticChainBuilder.newInstance(Id.create("rightChain", LogisticChain.class))
              .addLogisticChainElement(rightCarrierElement)
              .build();

      final InitialShipmentAssigner shipmentAssigner =
          MultipleChainsUtils.createRandomLogisticChainShipmentAssigner();
      multipleOneEchelonChainsPlan =
          LSPUtils.createLSPPlan()
              .addLogisticChain(leftChain)
              .addLogisticChain(rightChain)
              .setInitialShipmentAssigner(shipmentAssigner);

      multipleOneEchelonChainsPlan.setType(
          MultipleChainsUtils.LspPlanTypes.MULTIPLE_ONE_ECHELON_CHAINS.toString());
    }

    List<LSPPlan> lspPlans = new ArrayList<>();
    lspPlans.add(singleOneEchelonChainPlan);
    lspPlans.add(multipleOneEchelonChainsPlan);

    LSP lsp =
        LSPUtils.LSPBuilder.getInstance(Id.create("myLSP", LSP.class))
            .setInitialPlan(singleOneEchelonChainPlan)
            .setLogisticChainScheduler(
                ResourceImplementationUtils.createDefaultSimpleForwardLogisticChainScheduler(
                    createResourcesListFromLSPPlans(lspPlans)))
            .build();
    lsp.addPlan(multipleOneEchelonChainsPlan);

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
