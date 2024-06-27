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

package org.matsim.freight.logistics.example.lsp.multipleChains;

import static org.matsim.freight.logistics.example.lsp.multipleChains.MultipleChainsUtils.createLSPShipmentsFromCarrierShipments;

import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.GenericWorstPlanForRemovalSelector;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.controler.CarrierControlerUtils;
import org.matsim.freight.carriers.controler.CarrierScoringFunctionFactory;
import org.matsim.freight.carriers.controler.CarrierStrategyManager;
import org.matsim.freight.logistics.*;
import org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils;
import org.matsim.freight.logistics.shipment.LSPShipment;
import org.matsim.vehicles.VehicleType;


/**
* This bases on {@link ExampleGroceryDeliveryMultipleChains}.
 *  Now it will include two different LSPs
 *
*/
final class ExampleTwoLspsGroceryDeliveryMultipleChains {

  private static final Logger log =
      LogManager.getLogger(ExampleTwoLspsGroceryDeliveryMultipleChains.class);
  private static final Id<Link> HUB_LINK_ID_NEUKOELLN = Id.createLinkId("91085");
  private static final double TOLL_VALUE = 1000;
  static double HUBCOSTS_FIX = 100;

  private ExampleTwoLspsGroceryDeliveryMultipleChains() {}

  public static void main(String[] args) {
    log.info("Prepare config");
    Config config = prepareConfig(args);

    log.info("Prepare scenario");
    Scenario scenario = prepareScenario(config);

    log.info("Prepare controler");
    Controler controler = new Controler(scenario);
    controler.addOverridingModule(
        new AbstractModule() {
          @Override
          public void install() {
            install(new LSPModule());
          }
        });

    controler.addOverridingModule(
        new AbstractModule() {
          @Override
          public void install() {
            final EventBasedCarrierScorer_MultipleChains carrierScorer =
                new EventBasedCarrierScorer_MultipleChains();
            carrierScorer.setToll(TOLL_VALUE);
            bind(CarrierScoringFunctionFactory.class).toInstance(carrierScorer);
            bind(LSPScorerFactory.class).toInstance(MyLSPScorer::new);
            bind(CarrierStrategyManager.class)
                .toProvider(
                    () -> {
                      CarrierStrategyManager strategyManager =
                          CarrierControlerUtils.createDefaultCarrierStrategyManager();
                      strategyManager.addStrategy(
                          new GenericPlanStrategyImpl<>(new BestPlanSelector<>()), null, 1);
                      return strategyManager;
                    });
            bind(LSPStrategyManager.class)
                .toProvider(
                    () -> {
                      LSPStrategyManager strategyManager = new LSPStrategyManagerImpl();
                      strategyManager.addStrategy(new GenericPlanStrategyImpl<>(new ExpBetaPlanSelector<>(new ScoringConfigGroup())), null, 1);
                      //					strategyManager.addStrategy(new
                      // RebalancingShipmentsStrategyFactory().createStrategy(), null, 2);
                      strategyManager.addStrategy(new RandomShiftingStrategyFactory().createStrategy(), null, 4);
                      //					strategyManager.addStrategy(new
                      // ProximityStrategyFactory(scenario.getNetwork()).createStrategy(), null, 1);
                      strategyManager.setMaxPlansPerAgent(5);
                      strategyManager.setPlanSelectorForRemoval(new GenericWorstPlanForRemovalSelector<>());
                      return strategyManager;
                    });
          }
        });

    log.info("Run MATSim");

    // The VSP default settings are designed for person transport simulation. After talking to Kai,
    // they will be set to WARN here. Kai MT may'23
    controler
        .getConfig()
        .vspExperimental()
        .setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);
    controler.run();

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
      config.controller().setOutputDirectory("output/groceryDelivery_kmt2");
      config.controller().setLastIteration(10);
    }

    config.network().setInputFile(
            "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz");
    config.global().setCoordinateSystem("EPSG:31468");
    config.global().setRandomSeed(4177);
    config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
    config.controller().setWriteEventsInterval(1);

    FreightCarriersConfigGroup freightConfig = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
    freightConfig.setTimeWindowHandling(FreightCarriersConfigGroup.TimeWindowHandling.ignore);

    return config;
  }

  private static Scenario prepareScenario(Config config) {
    Scenario scenario = ScenarioUtils.loadScenario(config);

    log.info("Add LSP to the scenario");
    Collection<LSP> lsps = new LinkedList<>();
    lsps.add(createLSP(scenario, "myLSP2", "edeka_SUPERMARKT_TROCKEN", HUB_LINK_ID_NEUKOELLN));
    lsps.add(createLSP(scenario, "myLSP1", "kaufland_VERBRAUCHERMARKT_TROCKEN", HUB_LINK_ID_NEUKOELLN));
    LSPUtils.addLSPs(scenario, new LSPs(lsps));

    return scenario;
  }

/**
*
 * @param scenario
 * @param lspName String of LSP's Id
 * @param carrierIdString Name of the carrier, the (lsp's) demand (shipments) are created from.
 * @param hubLinkId
 * @return
*/
  private static LSP createLSP(Scenario scenario, String lspName, String carrierIdString, Id<Link> hubLinkId) {
    String carrierPlanFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/freight/foodRetailing_wo_rangeConstraint/input/CarrierLEH_v2_withFleet_Shipment_OneTW_PickupTime_ICEVandBEV.xml";
    String vehicleTypeFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/freight/foodRetailing_wo_rangeConstraint/input/vehicleTypesBVWP100_DC_noTax.xml";

    CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes();
    CarrierVehicleTypeReader vehicleTypeReader = new CarrierVehicleTypeReader(vehicleTypes);
    vehicleTypeReader.readFile(vehicleTypeFile);

    Carriers carriers = new Carriers();
    CarrierPlanXmlReader carrierReader = new CarrierPlanXmlReader(carriers, vehicleTypes);
    carrierReader.readFile(carrierPlanFile);

    Carrier carrier = carriers.getCarriers()
            .get(Id.create(carrierIdString, CarrierImpl.class));
    //		Id<Link> depotLinkFromShipments =
    // carrier.getShipments().values().iterator().next().getFrom();
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
        Carrier singleCarrier = CarriersUtils.createCarrier(Id.create(lspName+"_singleCarrier", Carrier.class));
        singleCarrier.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);

        CarriersUtils.addCarrierVehicle(singleCarrier,
            CarrierVehicle.newInstance(
                Id.createVehicleId("singleCarrierTruck"),
                depotLinkFromVehicles,
                vehicleTypes.getVehicleTypes().get(Id.create("heavy40t", VehicleType.class))));
        LSPResource singleCarrierResource =
            ResourceImplementationUtils.DistributionCarrierResourceBuilder.newInstance(
                    singleCarrier, scenario.getNetwork())
                .setDistributionScheduler(
                    ResourceImplementationUtils.createDefaultDistributionCarrierScheduler())
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
        Carrier mainCarrier = CarriersUtils.createCarrier(Id.create(lspName+"_mainCarrier", Carrier.class));
        mainCarrier.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);

        CarriersUtils.addCarrierVehicle(
            mainCarrier,
            CarrierVehicle.newInstance(
                Id.createVehicleId("mainTruck"),
                depotLinkFromVehicles,
                vehicleTypes.getVehicleTypes().get(Id.create("heavy40t", VehicleType.class))));
        LSPResource mainCarrierResource =
            ResourceImplementationUtils.MainRunCarrierResourceBuilder.newInstance(
                    mainCarrier, scenario.getNetwork())
                .setFromLinkId(depotLinkFromVehicles)
                .setMainRunCarrierScheduler(
                    ResourceImplementationUtils.createDefaultMainRunCarrierScheduler())
                .setToLinkId(hubLinkId)
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
                    Id.create(lspName+"_Hub", LSPResource.class), hubLinkId, scenario)
                .setTransshipmentHubScheduler(hubScheduler)
                .build();

        LSPUtils.setFixedCost(hubResource, HUBCOSTS_FIX);

        LogisticChainElement hubElement =
            LSPUtils.LogisticChainElementBuilder.newInstance(
                    Id.create("HubElement", LogisticChainElement.class))
                .setResource(hubResource)
                .build();

        Carrier distributionCarrier =
            CarriersUtils.createCarrier(Id.create(lspName+"_distributionCarrier", Carrier.class));
        distributionCarrier
            .getCarrierCapabilities()
            .setFleetSize(CarrierCapabilities.FleetSize.INFINITE);

        CarriersUtils.addCarrierVehicle(
            distributionCarrier,
            CarrierVehicle.newInstance(
                Id.createVehicleId("distributionTruck"),
                    hubLinkId,
                vehicleTypes
                    .getVehicleTypes()
                    .get(Id.create("heavy40t_electro", VehicleType.class))));
        LSPResource distributionCarrierResource =
            ResourceImplementationUtils.DistributionCarrierResourceBuilder.newInstance(
                    distributionCarrier, scenario.getNetwork())
                .setDistributionScheduler(
                    ResourceImplementationUtils.createDefaultDistributionCarrierScheduler())
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
        LSPUtils.LSPBuilder.getInstance(Id.create(lspName, LSP.class))
            .setInitialPlan(multipleMixedEchelonChainsPlan)
            .setLogisticChainScheduler(
                ResourceImplementationUtils.createDefaultSimpleForwardLogisticChainScheduler(
                    createResourcesListFromLSPPlans(lspPlans)))
            .build();

    log.info("create initial LSPShipments");
    log.info("assign the shipments to the LSP");
    for (LSPShipment shipment : createLSPShipmentsFromCarrierShipments(carrier)) {
      lsp.assignShipmentToLSP(shipment);
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
