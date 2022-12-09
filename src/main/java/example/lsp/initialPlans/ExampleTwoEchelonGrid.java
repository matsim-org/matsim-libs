/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2022 by the members listed in the COPYING,        *
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

package example.lsp.initialPlans;

import lsp.*;
import lsp.shipment.LSPShipment;
import lsp.shipment.ShipmentUtils;
import lsp.usecase.UsecaseUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.controler.CarrierScoringFunctionFactory;
import org.matsim.contrib.freight.controler.CarrierStrategyManager;
import org.matsim.contrib.freight.controler.CarrierStrategyManagerImpl;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.vehicles.VehicleType;

import java.util.*;

/**
 * This is an academic example for the 2-echelon problem.
 * It uses the 9x9-grid network from the matsim-examples.
 * <p>
 * The depot is located at the outer border of the network, while the jobs are located in the middle area.
 * The {@link lsp.LSP} has two different {@link lsp.LSPPlan}s:
 * 1) direct delivery from the depot
 * 2) Using a TransshipmentHub: All goods were brought from the depot to the hub, reloaded and then brought from the hub to the customers
 * <p>
 * The decision which of these plans is chosen should be made via the Score of the plans.
 * We will modify the costs of the vehicles and/or for using(having) the Transshipment hub. Depending on this setting,
 * the plan selection should be done accordingly.
 * <p>
 * Please note: This example is in part on existing examples, but I start from the scratch for a) see, if this works and b) have a "clean" class :)
 *
 * @author Kai Martins-Turner (kturner)
 */
final class ExampleTwoEchelonGrid {

	//Run Settings
	static final double HUBCOSTS_FIX = 100;
	private static final DemandSetting demandSetting = DemandSetting.tenCustomers;
	private static final CarrierCostSetting costSetting = CarrierCostSetting.lowerCost4LastMile;
	private static final double TOLL_VALUE = 1000;


	private static final Logger log = LogManager.getLogger(ExampleTwoEchelonGrid.class);

	private static final Id<Link> DEPOT_LINK_ID = Id.createLinkId("i(5,0)");
	private static final Id<Link> HUB_LINK_ID = Id.createLinkId("j(5,3)");

	enum DemandSetting {oneCustomer, tenCustomers}
	enum CarrierCostSetting {sameCost, lowerCost4LastMile}


	private static final VehicleType VEH_TYPE_LARGE_50 = CarrierVehicleType.Builder.newInstance(Id.create("large50", VehicleType.class))
			.setCapacity(50)
			.setMaxVelocity(10)
			.setFixCost(150)
			.setCostPerDistanceUnit(0.01)
			.setCostPerTimeUnit(0.01)
			.build();

	private static final VehicleType VEH_TYPE_SMALL_05 = CarrierVehicleType.Builder.newInstance(Id.create("small05", VehicleType.class))
			.setCapacity(5)
			.setMaxVelocity(10)
			.setFixCost(25)
			.setCostPerDistanceUnit(0.001)
			.setCostPerTimeUnit(0.005)
			.build();


	private ExampleTwoEchelonGrid() {
	} // so it cannot be instantiated

	public static void main(String[] args) {
		log.info("Prepare Config");
		Config config = prepareConfig(args);

		log.info("Prepare scenario");
		Scenario scenario = prepareScenario(config);

		log.info("Prepare Controler");
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				install(new LSPModule());
			}
		});

		controler.addOverridingModule( new AbstractModule(){
			@Override public void install(){
//				bind( CarrierScoringFunctionFactory.class ).toInstance( new MyCarrierScorer());
				final MyEventBasedCarrierScorer carrierScorer = new MyEventBasedCarrierScorer();
				carrierScorer.setToll(TOLL_VALUE);

				bind( CarrierScoringFunctionFactory.class ).toInstance(carrierScorer);
				bind( CarrierStrategyManager.class ).toProvider(() -> {
					CarrierStrategyManager strategyManager = new CarrierStrategyManagerImpl();
					strategyManager.addStrategy(new GenericPlanStrategyImpl<>(new BestPlanSelector<>()), null, 1);
					return strategyManager;
				});
				bind( LSPStrategyManager.class ).toProvider(() -> {
					LSPStrategyManager strategyManager = new LSPStrategyManagerImpl();
					strategyManager.addStrategy(new GenericPlanStrategyImpl<>(new BestPlanSelector<>()), null, 1);
					return strategyManager;
				});
				bind( LSPScorerFactory.class ).toInstance( ( lsp) -> new MyLSPScorer() );
			}
		} );

		log.info("Run MATSim");
		log.warn("Runs settings were: Demand: "  + demandSetting +  "\n CarrierCosts: "  + costSetting  + "\n HubCosts: "  + HUBCOSTS_FIX + "\n tollValue: "  + TOLL_VALUE);
		controler.run();

		log.info("Some results ....");

		for (LSP lsp : LSPUtils.getLSPs(controler.getScenario()).getLSPs().values()) {
			UsecaseUtils.printScores(controler.getControlerIO().getOutputPath(), lsp);
			UsecaseUtils.printShipmentsOfLSP(controler.getControlerIO().getOutputPath(), lsp);
			UsecaseUtils.printResults_shipmentPlan(controler.getControlerIO().getOutputPath(), lsp);
			UsecaseUtils.printResults_shipmentLog(controler.getControlerIO().getOutputPath(), lsp);
		}
		log.info("Done.");
	}

	private static Config prepareConfig(String[] args) {
		Config config = ConfigUtils.createConfig();
		if (args.length != 0) {
			for (String arg : args) {
				log.warn(arg);
			}
			ConfigUtils.applyCommandline(config, args);

			CommandLine cmd = ConfigUtils.getCommandLine(args);
		} else {
			config.controler().setOutputDirectory("output/2echelon_"+demandSetting +  "_"  + costSetting  + "_"  + HUBCOSTS_FIX+ "_"  + TOLL_VALUE);
			config.controler().setLastIteration(2);
		}

		config.network().setInputFile(String.valueOf(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("freight-chessboard-9x9"), "grid9x9.xml")));
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setWriteEventsInterval(1);

		FreightConfigGroup freightConfig = ConfigUtils.addOrGetModule(config, FreightConfigGroup.class);
		freightConfig.setTimeWindowHandling(FreightConfigGroup.TimeWindowHandling.ignore);

		return config;
	}

	private static Scenario prepareScenario(Config config) {
		Scenario scenario = ScenarioUtils.loadScenario(config);

		//Change speed on all links to 30 km/h (8.33333 m/s) for easier computation --> Freeflow TT per link is 2min
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

		LSPPlan lspPlan_direct;
		{
			log.info("Create lspPlan for direct delivery");

			Carrier directCarrier = CarrierUtils.createCarrier(Id.create("directCarrier", Carrier.class));
			directCarrier.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);

			CarrierUtils.addCarrierVehicle(directCarrier, CarrierVehicle.newInstance(Id.createVehicleId("directTruck"), DEPOT_LINK_ID, VEH_TYPE_LARGE_50));
			LSPResource directCarrierRessource = UsecaseUtils.DistributionCarrierResourceBuilder.newInstance(Id.create("directCarrierRes", LSPResource.class), network)
					.setCarrier(directCarrier)
					.setDistributionScheduler(UsecaseUtils.createDefaultDistributionCarrierScheduler())
					.build();

			LogisticChainElement directCarrierElement = LSPUtils.LogisticChainElementBuilder.newInstance(Id.create("directCarrierLSE", LogisticChainElement.class))
					.setResource(directCarrierRessource)
					.build();


			LogisticChain solution_direct = LSPUtils.LogisticChainBuilder.newInstance(Id.create("directSolution", LogisticChain.class))
					.addLogisticChainElement(directCarrierElement)
					.build();

			final ShipmentAssigner singleSolutionShipmentAssigner = UsecaseUtils.createSingleLogisticChainShipmentAssigner();
			lspPlan_direct = LSPUtils.createLSPPlan()
					.addLogisticChain(solution_direct)
					.setAssigner(singleSolutionShipmentAssigner);
		}

		LSPPlan lspPlan_withHub;
		{
			log.info("Create lspPlan with Hub");

			Carrier mainCarrier = CarrierUtils.createCarrier(Id.create("mainCarrier", Carrier.class));
			mainCarrier.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);

			CarrierUtils.addCarrierVehicle(mainCarrier, CarrierVehicle.newInstance(Id.createVehicleId("mainTruck"), DEPOT_LINK_ID, VEH_TYPE_LARGE_50));
			LSPResource mainCarrierRessource = UsecaseUtils.MainRunCarrierResourceBuilder.newInstance(Id.create("mainCarrierRes", LSPResource.class), network)
					.setCarrier(mainCarrier)
					.setFromLinkId(DEPOT_LINK_ID)
					.setMainRunCarrierScheduler(UsecaseUtils.createDefaultMainRunCarrierScheduler())
					.setToLinkId(HUB_LINK_ID)
					.setVehicleReturn(UsecaseUtils.VehicleReturn.returnToFromLink)
					.build();

			LogisticChainElement mainCarrierLSE = LSPUtils.LogisticChainElementBuilder.newInstance(Id.create("mainCarrierLSE", LogisticChainElement.class))
					.setResource(mainCarrierRessource)
					.build();

			//The scheduler for the first reloading point is created --> this will be the depot in this use case
			LSPResourceScheduler hubScheduler = UsecaseUtils.TranshipmentHubSchedulerBuilder.newInstance()
					.setCapacityNeedFixed(10) //Time needed, fixed (for Scheduler)
					.setCapacityNeedLinear(1) //additional time needed per shipmentSize (for Scheduler)
					.build();

			//The scheduler is added to the Resource and the Resource is created
			LSPResource hubResource = UsecaseUtils.TransshipmentHubBuilder.newInstance(Id.create("Hub", LSPResource.class), HUB_LINK_ID, scenario)
					.setTransshipmentHubScheduler(hubScheduler)
					.build();
			LSPUtils.setFixedCost(hubResource, HUBCOSTS_FIX); //Set fixed costs (per day) for the availability of the hub.

			LogisticChainElement hubLSE = LSPUtils.LogisticChainElementBuilder.newInstance(Id.create("HubLSE", LogisticChainElement.class))
					.setResource(hubResource)
					.build(); //Nicht unbedingt nötig, aber nehme den alten Hub nun als Depot. Waren werden dann dort "Zusammengestellt".

			Carrier distributionCarrier = CarrierUtils.createCarrier(Id.create("distributionCarrier", Carrier.class));
			distributionCarrier.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);

			final VehicleType vehType;
			switch (costSetting) {
				case sameCost ->  vehType = VEH_TYPE_LARGE_50;	
				case lowerCost4LastMile -> 	vehType = VEH_TYPE_SMALL_05;
				default -> throw new IllegalStateException("Unexpected value: " + costSetting);
			}
			CarrierUtils.addCarrierVehicle(distributionCarrier, CarrierVehicle.newInstance(Id.createVehicleId("distributionTruck"), HUB_LINK_ID, vehType));
			LSPResource distributionCarrierRessource = UsecaseUtils.DistributionCarrierResourceBuilder.newInstance(Id.create("distributionCarrierRes", LSPResource.class), network)
					.setCarrier(distributionCarrier)
					.setDistributionScheduler(UsecaseUtils.createDefaultDistributionCarrierScheduler())
					.build();

			LogisticChainElement distributionCarrierElement = LSPUtils.LogisticChainElementBuilder.newInstance(Id.create("distributionCarrierLSE", LogisticChainElement.class))
					.setResource(distributionCarrierRessource)
					.build();

			//Kettenbildung per hand, damit dann klar ist, wie das Scheduling ablaufen soll. TODO: Vielleicht bekommt man das noch eleganter hin.
			// z.B. in der Reihenfolge in der die solutionsElements der LogisticsSolution zugeordnet werden: ".addSolutionElement(..)"
			mainCarrierLSE.connectWithNextElement(hubLSE);
			hubLSE.connectWithNextElement(distributionCarrierElement);

			LogisticChain solution_withHub = LSPUtils.LogisticChainBuilder.newInstance(Id.create("hubSolution", LogisticChain.class))
					.addLogisticChainElement(mainCarrierLSE)
					.addLogisticChainElement(hubLSE)
					.addLogisticChainElement(distributionCarrierElement)
					.build();

			lspPlan_withHub = LSPUtils.createLSPPlan()
					.addLogisticChain(solution_withHub)
					.setAssigner(UsecaseUtils.createSingleLogisticChainShipmentAssigner());
		}

		//Todo: Auch das ist wirr: Muss hier alle sommeln, damit man die dann im LSPBuilder dem SolutionScheduler mitgeben kann. Im Nachgang packt man dann aber erst den zweiten Plan dazu ... urgs KMT'Jul22
		List<LSPPlan> lspPlans = new ArrayList<>();
		lspPlans.add(lspPlan_withHub);
		lspPlans.add(lspPlan_direct);

		LSP lsp = LSPUtils.LSPBuilder.getInstance(Id.create("myLSP", LSP.class))
				.setInitialPlan(lspPlan_direct)
				.setLogisticChainScheduler(UsecaseUtils.createDefaultSimpleForwardLogisticChainScheduler(createResourcesListFromLSPPlans(lspPlans)))
				.build();
		lsp.addPlan(lspPlan_withHub); //add the second plan to the lsp

		log.info("create initial LSPShipments");
		log.info("assign the shipments to the LSP");
		for (LSPShipment shipment : createInitialLSPShipments(network)) {
			lsp.assignShipmentToLSP(shipment);
		}

		log.info("schedule the LSP with the shipments and according to the scheduler of the Resource");
		lsp.scheduleLogisticChains();

		return lsp;
	}

	private static Collection<LSPShipment> createInitialLSPShipments(Network network) {
		List<LSPShipment> shipmentList = new ArrayList<>();

		switch (demandSetting) {
			case oneCustomer -> {
				Id<LSPShipment> id = Id.create("Shipment_" + 1, LSPShipment.class);
				int capacityDemand = 1;

				ShipmentUtils.LSPShipmentBuilder builder = ShipmentUtils.LSPShipmentBuilder.newInstance(id);

				builder.setCapacityDemand(capacityDemand);
				builder.setFromLinkId(DEPOT_LINK_ID);
				builder.setToLinkId(Id.createLinkId("i(5,5)R"));
				builder.setEndTimeWindow(TimeWindow.newInstance(0, (24 * 3600)));
				builder.setStartTimeWindow(TimeWindow.newInstance(0, (24 * 3600)));
				builder.setDeliveryServiceTime(capacityDemand * 60);

				shipmentList.add(builder.build());

				return shipmentList;
			}
			case tenCustomers -> {
				Random rand1 = MatsimRandom.getLocalInstance();
				Random rand2 = MatsimRandom.getLocalInstance();


				List<String> zoneLinkList = Arrays.asList("i(4,4)", "i(5,4)", "i(6,4)", "i(4,6)", "i(5,6)", "i(6,6)",
						"j(3,5)", "j(3,6)", "j(3,7)", "j(5,5)", "j(5,6)", "j(5,7)",
						"i(4,5)R", "i(5,5)R", "i(6,5)R", "i(4,7)R", "i(5,7)R", "i(6,7)R",
						"j(4,5)R", "j(4,6)R", "j(4,7)R", "j(6,5)R", "j(6,6)R", "j(6,7)R");
				for (String linkIdString : zoneLinkList) {
					if (!network.getLinks().containsKey( Id.createLinkId(linkIdString))) {
						throw new RuntimeException("Link is not in Network!");
					}
				}
				
				for(int i = 1; i <= 10; i++) {
					Id<LSPShipment> id = Id.create("Shipment_" + i, LSPShipment.class);
					ShipmentUtils.LSPShipmentBuilder builder = ShipmentUtils.LSPShipmentBuilder.newInstance(id);

					int capacityDemand = rand1.nextInt(4);
					builder.setCapacityDemand(1+capacityDemand);

					builder.setFromLinkId(DEPOT_LINK_ID);
					final Id<Link> toLinkId = Id.createLinkId(zoneLinkList.get(rand2.nextInt(zoneLinkList.size()-1)));
					builder.setToLinkId(toLinkId);

					builder.setEndTimeWindow(TimeWindow.newInstance(0, (24 * 3600)));
					builder.setStartTimeWindow(TimeWindow.newInstance(0, (24 * 3600)));
					builder.setDeliveryServiceTime(capacityDemand * 60);

					shipmentList.add(builder.build());
				}
				return shipmentList;
			}
			default -> throw new IllegalStateException("Unexpected value: " + demandSetting);
		}
	}

	//TODO: This is maybe something that can go into a utils class ... KMT jul22
	private static List<LSPResource> createResourcesListFromLSPPlans(List<LSPPlan> lspPlans) {
		log.info("Collecting all LSPResources from the LSPPlans");
		List<LSPResource> resourcesList = new ArrayList<>();            //TODO: Mache daraus ein Set, damit jede Resource nur einmal drin ist? kmt Feb22
		for (LSPPlan lspPlan : lspPlans) {
			for (LogisticChain solution : lspPlan.getLogisticChain()) {
				for (LogisticChainElement solutionElement : solution.getLogisticChainElements()) {
					resourcesList.add(solutionElement.getResource());
				}
			}
		}
		return resourcesList;
	}


	//		@Override public ScoringFunction createScoringFunction(Carrier carrier ){
//
//			return new ScoringFunction(){
//
//				private double score;
//
//				@Override public void handleActivity( Activity activity ){
//					score--;
//				}
//				@Override public void handleLeg( Leg leg ){
//					score = score - 10;
//				}
//				@Override public void agentStuck( double time ){
//				}
//				@Override public void addMoney( double amount ){
//				}
//				@Override public void addScore( double amount ){
//				}
//				@Override public void finish(){
//				}
//				@Override public double getScore(){
//					return score;
//				}
//				@Override public void handleEvent( Event event ){
//					score = score - 0.01;
//				}
//			};
//		}
//	}

}
