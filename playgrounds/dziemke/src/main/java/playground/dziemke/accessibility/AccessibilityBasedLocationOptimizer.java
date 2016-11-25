package playground.dziemke.accessibility;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accessibility.AccessibilityCalculator;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.CSVWriter;
import org.matsim.contrib.accessibility.FacilityTypes;
import org.matsim.contrib.accessibility.GridBasedAccessibilityShutdownListenerV3;
import org.matsim.contrib.accessibility.Labels;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.gis.GridUtils;
import org.matsim.contrib.accessibility.utils.AccessibilityUtils;
import org.matsim.contrib.matrixbasedptrouter.MatrixBasedPtRouterConfigGroup;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacilitiesFactoryImpl;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;

import playground.dziemke.utils.LogToOutputSaver;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * @author dziemke
 */
public class AccessibilityBasedLocationOptimizer {
	public static final Logger log = Logger.getLogger(AccessibilityBasedLocationOptimizer.class) ;

	private static final double cellSize = 500.;

	public static void main(String[] args) {
		// Input and output
		String folderStructure = "../../../"; // local on dz's computer
//		String folderStructure = "../../"; // server

		String networkFile = folderStructure + "matsimExamples/countries/ke/kibera/network/2015-11-05_kibera_paths_detailed.xml";
		String facilitiesFile = folderStructure + "matsimExamples/countries/ke/kibera/facilities/facilities.xml";

		String outputDirectory = "../../../../Workspace/data/accessibility/nairobi/optimization/01";
		LogToOutputSaver.setOutputDirectory(outputDirectory);
		
		
		// Parameters
		int searchInterval = 1; // only every searchInterval-th measuring point will be considered to speed up the analysis
		String actType = FacilityTypes.HOSPITAL;


		// config and scenario
		final Config config = ConfigUtils.createConfig(new AccessibilityConfigGroup(), new MatrixBasedPtRouterConfigGroup());
		config.network().setInputFile(networkFile);
		config.facilities().setInputFile(facilitiesFile);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(outputDirectory);
		config.controler().setLastIteration(0);

		final Scenario scenario = ScenarioUtils.loadScenario(config);


		Controler controler = new Controler(scenario) ;


		// for optimization
		ActivityFacilities analysisPoints =
				AccessibilityUtils.createMeasuringPointsFromNetworkBounds(scenario.getNetwork(), cellSize);

		ActivityFacilitiesFactory activityFacilityFactory = new ActivityFacilitiesFactoryImpl();


		// create listener for various analysis/measuring points
		int i = 1;
		final Map<Id<ActivityFacility>, GridBasedAccessibilityShutdownListenerV3> listenerMap =
				new HashMap<Id<ActivityFacility>, GridBasedAccessibilityShutdownListenerV3>();
		for (final Id<ActivityFacility> measuringPointId : analysisPoints.getFacilities().keySet()) {
			log.info("i = " + i + " -- i % searchInterval = " + i % searchInterval);

			if ((i % searchInterval) == 0) { // if e.g. searchInterval = 7, only look at 7th measurePoint

				final ActivityFacilities activityFacilites = AccessibilityUtils.collectActivityFacilitiesWithOptionOfType(scenario, actType);

				ActivityOption activityOption = activityFacilityFactory.createActivityOption(actType);

				Coord addedFacilityCoord = analysisPoints.getFacilities().get(measuringPointId).getCoord();
				final Id<ActivityFacility> addedFacilityId = Id.create("analysis_" + measuringPointId, ActivityFacility.class);

//				log.info("The coordinates of the facility added for analysis are " + addedFacilityCoord);
				ActivityFacility activityFacility = activityFacilityFactory.createActivityFacility(addedFacilityId, addedFacilityCoord);

				activityFacility.addActivityOption(activityOption);
				activityFacilites.addActivityFacility(activityFacility);

				controler.addOverridingModule(new AbstractModule() {
					@Override
					public void install() {
						addControlerListenerBinding().toProvider(new Provider<ControlerListener>() {
							@Inject Map<String, TravelTime> travelTimes;
							@Inject Map<String, TravelDisutilityFactory> travelDisutilityFactories;

							@Override
							public ControlerListener get() {
								BoundingBox bb = BoundingBox.createBoundingBox(scenario.getNetwork());
								ActivityFacilitiesImpl measuringPoints = GridUtils.createGridLayerByGridSizeByBoundingBoxV2(bb.getXMin(), bb.getYMin(), bb.getXMax(), bb.getYMax(), cellSize);
								AccessibilityCalculator accessibilityCalculator = new AccessibilityCalculator(scenario, measuringPoints);
								GridBasedAccessibilityShutdownListenerV3 listener = new GridBasedAccessibilityShutdownListenerV3(accessibilityCalculator, activityFacilites, null, scenario, bb.getXMin(), bb.getYMin(), bb.getXMax(),bb.getYMax(), cellSize);
								
								if ( true ) {
									throw new RuntimeException("the following needs to be replaced with more flexible syntax. kai, nov'16") ;
								}

//								accessibilityCalculator.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, true);
//								//				listener.setComputingAccessibilityForMode(Modes4Accessibility.car, true);
//								accessibilityCalculator.setComputingAccessibilityForMode(Modes4Accessibility.walk, true);
//								accessibilityCalculator.setComputingAccessibilityForMode(Modes4Accessibility.bike, true);
//								//				listener.setComputingAccessibilityForMode(Modes4Accessibility.pt, true);

								//				listener.addAdditionalFacilityData(homes) ;

								listener.writeToSubdirectoryWithName(addedFacilityId.toString());

								listener.setCalculateAggregateValues(true);
								listenerMap.put(measuringPointId, listener);
								return listener;
							}
						});
					}
				});
			}
			i++;
		}

		
		// run the controller
		controler.run();


		// collect sums and gini coefficients for different analysis/measuring points
		Map<Id<ActivityFacility>, Map<String, Double>> mapOfAccessibilitySumMaps = 
				new HashMap<>();
		Map<Id<ActivityFacility>, Map<String, Double>> mapOfAccessibilityGiniCoefficientMaps = 
				new HashMap<>();

		for (Id<ActivityFacility> measuringPointId : listenerMap.keySet()) {
//			log.warn("listenerMap = " + listenerMap);
			GridBasedAccessibilityShutdownListenerV3 listener = listenerMap.get(measuringPointId);

//			log.warn("listener = " + listener.toString());
			Map<String, Double> accessibilitySums = listener.getAccessibilitySums();
			mapOfAccessibilitySumMaps.put(measuringPointId, accessibilitySums);			

			Map<String, Double> accessibilityGiniCoefficients = listener.getAccessibilityGiniCoefficients();
			mapOfAccessibilityGiniCoefficientMaps.put(measuringPointId, accessibilityGiniCoefficients);
		}


		writePlottingData(outputDirectory, analysisPoints, mapOfAccessibilitySumMaps, "sums.csv");
		writePlottingData(outputDirectory, analysisPoints, mapOfAccessibilityGiniCoefficientMaps, "gini.csv");
	}


	/**
	 * This writes data
	 */
	private static void writePlottingData(String outputDirectory, ActivityFacilities analysisPoints,
			Map<Id<ActivityFacility>, Map<String, Double>> map, String fileName) {
		log.info("Writing plotting data for other analyis into " + outputDirectory + " ...");


		final CSVWriter writer = new CSVWriter(outputDirectory + "/" + fileName);

		writer.writeField(Labels.X_COORDINATE);
		writer.writeField(Labels.Y_COORDINATE);
		for (Modes4Accessibility mode : Modes4Accessibility.values()) {
			writer.writeField(mode.toString() + "_accessibility");
		}
		writer.writeNewLine();


		for (Id<ActivityFacility> analysisPointId : map.keySet()) {

			ActivityFacility analysisPoint = analysisPoints.getFacilities().get(analysisPointId);
			Coord coord = analysisPoint.getCoord();

			writer.writeField(coord.getX());
			writer.writeField(coord.getY());

			Map<String, Double> valuesByMode = map.get(analysisPointId);

			for (Modes4Accessibility mode : Modes4Accessibility.values()) {
//				double value = valuesByMode.get(mode);
				// TODO figure out how to deal with missing modes
				double value = valuesByMode.get(Modes4Accessibility.walk.name());
				Gbl.assertNotNull(value);
				writer.writeField(value);
			}
			writer.writeNewLine();
		}

		writer.close() ;
		log.info("Writing plotting data for other analysis done!");
	}
}