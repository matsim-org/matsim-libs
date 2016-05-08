package playground.dziemke.accessibility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.GridBasedAccessibilityControlerListenerV3;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.utils.AccessibilityRunUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacilities;

import playground.dziemke.utils.LogToOutputSaver;

public class AccessibilityComputationCottbus {
	public static final Logger log = Logger.getLogger(AccessibilityComputationNMBNew.class);
	
	private static final double cellSize = 200.;
	
	public static void main(String[] args) {
		// Input and output	
//		String runOutputFolder = "../../../public-svn/matsim/scenarios/countries/de/cottbus/cottbus-with-pt/output/cb02/";
		String runOutputFolder = "../../../public-svn/matsim/scenarios/countries/de/cottbus/commuter-population-only-car-traffic-only-100pct-2016-03-18/";

		// Parameters
		String crs = TransformationFactory.WGS84_UTM33N; // EPSG:32633 -- UTM33N
		

		// QGis parameters
		boolean createQGisOutput = true;
		boolean includeDensityLayer = true;
		Double lowerBound = .0;
		Double upperBound = 3.5;
		Integer range = 9;
		int symbolSize = 210;
		int populationThreshold = (int) (200 / (1000/cellSize * 1000/cellSize));
		double[] mapViewExtent = {447000,5729000,461000,5740000};
		
		
		//
//		Config config = ConfigUtils.loadConfig(runOutputFolder + "output_config.xml.gz", new AccessibilityConfigGroup());
//		Config config = ConfigUtils.loadConfig(runOutputFolder + "output_config_2.xml", new AccessibilityConfigGroup());
		Config config = ConfigUtils.loadConfig(runOutputFolder + "config.xml", new AccessibilityConfigGroup());
		//
		
		// Infrastructure
		String accessibilityOutputDirectory = runOutputFolder + "accessibilities/";
		LogToOutputSaver.setOutputDirectory(accessibilityOutputDirectory);

		// Config and scenario
//		final Config config = ConfigUtils.createConfig(new AccessibilityConfigGroup());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(accessibilityOutputDirectory);
		
//		config.network().setInputFile(runOutputFolder + "output_network.xml.gz");
		config.network().setInputFile(runOutputFolder + "network_wgs84_utm33n.xml.gz");
		
		config.controler().setLastIteration(0);
		
//		config.plans().setInputFile(runOutputFolder + "output_plans.xml.gz");
		config.plans().setInputFile(runOutputFolder + "commuter_population_wgs84_utm33n_car_only.xml");
		
//		config.transit().setTransitScheduleFile(runOutputFolder + "output_transitSchedule.xml.gz");
//		config.transit().setVehiclesFile(runOutputFolder + "output_transitVehicles.xml.gz");

		MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);
		
		ActivityFacilities activityFacilities = AccessibilityRunUtils.createFacilitiesFromPlans(scenario.getPopulation());
		scenario.setActivityFacilities(activityFacilities);

		// collect activity types
//		final List<String> activityTypes = AccessibilityRunUtils.collectAllFacilityOptionTypes(scenario);
		List<String> activityTypes = new ArrayList<String>();
		activityTypes.add("work"); // manually setting computation only for work

		// collect homes
		String activityFacilityType = "home";
		final ActivityFacilities homes = AccessibilityRunUtils.collectActivityFacilitiesWithOptionOfType(scenario, activityFacilityType);

		final Controler controler = new Controler(scenario);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				// Loop over activity types to add one GridBasedAccessibilityControlerListenerV3 for each
				for (final String actType : activityTypes) {
					addControlerListenerBinding().toProvider(new Provider<ControlerListener>() {
						@Inject Scenario scenario;
						@Inject Map<String, TravelTime> travelTimes;
						@Inject Map<String, TravelDisutilityFactory> travelDisutilityFactories;

						@Override
						public ControlerListener get() {
							GridBasedAccessibilityControlerListenerV3 listener =
									new GridBasedAccessibilityControlerListenerV3(AccessibilityRunUtils.collectActivityFacilitiesWithOptionOfType(scenario, actType), null, config, scenario, travelTimes, travelDisutilityFactories);
							listener.setComputingAccessibilityForMode(Modes4Accessibility.freeSpeed, true);
							listener.setComputingAccessibilityForMode(Modes4Accessibility.car, true);
							listener.setComputingAccessibilityForMode(Modes4Accessibility.walk, true);
							listener.setComputingAccessibilityForMode(Modes4Accessibility.bike, true);
//							listener.setComputingAccessibilityForMode(Modes4Accessibility.pt, true);

							listener.addAdditionalFacilityData(homes);
							listener.generateGridsAndMeasuringPointsByCustomBoundary(447759., 5729049., 460617., 5740192., cellSize);
//							listener.generateGridsAndMeasuringPointsByCustomBoundary(447000., 5729000., 461000., 5740000., cellSize);
							listener.writeToSubdirectoryWithName(actType);
							listener.setUrbansimMode(false); // avoid writing some (eventually: all) files that related to matsim4urbansim
							return listener;
						}
					});
				}
			}
		});
		controler.run();

		
		/* Write QGis output */
		if (createQGisOutput == true) {
			String osName = System.getProperty("os.name");
			String workingDirectory = config.controler().getOutputDirectory();

			for (String actType : activityTypes) {
				String actSpecificWorkingDirectory = workingDirectory + actType + "/";

				for ( Modes4Accessibility mode : Modes4Accessibility.values()) {
					VisualizationUtilsDZ.createQGisOutput(actType, mode, mapViewExtent, workingDirectory, crs, includeDensityLayer,
							lowerBound, upperBound, range, symbolSize, populationThreshold);
					VisualizationUtilsDZ.createSnapshot(actSpecificWorkingDirectory, mode, osName);
				}
			}  
		}
	}
}