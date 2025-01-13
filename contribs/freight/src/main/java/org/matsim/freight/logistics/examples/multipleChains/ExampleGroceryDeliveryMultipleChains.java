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

import static org.matsim.freight.logistics.examples.multipleChains.MultipleChainsUtils.createLSPShipmentsFromCarrierShipments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
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
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.controller.CarrierControllerUtils;
import org.matsim.freight.carriers.controller.CarrierScoringFunctionFactory;
import org.matsim.freight.carriers.controller.CarrierStrategyManager;
import org.matsim.freight.logistics.*;
import org.matsim.freight.logistics.examples.ExampleConstants;
import org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils;
import org.matsim.freight.logistics.shipment.LspShipment;
import org.matsim.vehicles.VehicleType;

final class ExampleGroceryDeliveryMultipleChains {

  private static final Logger log =
      LogManager.getLogger(ExampleGroceryDeliveryMultipleChains.class);
  private static final Id<Link> HUB_LINK_ID = Id.createLinkId("91085");
  private static final double TOLL_VALUE = 1000;
  static final double HUBCOSTS_FIX = 100;
  private static final List<String> TOLLED_LINKS = ExampleConstants.TOLLED_LINK_LIST_BERLIN_BOTH_DIRECTIONS;

  private ExampleGroceryDeliveryMultipleChains() {}

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
            carrierScorer.setToll(TOLL_VALUE);
            carrierScorer.setTolledVehicleTypes( List.of("heavy40t"));
            carrierScorer.setTolledLinks(TOLLED_LINKS);
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
                      strategyManager.addStrategy(new GenericPlanStrategyImpl<>(new ExpBetaPlanSelector<>(new ScoringConfigGroup())), null, 1);
                        strategyManager.addStrategy(RandomShiftingStrategyFactory.createStrategy(), null, 1);
                        strategyManager.setMaxPlansPerAgent(5);
                      strategyManager.setPlanSelectorForRemoval(new GenericWorstPlanForRemovalSelector<>());
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
      config.controller().setOutputDirectory("output/groceryDelivery_kmt");
      config.controller().setLastIteration(20);
    }

    config.network().setInputFile(
            "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz");
    config.controller().setOverwriteFileSetting(
            OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
    config.controller().setWriteEventsInterval(1);

    FreightCarriersConfigGroup freightConfig = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
    freightConfig.setTimeWindowHandling(FreightCarriersConfigGroup.TimeWindowHandling.ignore);

    return config;
  }

  private static Scenario prepareScenario(Config config) {
    Scenario scenario = ScenarioUtils.loadScenario(config);

    log.info("Add LSP to the scenario");
    LSPUtils.addLSPs(scenario, new LSPs(Collections.singletonList(createLSP(scenario))));

    return scenario;
  }

  private static LSP createLSP(Scenario scenario) {
    String carrierPlanFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/freight/foodRetailing_wo_rangeConstraint/input/CarrierLEH_v2_withFleet_Shipment_OneTW_PickupTime_ICEVandBEV.xml";
    String vehicleTypeFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/freight/foodRetailing_wo_rangeConstraint/input/vehicleTypesBVWP100_DC_noTax.xml";

    CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes();
    CarrierVehicleTypeReader vehicleTypeReader = new CarrierVehicleTypeReader(vehicleTypes);
    vehicleTypeReader.readFile(vehicleTypeFile);

    Carriers carriers = new Carriers();
    CarrierPlanXmlReader carrierReader = new CarrierPlanXmlReader(carriers, vehicleTypes);
    carrierReader.readFile(carrierPlanFile);

    Carrier carrier = carriers.getCarriers()
            .get(Id.create("kaufland_VERBRAUCHERMARKT_TROCKEN", CarrierImpl.class));
      Id<Link> depotLinkFromVehicles = carrier
            .getCarrierCapabilities()
            .getCarrierVehicles()
            .values()
            .iterator()
            .next()
            .getLinkId();

    log.info("create LSP");

    LSPPlan multipleMixedEchelonChainsPlan;
    {
      LogisticChain directChain;
      {
        Carrier singleCarrier = CarriersUtils.createCarrier(Id.create("singleCarrier", Carrier.class));
        singleCarrier.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);

        CarriersUtils.addCarrierVehicle(singleCarrier,
            CarrierVehicle.newInstance(
                Id.createVehicleId("singleCarrier"),
                depotLinkFromVehicles,
                vehicleTypes.getVehicleTypes().get(Id.create("heavy40t", VehicleType.class))));
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
                Id.createVehicleId("mainTruck"),
                depotLinkFromVehicles,
                vehicleTypes.getVehicleTypes().get(Id.create("heavy40t", VehicleType.class))));
        LSPResource mainCarrierResource =
            ResourceImplementationUtils.MainRunCarrierResourceBuilder.newInstance(
                    mainCarrier)
                .setFromLinkId(depotLinkFromVehicles)
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
                Id.createVehicleId("distributionTruck"),
                HUB_LINK_ID,
                vehicleTypes
                    .getVehicleTypes()
                    .get(Id.create("heavy40t_electro", VehicleType.class))));
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
              .addLogisticChain(directChain)
              .addLogisticChain(hubChain)
              .setInitialShipmentAssigner(MultipleChainsUtils.createRandomLogisticChainShipmentAssigner());
    }

    List<LSPPlan> lspPlans = new ArrayList<>();
    lspPlans.add(multipleMixedEchelonChainsPlan);

    LSP lsp =
        LSPUtils.LSPBuilder.getInstance(Id.create("myLSP", LSP.class))
            .setInitialPlan(multipleMixedEchelonChainsPlan)
            .setLogisticChainScheduler(
                ResourceImplementationUtils.createDefaultSimpleForwardLogisticChainScheduler(
                    createResourcesListFromLSPPlans(lspPlans)))
            .build();

    log.info("create initial LSPShipments");
    log.info("assign the shipments to the LSP");
    for (LspShipment lspShipment : createLSPShipmentsFromCarrierShipments(carrier)) {
      lsp.assignShipmentToLSP(lspShipment);
    }

    log.info("schedule the LSP with the shipments and according to the scheduler of the Resource");
    lsp.scheduleLogisticChains();

    return lsp;
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
