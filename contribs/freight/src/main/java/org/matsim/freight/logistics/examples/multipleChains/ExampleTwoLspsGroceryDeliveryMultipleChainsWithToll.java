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
import org.matsim.analysis.personMoney.PersonMoneyEventsAnalysisModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.roadpricing.*;
import org.matsim.core.config.CommandLine;
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
import org.matsim.core.utils.misc.Time;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.controller.CarrierControllerUtils;
import org.matsim.freight.carriers.controller.CarrierScoringFunctionFactory;
import org.matsim.freight.carriers.controller.CarrierStrategyManager;
import org.matsim.freight.logistics.*;
import org.matsim.freight.logistics.examples.ExampleConstants;
import org.matsim.freight.logistics.examples.MyLSPScorer;
import org.matsim.freight.logistics.resourceImplementations.CarrierSchedulerUtils;
import org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils;
import org.matsim.freight.logistics.shipment.LspShipment;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 * This bases on {@link ExampleTwoLspsGroceryDeliveryMultipleChains}.
 * It is extended in a way that it will use the roadpricing contrib...
 * This class is here only for development and will be merged into {@link ExampleTwoLspsGroceryDeliveryMultipleChains}
 * once the result is satisfying.
 * <p></p>
 * KMT, Jul'24
 */
final class ExampleTwoLspsGroceryDeliveryMultipleChainsWithToll {

	private static final Logger log = LogManager.getLogger(ExampleTwoLspsGroceryDeliveryMultipleChainsWithToll.class);

	private static double HUBCOSTS_FIX;

	private static final List<String> TOLLED_LINKS = ExampleConstants.TOLLED_LINK_LIST_BERLIN_BOTH_DIRECTIONS;
	private static List<String> TOLLED_VEHICLE_TYPES; //  Für welche Fahrzeugtypen soll das MautSchema gelten?
	private static  double TOLL_VALUE ;

	private static final String CARRIER_PLAN_FILE = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/freight/foodRetailing_wo_rangeConstraint/input/CarrierLEH_v2_withFleet_Shipment_OneTW_PickupTime_ICEVandBEV.xml";
	private static final String VEHICLE_TYPE_FILE = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/freight/foodRetailing_wo_rangeConstraint/input/vehicleTypesBVWP100_DC_noTax.xml";

	private static int MATSIM_ITERATIONS;
	private static String OUTPUT_DIRECTORY ;
	private static int jspritIterationsDistributionCarrier = 10;
	private static int jspritIterationsMainCarrier = 1;
	private static int jspritIterationsDirectCarrier = 10;

	enum TypeOfLsps {
		ONE_PLAN_ONLY_DIRECT_CHAIN,
		ONE_PLAN_ONLY_TWO_ECHELON_CHAIN,
		ONE_PLAN_BOTH_CHAINS,
		ONE_PLAN_ALL_LSPS,
		TWO_PLANS_DIRECT_AND_2ECHELON,
		THREE_PLANS_DIRECT_AND_2ECHELON_AND_BOTH
	}

	private record LspDefinition(String name, String carrierId, Id<Link> hubLinkId, List<String> vehTypesDirect, List<String> vehicleTypesMain, List<String> vehicleTypesDelivery) {}

	private ExampleTwoLspsGroceryDeliveryMultipleChainsWithToll() {}


	/**
	 * The following is configured using command line option.
	 * Please have a look to the {@link RunExampleTwoLspsGroceryDeliveryMultipleChainsWithToll} class and / or to the tests.
	 * @param args
	 * @throws CommandLine.ConfigurationException
	 */
	public static void main(String[] args) throws CommandLine.ConfigurationException {

		List<LspDefinition> lspDefinitionList = new LinkedList<>();

		CommandLine cmd = new CommandLine.Builder(args) //
			.allowAnyOption(true)
			.allowPositionalArguments(false)
			.build();

		//Muss noch weiter auf die anderen Optionen angepasst werden.
		MATSIM_ITERATIONS = cmd.getOption("matsimIterations").map(Integer::parseInt).orElse(1); // I know that MATSim-iters can be set more directly.
		OUTPUT_DIRECTORY = cmd.getOption("outputDirectory").orElse("output/groceryDeliveryWToll_"+MATSIM_ITERATIONS+"it"); // Todo: replace with the central setting: --config:controller.outputDirectory

		jspritIterationsMainCarrier = cmd.getOption("jspritIterationsMain").map(Integer::parseInt).orElse(1);
		jspritIterationsDirectCarrier = cmd.getOption("jspritIterationsDirect").map(Integer::parseInt).orElse(1);
		jspritIterationsDistributionCarrier = cmd.getOption("jspritIterationsDistribution").map(Integer::parseInt).orElse(1);
		TOLL_VALUE = cmd.getOption("tollValue").map(Double::parseDouble).orElse(0.0);
		TOLLED_VEHICLE_TYPES = cmd.getOption("tolledVehicleTypes")
			.map(s -> Arrays.asList(s.split(",")))
			.orElse(new ArrayList<>()); //  Für welche Fahrzeugtypen soll das MautSchema gelten?
		HUBCOSTS_FIX = cmd.getOption("HubCostsFix").map(Double::parseDouble).orElse(100.0);

		// Which types of LSP(s) should be created?
		final TypeOfLsps typeOfLsps = cmd.getOption("typeOfLsps")
			.map(TypeOfLsps::valueOf)
			.orElse(null); // Default is DIRECT_AND_TWO_ECHELON

		//lsp1
		LspDefinition lsp1Definition = new LspDefinition(
			cmd.getOption("lsp1Name").orElse(null),
			cmd.getOption("lsp1CarrierId").orElse(null), //The carrier used to build the LSP from.
			cmd.getOption("lsp1HubLinkId")
				.map(Id::createLinkId)
				.orElse(null), // Default is the hub link of Edeka in Berlin: 91085 = Neukölln nahe S-Bahn-Ring
			//Vehicle types for direct chain
			cmd.getOption("lsp1vehTypesDirect")
				.map(s -> Arrays.asList(s.split(",")))
				.orElse(null),
			//Vehicle types for main run of 2-echelon chain
			cmd.getOption("lsp1vehTypesMain")
				.map(s -> Arrays.asList(s.split(",")))
				.orElse(null),
			//Vehicle types for delivery run of 2-echelon chain
			cmd.getOption("lsp1vehTypesDelivery")
				.map(s -> Arrays.asList(s.split(",")))
				.orElse(null)
		);
		//these two values needs to be set for all uses. The others depend on the setting "typeOfLsps". Will not look at them here.
		if (lsp1Definition.name()!=null && lsp1Definition.carrierId()!=null) {
			lspDefinitionList.add(lsp1Definition);
		}


		//lsp2
		LspDefinition lsp2Definition = new LspDefinition(
			cmd.getOption("lsp2Name").orElse(null),
			cmd.getOption("lsp2CarrierId").orElse(null),
			cmd.getOption("lsp2HubLinkId")
				.map(Id::createLinkId)
				.orElse(null), // Default is the hub link of Kaufland in Berlin: 91085 = Neukölln nahe S-Bahn-Ring
			//Vehicle types for direct chain
			cmd.getOption("lsp2vehTypesDirect")
				.map(s -> Arrays.asList(s.split(",")))
				.orElse(null),
			//Vehicle types for main run of 2-echelon chain
			cmd.getOption("lsp2vehTypesMain")
				.map(s -> Arrays.asList(s.split(",")))
				.orElse(null),
			//Vehicle types for delivery run of 2-echelon chain
			cmd.getOption("lsp2vehTypesDelivery")
				.map(s -> Arrays.asList(s.split(",")))
				.orElse(null)
		);
		//these two values needs to be set for all uses. The others depend on the setting "typeOfLsps". Will not look at them here.
		if (lsp2Definition.name()!=null && lsp2Definition.carrierId()!=null) {
			lspDefinitionList.add(lsp2Definition);
		}


		log.info("Prepare config");
		Config config = prepareConfig(args);

		log.info("Prepare scenario");
		Scenario scenario = ScenarioUtils.loadScenario(config);

		CarrierVehicleTypes vehicleTypesAvailable = new CarrierVehicleTypes();
		CarrierVehicleTypeReader vehicleTypeReader = new CarrierVehicleTypeReader(vehicleTypesAvailable);
		vehicleTypeReader.readFile(VEHICLE_TYPE_FILE);
		//The following is needed, because since fall 2024 the vehicle types are not assigned to a network mode by default.
		for (VehicleType vehicleType : vehicleTypesAvailable.getVehicleTypes().values()) {
			vehicleType.setNetworkMode(TransportMode.car);
		}

		Carriers carriers = new Carriers();
		CarrierPlanXmlReader carrierReader = new CarrierPlanXmlReader(carriers, vehicleTypesAvailable);
		carrierReader.readFile(CARRIER_PLAN_FILE);

		RoadPricingScheme rpScheme = setUpRoadpricing(scenario);

		log.info("Add LSP(s) to the scenario");
		Collection<LSP> lsps = new LinkedList<>();

		for (LspDefinition lspDefinition : lspDefinitionList) {
			final Carrier carrier = carriers.getCarriers().get(Id.create(lspDefinition.carrierId(), CarrierImpl.class));
			final Collection<LspShipment> lspShipmentsFromCarrierShipments = MultipleChainsUtils.createLSPShipmentsFromCarrierShipments(carrier);

			final CarrierVehicleTypes vehTypesDirect = extractVehicleTypes(lspDefinition.vehTypesDirect(), vehicleTypesAvailable);
			final CarrierVehicleTypes vehTypesMain = extractVehicleTypes(lspDefinition.vehicleTypesMain(), vehicleTypesAvailable);
			final CarrierVehicleTypes vehTypesDelivery = extractVehicleTypes(lspDefinition.vehicleTypesDelivery(), vehicleTypesAvailable);

			switch (typeOfLsps) {
				case ONE_PLAN_ONLY_DIRECT_CHAIN -> {
					LSP lsp = createLsp1PlanWith1Chain_Direct(
						scenario,
						lspDefinition.name()+ "_DIRECT",
						getDepotLinkFromVehicle(carrier),
						vehTypesDirect);
					assignLspShipments(lsp, lspShipmentsFromCarrierShipments);
					lsps.add(lsp);
				}
				case ONE_PLAN_ONLY_TWO_ECHELON_CHAIN -> {
					LSP lsp = createLsp1PlanWith1Chain_2echelon(
						scenario,
						lspDefinition.name() + "_2echelon",
						getDepotLinkFromVehicle(carrier),
						lspDefinition.hubLinkId(),
						vehTypesMain,
						vehTypesDelivery);
					assignLspShipments(lsp, lspShipmentsFromCarrierShipments);
					lsps.add(lsp);
				}
				case ONE_PLAN_BOTH_CHAINS -> {
					LSP lsp = createLsp1Plan2Chains(
						scenario,
						lspDefinition.name(),
						getDepotLinkFromVehicle(carrier),
						lspDefinition.hubLinkId(),
						vehTypesDirect,
						vehTypesMain,
						vehTypesDelivery
					);
					assignLspShipments(lsp, lspShipmentsFromCarrierShipments);
					lsps.add(lsp);
				}
				case ONE_PLAN_ALL_LSPS -> {
					LSP lsp = createLsp1PlanWith1Chain_Direct(scenario,
						lspDefinition.name() + "_DIRECT",
						getDepotLinkFromVehicle(carrier),
						vehTypesDirect);
					assignLspShipments(lsp, lspShipmentsFromCarrierShipments);
					lsps.add(lsp);

					lsp = createLsp1PlanWith1Chain_2echelon(scenario,
						lspDefinition.name() + "_2echelon",
						getDepotLinkFromVehicle(carrier),
						lspDefinition.hubLinkId(),
						vehTypesMain,
						vehTypesDelivery);
					assignLspShipments(lsp, lspShipmentsFromCarrierShipments);
					lsps.add(lsp);

					lsp = createLsp1Plan2Chains(scenario,
						lspDefinition.name(),
						getDepotLinkFromVehicle(carrier),
						lspDefinition.hubLinkId(),
						vehTypesDirect,
						vehTypesMain,
						vehTypesDelivery
					);
					assignLspShipments(lsp, lspShipmentsFromCarrierShipments);
					lsps.add(lsp);
				}
				case TWO_PLANS_DIRECT_AND_2ECHELON -> {
					LSP lsp = createLsp2PlansOneChainEach(scenario,
						lspDefinition.name(),
						getDepotLinkFromVehicle(carrier),
						lspDefinition.hubLinkId(),
						vehTypesDirect,
						vehTypesMain,
						vehTypesDelivery
					);
					assignLspShipments(lsp, lspShipmentsFromCarrierShipments);
					lsps.add(lsp);
				}
				case THREE_PLANS_DIRECT_AND_2ECHELON_AND_BOTH -> {
					LSP lsp = createLsp3PlansWithOneChainEach(scenario,
						lspDefinition.name(),
						getDepotLinkFromVehicle(carrier),
						lspDefinition.hubLinkId(),
						vehTypesDirect,
						vehTypesMain,
						vehTypesDelivery
					);
					assignLspShipments(lsp, lspShipmentsFromCarrierShipments);
					lsps.add(lsp);
				}
				default -> throw new IllegalStateException("Unexpected value: " + typeOfLsps);
			}
		}

		LSPUtils.loadLspsIntoScenario(scenario, lsps);
		LSPUtils.scheduleLsps(LSPUtils.getLSPs(scenario));

		Controller controller = prepareController(scenario, rpScheme);

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
		}

		config.controller().setOutputDirectory(OUTPUT_DIRECTORY);
		config.controller().setLastIteration(MATSIM_ITERATIONS);

		config.network().setInputFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz");
		config.global().setCoordinateSystem("EPSG:31468");
		config.global().setRandomSeed(4177);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		FreightCarriersConfigGroup freightConfig = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfig.setTimeWindowHandling(FreightCarriersConfigGroup.TimeWindowHandling.ignore);

		return config;
	}

	private static Controller prepareController(Scenario scenario, RoadPricingScheme rpScheme) {
		log.info("Prepare controller");
		Controller controller = ControllerUtils.createController(scenario);
		controller.addOverridingModule(
			new AbstractModule() {
				@Override
				public void install() {
					install(new LSPModule());
					install(new PersonMoneyEventsAnalysisModule());
				}
			});

		controller.addOverridingModule(
			new AbstractModule() {
				@Override
				public void install() {
					bind(CarrierScoringFunctionFactory.class).toInstance(new EventBasedCarrierScorer4MultipleChainsInclToll());
					bind(LSPScorerFactory.class).toInstance(MyLSPScorer::new);
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
								strategyManager.addStrategy(new GenericPlanStrategyImpl<>(new ExpBetaPlanSelector<>(new ScoringConfigGroup())), null, 1);
								strategyManager.addStrategy(RandomShiftingStrategyFactory.createStrategy(), null, 4);
								strategyManager.setMaxPlansPerAgent(5);
								strategyManager.setPlanSelectorForRemoval(new GenericWorstPlanForRemovalSelector<>());
								return strategyManager;
							});
				}
			});
		if (!rpScheme.getTolledLinkIds().isEmpty()) {
			// RoadPricing.configure(controller);
			controller.addOverridingModule( new RoadPricingModule(rpScheme) );
		}
		return controller;
	}

	/*
	 *  Set up roadpricing --- this is a copy paste from KMT lecture in GVSim --> need some adaptions
	 * TODO Adapt settings
	 */


	/**
	 * Creates an LSP with direct chains:
	 *
	 * @param scenario           the scenario, used e.g. for getting the network and register some stuff
	 * @param lspName            String of LSP's Id
	 * @param depotLinkId        Id of the depot link
	 * @param vehicleTypesDirect vehicle types for the direct run (direct chain)
	 * @return the LSP
	 */
	private static LSP createLsp1PlanWith1Chain_Direct(Scenario scenario, String lspName, Id<Link> depotLinkId, CarrierVehicleTypes vehicleTypesDirect) {
		log.info("create LSP with direct chain");

		LSPPlan lspPlan = LSPUtils.createLSPPlan()
			.addLogisticChain(createDirectChain(scenario, lspName, depotLinkId, vehicleTypesDirect))
			.setInitialShipmentAssigner(MultipleChainsUtils.createRandomLogisticChainShipmentAssigner());

		return  LSPUtils.LSPBuilder.getInstance(Id.create(lspName, LSP.class))
			.setInitialPlan(lspPlan)
			.setLogisticChainScheduler(ResourceImplementationUtils.createDefaultSimpleForwardLogisticChainScheduler(createResourcesListFromLSPPlans(List.of(lspPlan))))
			.build();
	}

	/**
	 * Creates an LSP with one chains:
	 * - 2-echelon delivery
	 * <p></p>
	 *  TODO: Es sollten dann am Besten direkt die zur Verfügung stehenden VehicleTypes übergeben werden und diese dann hier jeweils (alle) hinzugefügt werden.
	 *  Aktuell erfolgt die Auswahl ja noch hier. Das sollte dann aber nicht mehr so sein, sondern bereits weiter upstream definiert werden.
	 *  kmt Jul'24
	 *
	 * @param scenario                    the scenario, used e.g. for getting the network and register some stuff
	 * @param lspName                     String of LSP's Id
	 * @param depotLinkId                 Id of the depot link
	 * @param hubLinkId                   location of the hub
	 * @param vehicleTypesMainRun         vehicle types for the main run (2e-chain)
	 * @param vehicleTypesDistributionRun vehicle types for the distribution run (2e-chain)
	 * @return the LSP
	 */
	@SuppressWarnings("SameParameterValue")
	private static LSP createLsp1PlanWith1Chain_2echelon(Scenario scenario, String lspName, Id<Link> depotLinkId, Id<Link> hubLinkId, CarrierVehicleTypes vehicleTypesMainRun, CarrierVehicleTypes vehicleTypesDistributionRun) {
		log.info("create LSP with 1 chains: 2-echelon");
		//Chains
		LogisticChain twoEchelonChain = createTwoEchelonChain(scenario, lspName, hubLinkId, depotLinkId, vehicleTypesMainRun, vehicleTypesDistributionRun);

		LSPPlan lspPlan = LSPUtils.createLSPPlan()
			.addLogisticChain(twoEchelonChain)
			.setInitialShipmentAssigner(MultipleChainsUtils.createRandomLogisticChainShipmentAssigner());

		return LSPUtils.LSPBuilder.getInstance(Id.create(lspName, LSP.class))
			.setInitialPlan(lspPlan)
			.setLogisticChainScheduler(ResourceImplementationUtils.createDefaultSimpleForwardLogisticChainScheduler(createResourcesListFromLSPPlans(List.of(lspPlan))))
			.build();
	}

	/**
	 * Creates an LSP with two chains in one plan:
	 * - direct delivery
	 * - 2-echelon delivery
	 * <p></p>
	 *  TODO: Es sollten dann am Besten direkt die zur Verfügung stehenden VehicleTypes übergeben werden und diese dann hier jeweils (alle) hinzugefügt werden.
	 *  Aktuell erfolgt die Auswahl ja noch hier. Das sollte dann aber nicht mehr so sein, sondern bereits weiter upstream definiert werden.
	 *  kmt Jul'24
	 *
	 * @param scenario                    the scenario, used e.g. for getting the network and register some stuff
	 * @param lspName                     String of LSP's Id
	 * @param depotLinkId                 Id of the depot link
	 * @param hubLinkId                   location of the hub
	 * @param vehicleTypesDirect          vehicle types for the direct run (direct chain)
	 * @param vehicleTypesMainRun         vehicle types for the main run (2e-chain)
	 * @param vehicleTypesDistributionRun vehicle types for the distribution run (2e-chain)
	 * @return the LSP
	 */
	@SuppressWarnings("SameParameterValue")
	private static LSP createLsp1Plan2Chains(Scenario scenario, String lspName, Id<Link> depotLinkId, Id<Link> hubLinkId, CarrierVehicleTypes vehicleTypesDirect, CarrierVehicleTypes vehicleTypesMainRun, CarrierVehicleTypes vehicleTypesDistributionRun) {
		log.info("create LSP with 2 chains: direct and two-echelon");
		//Chains
		LogisticChain directChain = createDirectChain(scenario, lspName, depotLinkId, vehicleTypesDirect);
		LogisticChain twoEchelonChain = createTwoEchelonChain(scenario, lspName, hubLinkId, depotLinkId, vehicleTypesMainRun, vehicleTypesDistributionRun);

		LSPPlan lspPlan = LSPUtils.createLSPPlan()
			.addLogisticChain(directChain)
			.addLogisticChain(twoEchelonChain)
			.setInitialShipmentAssigner(MultipleChainsUtils.createRandomLogisticChainShipmentAssigner());

		return LSPUtils.LSPBuilder.getInstance(Id.create(lspName, LSP.class))
			.setInitialPlan(lspPlan)
			.setLogisticChainScheduler(ResourceImplementationUtils.createDefaultSimpleForwardLogisticChainScheduler(createResourcesListFromLSPPlans(List.of(lspPlan))))
			.build();
	}

	/**
	 * Creates an LSP with two chains on different plans:
	 * - direct delivery
	 * - 2-echelon delivery
	 * <p></p>
	 *  TODO: Es sollten dann am Besten direkt die zur Verfügung stehenden VehicleTypes übergeben werden und diese dann hier jeweils (alle) hinzugefügt werden.
	 *  Aktuell erfolgt die Auswahl ja noch hier. Das sollte dann aber nicht mehr so sein, sondern bereits weiter upstream definiert werden.
	 *  kmt Jul'24
	 *
	 * @param scenario                    the scenario, used e.g. for getting the network and register some stuff
	 * @param lspName                     String of LSP's Id
	 * @param depotLinkId                 Id of the depot link
	 * @param hubLinkId                   location of the hub
	 * @param vehicleTypesDirect          vehicle types for the direct run (direct chain)
	 * @param vehicleTypesMainRun         vehicle types for the main run (2e-chain)
	 * @param vehicleTypesDistributionRun vehicle types for the distribution run (2e-chain)
	 * @return the LSP
	 */
	@SuppressWarnings("SameParameterValue")
	private static LSP createLsp2PlansOneChainEach(Scenario scenario, String lspName, Id<Link> depotLinkId, Id<Link> hubLinkId, CarrierVehicleTypes vehicleTypesDirect, CarrierVehicleTypes vehicleTypesMainRun, CarrierVehicleTypes vehicleTypesDistributionRun) {
		log.info("create LSP with 2 plans, containing 1 chain each: direct and two-echelon");

		List<LSPPlan> lspPlans = new ArrayList<>();
		//Plan 1 with direct chain
		lspPlans.add(LSPUtils.createLSPPlan()
			.addLogisticChain(createDirectChain(scenario, lspName, depotLinkId, vehicleTypesDirect))
			.setInitialShipmentAssigner(MultipleChainsUtils.createRandomLogisticChainShipmentAssigner())
			.setType(MultipleChainsUtils.LspPlanTypes.SINGLE_ONE_ECHELON_CHAIN.toString())
		);

		//Plan 2 with 2-echelon chain
		lspPlans.add(LSPUtils.createLSPPlan()
			.addLogisticChain(createTwoEchelonChain(scenario, lspName, hubLinkId, depotLinkId, vehicleTypesMainRun, vehicleTypesDistributionRun))
			.setInitialShipmentAssigner(MultipleChainsUtils.createRandomLogisticChainShipmentAssigner())
			.setType(MultipleChainsUtils.LspPlanTypes.SINGLE_TWO_ECHELON_CHAIN.toString())
		);

		return LSPUtils.LSPBuilder.getInstance(Id.create(lspName, LSP.class))
			.setInitialPlan(lspPlans.getFirst())
			.setLogisticChainScheduler(ResourceImplementationUtils.createDefaultSimpleForwardLogisticChainScheduler(createResourcesListFromLSPPlans(lspPlans)))
			.build();
	}

	/**
	 * Creates an LSP with three different plans with one differnen chains:
	 * - direct delivery
	 * - 2-echelon delivery
	 * - direct and 2-echelon delivery
	 * <p></p>
	 *  TODO: Es sollten dann am Besten direkt die zur Verfügung stehenden VehicleTypes übergeben werden und diese dann hier jeweils (alle) hinzugefügt werden.
	 *  Aktuell erfolgt die Auswahl ja noch hier. Das sollte dann aber nicht mehr so sein, sondern bereits weiter upstream definiert werden.
	 *  kmt Jul'24
	 *
	 * @param scenario                    the scenario, used e.g. for getting the network and register some stuff
	 * @param lspName                     String of LSP's Id
	 * @param depotLinkId                 Id of the depot link
	 * @param hubLinkId                   location of the hub
	 * @param vehicleTypesDirect          vehicle types for the direct run (direct chain)
	 * @param vehicleTypesMainRun         vehicle types for the main run (2e-chain)
	 * @param vehicleTypesDistributionRun vehicle types for the distribution run (2e-chain)
	 * @return the LSP
	 */
	@SuppressWarnings("SameParameterValue")
	private static LSP createLsp3PlansWithOneChainEach(Scenario scenario, String lspName, Id<Link> depotLinkId, Id<Link> hubLinkId, CarrierVehicleTypes vehicleTypesDirect, CarrierVehicleTypes vehicleTypesMainRun, CarrierVehicleTypes vehicleTypesDistributionRun) {
		log.info("create LSP with 3 plans containing 1 chain each: direct, two-echelon, both");

		List<LSPPlan> lspPlans = new ArrayList<>();
		//Plan 1 with direct chain
		lspPlans.add(LSPUtils.createLSPPlan()
			.addLogisticChain(createDirectChain(scenario, lspName, depotLinkId, vehicleTypesDirect))
			.setInitialShipmentAssigner(MultipleChainsUtils.createRandomLogisticChainShipmentAssigner())
			.setType(MultipleChainsUtils.LspPlanTypes.SINGLE_ONE_ECHELON_CHAIN.toString())
		);

		//Plan 2 with 2-echelon chain
		lspPlans.add(LSPUtils.createLSPPlan()
			.addLogisticChain(createTwoEchelonChain(scenario, lspName, hubLinkId, depotLinkId, vehicleTypesMainRun, vehicleTypesDistributionRun))
			.setInitialShipmentAssigner(MultipleChainsUtils.createRandomLogisticChainShipmentAssigner())
			.setType(MultipleChainsUtils.LspPlanTypes.SINGLE_TWO_ECHELON_CHAIN.toString())
		);

		lspPlans.add(LSPUtils.createLSPPlan()
			.addLogisticChain(createDirectChain(scenario, lspName, depotLinkId, vehicleTypesDirect))
			.addLogisticChain(createTwoEchelonChain(scenario, lspName, hubLinkId, depotLinkId, vehicleTypesMainRun, vehicleTypesDistributionRun))
			.setInitialShipmentAssigner(MultipleChainsUtils.createRandomLogisticChainShipmentAssigner())
			.setType(MultipleChainsUtils.LspPlanTypes.MULTIPLE_MIXED_ECHELON_CHAINS.toString())
		);

		return LSPUtils.LSPBuilder.getInstance(Id.create(lspName, LSP.class))
			.setInitialPlan(lspPlans.getFirst())
			.setLogisticChainScheduler(ResourceImplementationUtils.createDefaultSimpleForwardLogisticChainScheduler(createResourcesListFromLSPPlans(lspPlans)))
			.build();
	}


///////////////////////////
// Helping methods
//////////////////////////
	/**
	 * Creates a two-echelon chain:
	 * - main run (from depot to hub)
	 * - transshipment hub
	 * - distribution run (from hub to customer)
	 * <p></p>
	 * The vehicle types for the main run and distribution run are passed as parameters.
	 * PLEASE NOTE: currently (may'25) the main run does not perform a tour planning. It just takes the first entry... see {MainRunCarrierScheduler.class}
	 */
	private static LogisticChain createTwoEchelonChain(Scenario scenario, String lspName, Id<Link> hubLinkId, Id<Link> depotLinkFromVehicles, CarrierVehicleTypes vehicleTypesMainRun, CarrierVehicleTypes vehicleTypesDistributionRun) {
		LogisticChain hubChain;
		Carrier mainCarrier = CarriersUtils.createCarrier(Id.create(lspName +"_mainCarrier", Carrier.class));
		mainCarrier.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);
		CarriersUtils.setJspritIterations(mainCarrier, jspritIterationsMainCarrier);

		for (VehicleType vehicleType : vehicleTypesMainRun.getVehicleTypes().values()) {
			CarriersUtils.addCarrierVehicle(mainCarrier, CarrierVehicle.newInstance(Id.createVehicleId("mainTruck_" + vehicleType.getId()), depotLinkFromVehicles, vehicleType));
		}

		LSPResource mainCarrierResource = ResourceImplementationUtils.MainRunCarrierResourceBuilder.newInstance(mainCarrier)
			.setFromLinkId(depotLinkFromVehicles)
			.setMainRunCarrierScheduler(ResourceImplementationUtils.createDefaultMainRunCarrierScheduler(scenario))
			.setToLinkId(hubLinkId)
			.setVehicleReturn(ResourceImplementationUtils.VehicleReturn.returnToFromLink)
			.build();

		LogisticChainElement mainCarrierElement = LSPUtils.LogisticChainElementBuilder.newInstance(Id.create("mainCarrierElement", LogisticChainElement.class))
			.setResource(mainCarrierResource)
			.build();

		LSPResourceScheduler hubScheduler = ResourceImplementationUtils.TranshipmentHubSchedulerBuilder.newInstance()
			.setCapacityNeedFixed(10)
			.setCapacityNeedLinear(1)
			.build();

		LSPResource hubResource = ResourceImplementationUtils.TransshipmentHubBuilder.newInstance(Id.create(lspName +"_Hub", LSPResource.class), hubLinkId, scenario)
			.setTransshipmentHubScheduler(hubScheduler)
			.build();

		LSPUtils.setFixedCost(hubResource, HUBCOSTS_FIX);

		LogisticChainElement hubElement =  LSPUtils.LogisticChainElementBuilder.newInstance(Id.create("HubElement", LogisticChainElement.class))
			.setResource(hubResource)
			.build();

		Carrier distributionCarrier = CarriersUtils.createCarrier(Id.create(lspName +"_distributionCarrier", Carrier.class));
		distributionCarrier.getCarrierCapabilities()
			//.setNumberOfJspritIterations // TODO Das mal hier einbauen. --> Ist aktuell in CarrierUtils.
			.setFleetSize(CarrierCapabilities.FleetSize.INFINITE);
		CarriersUtils.setJspritIterations(distributionCarrier, jspritIterationsDistributionCarrier);

		CarrierSchedulerUtils.setVrpLogic(distributionCarrier, LSPUtils.LogicOfVrp.shipmentBased);

		for (VehicleType vehicleType : vehicleTypesDistributionRun.getVehicleTypes().values()) {
			CarriersUtils.addCarrierVehicle(distributionCarrier, CarrierVehicle.newInstance(Id.createVehicleId("distributionTruck_" + vehicleType.getId()), hubLinkId, vehicleType));
		}


		LSPResource distributionCarrierResource = ResourceImplementationUtils.DistributionCarrierResourceBuilder.newInstance(distributionCarrier)
			.setDistributionScheduler(ResourceImplementationUtils.createDefaultDistributionCarrierScheduler(scenario))
			.build();

		LogisticChainElement distributionCarrierElement = LSPUtils.LogisticChainElementBuilder.newInstance(Id.create("distributionCarrierElement", LogisticChainElement.class))
			.setResource(distributionCarrierResource)
			.build();

		mainCarrierElement.connectWithNextElement(hubElement);
		hubElement.connectWithNextElement(distributionCarrierElement);

		hubChain = LSPUtils.LogisticChainBuilder.newInstance(Id.create("hubChain", LogisticChain.class))
			.addLogisticChainElement(mainCarrierElement)
			.addLogisticChainElement(hubElement)
			.addLogisticChainElement(distributionCarrierElement)
			.build();
		return hubChain;
	}


	private static LogisticChain createDirectChain(Scenario scenario, String lspName, Id<Link> depotLinkFromVehicles, CarrierVehicleTypes vehicleTypes) {
		LogisticChain directChain;
		Carrier directCarrier = CarriersUtils.createCarrier(Id.create(lspName + "_directCarrier", Carrier.class));
		directCarrier.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);
		CarriersUtils.setJspritIterations(directCarrier, jspritIterationsDirectCarrier);


		for (VehicleType vehicleType : vehicleTypes.getVehicleTypes().values()) {
			CarriersUtils.addCarrierVehicle(directCarrier, CarrierVehicle.newInstance(Id.createVehicleId("directCarrierTruck_" + vehicleType.getId()), depotLinkFromVehicles, vehicleType));
		}

		LSPResource singleCarrierResource = ResourceImplementationUtils.DistributionCarrierResourceBuilder.newInstance(directCarrier)
			.setDistributionScheduler(ResourceImplementationUtils.createDefaultDistributionCarrierScheduler(scenario))
			.build();
		CarrierSchedulerUtils.setVrpLogic(directCarrier, LSPUtils.LogicOfVrp.shipmentBased);

		LogisticChainElement singleCarrierElement = LSPUtils.LogisticChainElementBuilder.newInstance(Id.create("directCarrierElement", LogisticChainElement.class))
			.setResource(singleCarrierResource)
			.build();

		directChain = LSPUtils.LogisticChainBuilder.newInstance(Id.create("directChain", LogisticChain.class))
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

	/**
	 * Extract the vehicle types from the available types
	 *
	 * @param typeNames List of names of vehicle types to extract
	 * @param availableTypes container with all available vehicle types
	 * @return the extracted vehicle types
	 */
	private static CarrierVehicleTypes extractVehicleTypes(List<String> typeNames, CarrierVehicleTypes availableTypes) {
		CarrierVehicleTypes result = new CarrierVehicleTypes();
		if (typeNames != null) {
			typeNames.forEach(type ->
				result.getVehicleTypes().put(
					Id.createVehicleTypeId(type),
					availableTypes.getVehicleTypes().get(Id.createVehicleTypeId(type))
				)
			);
		}
		return result;
	}

	private static Id<Link> getDepotLinkFromVehicle(Carrier carrier) {
		log.info("Please note: that this method assumes that all vehicles are located at the same depot link.");
		return carrier.getCarrierCapabilities().getCarrierVehicles().values().iterator().next().getLinkId();
	}

	/**
	 * Assigns the LSP shipments to the LSP.
	 * Splits the shipments if needed.
	 *
	 * @param lsp
	 * @param lspShipments
	 */
	private static void assignLspShipments(LSP lsp, Collection<LspShipment> lspShipments) {
		//TODO: Maybe move this out and do this after creating all the LSPs?
		log.info("assign the shipments to the LSP");
		for (LspShipment lspShipment : lspShipments) {
			lsp.assignShipmentToLspPlan(lspShipment);
		}

		//If one of the carriers is not able to handle the shipments, it will be split into smaller shipments.
		lsp = LSPUtils.splitShipmentsIfNeeded(lsp);
	}

	private static RoadPricingSchemeUsingTollFactor setUpRoadpricing(Scenario scenario) {

		//Create Rp Scheme from code.
		RoadPricingSchemeImpl scheme = RoadPricingUtils.addOrGetMutableRoadPricingScheme(scenario );

		/* Configure roadpricing scheme. */
		RoadPricingUtils.setName(scheme, "MautFromCodeKMT");
		RoadPricingUtils.setType(scheme, RoadPricingScheme.TOLL_TYPE_LINK);
		RoadPricingUtils.setDescription(scheme, "Mautdaten erstellt aus Link-Liste.");

		/* Add general toll. */
		for (String linkIdString : TOLLED_LINKS) {
			RoadPricingUtils.addLink(scheme, Id.createLinkId(linkIdString));
		}

		RoadPricingUtils.createAndAddGeneralCost(scheme,
			Time.parseTime("00:00:00"),
			Time.parseTime("72:00:00"),
			TOLL_VALUE);
		///___ End creating from Code

		// Wenn FzgTypId in Liste, erfolgt die Bemautung mit dem Kostensatz (Faktor = 1),
		// sonst mit 0 (Faktor = 0). ((MATSim seite)
		TollFactor tollFactor =
			(personId, vehicleId, linkId, time) -> {
				var vehTypeId = VehicleUtils.findVehicle(vehicleId, scenario).getType().getId();
				if (TOLLED_VEHICLE_TYPES.contains(vehTypeId.toString())) {
					return 1;
				} else {
					return 0;
				}
			};

		return new RoadPricingSchemeUsingTollFactor(scheme, tollFactor);
	}
}
