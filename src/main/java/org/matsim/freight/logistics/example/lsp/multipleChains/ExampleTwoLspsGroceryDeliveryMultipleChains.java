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

  private static final Logger log = LogManager.getLogger(ExampleTwoLspsGroceryDeliveryMultipleChains.class);
  private static final Id<Link> HUB_LINK_ID_NEUKOELLN = Id.createLinkId("91085");
  private static final double TOLL_VALUE = 1000;
  private static final double HUBCOSTS_FIX = 100;
  private static final String CARRIER_PLAN_FILE = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/freight/foodRetailing_wo_rangeConstraint/input/CarrierLEH_v2_withFleet_Shipment_OneTW_PickupTime_ICEVandBEV.xml";
  private static final String VEHICLE_TYPE_FILE = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/freight/foodRetailing_wo_rangeConstraint/input/vehicleTypesBVWP100_DC_noTax.xml";
  public static final String EDEKA_SUPERMARKT_TROCKEN = "edeka_SUPERMARKT_TROCKEN";
  public static final String KAUFLAND_VERBRAUCHERMARKT_TROCKEN = "kaufland_VERBRAUCHERMARKT_TROCKEN";

  private ExampleTwoLspsGroceryDeliveryMultipleChains() {}

  public static void main(String[] args) {
    log.info("Prepare config");
    Config config = prepareConfig(args);

    log.info("Prepare scenario");
    Scenario scenario = ScenarioUtils.loadScenario(config);

    log.info("Add LSP(s) to the scenario");
    Collection<LSP> lsps = new LinkedList<>();
    lsps.add(createLspWithTwoChains(scenario, "myLSP2", EDEKA_SUPERMARKT_TROCKEN, HUB_LINK_ID_NEUKOELLN));
    lsps.add(createLspWithTwoChains(scenario, "myLSP1", KAUFLAND_VERBRAUCHERMARKT_TROCKEN, HUB_LINK_ID_NEUKOELLN));
    lsps.add(createLspWithDirectChain(scenario, "myLSP2_DIRECT", EDEKA_SUPERMARKT_TROCKEN));
    lsps.add(createLspWithDirectChain(scenario, "myLSP1_DIRECT", KAUFLAND_VERBRAUCHERMARKT_TROCKEN));
    LSPUtils.addLSPs(scenario, new LSPs(lsps));


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
                                  strategyManager.addStrategy(RandomShiftingStrategyFactory.createStrategy(), null, 4);
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
      config.controller().setOutputDirectory("output/groceryDelivery_kmt8_10it_b");
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

  /**
   * Creates an LSP with two chains:
   * - direct delivery
   * - 2-echelon delivery
   *
   * @param scenario the scenria, used e.g. for getting the network and register some stuff
   * @param lspName String of LSP's Id
   * @param carrierIdString Name of the carrier, the (lsp's) demand (shipments) are created from.
   * @param hubLinkId location of the hub
   * @return the LSP
   */
  private static LSP createLspWithTwoChains(Scenario scenario, String lspName, String carrierIdString, Id<Link> hubLinkId) {

    CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes();
    CarrierVehicleTypeReader vehicleTypeReader = new CarrierVehicleTypeReader(vehicleTypes);
    vehicleTypeReader.readFile(VEHICLE_TYPE_FILE);

    Carriers carriers = new Carriers();
    CarrierPlanXmlReader carrierReader = new CarrierPlanXmlReader(carriers, vehicleTypes);
    carrierReader.readFile(CARRIER_PLAN_FILE);

    Carrier carrier = carriers.getCarriers()
            .get(Id.create(carrierIdString, CarrierImpl.class));

    Id<Link> depotLinkFromVehicles = carrier
            .getCarrierCapabilities()
            .getCarrierVehicles()
            .values()
            .iterator()
            .next()
            .getLinkId();

    log.info("create LSP");

    //Chains
    LogisticChain directChain = createDirectChain(scenario, lspName, depotLinkFromVehicles, vehicleTypes);
    LogisticChain twoEchelonChain = createTwoEchelonChain(scenario, lspName, hubLinkId, depotLinkFromVehicles, vehicleTypes);

    LSPPlan multipleMixedEchelonChainsPlan =
            LSPUtils.createLSPPlan()
                    .addLogisticChain(directChain)
                    .addLogisticChain(twoEchelonChain)
                    .setInitialShipmentAssigner(MultipleChainsUtils.createRandomLogisticChainShipmentAssigner());

    List<LSPPlan> lspPlans = List.of(multipleMixedEchelonChainsPlan);

    LSP lsp = LSPUtils.LSPBuilder.getInstance(Id.create(lspName, LSP.class))
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

  private static LogisticChain createTwoEchelonChain(Scenario scenario, String lspName, Id<Link> hubLinkId, Id<Link> depotLinkFromVehicles, CarrierVehicleTypes vehicleTypes) {
    LogisticChain hubChain;
    Carrier mainCarrier = CarriersUtils.createCarrier(Id.create(lspName +"_mainCarrier", Carrier.class));
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
                            Id.create(lspName +"_Hub", LSPResource.class), hubLinkId, scenario)
                    .setTransshipmentHubScheduler(hubScheduler)
                    .build();

    LSPUtils.setFixedCost(hubResource, HUBCOSTS_FIX);

    LogisticChainElement hubElement =
            LSPUtils.LogisticChainElementBuilder.newInstance(
                            Id.create("HubElement", LogisticChainElement.class))
                    .setResource(hubResource)
                    .build();

    Carrier distributionCarrier =
            CarriersUtils.createCarrier(Id.create(lspName +"_distributionCarrier", Carrier.class));
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
    return hubChain;
  }

  /**
   * Creates an LSP with direct chains:
   *
   * @param scenario the scenria, used e.g. for getting the network and register some stuff
   * @param lspName String of LSP's Id
   * @param carrierIdString Name of the carrier, the (lsp's) demand (shipments) are created from.
   * @return the LSP
   */
  private static LSP createLspWithDirectChain(Scenario scenario, String lspName, String carrierIdString) {
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

    Id<Link> depotLinkFromVehicles = carrier
            .getCarrierCapabilities()
            .getCarrierVehicles()
            .values()
            .iterator()
            .next()
            .getLinkId();

    log.info("create LSP");

    LSPPlan lspPlan;
    {
      LogisticChain directChain;
      directChain = createDirectChain(scenario, lspName, depotLinkFromVehicles, vehicleTypes);

      lspPlan =
              LSPUtils.createLSPPlan()
                      .addLogisticChain(directChain)
                      .setInitialShipmentAssigner(MultipleChainsUtils.createRandomLogisticChainShipmentAssigner());
    }

    List<LSPPlan> lspPlans = List.of(lspPlan);

    LSP lsp =
            LSPUtils.LSPBuilder.getInstance(Id.create(lspName, LSP.class))
                    .setInitialPlan(lspPlan)
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


  private static LogisticChain createDirectChain(Scenario scenario, String lspName, Id<Link> depotLinkFromVehicles, CarrierVehicleTypes vehicleTypes) {
    LogisticChain directChain;
    Carrier directCarrier = CarriersUtils.createCarrier(Id.create(lspName +"_directCarrier", Carrier.class));
    directCarrier.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);

    CarriersUtils.addCarrierVehicle(directCarrier,
            CarrierVehicle.newInstance(
                    Id.createVehicleId("directCarrierTruck"),
                    depotLinkFromVehicles,
                    vehicleTypes.getVehicleTypes().get(Id.create("heavy40t", VehicleType.class))));
    LSPResource singleCarrierResource =
            ResourceImplementationUtils.DistributionCarrierResourceBuilder.newInstance(
                            directCarrier, scenario.getNetwork())
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
    return directChain;
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
