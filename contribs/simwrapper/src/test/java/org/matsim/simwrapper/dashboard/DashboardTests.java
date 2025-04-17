package org.matsim.simwrapper.dashboard;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimApplication;
import org.matsim.application.options.CsvOptions;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.*;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.controller.CarrierModule;
import org.matsim.freight.logistics.*;
import org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils;
import org.matsim.freight.logistics.shipment.LspShipment;
import org.matsim.freight.logistics.shipment.LspShipmentPlanElement;
import org.matsim.freight.logistics.shipment.LspShipmentUtils;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.TestScenario;
import org.matsim.simwrapper.viz.CarrierViewer;
import org.matsim.simwrapper.viz.TransitViewer;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class DashboardTests {
	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	private void run(Dashboard... dashboards) {

		Config config = TestScenario.loadConfig(utils);

		config.controller().setLastIteration(1);

		SimWrapperConfigGroup group = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);
		group.setSampleSize(0.001);

		SimWrapper sw = SimWrapper.create(config);
		for (Dashboard d : dashboards) {
			sw.addDashboard(d);
		}

		Controler controler = MATSimApplication.prepare(new TestScenario(sw), config);
		controler.run();
	}

	private void runCarrierScenario(Dashboard... dashboards) {

		Config config;

		config = ConfigUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("freight-chessboard-9x9"), "config.xml"));
		config.plans().setInputFile(null); // remove passenger input
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(0);  // no iterations; for iterations see RunFreightWithIterationsExample.  kai, jan'23

		FreightCarriersConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfigGroup.setCarriersFile("singleCarrierFiveActivitiesWithoutRoutes.xml");
		freightConfigGroup.setCarriersVehicleTypesFile("vehicleTypes.xml");

		// load scenario (this is not loading the freight material):
		Scenario scenario = ScenarioUtils.loadScenario(config);

		//load carriers according to freight config
		CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);

		// Solving the VRP (generate carrier's tour plans)
		try {
			CarriersUtils.runJsprit(scenario);
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		SimWrapperConfigGroup group = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);
		group.setSampleSize(0.001);

		SimWrapper sw = SimWrapper.create(config);
		for (Dashboard d : dashboards) {
			sw.addDashboard(d);
		}

		// ## MATSim configuration:  ##
		final Controler controler = new Controler(scenario);
		controler.addOverridingModule(new CarrierModule());

		controler.run();
	}

	private void runLogisticScenario(Dashboard... dashboards) {
		Config config = new Config();
		config.addCoreModules();

		FreightCarriersConfigGroup freightConfig =
			ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfig.setTimeWindowHandling(FreightCarriersConfigGroup.TimeWindowHandling.ignore);

		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork())
			.readFile(ExamplesUtils.getTestScenarioURL("logistics-2regions") + "2regions-network.xml");

		// Create LSP and lspShipments
		LSP lsp = createInitialLSP(scenario);
		Collection<LspShipment> lspShipments = createInitialLSPShipments(scenario.getNetwork());

		// assign the lspShipments to the LSP
		for (LspShipment lspShipment : lspShipments) {
			lsp.assignShipmentToLSP(lspShipment);
		}

		// schedule the LSP with the lspShipments and according to the scheduler of the Resource
		lsp.scheduleLogisticChains();

		// set up simulation controller and LSPModule
		LSPUtils.loadLspsIntoScenario(scenario, Collections.singletonList(lsp));


		config.controller().setLastIteration(0);
		config.plans().setInputFile(null);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());

		Controller controller = ControllerUtils.createController(scenario);
		controller.addOverridingModule(
			new AbstractModule() {
				@Override
				public void install() {
					install(new LSPModule());
				}
			});

		SimWrapper sw = SimWrapper.create(config);
		for (Dashboard d : dashboards) {
			sw.addDashboard(d);
		}

		// The VSP default settings are designed for person transport simulation. After talking to Kai,
		// they will be set to WARN here. Kai MT may'23
		controller
			.getConfig()
			.vspExperimental()
			.setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);
		controller.run();
	}


	@Test
	void defaults() {

		Path out = Path.of(utils.getOutputDirectory(), "analysis");

		run();

		// Ensure default dashboards have been added
		Assertions.assertThat(out)
			// Stuck agents
			.isDirectoryRecursivelyContaining("glob:**stuck_agents.csv")
			// Trip stats
			.isDirectoryRecursivelyContaining("glob:**trip_stats.csv")
			.isDirectoryRecursivelyContaining("glob:**mode_share.csv")
			.isDirectoryRecursivelyContaining("glob:**mode_share_per_purpose.csv")
			.isDirectoryRecursivelyContaining("glob:**mode_shift.csv")
			// Traffic stats
			.isDirectoryRecursivelyContaining("glob:**traffic_stats_by_link_daily.csv")
			.isDirectoryRecursivelyContaining("glob:**traffic_stats_by_road_type_and_hour.csv")
			.isDirectoryRecursivelyContaining("glob:**traffic_stats_by_road_type_daily.csv")
			// PT
			.isDirectoryRecursivelyContaining("glob:**pt_pax_volumes.csv.gz");

	}

	@Test
	void tripPersonFilter() throws IOException {

		Path out = Path.of(utils.getOutputDirectory(), "analysis", "population");

		run(new TripDashboard().setAnalysisArgs("--person-filter", "subpopulation=person"));
		Assertions.assertThat(out)
			.isDirectoryContaining("glob:**trip_stats.csv")
			.isDirectoryContaining("glob:**mode_share.csv");

		Table tripStats = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(Path.of(utils.getOutputDirectory(), "analysis", "population", "trip_stats.csv").toString()))
			.sample(false)
			.separator(CsvOptions.detectDelimiter(Path.of(utils.getOutputDirectory(), "analysis", "population", "mode_share.csv").toString())).build());

		Assertions.assertThat(tripStats.containsColumn("freight")).isFalse();
	}

	@Test
	void tripRef() {

		Path out = Path.of(utils.getOutputDirectory(), "analysis", "population");

		TripDashboard dashboard = new TripDashboard("mode_share_ref.csv", "mode_share_per_dist_ref.csv", "mode_users_ref.csv")
			.withGroupedRefData("mode_share_per_group_dist_ref.csv")
			.withDistanceDistribution("mode_share_distance_distribution.csv")
			.withChoiceEvaluation(true);

		run(dashboard);
		Assertions.assertThat(out)
			.isDirectoryContaining("glob:**trip_stats.csv")
			.isDirectoryContaining("glob:**mode_share.csv")
			.isDirectoryContaining("glob:**mode_choices.csv")
			.isDirectoryContaining("glob:**mode_choice_evaluation.csv")
			.isDirectoryContaining("glob:**mode_confusion_matrix.csv");

	}

	@Test
	void populationAttribute() {

		Path out = Path.of(utils.getOutputDirectory(), "analysis", "population");

		run(new PopulationAttributeDashboard());
		Assertions.assertThat(out)
			.isDirectoryContaining("glob:**amount_per_age_group.csv")
			.isDirectoryContaining("glob:**amount_per_sex_group.csv")
			.isDirectoryContaining("glob:**total_agents.csv");


	}

	@Test
	void odTrips() {
		run(new ODTripDashboard(Set.of("car", "pt", "walk", "bike", "ride"), "EPSG:25832"));

		Path out = Path.of(utils.getOutputDirectory(), "analysis", "population");

		Assertions.assertThat(out)
			.isDirectoryContaining("glob:**trips_per_mode_car.csv")
			.isDirectoryContaining("glob:**trips_per_mode_bike.csv")
			.isDirectoryContaining("glob:**trips_per_mode_ride.csv");

	}

	@Test
	void ptCustom() {
		PublicTransitDashboard pt = new PublicTransitDashboard();

		// bus
		TransitViewer.CustomRouteType crt = TransitViewer.customRouteType("Bus", "#109192");
		crt.addMatchGtfsRouteType(3);

		// rail
		TransitViewer.CustomRouteType crtRail = TransitViewer.customRouteType("Rail", "#EC0016");
		crtRail.addMatchGtfsRouteType(2);

		pt.withCustomRouteTypes(crt, crtRail);

		run(pt);

		Path out = Path.of(utils.getOutputDirectory(), "analysis", "pt");

		Assertions.assertThat(out)
			.isDirectoryContaining("glob:**pt_pax_volumes.csv.gz")
			.isDirectoryContaining("glob:**pt_pax_per_hour_and_vehicle_type_and_agency.csv");
	}

	@Test
	void activity() {
		ActivityDashboard ad = new ActivityDashboard("kehlheim_shape.shp");

		ad.addActivityType(
			"work",
			List.of("work"),
			List.of(ActivityDashboard.Indicator.COUNTS, ActivityDashboard.Indicator.RELATIVE_DENSITY, ActivityDashboard.Indicator.DENSITY), true,
			"kehlheim_ref.csv"
		);

		run(ad);
	}

	@Test
	void carrierViewer() {
		CarrierDashboard cd = new CarrierDashboard();

		runCarrierScenario(cd);

		Path out = Path.of(utils.getOutputDirectory());

		Assertions.assertThat(out)
			.isDirectoryContaining("glob:**output_carriers.xml.gz")
			.isDirectoryContaining("glob:**output_network.xml.gz");

	}

	@Test
	void logisticViewer() {
		LogisticDashboard ld = new LogisticDashboard();

		runLogisticScenario(ld);

		Path out = Path.of(utils.getOutputDirectory());

		Assertions.assertThat(out)
			.isDirectoryContaining("glob:**output_carriers.xml.gz")
			.isDirectoryContaining("glob:**output_lsps.xml.gz")
			.isDirectoryContaining("glob:**output_network.xml.gz");

	}





	// setup functions for Logistic test scenario
	private static LSP createInitialLSP(Scenario scenario) {

		// The Carrier for the resource of the sole LogisticsSolutionElement of the LSP is created
		Id<Carrier> carrierId = Id.create("CollectionCarrier", Carrier.class);
		Id<VehicleType> vehicleTypeId = Id.create("CollectionCarrierVehicleType", VehicleType.class);
		VehicleType collectionVehType = VehicleUtils.createVehicleType(vehicleTypeId, TransportMode.car);
		collectionVehType.getCapacity().setOther(10);
		collectionVehType.getCostInformation().setCostsPerMeter(0.0004);
		collectionVehType.getCostInformation().setCostsPerSecond(0.38);
		collectionVehType.getCostInformation().setFixedCost(49.);
		collectionVehType.setMaximumVelocity(50 / 3.6);

		Id<Link> collectionLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> vollectionVehicleId = Id.createVehicleId("CollectionVehicle");
		CarrierVehicle carrierVehicle =
			CarrierVehicle.newInstance(vollectionVehicleId, collectionLinkId, collectionVehType);

		CarrierCapabilities capabilities = CarrierCapabilities.Builder.newInstance()
			.addVehicle(carrierVehicle)
			.setFleetSize(CarrierCapabilities.FleetSize.INFINITE)
			.build();

		Carrier carrier = CarriersUtils.createCarrier(carrierId);
		carrier.setCarrierCapabilities(capabilities);

		// The Resource i.e. the Resource is created
		LSPResource collectionResource =
			ResourceImplementationUtils.CollectionCarrierResourceBuilder.newInstance(carrier)
				.setCollectionScheduler(
					ResourceImplementationUtils.createDefaultCollectionCarrierScheduler(scenario))
				.setLocationLinkId(collectionLinkId)
				.build();

		// The adapter is now inserted into the only LogisticsSolutionElement of the only
		// LogisticsSolution of the LSP
		LogisticChainElement collectionElement =
			LSPUtils.LogisticChainElementBuilder.newInstance(
					Id.create("CollectionElement", LogisticChainElement.class))
				.setResource(collectionResource)
				.build();

		// The LogisticsSolutionElement is now inserted into the only LogisticsSolution of the LSP
		LogisticChain collectionSolution =
			LSPUtils.LogisticChainBuilder.newInstance(
					Id.create("CollectionSolution", LogisticChain.class))
				.addLogisticChainElement(collectionElement)
				.build();

		// The initial plan of the lsp is generated and the assigner and the solution from above are
		// added
		LSPPlan collectionPlan = LSPUtils.createLSPPlan();
		InitialShipmentAssigner assigner =
			ResourceImplementationUtils.createSingleLogisticChainShipmentAssigner();
		collectionPlan.setInitialShipmentAssigner(assigner);
		collectionPlan.addLogisticChain(collectionSolution);

		LSPUtils.LSPBuilder collectionLSPBuilder =
			LSPUtils.LSPBuilder.getInstance(Id.create("CollectionLSP", LSP.class));
		collectionLSPBuilder.setInitialPlan(collectionPlan);

		// The exogenous list of Resources for the SolutionScheduler is compiled and the Scheduler is
		// added to the LSPBuilder
		ArrayList<LSPResource> resourcesList = new ArrayList<>();
		resourcesList.add(collectionResource);
		LogisticChainScheduler simpleScheduler =
			ResourceImplementationUtils.createDefaultSimpleForwardLogisticChainScheduler(resourcesList);
		collectionLSPBuilder.setLogisticChainScheduler(simpleScheduler);

		return collectionLSPBuilder.build();
	}

	private static Collection<LspShipment> createInitialLSPShipments(Network network) {
		ArrayList<LspShipment> shipmentList = new ArrayList<>();
		ArrayList<Link> linkList = new ArrayList<>(network.getLinks().values());

		// Create five LSPShipments that are located in the left half of the network.
		for (int i = 1; i < 6; i++) {
			Id<LspShipment> id = Id.create(i, LspShipment.class);
			LspShipmentUtils.LspShipmentBuilder builder = LspShipmentUtils.LspShipmentBuilder.newInstance(id);
			Random random = new Random(1);
			int capacityDemand = random.nextInt(4);
			builder.setCapacityDemand(capacityDemand);

			while (true) {
				Collections.shuffle(linkList, random);
				Link pendingFromLink = linkList.getFirst();
				if (pendingFromLink.getFromNode().getCoord().getX() <= 4000
					&& pendingFromLink.getFromNode().getCoord().getY() <= 4000
					&& pendingFromLink.getToNode().getCoord().getX() <= 4000
					&& pendingFromLink.getToNode().getCoord().getY() <= 4000) {
					builder.setFromLinkId(pendingFromLink.getId());
					break;
				}
			}

			builder.setToLinkId(Id.createLinkId("(4 2) (4 3)"));
			TimeWindow endTimeWindow = TimeWindow.newInstance(0, (24 * 3600));
			builder.setEndTimeWindow(endTimeWindow);
			TimeWindow startTimeWindow = TimeWindow.newInstance(0, (24 * 3600));
			builder.setStartTimeWindow(startTimeWindow);
			builder.setDeliveryServiceTime(capacityDemand * 60);
			shipmentList.add(builder.build());
		}
		return shipmentList;
	}


}
