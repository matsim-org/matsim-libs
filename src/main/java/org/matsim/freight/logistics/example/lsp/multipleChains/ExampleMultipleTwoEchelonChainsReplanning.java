package org.matsim.freight.logistics.example.lsp.multipleChains;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
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
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.controler.CarrierControlerUtils;
import org.matsim.freight.carriers.controler.CarrierScoringFunctionFactory;
import org.matsim.freight.carriers.controler.CarrierStrategyManager;
import org.matsim.freight.logistics.*;
import org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils;
import org.matsim.freight.logistics.resourceImplementations.transshipmentHub.TranshipmentHubUtils;
import org.matsim.freight.logistics.shipment.LSPShipment;
import org.matsim.freight.logistics.shipment.ShipmentUtils;
import org.matsim.vehicles.VehicleType;

final class ExampleMultipleTwoEchelonChainsReplanning {

  static final double HUBCOSTS_FIX = 100;
  private static final Logger log =
      LogManager.getLogger(ExampleMultipleTwoEchelonChainsReplanning.class);
  private static final Id<Link> DEPOT_LINK_ID = Id.createLinkId("i(5,0)");
  private static final Id<Link> HUB_LEFT_LINK_ID = Id.createLinkId("i(1,5)R");
  private static final Id<Link> HUB_RIGHT_LINK_ID = Id.createLinkId("j(9,5)");

  private static final VehicleType VEH_TYPE_SMALL_05 =
      CarrierVehicleType.Builder.newInstance(Id.create("small05", VehicleType.class))
          .setCapacity(5)
          .setMaxVelocity(10)
          .setFixCost(5)
          .setCostPerDistanceUnit(0.001)
          .setCostPerTimeUnit(0.01)
          .build();

  private static final VehicleType VEH_TYPE_LARGE_50 =
      CarrierVehicleType.Builder.newInstance(Id.create("large50", VehicleType.class))
          .setCapacity(50)
          .setMaxVelocity(10)
          .setFixCost(150)
          .setCostPerDistanceUnit(0.01)
          .setCostPerTimeUnit(0.01)
          .build();

  private ExampleMultipleTwoEchelonChainsReplanning() {}

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
                      strategyManager.addStrategy(
                          new GenericPlanStrategyImpl<>(
                              new ExpBetaPlanSelector<>(new ScoringConfigGroup())),
                          null,
                          1);
                      strategyManager.addStrategy(
                          ProximityStrategyFactory.createStrategy(scenario.getNetwork()), null, 1);
                      strategyManager.setMaxPlansPerAgent(5);
                      strategyManager.setPlanSelectorForRemoval(
                          new LSPWorstPlanForRemovalSelector());
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
            ResourceImplementationUtils.MainRunCarrierResourceBuilder.newInstance(mainCarrierLeft, network)
                .setFromLinkId(DEPOT_LINK_ID)
                .setMainRunCarrierScheduler(
                    ResourceImplementationUtils.createDefaultMainRunCarrierScheduler())
                .setToLinkId(HUB_LEFT_LINK_ID)
                .setVehicleReturn(ResourceImplementationUtils.VehicleReturn.returnToFromLink)
                .build();

        LogisticChainElement mainCarrierElementLeft =
            LSPUtils.LogisticChainElementBuilder.newInstance(
                    Id.create("mainCarrierElementLeft", LogisticChainElement.class))
                .setResource(mainCarrierResourceLeft)
                .build();

        LSPResourceScheduler hubSchedulerLeft =
            TranshipmentHubUtils.TranshipmentHubSchedulerBuilder.newInstance()
                .setCapacityNeedFixed(10)
                .setCapacityNeedLinear(1)
                .build();

        LSPResource hubResourceLeft =
            TranshipmentHubUtils.TransshipmentHubBuilder.newInstance(
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
                    distributionCarrierLeft, network)
                .setDistributionScheduler(
                    ResourceImplementationUtils.createDefaultDistributionCarrierScheduler())
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
            ResourceImplementationUtils.MainRunCarrierResourceBuilder.newInstance(mainCarrier, network)
                .setFromLinkId(DEPOT_LINK_ID)
                .setMainRunCarrierScheduler(
                    ResourceImplementationUtils.createDefaultMainRunCarrierScheduler())
                .setToLinkId(HUB_RIGHT_LINK_ID)
                .setVehicleReturn(ResourceImplementationUtils.VehicleReturn.returnToFromLink)
                .build();

        LogisticChainElement mainCarrierElement =
            LSPUtils.LogisticChainElementBuilder.newInstance(
                    Id.create("mainCarrierElement", LogisticChainElement.class))
                .setResource(mainCarrierResource)
                .build();

        LSPResourceScheduler hubScheduler =
            TranshipmentHubUtils.TranshipmentHubSchedulerBuilder.newInstance()
                .setCapacityNeedFixed(10)
                .setCapacityNeedLinear(1)
                .build();

        LSPResource hubResourceRight =
            TranshipmentHubUtils.TransshipmentHubBuilder.newInstance(
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
                    distributionCarrier, network)
                .setDistributionScheduler(
                    ResourceImplementationUtils.createDefaultDistributionCarrierScheduler())
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
    for (LSPShipment shipment : createInitialLSPShipments()) {
      lsp.assignShipmentToLSP(shipment);
    }

    log.info("schedule the LSP with the shipments and according to the scheduler of the Resource");
    lsp.scheduleLogisticChains();

    return lsp;
  }

  private static Collection<LSPShipment> createInitialLSPShipments() {
    List<LSPShipment> shipmentList = new ArrayList<>();
    int capacityDemand = 1;

    for (int i = 1; i <= 10; i++) {
      if (i % 2 != 0) {
        Id<LSPShipment> id = Id.create("ShipmentLeft_" + i, LSPShipment.class);
        ShipmentUtils.LSPShipmentBuilder builder = ShipmentUtils.LSPShipmentBuilder.newInstance(id);

        builder.setCapacityDemand(capacityDemand);
        builder.setFromLinkId(DEPOT_LINK_ID);
        final Id<Link> shipmentLeftLinkId = Id.createLinkId("i(1,9)R");
        builder.setToLinkId(shipmentLeftLinkId);

        builder.setEndTimeWindow(TimeWindow.newInstance(0, (24 * 3600)));
        builder.setStartTimeWindow(TimeWindow.newInstance(0, (24 * 3600)));
        builder.setDeliveryServiceTime(capacityDemand * 60);

        shipmentList.add(builder.build());
      } else {
        Id<LSPShipment> id = Id.create("ShipmentRight_" + i, LSPShipment.class);
        ShipmentUtils.LSPShipmentBuilder builder = ShipmentUtils.LSPShipmentBuilder.newInstance(id);

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
