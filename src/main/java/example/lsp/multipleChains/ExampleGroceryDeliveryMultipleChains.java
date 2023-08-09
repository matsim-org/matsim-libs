package example.lsp.multipleChains;

import lsp.*;
import lsp.resourceImplementations.ResourceImplementationUtils;
import lsp.resourceImplementations.distributionCarrier.DistributionCarrierUtils;
import lsp.shipment.LSPShipment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
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
import org.matsim.vehicles.VehicleType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static example.lsp.multipleChains.MultipleChainsUtils.createLSPShipmentsFromCarrierShipments;

public class ExampleGroceryDeliveryMultipleChains {

	private static final Logger log = LogManager.getLogger(ExampleGroceryDeliveryMultipleChains.class);

	private ExampleGroceryDeliveryMultipleChains() {}

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
				bind(CarrierScoringFunctionFactory.class).toInstance(carrierScorer);
				bind(LSPScorerFactory.class).toInstance(MyLSPScorer::new);
				bind(CarrierStrategyManager.class).toProvider(() -> {
					CarrierStrategyManager strategyManager = FreightUtils.createDefaultCarrierStrategyManager();
					strategyManager.addStrategy(new GenericPlanStrategyImpl<>(new BestPlanSelector<>()), null, 1);
					return strategyManager;
				});
				bind(LSPStrategyManager.class).toProvider(() -> {
					LSPStrategyManager strategyManager = new LSPStrategyManagerImpl();
					strategyManager.addStrategy(new GenericPlanStrategyImpl<>(new BestPlanSelector<>()), null, 1);
					return strategyManager;
				});
			}
		});

		log.info("Run MATSim");

		//The VSP default settings are designed for person transport simulation. After talking to Kai, they will be set to WARN here. Kai MT may'23
		controler.getConfig().vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);
		controler.run();

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
			config.controler().setOutputDirectory("output/groceryDelivery");
			config.controler().setLastIteration(0);
		}

//		config.network().setInputFile("/Users/niclasrichter/Documents/Studium/Master/Thesis/groceryDeliveryScenario/berlin_v5.5-network.xml.gz");
		config.network().setInputFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin_v5.5-network.xml.gz");
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setWriteEventsInterval(1);

		FreightConfigGroup freightConfig = ConfigUtils.addOrGetModule(config, FreightConfigGroup.class);
		freightConfig.setTimeWindowHandling(FreightConfigGroup.TimeWindowHandling.ignore);

		return config;
	}

	private static Scenario prepareScenario(Config config) {
		Scenario scenario = ScenarioUtils.loadScenario(config);

		log.info("Add LSP to the scenario");
		LSPUtils.addLSPs(scenario, new LSPs(Collections.singletonList(createLSP(scenario))));

		return scenario;
	}

	private static LSP createLSP(Scenario scenario) {

		//TODO vielleicht kann man carrier und vehicleTypes auch eleganter direkt von der Website laden
		String carrierPlanFile = "/Users/niclasrichter/Documents/Studium/Master/Thesis/groceryDeliveryScenario/CarrierLEH_v2_withFleet_Shipment_OneTW_PickupTime_ICEV.xml";
		String vehicleTypeFile = "/Users/niclasrichter/Documents/Studium/Master/Thesis/groceryDeliveryScenario/vehicleTypesBVWP100_DC_noTax.xml";

		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes();
		CarrierVehicleTypeReader vehicleTypeReader = new CarrierVehicleTypeReader(vehicleTypes);
		vehicleTypeReader.readFile(vehicleTypeFile);

		Carriers carriers = new Carriers();
		CarrierPlanXmlReader carrierReader = new CarrierPlanXmlReader(carriers, vehicleTypes);
		carrierReader.readFile(carrierPlanFile);

		Carrier carrier = carriers.getCarriers().get(Id.create("kaufland_VERBRAUCHERMARKT_TROCKEN", CarrierImpl.class));
		// Depotlink vielleicht lieber von den Fahrzeugen nehmen, ist logischer
		Id<Link> depotLink = carrier.getShipments().values().iterator().next().getFrom();

		log.info("create LSP");

		// Create a lsp plan with direct delivery
		LSPPlan singleOneEchelonChainPlan;
		{
			LogisticChain singleChain;
			{
				Carrier singleCarrier = CarrierUtils.createCarrier(Id.create("singleCarrier", Carrier.class));
				singleCarrier.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);

				CarrierUtils.addCarrierVehicle(singleCarrier, CarrierVehicle.newInstance(Id.createVehicleId("singleCarrier"), depotLink, vehicleTypes.getVehicleTypes().get(Id.create("heavy40t", VehicleType.class))));
				LSPResource singleCarrierResource = DistributionCarrierUtils.DistributionCarrierResourceBuilder.newInstance(singleCarrier, scenario.getNetwork())
						.setDistributionScheduler(DistributionCarrierUtils.createDefaultDistributionCarrierScheduler())
						.build();

				LogisticChainElement singleCarrierElement = LSPUtils.LogisticChainElementBuilder.newInstance(Id.create("singleCarrierElement", LogisticChainElement.class))
						.setResource(singleCarrierResource)
						.build();

				singleChain = LSPUtils.LogisticChainBuilder.newInstance(Id.create("singleChain", LogisticChain.class))
						.addLogisticChainElement(singleCarrierElement)
						.build();
			}

			singleOneEchelonChainPlan = LSPUtils.createLSPPlan()
					.addLogisticChain(singleChain)
					.setAssigner(MultipleChainsUtils.createPrimaryLogisticChainShipmentAssigner());
		}

		List<LSPPlan> lspPlans = new ArrayList<>();
		lspPlans.add(singleOneEchelonChainPlan);

		LSP lsp = LSPUtils.LSPBuilder.getInstance(Id.create("myLSP", LSP.class))
				.setInitialPlan(singleOneEchelonChainPlan)
				.setLogisticChainScheduler(ResourceImplementationUtils.createDefaultSimpleForwardLogisticChainScheduler(createResourcesListFromLSPPlans(lspPlans)))
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
