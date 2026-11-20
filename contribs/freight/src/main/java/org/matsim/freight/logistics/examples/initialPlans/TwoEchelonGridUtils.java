/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2026 by the members listed in the COPYING,        *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.freight.logistics.examples.initialPlans;

import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controller;
import org.matsim.core.controler.ControllerUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
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
import org.matsim.freight.logistics.examples.LogisticsExampleUtils;
import org.matsim.freight.logistics.examples.MyLSPScorer;
import org.matsim.freight.logistics.resourceImplementations.CarrierSchedulerUtils;
import org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils;
import org.matsim.freight.logistics.shipment.LspShipment;
import org.matsim.freight.logistics.shipment.LspShipmentUtils;
import org.matsim.vehicles.VehicleType;

/**
 * Material shared by the 2-echelon examples on the 9x9 grid network: {@link ExampleTwoEchelonGrid}, {@link
 * ExampleTwoEchelonGrid_NR}, and {@link ExampleTwoEchelonGrid_shipmentBasedDistribution}. The examples differ only in
 * their run settings and in what they write after the run.
 *
 * <p>The {@link LSP} has two {@link LSPPlan}s: 1) direct delivery from the depot, 2) delivery via a transshipment hub.
 *
 * @author Kai Martins-Turner (kturner)
 */
final class TwoEchelonGridUtils {

	private static final Logger log = LogManager.getLogger(TwoEchelonGridUtils.class);

	static final Id<Link> DEPOT_LINK_ID = Id.createLinkId("i(5,0)");
	static final Id<Link> HUB_LINK_ID = Id.createLinkId("j(5,3)");

	enum DemandSetting {
		oneCustomer,
		tenCustomers
	}

	enum CarrierCostSetting {
		sameCost,
		lowerCost4LastMile
	}

	private TwoEchelonGridUtils() {
	} // so it cannot be instantiated

	/**
	 * Creates the config with the 9x9 grid network. If command line arguments are given, they are applied to the config;
	 * otherwise, the output directory is set to the given one and the last iteration to 2.
	 */
	static Config prepareConfig(String[] args, String outputDirectory, OutputDirectoryHierarchy.OverwriteFileSetting overwriteFileSetting) {
		Config config = ConfigUtils.createConfig();
		if (args.length != 0) {
			for (String arg : args) {
				log.warn(arg);
			}
			ConfigUtils.applyCommandline(config, args);
		} else {
			config.controller().setOutputDirectory(outputDirectory);
			config.controller().setLastIteration(2);
		}

		config.network().setInputFile(String.valueOf(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("freight-chessboard-9x9"), "grid9x9.xml")));
		config.controller().setOverwriteFileSetting(overwriteFileSetting);
		config.controller().setWriteEventsInterval(1);

		FreightCarriersConfigGroup freightConfig = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfig.setTimeWindowHandling(FreightCarriersConfigGroup.TimeWindowHandling.ignore);

		return config;
	}

	/**
	 * Loads the scenario, sets free speed and capacity of all links (see {@link
	 * LogisticsExampleUtils#setFreespeed30KmhAndCapacity1000OnAllLinks}), and adds the scheduled LSP.
	 *
	 * @param vrpLogicDistribution the VRP logic of the carriers that deliver to the customers (direct carrier and
	 *                             distribution carrier). The main run carrier is always service-based.
	 */
	static Scenario prepareScenario(Config config, DemandSetting demandSetting, CarrierCostSetting costSetting, double hubCostsFix,
									LSPUtils.LogicOfVrp vrpLogicDistribution) {
		Scenario scenario = ScenarioUtils.loadScenario(config);
		LogisticsExampleUtils.setFreespeed30KmhAndCapacity1000OnAllLinks(scenario.getNetwork());

		log.info("Add LSP to the scenario");
		LSPUtils.loadLspsIntoScenario(scenario, Collections.singletonList(createLSP(scenario, demandSetting, costSetting, hubCostsFix, vrpLogicDistribution)));

		return scenario;
	}

	/**
	 * Creates the controller with the LSP module, the carrier scorer with the given toll, best-plan selection for carriers
	 * and LSPs, and {@link MyLSPScorer}. The VSP default checks are set to warn.
	 */
	static Controller prepareController(Scenario scenario, double tollValue) {
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
					final MyEventBasedCarrierScorer carrierScorer = new MyEventBasedCarrierScorer();
					carrierScorer.setToll(tollValue);

					bind(CarrierScoringFunctionFactory.class).toInstance(carrierScorer);
					addControllerListenerBinding().toInstance(carrierScorer);
					bind(CarrierStrategyManager.class)
						.toProvider(
							() -> {
								CarrierStrategyManager strategyManager = CarrierControllerUtils.createDefaultCarrierStrategyManager();
								strategyManager.addStrategy(new GenericPlanStrategyImpl<>(new BestPlanSelector<>()), null, 1);
								return strategyManager;
							});
					bind(LSPStrategyManager.class)
						.toProvider(
							() -> {
								LSPStrategyManager strategyManager = new LSPStrategyManagerImpl();
								strategyManager.addStrategy(new GenericPlanStrategyImpl<>(new BestPlanSelector<>()), null, 1);
								return strategyManager;
							});
					bind(LSPScorerFactory.class).toInstance(MyLSPScorer::new);
				}
			});

		// The VSP default settings are designed for person transport simulation. After talking to Kai,
		// they will be set to WARN here. Kai MT may'23
		controller.getConfig().vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);
		return controller;
	}

	/** Writes scores, shipments, shipment plans, and shipment logs of all LSPs into the output directory. */
	static void printResults(Controller controller) {
		for (LSP lsp : LSPUtils.getLSPs(controller.getScenario()).getLSPs().values()) {
			ResourceImplementationUtils.printScores(controller.getControllerIO().getOutputPath(), lsp);
			ResourceImplementationUtils.printShipmentsOfLSP(controller.getControllerIO().getOutputPath(), lsp);
			ResourceImplementationUtils.printResults_shipmentPlan(controller.getControllerIO().getOutputPath(), lsp);
			ResourceImplementationUtils.printResults_shipmentLog(controller.getControllerIO().getOutputPath(), lsp);
		}
	}

	private static LSP createLSP(Scenario scenario, DemandSetting demandSetting, CarrierCostSetting costSetting, double hubCostsFix,
								 LSPUtils.LogicOfVrp vrpLogicDistribution) {
		log.info("create LSP");
		Network network = scenario.getNetwork();
		VehicleType vehTypeLarge50 = LogisticsExampleUtils.createVehTypeLarge50();
		VehicleType vehTypeSmall05 = LogisticsExampleUtils.createVehTypeSmall05();

		LSPPlan lspPlan_direct;
		{
			log.info("Create lspPlan for direct delivery");

			Carrier directCarrier = CarriersUtils.createCarrier(Id.create("directCarrier", Carrier.class));
			directCarrier.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);
			CarrierSchedulerUtils.setVrpLogic(directCarrier, vrpLogicDistribution);

			CarriersUtils.addCarrierVehicle(directCarrier,
				CarrierVehicle.newInstance(Id.createVehicleId("directTruck"), DEPOT_LINK_ID, vehTypeLarge50));
			LSPResource directCarrierRessource = ResourceImplementationUtils.DistributionCarrierResourceBuilder.newInstance(directCarrier)
				.setDistributionScheduler(ResourceImplementationUtils.createDefaultDistributionCarrierScheduler(scenario))
				.build();

			LogisticChainElement directCarrierElement = LSPUtils.LogisticChainElementBuilder.newInstance(Id.create("directCarrierLSE", LogisticChainElement.class))
				.setResource(directCarrierRessource)
				.build();

			LogisticChain directChain = LSPUtils.LogisticChainBuilder.newInstance(Id.create("directChain", LogisticChain.class))
				.addLogisticChainElement(directCarrierElement)
				.build();

			final InitialShipmentAssigner singleChainShipmentAssigner = ResourceImplementationUtils.createSingleLogisticChainShipmentAssigner();
			lspPlan_direct = LSPUtils.createLSPPlan()
				.addLogisticChain(directChain)
				.setInitialShipmentAssigner(singleChainShipmentAssigner);
		}

		LSPPlan lspPlan_withHub;
		{
			log.info("Create lspPlan with Hub");

			Carrier mainCarrier = CarriersUtils.createCarrier(Id.create("mainCarrier", Carrier.class));
			mainCarrier.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);
			CarrierSchedulerUtils.setVrpLogic(mainCarrier, LSPUtils.LogicOfVrp.serviceBased); //only Service-based is implemented for the mainCarrier until now (feb'25)

			CarriersUtils.addCarrierVehicle(mainCarrier,
				CarrierVehicle.newInstance(Id.createVehicleId("mainTruck"), DEPOT_LINK_ID, vehTypeLarge50));
			LSPResource mainCarrierRessource = ResourceImplementationUtils.MainRunCarrierResourceBuilder.newInstance(mainCarrier)
				.setFromLinkId(DEPOT_LINK_ID)
				.setMainRunCarrierScheduler(ResourceImplementationUtils.createDefaultMainRunCarrierScheduler(scenario))
				.setToLinkId(HUB_LINK_ID)
				.setVehicleReturn(ResourceImplementationUtils.VehicleReturn.returnToFromLink)
				.build();

			LogisticChainElement mainCarrierLCE = LSPUtils.LogisticChainElementBuilder.newInstance(Id.create("mainCarrierLSE", LogisticChainElement.class))
				.setResource(mainCarrierRessource)
				.build();

			// The scheduler for the first reloading point is created --> this will be the depot in this
			// use case
			LSPResourceScheduler hubScheduler = ResourceImplementationUtils.TranshipmentHubSchedulerBuilder.newInstance()
				.setCapacityNeedFixed(10) // Time needed, fixed (for Scheduler)
				.setCapacityNeedLinear(1) // additional time needed per shipmentSize (for Scheduler)
				.build();

			// The scheduler is added to the Resource and the Resource is created
			LSPResource hubResource = ResourceImplementationUtils.TransshipmentHubBuilder.newInstance(Id.create("Hub", LSPResource.class), HUB_LINK_ID, scenario)
				.setTransshipmentHubScheduler(hubScheduler)
				.build();
			LSPUtils.setFixedCost(hubResource, hubCostsFix); // Set fixed costs (per day) for the availability of the hub.

			LogisticChainElement hubLCE = LSPUtils.LogisticChainElementBuilder.newInstance(Id.create("HubLSE", LogisticChainElement.class))
				.setResource(hubResource)
				.build(); // Nicht unbedingt nötig, aber nehme den alten Hub nun als Depot. Waren
			// werden dann dort "Zusammengestellt".

			Carrier distributionCarrier = CarriersUtils.createCarrier(Id.create("distributionCarrier", Carrier.class));
			distributionCarrier
				.getCarrierCapabilities()
				.setFleetSize(CarrierCapabilities.FleetSize.INFINITE);
			CarrierSchedulerUtils.setVrpLogic(distributionCarrier, vrpLogicDistribution);

			final VehicleType vehType;
			switch (costSetting) {
				case sameCost -> vehType = vehTypeLarge50;
				case lowerCost4LastMile -> vehType = vehTypeSmall05;
				default -> throw new IllegalStateException("Unexpected value: " + costSetting);
			}
			CarriersUtils.addCarrierVehicle(distributionCarrier,
				CarrierVehicle.newInstance(Id.createVehicleId("distributionTruck"), HUB_LINK_ID, vehType));
			LSPResource distributionCarrierRessource = ResourceImplementationUtils.DistributionCarrierResourceBuilder.newInstance(distributionCarrier)
				.setDistributionScheduler(ResourceImplementationUtils.createDefaultDistributionCarrierScheduler(scenario))
				.build();

			LogisticChainElement distributionCarrierLCE = LSPUtils.LogisticChainElementBuilder.newInstance(Id.create("distributionCarrierLSE", LogisticChainElement.class))
				.setResource(distributionCarrierRessource)
				.build();

			// The builder connects the elements in the order in which they are added. This order defines the order of the scheduling.
			LogisticChain hubChain = LSPUtils.LogisticChainBuilder.newInstance(Id.create("hubChain", LogisticChain.class))
				.addLogisticChainElement(mainCarrierLCE)
				.addLogisticChainElement(hubLCE)
				.addLogisticChainElement(distributionCarrierLCE)
				.build();

			lspPlan_withHub = LSPUtils.createLSPPlan()
				.addLogisticChain(hubChain)
				.setInitialShipmentAssigner(ResourceImplementationUtils.createSingleLogisticChainShipmentAssigner());
		}

		// Todo: Auch das ist wirr: Muss hier alle sammeln, damit man die dann im LSPBuilder dem
		// LogisticChainScheduler mitgeben kann. Im Nachgang packt man dann aber erst den zweiten Plan dazu
		// ... urgs KMT'Jul22
		List<LSPPlan> lspPlans = new ArrayList<>();
		lspPlans.add(lspPlan_withHub);
		lspPlans.add(lspPlan_direct);

		LSP lsp = LSPUtils.LSPBuilder.getInstance(Id.create("myLSP", LSP.class))
			.setInitialPlan(lspPlan_direct)
			.setLogisticChainScheduler(ResourceImplementationUtils.createDefaultSimpleForwardLogisticChainScheduler(LogisticsExampleUtils.createResourcesListFromLSPPlans(lspPlans)))
			.build();
		lsp.addPlan(lspPlan_withHub); // add the second plan to the lsp

		log.info("create initial LSPShipments");
		log.info("assign the shipments to the LSP");
		for (LspShipment lspShipment : createInitialLSPShipments(network, demandSetting)) {
			lsp.assignShipmentToLspPlan(lspShipment);
		}

		log.info("schedule the LSP with the shipments and according to the scheduler of the Resource");
		lsp.scheduleLogisticChains();

		return lsp;
	}

	private static Collection<LspShipment> createInitialLSPShipments(Network network, DemandSetting demandSetting) {
		List<LspShipment> shipmentList = new ArrayList<>();

		switch (demandSetting) {
			case oneCustomer -> {
				Id<LspShipment> id = Id.create("Shipment_" + 1, LspShipment.class);
				int capacityDemand = 1;

				LspShipmentUtils.LspShipmentBuilder builder = LspShipmentUtils.LspShipmentBuilder.newInstance(id);

				builder.setCapacityDemand(capacityDemand);
				builder.setFromLinkId(DEPOT_LINK_ID);
				builder.setToLinkId(Id.createLinkId("i(5,5)R"));
				builder.setEndTimeWindow(TimeWindow.newInstance(0, (24 * 3600)));
				builder.setStartTimeWindow(TimeWindow.newInstance(0, (24 * 3600)));
				builder.setDeliveryServiceTime(capacityDemand * 60);
				builder.setPickupServiceTime(capacityDemand * 60);

				shipmentList.add(builder.build());

				return shipmentList;
			}
			case tenCustomers -> {
				Random rand1 = MatsimRandom.getLocalInstance();
				Random rand2 = MatsimRandom.getLocalInstance();

				List<String> zoneLinkList =
					Arrays.asList(
						"i(4,4)", "i(5,4)", "i(6,4)", "i(4,6)", "i(5,6)", "i(6,6)", "j(3,5)", "j(3,6)",
						"j(3,7)", "j(5,5)", "j(5,6)", "j(5,7)", "i(4,5)R", "i(5,5)R", "i(6,5)R", "i(4,7)R",
						"i(5,7)R", "i(6,7)R", "j(4,5)R", "j(4,6)R", "j(4,7)R", "j(6,5)R", "j(6,6)R",
						"j(6,7)R");
				for (String linkIdString : zoneLinkList) {
					if (!network.getLinks().containsKey(Id.createLinkId(linkIdString))) {
						throw new RuntimeException("Link is not in Network!");
					}
				}

				for (int i = 1; i <= 10; i++) {
					Id<LspShipment> id = Id.create("Shipment_" + i, LspShipment.class);
					LspShipmentUtils.LspShipmentBuilder builder =
						LspShipmentUtils.LspShipmentBuilder.newInstance(id);

					int capacityDemand = rand1.nextInt(5) + 1; // Random is drawn from 0 (incl) to bound (excl) -> adding 1.
					builder.setCapacityDemand(capacityDemand);

					builder.setFromLinkId(DEPOT_LINK_ID);
					final Id<Link> toLinkId = Id.createLinkId(zoneLinkList.get(rand2.nextInt(zoneLinkList.size() - 1)));
					builder.setToLinkId(toLinkId);

					builder.setEndTimeWindow(TimeWindow.newInstance(0, (24 * 3600)));
					builder.setStartTimeWindow(TimeWindow.newInstance(0, (24 * 3600)));
					builder.setDeliveryServiceTime(capacityDemand * 60);
					builder.setPickupServiceTime(capacityDemand * 60);

					shipmentList.add(builder.build());
				}
				return shipmentList;
			}
			default -> throw new IllegalStateException("Unexpected value: " + demandSetting);
		}
	}
}
