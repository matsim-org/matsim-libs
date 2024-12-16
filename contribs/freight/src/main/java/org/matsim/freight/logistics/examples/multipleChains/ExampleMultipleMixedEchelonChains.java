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

import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.*;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.controller.CarrierControllerUtils;
import org.matsim.freight.carriers.controller.CarrierScoringFunctionFactory;
import org.matsim.freight.carriers.controller.CarrierStrategyManager;
import org.matsim.freight.logistics.*;
import org.matsim.freight.logistics.examples.ExampleConstants;
import org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils;
import org.matsim.freight.logistics.shipment.LspShipment;
import org.matsim.freight.logistics.shipment.LspShipmentUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

final class ExampleMultipleMixedEchelonChains {

  static final double HUBCOSTS_FIX = 100;
  private static final Logger log = LogManager.getLogger(ExampleMultipleMixedEchelonChains.class);
  private static final double TOLL_VALUE = 1000;
  private static final Id<Link> DEPOT_LINK_ID = Id.createLinkId("i(5,0)");
  private static final Id<Link> HUB_LINK_ID = Id.createLinkId("j(5,3)");
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

  private ExampleMultipleMixedEchelonChains() {}

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
            carrierScorer.setToll(TOLL_VALUE);
            carrierScorer.setTolledVehicleTypes( List.of("heavy40t"));
            carrierScorer.setTolledLinks(ExampleConstants.TOLLED_LINK_LIST_BERLIN_BOTH_DIRECTIONS);
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
                          new GenericPlanStrategyImpl<>(new BestPlanSelector<>()), null, 1);
                      return strategyManager;
                    });
          }
        });

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
      config.controller().setOutputDirectory("output/multipleMixedEchelonChains_twoPlans");
      config.controller().setLastIteration(2);
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

    LSPPlan singleTwoEchelonChainPlan;
    {
      LogisticChain hubChain1;
      {
        Carrier mainCarrier1 = CarriersUtils.createCarrier(Id.create("mainCarrier", Carrier.class));
        mainCarrier1.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);

        CarriersUtils.addCarrierVehicle(
            mainCarrier1,
            CarrierVehicle.newInstance(
                Id.createVehicleId("mainTruck"), DEPOT_LINK_ID, VEH_TYPE_LARGE_50));
        LSPResource mainCarrierResource1 =
            ResourceImplementationUtils.MainRunCarrierResourceBuilder.newInstance(
                    mainCarrier1)
                .setFromLinkId(DEPOT_LINK_ID)
                .setMainRunCarrierScheduler(
                    ResourceImplementationUtils.createDefaultMainRunCarrierScheduler(scenario))
                .setToLinkId(HUB_LINK_ID)
                .setVehicleReturn(ResourceImplementationUtils.VehicleReturn.returnToFromLink)
                .build();

        LogisticChainElement mainCarrierElement1 =
            LSPUtils.LogisticChainElementBuilder.newInstance(
                    Id.create("mainCarrierElement", LogisticChainElement.class))
                .setResource(mainCarrierResource1)
                .build();

        LSPResourceScheduler hubScheduler1 =
            ResourceImplementationUtils.TranshipmentHubSchedulerBuilder.newInstance()
                .setCapacityNeedFixed(10)
                .setCapacityNeedLinear(1)
                .build();

        LSPResource hubResource1 =
            ResourceImplementationUtils.TransshipmentHubBuilder.newInstance(
                    Id.create("Hub", LSPResource.class), HUB_LINK_ID, scenario)
                .setTransshipmentHubScheduler(hubScheduler1)
                .build();
        LSPUtils.setFixedCost(hubResource1, HUBCOSTS_FIX);

        LogisticChainElement hubElement1 =
            LSPUtils.LogisticChainElementBuilder.newInstance(
                    Id.create("HubElement", LogisticChainElement.class))
                .setResource(hubResource1)
                .build();

        Carrier distributionCarrier1 =
            CarriersUtils.createCarrier(Id.create("distributionCarrier", Carrier.class));
        distributionCarrier1
            .getCarrierCapabilities()
            .setFleetSize(CarrierCapabilities.FleetSize.INFINITE);

        CarriersUtils.addCarrierVehicle(
            distributionCarrier1,
            CarrierVehicle.newInstance(
                Id.createVehicleId("distributionTruck"), HUB_LINK_ID, VEH_TYPE_SMALL_05));
        LSPResource distributionCarrierResource1 =
            ResourceImplementationUtils.DistributionCarrierResourceBuilder.newInstance(
                    distributionCarrier1)
                .setDistributionScheduler(
                    ResourceImplementationUtils.createDefaultDistributionCarrierScheduler(scenario))
                .build();

        LogisticChainElement distributionCarrierElement1 =
            LSPUtils.LogisticChainElementBuilder.newInstance(
                    Id.create("distributionCarrierElement", LogisticChainElement.class))
                .setResource(distributionCarrierResource1)
                .build();

        mainCarrierElement1.connectWithNextElement(hubElement1);
        hubElement1.connectWithNextElement(distributionCarrierElement1);

        hubChain1 =
            LSPUtils.LogisticChainBuilder.newInstance(Id.create("hubChain", LogisticChain.class))
                .addLogisticChainElement(mainCarrierElement1)
                .addLogisticChainElement(hubElement1)
                .addLogisticChainElement(distributionCarrierElement1)
                .build();
      }

      singleTwoEchelonChainPlan =
          LSPUtils.createLSPPlan()
              .addLogisticChain(hubChain1)
              .setInitialShipmentAssigner(
                  MultipleChainsUtils.createPrimaryLogisticChainShipmentAssigner());
    }

    // A plan with a direct chain and a hub chain is created
    LSPPlan multipleMixedEchelonChainsPlan;
    {
      LogisticChain directChain;
      {
        Carrier singleCarrier =
            CarriersUtils.createCarrier(Id.create("singleCarrier", Carrier.class));
        singleCarrier.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);

        CarriersUtils.addCarrierVehicle(
            singleCarrier,
            CarrierVehicle.newInstance(
                Id.createVehicleId("singleCarrier"), DEPOT_LINK_ID, VEH_TYPE_SMALL_05));
        LSPResource singleCarrierResource =
            ResourceImplementationUtils.DistributionCarrierResourceBuilder.newInstance(
                    singleCarrier)
                .setDistributionScheduler(
                    ResourceImplementationUtils.createDefaultDistributionCarrierScheduler(scenario))
                .build();

        LogisticChainElement singleCarrierElement =
            LSPUtils.LogisticChainElementBuilder.newInstance(
                    Id.create("directCarrierElement", LogisticChainElement.class))
                .setResource(singleCarrierResource)
                .build();

        directChain =
            LSPUtils.LogisticChainBuilder.newInstance(Id.create("directChain", LogisticChain.class))
                .addLogisticChainElement(singleCarrierElement)
                .build();
      }

      LogisticChain hubChain;
      {
        Carrier mainCarrier = CarriersUtils.createCarrier(Id.create("mainCarrier", Carrier.class));
        mainCarrier.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);

        CarriersUtils.addCarrierVehicle(
            mainCarrier,
            CarrierVehicle.newInstance(
                Id.createVehicleId("mainTruck"), DEPOT_LINK_ID, VEH_TYPE_LARGE_50));
        LSPResource mainCarrierResource =
            ResourceImplementationUtils.MainRunCarrierResourceBuilder.newInstance(
                    mainCarrier)
                .setFromLinkId(DEPOT_LINK_ID)
                .setMainRunCarrierScheduler(
                    ResourceImplementationUtils.createDefaultMainRunCarrierScheduler(scenario))
                .setToLinkId(HUB_LINK_ID)
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

        LSPResource hubResource =
            ResourceImplementationUtils.TransshipmentHubBuilder.newInstance(
                    Id.create("Hub", LSPResource.class), HUB_LINK_ID, scenario)
                .setTransshipmentHubScheduler(hubScheduler)
                .build();
        LSPUtils.setFixedCost(hubResource, HUBCOSTS_FIX);

        LogisticChainElement hubElement =
            LSPUtils.LogisticChainElementBuilder.newInstance(
                    Id.create("HubElement", LogisticChainElement.class))
                .setResource(hubResource)
                .build();

        Carrier distributionCarrier =
            CarriersUtils.createCarrier(Id.create("distributionCarrier", Carrier.class));
        distributionCarrier
            .getCarrierCapabilities()
            .setFleetSize(CarrierCapabilities.FleetSize.INFINITE);

        CarriersUtils.addCarrierVehicle(
            distributionCarrier,
            CarrierVehicle.newInstance(
                Id.createVehicleId("distributionTruck"), HUB_LINK_ID, VEH_TYPE_SMALL_05));
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

        mainCarrierElement.connectWithNextElement(hubElement);
        hubElement.connectWithNextElement(distributionCarrierElement);

        hubChain =
            LSPUtils.LogisticChainBuilder.newInstance(Id.create("hubChain", LogisticChain.class))
                .addLogisticChainElement(mainCarrierElement)
                .addLogisticChainElement(hubElement)
                .addLogisticChainElement(distributionCarrierElement)
                .build();
      }

      multipleMixedEchelonChainsPlan =
          LSPUtils.createLSPPlan()
              .addLogisticChain(hubChain)
              .addLogisticChain(directChain)
              .setInitialShipmentAssigner(
                  MultipleChainsUtils.createRoundRobinLogisticChainShipmentAssigner());
    }

    List<LSPPlan> lspPlans = new ArrayList<>();
    lspPlans.add(singleTwoEchelonChainPlan);
    lspPlans.add(multipleMixedEchelonChainsPlan);

    LSP lsp =
        LSPUtils.LSPBuilder.getInstance(Id.create("myLSP", LSP.class))
            .setInitialPlan(singleTwoEchelonChainPlan)
            .setLogisticChainScheduler(
                ResourceImplementationUtils.createDefaultSimpleForwardLogisticChainScheduler(
                    createResourcesListFromLSPPlans(lspPlans)))
            .build();

    log.info("create initial LSPShipments");
    log.info("assign the shipments to the LSP");
    for (LspShipment lspShipment : createInitialLSPShipments(network)) {
      lsp.assignShipmentToLSP(lspShipment);
    }

    log.info("schedule the LSP with the shipments and according to the scheduler of the Resource");
    lsp.scheduleLogisticChains();

    return lsp;
  }

  private static Collection<LspShipment> createInitialLSPShipments(Network network) {
    List<LspShipment> shipmentList = new ArrayList<>();

    Random rand = MatsimRandom.getLocalInstance();

    int capacityDemand = 1;

    List<String> zoneLinkList =
        Arrays.asList(
            "i(4,4)", "i(5,4)", "i(6,4)", "i(4,6)", "i(5,6)", "i(6,6)", "j(3,5)", "j(3,6)",
            "j(3,7)", "j(5,5)", "j(5,6)", "j(5,7)", "i(4,5)R", "i(5,5)R", "i(6,5)R", "i(4,7)R",
            "i(5,7)R", "i(6,7)R", "j(4,5)R", "j(4,6)R", "j(4,7)R", "j(6,5)R", "j(6,6)R", "j(6,7)R");
    for (String linkIdString : zoneLinkList) {
      if (!network.getLinks().containsKey(Id.createLinkId(linkIdString))) {
        throw new RuntimeException("Link is not in Network!");
      }
    }

    for (int i = 1; i <= 10; i++) {
      if (i % 2 != 0) {
        Id<LspShipment> id = Id.create("ShipmentInside_" + i, LspShipment.class);
        LspShipmentUtils.LspShipmentBuilder builder = LspShipmentUtils.LspShipmentBuilder.newInstance(id);

        builder.setCapacityDemand(capacityDemand);
        builder.setFromLinkId(DEPOT_LINK_ID);
        final Id<Link> toLinkId =
            Id.createLinkId(zoneLinkList.get(rand.nextInt(zoneLinkList.size() - 1)));
        builder.setToLinkId(toLinkId);

        builder.setEndTimeWindow(TimeWindow.newInstance(0, (24 * 3600)));
        builder.setStartTimeWindow(TimeWindow.newInstance(0, (24 * 3600)));
        builder.setDeliveryServiceTime(capacityDemand * 60);

        shipmentList.add(builder.build());
      } else {
        Id<LspShipment> id = Id.create("ShipmentOutside_" + i, LspShipment.class);
        LspShipmentUtils.LspShipmentBuilder builder = LspShipmentUtils.LspShipmentBuilder.newInstance(id);

        builder.setCapacityDemand(capacityDemand);
        builder.setFromLinkId(DEPOT_LINK_ID);
        final Id<Link> toLinkId = Id.createLinkId("i(9,0)");
        builder.setToLinkId(toLinkId);

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

  enum AssignerSetting {
    primary,
    roundRobin
  }
}
