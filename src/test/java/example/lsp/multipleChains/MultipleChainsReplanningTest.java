package example.lsp.multipleChains;

import lsp.*;
import lsp.resourceImplementations.ResourceImplementationUtils;
import lsp.resourceImplementations.distributionCarrier.DistributionCarrierUtils;
import lsp.shipment.LSPShipment;
import lsp.shipment.ShipmentUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
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
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MultipleChainsReplanningTest {

	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	private static final Id<Link> DEPOT_LINK_ID = Id.createLinkId("i(5,0)");

	private static final VehicleType VEH_TYPE_LARGE_50 = CarrierVehicleType.Builder.newInstance(Id.create("large50", VehicleType.class))
			.setCapacity(50)
			.setMaxVelocity(10)
			.setFixCost(150)
			.setCostPerDistanceUnit(0.01)
			.setCostPerTimeUnit(0.01)
			.build();

	Integer planCountBefore;
	Integer planCountAfter;

	Integer shipmentCountBefore;
	Integer shipmentCountAfter;

	LSP lsp;

	@Before
	public void initialize() {

		Config config = prepareConfig();

		Scenario scenario = prepareScenario(config);

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
				bind(CarrierScoringFunctionFactory.class).toInstance(carrierScorer);
				bind(LSPScorerFactory.class).toInstance(MyLSPScorer::new);
				bind(CarrierStrategyManager.class).toProvider(() -> {
					CarrierStrategyManager strategyManager = FreightUtils.createDefaultCarrierStrategyManager();
					strategyManager.addStrategy(new GenericPlanStrategyImpl<>(new BestPlanSelector<>()), null, 1);
					return strategyManager;
				});
				bind(LSPStrategyManager.class).toProvider(() -> {
					LSPStrategyManager strategyManager = new LSPStrategyManagerImpl();
					strategyManager.addStrategy(new RandomShiftingStrategyFactory().createStrategy(), null, 1);
					return strategyManager;
				});
			}
		});

		controler.getConfig().vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);

		lsp = LSPUtils.getLSPs(controler.getScenario()).getLSPs().values().iterator().next();

		planCountBefore = lsp.getPlans().size();
		shipmentCountBefore = lsp.getPlans().get(0).getLogisticChains().iterator().next().getShipmentIds().size();

		controler.run();

		planCountAfter = lsp.getPlans().size();
		shipmentCountAfter = lsp.getPlans().get(1).getLogisticChains().iterator().next().getShipmentIds().size();
	}

	private Config prepareConfig() {
		Config config = ConfigUtils.createConfig();

		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(1);

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

		LSPUtils.addLSPs(scenario, new LSPs(Collections.singletonList(createLSP(scenario))));

		return scenario;
	}

	private static LSP createLSP(Scenario scenario) {
		Network network = scenario.getNetwork();

		// A plan with two different logistic chains on the left and right, with respective carriers is created
		LSPPlan multipleOneEchelonChainsPlan;
		{
			LogisticChainElement leftCarrierElement;
			{
				Carrier carrierLeft = CarrierUtils.createCarrier(Id.create("carrierLeft", Carrier.class));
				carrierLeft.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);

				CarrierUtils.addCarrierVehicle(carrierLeft, CarrierVehicle.newInstance(Id.createVehicleId("veh_small"), DEPOT_LINK_ID, VEH_TYPE_LARGE_50));
				LSPResource carrierLeftResource = DistributionCarrierUtils.DistributionCarrierResourceBuilder.newInstance(carrierLeft, network)
						.setDistributionScheduler(DistributionCarrierUtils.createDefaultDistributionCarrierScheduler())
						.build();

				leftCarrierElement = LSPUtils.LogisticChainElementBuilder.newInstance(Id.create("leftCarrierElement", LogisticChainElement.class))
						.setResource(carrierLeftResource)
						.build();
			}

			LogisticChainElement rightCarrierElement;
			{
				Carrier carrierRight = CarrierUtils.createCarrier(Id.create("carrierRight", Carrier.class));
				carrierRight.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);

				CarrierUtils.addCarrierVehicle(carrierRight, CarrierVehicle.newInstance(Id.createVehicleId("veh_small"), DEPOT_LINK_ID, VEH_TYPE_LARGE_50));
				LSPResource carrierRightResource = DistributionCarrierUtils.DistributionCarrierResourceBuilder.newInstance(carrierRight, network)
						.setDistributionScheduler(DistributionCarrierUtils.createDefaultDistributionCarrierScheduler())
						.build();

				rightCarrierElement = LSPUtils.LogisticChainElementBuilder.newInstance(Id.create("rightCarrierElement", LogisticChainElement.class))
						.setResource(carrierRightResource)
						.build();
			}

			LogisticChain leftChain = LSPUtils.LogisticChainBuilder.newInstance(Id.create("leftChain", LogisticChain.class))
					.addLogisticChainElement(leftCarrierElement)
					.build();

			LogisticChain rightChain = LSPUtils.LogisticChainBuilder.newInstance(Id.create("rightChain", LogisticChain.class))
					.addLogisticChainElement(rightCarrierElement)
					.build();

			final ShipmentAssigner shipmentAssigner = Utils.createRandomLogisticChainShipmentAssigner();
			multipleOneEchelonChainsPlan = LSPUtils.createLSPPlan()
					.addLogisticChain(leftChain)
					.addLogisticChain(rightChain)
					.setAssigner(shipmentAssigner);

			multipleOneEchelonChainsPlan.setType(Utils.LspPlanTypes.MULTIPLE_ONE_ECHELON_CHAINS.toString());
		}

		List<LSPPlan> lspPlans = new ArrayList<>();
		lspPlans.add(multipleOneEchelonChainsPlan);

		LSP lsp = LSPUtils.LSPBuilder.getInstance(Id.create("myLSP", LSP.class))
				.setInitialPlan(multipleOneEchelonChainsPlan)
				.setLogisticChainScheduler(ResourceImplementationUtils.createDefaultSimpleForwardLogisticChainScheduler(createResourcesListFromLSPPlans(lspPlans)))
				.build();

		for (LSPShipment shipment : createInitialLSPShipments()) {
			lsp.assignShipmentToLSP(shipment);
		}

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


	@Test
	public void generatedInnovatedPlan() {
		assert planCountBefore != planCountAfter;
	}

	@Test
	public void shipmentDistributionChanged() {
		assert shipmentCountBefore != shipmentCountAfter;
	}
}
