package playground.ikaddoura.cottbus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

import com.vividsolutions.jts.geom.Envelope;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accessibility.AccessibilityCalculator;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.GridBasedAccessibilityShutdownListenerV3;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.gis.GridUtils;
import org.matsim.contrib.accessibility.utils.AccessibilityUtils;
import org.matsim.contrib.accessibility.utils.VisualizationUtils;
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
	public static final Logger log = Logger.getLogger(AccessibilityComputationCottbus.class);
	
	private static final double cellSize = 100.;
	
	public static void main(String[] args) {
		String runOutputFolder = "../../../public-svn/matsim/scenarios/countries/de/cottbus/commuter-population-only-car-traffic-only-100pct-2016-03-18/";
		String crs = TransformationFactory.WGS84_UTM33N; // EPSG:32633 -- UTM33N
		
		// QGis parameters
		boolean createQGisOutput = true;
		boolean includeDensityLayer = true;
		Double lowerBound = .0;
		Double upperBound = 3.5;
		Integer range = 9;
		int symbolSize = 110;
		int populationThreshold = (int) (200 / (1000/cellSize * 1000/cellSize));
		Envelope envelope = new Envelope(447000,5729000,461000,5740000);
		
		Config config = ConfigUtils.loadConfig(runOutputFolder + "config.xml", new AccessibilityConfigGroup());
		
		// Infrastructure
		String accessibilityOutputDirectory = runOutputFolder + "accessibilities_final/";
		LogToOutputSaver.setOutputDirectory(accessibilityOutputDirectory);

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(accessibilityOutputDirectory);
		
		config.network().setInputFile(runOutputFolder + "network_wgs84_utm33n.xml.gz");
		
		config.controler().setLastIteration(0);
		config.facilities().setInputFile("/Users/ihab/Documents/workspace/public-svn/matsim/scenarios/countries/de/cottbus/facilities_final_WGS84_UTM33N.xml");
		
		config.plans().setInputFile(runOutputFolder + "commuter_population_wgs84_utm33n_car_only.xml");

		MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);
		
//		ActivityFacilities activityFacilities = AccessibilityRunUtils.createFacilitiesFromPlans(scenario.getPopulation());
//		scenario.setActivityFacilities(activityFacilities);

		
		// collect activity types
		List<String> activityTypes = new ArrayList<String>();
		activityTypes.add("work"); 
		activityTypes.add("education"); 
		activityTypes.add("grave_yard");
		activityTypes.add("police");
		activityTypes.add("medical");
		activityTypes.add("fire_station");

		// collect homes
		String activityFacilityType = "home";
		final ActivityFacilities homes = AccessibilityUtils.collectActivityFacilitiesWithOptionOfType(scenario, activityFacilityType);

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
							AccessibilityCalculator accessibilityCalculator = new AccessibilityCalculator(travelTimes, travelDisutilityFactories, (Scenario) scenario);
							accessibilityCalculator.setMeasuringPoints(GridUtils.createGridLayerByGridSizeByBoundingBoxV2(447759., 5729049., 460617., 5740192., cellSize));
							GridBasedAccessibilityShutdownListenerV3 listener = new GridBasedAccessibilityShutdownListenerV3(accessibilityCalculator, (ActivityFacilities) AccessibilityUtils.collectActivityFacilitiesWithOptionOfType(scenario, actType), null, config, scenario, travelTimes, travelDisutilityFactories, 447759., 5729049., 460617., 5740192., cellSize);
							accessibilityCalculator.setComputingAccessibilityForMode(Modes4Accessibility.freeSpeed, true);
							accessibilityCalculator.setComputingAccessibilityForMode(Modes4Accessibility.car, false);
							accessibilityCalculator.setComputingAccessibilityForMode(Modes4Accessibility.walk, true);
							accessibilityCalculator.setComputingAccessibilityForMode(Modes4Accessibility.bike, false);
							accessibilityCalculator.setComputingAccessibilityForMode(Modes4Accessibility.pt, false);

							listener.addAdditionalFacilityData(homes);
							listener.writeToSubdirectoryWithName(actType);
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
					VisualizationUtils.createQGisOutput(actType, mode, envelope, workingDirectory, crs, includeDensityLayer,
							lowerBound, upperBound, range, symbolSize, populationThreshold);
					VisualizationUtils.createSnapshot(actSpecificWorkingDirectory, mode, osName);
				}
			}  
		}
	}
}