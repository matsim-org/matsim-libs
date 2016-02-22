package gunnar.ihop2;

import gunnar.ihop2.regent.demandreading.PopulationCreator;
import gunnar.ihop2.transmodeler.networktransformation.Transmodeler2MATSimNetwork;

import java.io.IOException;

import org.matsim.contrib.signals.router.InvertedNetworkRoutingModuleModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.roadpricing.ControlerDefaultsWithRoadPricingModule;
import org.matsim.roadpricing.RoadPricingConfigGroup;

import saleem.stockholmscenario.utils.StockholmTransformationFactory;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class ProductionRunner {

	public ProductionRunner() {
	}

	public static void main(String[] args) throws IOException {

		/*
		 * ALL POSSIBLE PARAMETERS
		 */

		final String nodesFile = "./ihop2-data/network-input/Nodes.csv";
		final String segmentsFile = "./ihop2-data/network-input/Segments.csv";
		final String lanesFile = "./ihop2-data/network-input/Lanes.csv";
		final String laneConnectorsFile = "./ihop2-data/network-input/Lane Connectors.csv";
		final String linksFile = "./ihop2-data/network-input/Links.csv";

		final String matsimNetworkFile = "./ihop2-data/network-output/network.xml";
		final String matsimFullNetworkFile = "./ihop2-data/network-output/network-full.xml";
		final String linkAttributesFile = "./ihop2-data/network-output/link-attributes.xml";
		final String matsimLanesFile11 = "./ihop2-data/network-output/lanes11.xml";
		final String matsimLanesFile20 = "./ihop2-data/network-output/lanes20.xml";
		final String matsimTollFile = "./ihop2-data/network-output/toll.xml";

		final double populationSample = 0.01;
		final String zonesShapeFileName = "./ihop2-data/demand-input/sverige_TZ_EPSG3857.shp";
		final String buildingShapeFileName = "./ihop2-data/demand-input/by_full_EPSG3857_2.shp";
		final String populationFileName = "./ihop2-data/demand-input/trips.xml";

		final String initialPlansFile = "./ihop2-data/without-toll/ITERS/it.200/200.plans.xml.gz";
		// "./ihop2-data/demand-output/initial-plans_" + populationSample + ".xml";

		final String configFileName = "./ihop2-data/matsim-input/matsim-config.xml";
		final double networkUpscaleFactor = 2;
		final String lastIteration = "200";
		final boolean useLanes = false;
		final boolean useRoadPricing = false;
		final boolean doRouteChoice = true; // changes "module 2"'s choice proba
		final boolean doTimeChoice = true; // changes "module 3"'s choice proba

		/*
		 * DECIDE WHAT TO ACTUALLY DO
		 */

		final boolean doNetworkConversion = false;
		final boolean doPopulationGeneration = false;
		final boolean runMATSim = true;

		/*
		 * TRANSMODELER -> MATSIM NETWORK CONVERSION
		 */

		if (doNetworkConversion) {
			final Transmodeler2MATSimNetwork tm2MATSim = new Transmodeler2MATSimNetwork(
					nodesFile, linksFile, segmentsFile, lanesFile,
					laneConnectorsFile, matsimNetworkFile,
					matsimFullNetworkFile, linkAttributesFile,
					matsimLanesFile11, matsimLanesFile20, matsimTollFile);
			tm2MATSim.run();
		}

		/*
		 * REGENT -> MATSIM POPULATION CONVERSION
		 */
		if (doPopulationGeneration) {
			final PopulationCreator populationCreator = new PopulationCreator(
					matsimNetworkFile, zonesShapeFileName,
					StockholmTransformationFactory.WGS84_EPSG3857,
					populationFileName);
			populationCreator.setBuildingsFileName(buildingShapeFileName);
			populationCreator.setPopulationSampleFactor(populationSample);
			populationCreator.run(initialPlansFile);
		}

		/*
		 * MATSIM ITERATIONS
		 */
		if (runMATSim) {
			final Config config = ConfigUtils.loadConfig(configFileName,
					new RoadPricingConfigGroup());
			config.getModule("qsim").addParam("flowCapacityFactor",
					Double.toString(networkUpscaleFactor * populationSample));
			config.getModule("qsim").addParam("storageCapacityFactor",
					Double.toString(networkUpscaleFactor * populationSample));
			config.getModule("network").addParam("inputNetworkFile",
					matsimNetworkFile);
			config.getModule("plans").addParam("inputPlansFile",
					initialPlansFile);
			config.getModule("controler").addParam("lastIteration",
					lastIteration);
			config.controler()
					.setOverwriteFileSetting(
							OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

			if (useLanes) {
				config.network().setLaneDefinitionsFile(matsimLanesFile20);
				config.qsim().setUseLanes(true);
				config.travelTimeCalculator()
						.setCalculateLinkToLinkTravelTimes(true);
				config.controler().setLinkToLinkRoutingEnabled(true);
				config.getModule("qsim").addParam("stuckTime", "1e6");
				config.getModule("qsim").addParam("endTime", "99:00:00");
			} else {
				config.getModule("qsim").addParam("stuckTime", "10");
			}

			if (doRouteChoice) {
				config.getModule("strategy").addParam("ModuleProbability_2",
						"0.1");
			} else {
				config.getModule("strategy").addParam("ModuleProbability_2",
						"0.0");
			}

			if (doTimeChoice) {
				config.getModule("strategy").addParam("ModuleProbability_3",
						"0.1");
			} else {
				config.getModule("strategy").addParam("ModuleProbability_3",
						"0.0");
			}

			final Controler controler = new Controler(config);
			if (useLanes) {
				controler
						.addOverridingModule(new InvertedNetworkRoutingModuleModule());
			}
			if (useRoadPricing) {
				controler
						.setModules(new ControlerDefaultsWithRoadPricingModule(controler.getScenario()));
			}

			controler.run();
		}
	}
}
