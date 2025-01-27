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

import java.io.IOException;
import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
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
import org.matsim.freight.carriers.analysis.CarriersAnalysis;
import org.matsim.freight.carriers.controller.CarrierControllerUtils;
import org.matsim.freight.carriers.controller.CarrierScoringFunctionFactory;
import org.matsim.freight.carriers.controller.CarrierStrategyManager;
import org.matsim.freight.logistics.*;
import org.matsim.freight.logistics.examples.ExampleConstants;
import org.matsim.freight.logistics.resourceImplementations.CarrierSchedulerUtils;
import org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils;
import org.matsim.freight.logistics.shipment.LspShipment;
import org.matsim.vehicles.VehicleType;

/**
 * This bases on {@link ExampleGroceryDeliveryMultipleChains}.
 *  Now it will include two different LSPs
 *
 */
final class ExampleTwoLspsGroceryDeliveryMultipleChains {

  private static final Logger log = LogManager.getLogger(ExampleTwoLspsGroceryDeliveryMultipleChains.class);

  private static final Id<Link> HUB_LINK_ID_NEUKOELLN = Id.createLinkId("91085");
  private static final double HUBCOSTS_FIX = 100;

  private static final List<String> TOLLED_LINKS = ExampleConstants.TOLLED_LINK_LIST_BERLIN_BOTH_DIRECTIONS;
  private static final List<String> TOLLED_VEHICLE_TYPES = List.of("heavy40t"); //  Für welche Fahrzeugtypen soll das MautSchema gelten?
  private static final double TOLL_VALUE = 1000;

  private static final String CARRIER_PLAN_FILE = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/freight/foodRetailing_wo_rangeConstraint/input/CarrierLEH_v2_withFleet_Shipment_OneTW_PickupTime_ICEVandBEV.xml";
  private static final String VEHICLE_TYPE_FILE = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/freight/foodRetailing_wo_rangeConstraint/input/vehicleTypesBVWP100_DC_noTax.xml";
  private static final String EDEKA_SUPERMARKT_TROCKEN = "edeka_SUPERMARKT_TROCKEN";
  private static final String KAUFLAND_VERBRAUCHERMARKT_TROCKEN = "kaufland_VERBRAUCHERMARKT_TROCKEN";

  private static final String OUTPUT_DIRECTORY = "output/groceryDelivery_kmt_1000_1LSPb";


  private ExampleTwoLspsGroceryDeliveryMultipleChains() {}

  public static void main(String[] args) {
    log.info("Prepare config");
    Config config = prepareConfig(args);

    log.info("Prepare scenario");
    Scenario scenario = ScenarioUtils.loadScenario(config);

    CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes();
    CarrierVehicleTypeReader vehicleTypeReader = new CarrierVehicleTypeReader(vehicleTypes);
    vehicleTypeReader.readFile(VEHICLE_TYPE_FILE);
    //The following is needed, because since fall 2024 the vehicle types are not assigned to a network mode by default.
    for (VehicleType vehicleType : vehicleTypes.getVehicleTypes().values()) {
      vehicleType.setNetworkMode(TransportMode.car);
    }

    Carriers carriers = new Carriers();
    CarrierPlanXmlReader carrierReader = new CarrierPlanXmlReader(carriers, vehicleTypes);
    carrierReader.readFile(CARRIER_PLAN_FILE);

    Carrier carrierEdeka = carriers.getCarriers().get(Id.create(EDEKA_SUPERMARKT_TROCKEN, CarrierImpl.class));
    Carrier carrierKaufland = carriers.getCarriers().get(Id.create(KAUFLAND_VERBRAUCHERMARKT_TROCKEN, CarrierImpl.class));

    log.info("Add LSP(s) to the scenario");
    Collection<LSP> lsps = new LinkedList<>();
    lsps.add(createLspWithTwoChains(scenario, "Edeka", MultipleChainsUtils.createLSPShipmentsFromCarrierShipments(carrierEdeka), getDepotLinkFromVehicle(carrierEdeka), HUB_LINK_ID_NEUKOELLN, vehicleTypes, vehicleTypes, vehicleTypes));
    lsps.add(createLspWithTwoChains(scenario, "Kaufland", MultipleChainsUtils.createLSPShipmentsFromCarrierShipments(carrierKaufland), getDepotLinkFromVehicle(carrierKaufland), HUB_LINK_ID_NEUKOELLN, vehicleTypes, vehicleTypes, vehicleTypes));
    lsps.add(createLspWithDirectChain(scenario, "Edeka_DIRECT", MultipleChainsUtils.createLSPShipmentsFromCarrierShipments(carrierEdeka), getDepotLinkFromVehicle(carrierEdeka), vehicleTypes));
    lsps.add(createLspWithDirectChain(scenario, "Kaufland_DIRECT", MultipleChainsUtils.createLSPShipmentsFromCarrierShipments(carrierKaufland), getDepotLinkFromVehicle(carrierKaufland), vehicleTypes));
    LSPUtils.addLSPs(scenario, new LSPs(lsps));

    Controller controller = prepareController(scenario);

    log.info("Run MATSim");

    controller.run();

    runCarrierAnalysis(controller.getControlerIO().getOutputPath(), config);

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
      config.controller().setOutputDirectory(OUTPUT_DIRECTORY);
      config.controller().setLastIteration(1);
    }

    config.network().setInputFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz");
    config.global().setCoordinateSystem("EPSG:31468");
    config.global().setRandomSeed(4177);
    config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

    // The VSP default settings are designed for person transport simulation. After talking to Kai,
    // they will be set to WARN here. Kai MT may'23
    config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);

    FreightCarriersConfigGroup freightConfig = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
    freightConfig.setTimeWindowHandling(FreightCarriersConfigGroup.TimeWindowHandling.ignore);

    return config;
  }

  private static Controller prepareController(Scenario scenario) {
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
                carrierScorer.setTolledVehicleTypes(TOLLED_VEHICLE_TYPES);
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
                                  strategyManager.addStrategy(RandomShiftingStrategyFactory.createStrategy(), null, 4);
                                  strategyManager.setMaxPlansPerAgent(5);
                                  strategyManager.setPlanSelectorForRemoval(new GenericWorstPlanForRemovalSelector<>());
                                  return strategyManager;
                                });
              }
            });
    return controller;
  }

  private static void runCarrierAnalysis(String outputPath, Config config) {
    CarriersAnalysis carriersAnalysis = new CarriersAnalysis(outputPath +"/", outputPath +"/Analysis/", config.controller().getRunId(), config.global().getCoordinateSystem());
	carriersAnalysis.runCarrierAnalysis(CarriersAnalysis.CarrierAnalysisType.carriersAndEvents);
  }

  /**
   * Creates an LSP with two chains:
   * - direct delivery
   * - 2-echelon delivery
   *  <p></p>
   *  TODO: Es sollten dann am Besten direkt die zur Verfügung stehenden VehicleTypes übergeben werden und diese dann hier jeweils (alle) hinzugefügt werden.
   *  Aktuell erfolgt die Auswahl ja noch hier. Das sollte dann aber nicht mehr so sein, sondern bereits weiter upstream definiert werden.
   *  kmt Jul'24
   *
   * @param scenario                    the scenario, used e.g. for getting the network and register some stuff
   * @param lspName                     String of LSP's Id
   * @param lspShipments                Collection of LSPShipments to be assigned to the LSP
   * @param depotLinkId                 Id of the depot link
   * @param hubLinkId                   location of the hub
   * @param vehicleTypesMainRun         vehicle types for the main run (2e-chain)
   * @param vehicleTypesDistributionRun vehicle types for the distribution run (2e-chain)
   * @param vehicleTypesDirect          vehicle types for the direct run (direct chain)
   * @return the LSP
   */
  private static LSP createLspWithTwoChains(Scenario scenario, String lspName, Collection<LspShipment> lspShipments, Id<Link> depotLinkId, Id<Link> hubLinkId, CarrierVehicleTypes vehicleTypesMainRun, CarrierVehicleTypes vehicleTypesDistributionRun, CarrierVehicleTypes vehicleTypesDirect) {
    log.info("create LSP");
    //Chains
    LogisticChain directChain = createDirectChain(scenario, lspName, depotLinkId, vehicleTypesDirect);
    LogisticChain twoEchelonChain = createTwoEchelonChain(scenario, lspName, hubLinkId, depotLinkId, vehicleTypesMainRun, vehicleTypesDistributionRun);

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

    log.info("assign the shipments to the LSP");
    for (LspShipment lspShipment : lspShipments) {
      lsp.assignShipmentToLSP(lspShipment);
    }

    log.info("schedule the LSP with the shipments and according to the scheduler of the Resource");
    lsp.scheduleLogisticChains();

    return lsp;
  }

  private static Id<Link> getDepotLinkFromVehicle(Carrier carrier) {
    return carrier
            .getCarrierCapabilities()
            .getCarrierVehicles()
            .values()
            .iterator()
            .next()
            .getLinkId();
  }

  /*
 TODO: Es sollten dann am Besten direkt die zur Verfügung stehenden VehicleTypes übergeben werden und diese dann hier jeweils (alle) hinzugefügt werden.
 Aktuell erfolgt die Auswahl ja noch hier. Das sollte dann aber nicht mehr so sein, sondern bereits weiter upstream definiert werden.
 kmt Jul'24
 */
  private static LogisticChain createTwoEchelonChain(Scenario scenario, String lspName, Id<Link> hubLinkId, Id<Link> depotLinkFromVehicles, CarrierVehicleTypes vehicleTypesMainRun, CarrierVehicleTypes vehicleTypesDistributionRun) {
    LogisticChain hubChain;
    Carrier mainCarrier = CarriersUtils.createCarrier(Id.create(lspName +"_mainCarrier", Carrier.class));
    mainCarrier.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);

    CarriersUtils.addCarrierVehicle(
            mainCarrier,
            CarrierVehicle.newInstance(
                    Id.createVehicleId("mainTruck"),
                    depotLinkFromVehicles,
                    vehicleTypesMainRun.getVehicleTypes().get(Id.create("heavy40t", VehicleType.class))));
    LSPResource mainCarrierResource =
            ResourceImplementationUtils.MainRunCarrierResourceBuilder.newInstance(
                            mainCarrier)
                    .setFromLinkId(depotLinkFromVehicles)
                    .setMainRunCarrierScheduler(
                            ResourceImplementationUtils.createDefaultMainRunCarrierScheduler(scenario))
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
            //.setNumberOfJspritIterations // TODO Das mal hier einbauen. --> Ist aktuell in CarrierUtils.
            .setFleetSize(CarrierCapabilities.FleetSize.INFINITE);
   CarrierSchedulerUtils.setVrpLogic(distributionCarrier, LSPUtils.LogicOfVrp.serviceBased);

    CarriersUtils.addCarrierVehicle(
            distributionCarrier,
            CarrierVehicle.newInstance(
                    Id.createVehicleId("distributionTruck"),
                    hubLinkId,
                    vehicleTypesDistributionRun
                            .getVehicleTypes()
                            .get(Id.create("heavy40t_electro", VehicleType.class))));
    LSPResource distributionCarrierResource =
            ResourceImplementationUtils.DistributionCarrierResourceBuilder.newInstance(
                            distributionCarrier)
                    .setDistributionScheduler(ResourceImplementationUtils.createDefaultDistributionCarrierScheduler(scenario))
                    .build();

    LogisticChainElement distributionCarrierElement =
            LSPUtils.LogisticChainElementBuilder.newInstance(
                            Id.create("distributionCarrierElement", LogisticChainElement.class))
                    .setResource(distributionCarrierResource)
                    .build();

    mainCarrierElement.connectWithNextElement(hubElement);
    hubElement.connectWithNextElement(distributionCarrierElement);

    //TODO: Hier das Verbinden einfügen und in der Reihenfolge ist es. KMT Nov'24
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
   * @param scenario           the scenario, used e.g. for getting the network and register some stuff
   * @param lspName            String of LSP's Id
   * @param lspShipments                Collection of LSPShipments to be assigned to the LSP
   * @param depotLinkId                   Id of the depot link
   * @param vehicleTypesDirect          vehicle types for the direct run (direct chain)
   * @return the LSP
   */
  private static LSP createLspWithDirectChain(Scenario scenario, String lspName, Collection<LspShipment> lspShipments, Id<Link> depotLinkId, CarrierVehicleTypes vehicleTypesDirect) {
    log.info("create LSP");

    LSPPlan lspPlan = LSPUtils.createLSPPlan()
            .addLogisticChain(createDirectChain(scenario, lspName, depotLinkId, vehicleTypesDirect))
            .setInitialShipmentAssigner(MultipleChainsUtils.createRandomLogisticChainShipmentAssigner());

    LSP lsp =
            LSPUtils.LSPBuilder.getInstance(Id.create(lspName, LSP.class))
                    .setInitialPlan(lspPlan)
                    .setLogisticChainScheduler(
                            ResourceImplementationUtils.createDefaultSimpleForwardLogisticChainScheduler(
                                    createResourcesListFromLSPPlans(List.of(lspPlan))))
                    .build();

    log.info("assign the shipments to the LSP");
    for (LspShipment lspShipment : lspShipments) {
      lsp.assignShipmentToLSP(lspShipment);
    }

    log.info("schedule the LSP with the shipments and according to the scheduler of the Resource");
    lsp.scheduleLogisticChains();

    return lsp;
  }


  private static LogisticChain createDirectChain(Scenario scenario, String lspName, Id<Link> depotLinkFromVehicles, CarrierVehicleTypes vehicleTypes) {
    LogisticChain directChain;
    Carrier directCarrier = CarriersUtils.createCarrier(Id.create(lspName +"_directCarrier", Carrier.class));
    directCarrier.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);
    CarrierSchedulerUtils.setVrpLogic(directCarrier, LSPUtils.LogicOfVrp.serviceBased);

    CarriersUtils.addCarrierVehicle(directCarrier,
            CarrierVehicle.newInstance(
                    Id.createVehicleId("directCarrierTruck"),
                    depotLinkFromVehicles,
                    vehicleTypes.getVehicleTypes().get(Id.create("heavy40t", VehicleType.class))));
    LSPResource singleCarrierResource =
            ResourceImplementationUtils.DistributionCarrierResourceBuilder.newInstance(directCarrier)
                    .setDistributionScheduler(ResourceImplementationUtils.createDefaultDistributionCarrierScheduler(scenario))
                    .build();

    LogisticChainElement singleCarrierElement =
            LSPUtils.LogisticChainElementBuilder.newInstance(Id.create("directCarrierElement", LogisticChainElement.class))
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
