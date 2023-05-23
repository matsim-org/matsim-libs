package example.lsp.multipleChains;

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
import org.matsim.contrib.freight.controler.FreightUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
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

public class ExampleThreeChains {

	static final double HUBCOSTS_FIX = 100;
	private static final double TOLL_VALUE = 0;

	private static final Logger log = LogManager.getLogger(ExampleThreeChains.class);

	private static final Id<Link> DEPOT_SOUTH_LINK_ID = Id.createLinkId("i(1,0)");
	private static final Id<Link> DEPOT_NORTH_LINK_ID = Id.createLinkId("i(1,8)");
	private static final Id<Link> HUB_LINK_ID = Id.createLinkId("i(5,8)");

	private static final VehicleType VEH_TYPE_LARGE_50 = CarrierVehicleType.Builder.newInstance(Id.create("large50", VehicleType.class))
			.setCapacity(50)
			.setMaxVelocity(10)
			.setFixCost(100)
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

	private ExampleThreeChains() {
	}

	public static void main(String[] args) {
		log.info("Prepare config");
		Config config = prepareConfig(args);

		log.info("Prepare scenario");
		Scenario scenario = prepareScenario(config);

		log.info("Prepare controler");
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				install(new LSPModule());
			}
		});

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				final MyEventBasedCarrierScorer carrierScorer = new MyEventBasedCarrierScorer();
				carrierScorer.setToll(TOLL_VALUE);

				bind(CarrierScoringFunctionFactory.class).toInstance(carrierScorer);
				bind(CarrierStrategyManager.class).toProvider(() -> {
					CarrierStrategyManager strategyManager = FreightUtils.createDefaultCarrierStrategyManager();
					strategyManager.addStrategy(new GenericPlanStrategyImpl<>(new BestPlanSelector<>()), null, 1);
					return strategyManager;
				});
				bind( LSPStrategyManager.class ).toProvider(() -> {
					LSPStrategyManager strategyManager = new LSPStrategyManagerImpl();
					strategyManager.addStrategy(new GenericPlanStrategyImpl<>(new BestPlanSelector<>()), null, 1);
					return strategyManager;
				});
				bind(LSPScorerFactory.class).toInstance( () -> new MyLSPScorer());
			}
		});

		log.info("Run MATSim");

		//The VSP default settings are designed for person transport simulation. After talking to Kai, they will be set to WARN here. Kai MT may'23
		controler.getConfig().vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);
		controler.run();

		new LSPPlanXmlWriter(LSPUtils.getLSPs(controler.getScenario())).write(controler.getConfig().controler().getOutputDirectory() + "/lsps.xml");
		new LSPPlanXmlReader(LSPUtils.getLSPs(controler.getScenario()), FreightUtils.getCarriers(controler.getScenario()));
		new CarrierPlanWriter(FreightUtils.getCarriers(controler.getScenario())).write(controler.getConfig().controler().getOutputDirectory() + "/carriers.xml");

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
			ConfigUtils.applyCommandline(config,args);
		} else {
			config.controler().setOutputDirectory("output/3chains");
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
		LSPPlan lspPlan_oneChain;
		{
			Carrier singleCarrier = CarrierUtils.createCarrier(Id.create("singleCarrier", Carrier.class));
			singleCarrier.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);

			CarrierUtils.addCarrierVehicle(singleCarrier, CarrierVehicle.newInstance(Id.createVehicleId("directTruck"), DEPOT_SOUTH_LINK_ID, VEH_TYPE_LARGE_50));
			LSPResource singleCarrierResource = UsecaseUtils.DistributionCarrierResourceBuilder.newInstance(singleCarrier, network)
					.setDistributionScheduler(UsecaseUtils.createDefaultDistributionCarrierScheduler())
					.build();

			LogisticChainElement singleCarrierElement = LSPUtils.LogisticChainElementBuilder.newInstance(Id.create("singleCarrierElement", LogisticChainElement.class))
					.setResource(singleCarrierResource)
					.build();

			LogisticChain singleChain = LSPUtils.LogisticChainBuilder.newInstance(Id.create("singleChain", LogisticChain.class))
					.addLogisticChainElement(singleCarrierElement)
					.build();

			final ShipmentAssigner singleSolutionShipmentAssigner = UsecaseUtils.createSingleLogisticChainShipmentAssigner();
			lspPlan_oneChain = LSPUtils.createLSPPlan()
					.addLogisticChain(singleChain)
					.setAssigner(singleSolutionShipmentAssigner);
		}

		// A plan with two different logistic chain, one with hub, one with direct delivery
		LSPPlan lspPlan_threeChains;
		{
			LogisticChain hubChain;
			{
				LogisticChainElement mainCarrierElement;
				{
					Carrier mainCarrier = CarrierUtils.createCarrier(Id.create("mainCarrier", Carrier.class));
					mainCarrier.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);

					CarrierUtils.addCarrierVehicle(mainCarrier, CarrierVehicle.newInstance(Id.createVehicleId("mainTruck"), DEPOT_SOUTH_LINK_ID, VEH_TYPE_LARGE_50));
					LSPResource mainCarrierRessource = UsecaseUtils.MainRunCarrierResourceBuilder.newInstance(mainCarrier, network)
							.setFromLinkId(DEPOT_SOUTH_LINK_ID)
							.setMainRunCarrierScheduler(UsecaseUtils.createDefaultMainRunCarrierScheduler())
							.setToLinkId(HUB_LINK_ID)
							.setVehicleReturn(UsecaseUtils.VehicleReturn.returnToFromLink)
							.build();

					mainCarrierElement = LSPUtils.LogisticChainElementBuilder.newInstance(Id.create("mainCarrierElement", LogisticChainElement.class))
							.setResource(mainCarrierRessource)
							.build();
				}

				LogisticChainElement hubElement;
				{
					LSPResourceScheduler hubScheduler = UsecaseUtils.TranshipmentHubSchedulerBuilder.newInstance()
							.setCapacityNeedFixed(10) //Time needed, fixed (for Scheduler)
							.setCapacityNeedLinear(1) //additional time needed per shipmentSize (for Scheduler)
							.build();

					LSPResource hubResource = UsecaseUtils.TransshipmentHubBuilder.newInstance(Id.create("hub", LSPResource.class), HUB_LINK_ID, scenario)
							.setTransshipmentHubScheduler(hubScheduler)
							.build();
					LSPUtils.setFixedCost(hubResource, HUBCOSTS_FIX);

					hubElement = LSPUtils.LogisticChainElementBuilder.newInstance(Id.create("hubElement", LogisticChainElement.class))
							.setResource(hubResource)
							.build();
				}

				LogisticChainElement distributionCarrierElement;
				{
					Carrier distributionCarrier = CarrierUtils.createCarrier(Id.create("distributionCarrier", Carrier.class));
					distributionCarrier.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);

					CarrierUtils.addCarrierVehicle(distributionCarrier, CarrierVehicle.newInstance(Id.createVehicleId("distributionTruck"), HUB_LINK_ID, VEH_TYPE_SMALL_05));
					LSPResource distributionCarrierRessource = UsecaseUtils.DistributionCarrierResourceBuilder.newInstance(distributionCarrier, network)
							.setDistributionScheduler(UsecaseUtils.createDefaultDistributionCarrierScheduler())
							.build();

					distributionCarrierElement = LSPUtils.LogisticChainElementBuilder.newInstance(Id.create("distributionCarrierElement", LogisticChainElement.class))
							.setResource(distributionCarrierRessource)
							.build();
				}

				mainCarrierElement.connectWithNextElement(hubElement);
				hubElement.connectWithNextElement(distributionCarrierElement);

				hubChain = LSPUtils.LogisticChainBuilder.newInstance(Id.create("hubChain", LogisticChain.class))
						.addLogisticChainElement(mainCarrierElement)
						.addLogisticChainElement(hubElement)
						.addLogisticChainElement(distributionCarrierElement)
						.build();
			}

			LogisticChain southChain = createLogisticChainSouth(network);
			LogisticChain northChain = createLogisticChainNorth(network);

			lspPlan_threeChains = LSPUtils.createLSPPlan()
					.addLogisticChain(southChain)
					.addLogisticChain(northChain)
					.addLogisticChain(hubChain)
					.setAssigner(Utils.createConsecutiveLogisticChainShipmentAssigner());
		}


		List<LSPPlan> lspPlans = new ArrayList<>();
//		lspPlans.add(lspPlan_oneChain);
		lspPlans.add(lspPlan_threeChains);

		LSP lsp = LSPUtils.LSPBuilder.getInstance(Id.create("myLSP", LSP.class))
				.setInitialPlan(lspPlan_threeChains)
				.setLogisticChainScheduler(UsecaseUtils.createDefaultSimpleForwardLogisticChainScheduler(createResourcesListFromLSPPlans(lspPlans)))
				.build();
//		lsp.addPlan(lspPlan_threeChains);

		log.info("create initial LSPShipments");
		log.info("assign the shipments to the LSP");
		for (LSPShipment shipment : createInitialLSPShipments(network)) {
			lsp.assignShipmentToLSP(shipment);
		}

		log.info("schedule the LSP with the shipments and according to the scheduler of the Resource");
		lsp.scheduleLogisticChains();

		return lsp;
	}

	private static LogisticChain createLogisticChainSouth(Network network) {
		LogisticChain southChain;
		LogisticChainElement southCarrierElement;
		{
			Carrier carrierSouth = CarrierUtils.createCarrier(Id.create("carrierSouth", Carrier.class));
			carrierSouth.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);

			CarrierUtils.addCarrierVehicle(carrierSouth, CarrierVehicle.newInstance(Id.createVehicleId("directTruck"), DEPOT_SOUTH_LINK_ID, VEH_TYPE_LARGE_50));
			LSPResource carrierSouthResource = UsecaseUtils.DistributionCarrierResourceBuilder.newInstance(carrierSouth, network)
					.setDistributionScheduler(UsecaseUtils.createDefaultDistributionCarrierScheduler())
					.build();

			southCarrierElement = LSPUtils.LogisticChainElementBuilder.newInstance(Id.create("southCarrierElement", LogisticChainElement.class))
					.setResource(carrierSouthResource)
					.build();
		}
		southChain = LSPUtils.LogisticChainBuilder.newInstance(Id.create("southChain", LogisticChain.class))
				.addLogisticChainElement(southCarrierElement)
				.build();
		return southChain;
	}

	private static LogisticChain createLogisticChainNorth(Network network) {
		LogisticChain northChain;
		LogisticChainElement northCarrierElement;
		{
			Carrier carrierNorth = CarrierUtils.createCarrier(Id.create("carrierNorth", Carrier.class));
			carrierNorth.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);

			CarrierUtils.addCarrierVehicle(carrierNorth, CarrierVehicle.newInstance(Id.createVehicleId("directTruck"), DEPOT_NORTH_LINK_ID, VEH_TYPE_LARGE_50));
			LSPResource carrierNorthResource = UsecaseUtils.DistributionCarrierResourceBuilder.newInstance(carrierNorth, network)
					.setDistributionScheduler(UsecaseUtils.createDefaultDistributionCarrierScheduler())
					.build();

			northCarrierElement = LSPUtils.LogisticChainElementBuilder.newInstance(Id.create("northCarrierElement", LogisticChainElement.class))
					.setResource(carrierNorthResource)
					.build();
		}
		northChain = LSPUtils.LogisticChainBuilder.newInstance(Id.create("northChain", LogisticChain.class))
				.addLogisticChainElement(northCarrierElement)
				.build();
		return northChain;
	}

	private static Collection<LSPShipment> createInitialLSPShipments(Network network) {
		List<LSPShipment> shipmentList = new ArrayList<>();

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

			int capacityDemand = 1;
			builder.setCapacityDemand(capacityDemand);

			builder.setFromLinkId(DEPOT_SOUTH_LINK_ID);
			final Id<Link> toLinkId = Id.createLinkId(zoneLinkList.get(rand2.nextInt(zoneLinkList.size()-1)));
			builder.setToLinkId(toLinkId);

			builder.setEndTimeWindow(TimeWindow.newInstance(0, (24 * 3600)));
			builder.setStartTimeWindow(TimeWindow.newInstance(0, (24 * 3600)));
			builder.setDeliveryServiceTime(capacityDemand * 60);

			shipmentList.add(builder.build());
		}
		return shipmentList;
	}

	private static List<LSPResource> createResourcesListFromLSPPlans(List<LSPPlan> lspPlans) {
		log.info("Collecting all LSPResources from the LSPPlans");
		List<LSPResource> resourceList = new ArrayList<>();
		for (LSPPlan lspPlan : lspPlans) {
			for (LogisticChain solution : lspPlan.getLogisticChains()) {
				for (LogisticChainElement solutionElement : solution.getLogisticChainElements()) {
					resourceList.add(solutionElement.getResource());
				}
			}
		}
		return resourceList;
	}

}
