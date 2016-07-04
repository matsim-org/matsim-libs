package org.matsim.integration.daily.accessibility;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javafx.geometry.BoundingBox;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.AccessibilityStartupListener;
import org.matsim.contrib.accessibility.FacilityTypes;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.utils.AccessibilityRunUtils;
import org.matsim.contrib.accessibility.utils.VisualizationUtils;
import org.matsim.contrib.matrixbasedptrouter.MatrixBasedPtRouterConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspDefaultsCheckingLevel;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.testcases.MatsimTestUtils;

public class AccessibilityComputationKiberaTest {
	public static final Logger log = Logger.getLogger( AccessibilityComputationKiberaTest.class ) ;

	private static final Double cellSize = 10.;
//	private static final Double cellSize = 100.;

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;


	@Test
	public void doAccessibilityTest() throws IOException {
		// Input
		String folderStructure = "../../";
		String networkFile = "matsimExamples/countries/ke/kibera/2015-11-05_network_paths_detailed.xml";

		// adapt folder structure that may be different on different machines, esp. on server
		folderStructure = PathUtils.tryANumberOfFolderStructures(folderStructure, networkFile);

		networkFile = folderStructure + networkFile ;
		String facilitiesFile = folderStructure + "matsimExamples/countries/ke/kibera/2015-11-05_facilities.xml";

		// no pt input
		
		
		// Parameters
		boolean createQGisOutput = true;
		boolean includeDensityLayer = false;
		String crs = "EPSG:21037"; // = Arc 1960 / UTM zone 37S, for Nairobi, Kenya
		String name = "ke_kibera_" + cellSize.toString().split("\\.")[0];
		
		Double lowerBound = 0.;
		Double upperBound = 3.5;
		Integer range = 9; // in the current implementation, this need always be 9
		int symbolSize = 110;
		int populationThreshold = (int) (200 / (1000/cellSize * 1000/cellSize));

		
		// config and scenario
		final Config config = ConfigUtils.createConfig(new MatrixBasedPtRouterConfigGroup());
		config.network().setInputFile(networkFile);
		config.facilities().setInputFile(facilitiesFile);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);

		config.vspExperimental().setVspDefaultsCheckingLevel(VspDefaultsCheckingLevel.abort);

		// some (otherwise irrelevant) settings to make the vsp check happy:
		config.timeAllocationMutator().setMutationRange(7200.);
		config.timeAllocationMutator().setAffectingDuration(false);
		config.plans().setRemovingUnneccessaryPlanAttributes(true);
		config.plans().setActivityDurationInterpretation( PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration );

		{
			StrategySettings stratSets = new StrategySettings();
			stratSets.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
			stratSets.setWeight(1.);
			config.strategy().addStrategySettings(stratSets);
		}

		AccessibilityConfigGroup accessibilityConfigGroup = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.GROUP_NAME, AccessibilityConfigGroup.class);
		accessibilityConfigGroup.setComputingAccessibilityForMode(Modes4Accessibility.freeSpeed, true);
		accessibilityConfigGroup.setComputingAccessibilityForMode(Modes4Accessibility.car, true);
		accessibilityConfigGroup.setComputingAccessibilityForMode(Modes4Accessibility.walk, true);
		accessibilityConfigGroup.setComputingAccessibilityForMode(Modes4Accessibility.bike, true);
		accessibilityConfigGroup.setComputingAccessibilityForMode(Modes4Accessibility.pt, false);

		config.vspExperimental().setVspDefaultsCheckingLevel(VspDefaultsCheckingLevel.warn);
		// yy For a test, "abort" may be too strict.  kai, may'16

		final Scenario scenario = ScenarioUtils.loadScenario(config);
		
		
		final BoundingBox boundingBox = new BoundingBox(251000, 9853000, 256000, 9857000);
//		final BoundingBox boundingBox = BoundingBox.createBoundingBox(250500, 9852500, 256500, 9857500);
//		final BoundingBox boundingBox = BoundingBox.createBoundingBox(250000, 9852000, 257000, 9858000);
//		final BoundingBox boundingBox = BoundingBox.createBoundingBox(240000, 9844000, 280000, 9874000);
//		BoundingBox boundingBox = BoundingBox.createBoundingBox(scenario.getNetwork());
//		double xMin = boundingBox.getXMin();
//		double xMax = boundingBox.getXMax();
//		double yMin = boundingBox.getYMin();
//		double yMax = boundingBox.getYMax();
//		double[] mapViewExtent = {xMin, yMin, xMax, yMax};

		
		// no pt block
		
		
		assertNotNull(config);
		

		// collect activity types
		List<String> activityTypes = new LinkedList<>();
//		activityTypes = AccessibilityRunUtils.collectAllFacilityOptionTypes(scenario);
		activityTypes.add(FacilityTypes.DRINKING_WATER);
//		activityTypes.add(FacilityTypes.TOILETS);
		// yyyy there is some problem with activity types: in some algorithms, only the first letter is interpreted, in some
		// other algorithms, the whole string.  BEWARE!  This is not good software design and should be changed.  kai, feb'14

		// collect homes
//		String activityFacilityType = "h";
//		ActivityFacilities homes = AccessibilityRunUtils.collectActivityFacilitiesOfType(scenario, activityFacilityType);


//		Map<String, ActivityFacilities> activityFacilitiesMap = new HashMap<String, ActivityFacilities>();
		
		// network density points
		ActivityFacilities measuringPoints = 
				AccessibilityRunUtils.createMeasuringPointsFromNetwork(scenario.getNetwork(), cellSize);		
		
		double maximumAllowedDistance = 0.5 * cellSize;
		final ActivityFacilities networkDensityFacilities = AccessibilityRunUtils.createNetworkDensityFacilities(
				scenario.getNetwork(), measuringPoints, maximumAllowedDistance);		

		final Controler controler = new Controler(scenario) ;
		controler.addControlerListener(new AccessibilityStartupListener(activityTypes, networkDensityFacilities, crs, name, cellSize));
		controler.run();


		if (createQGisOutput == true) {
			String osName = System.getProperty("os.name");
			String workingDirectory =  config.controler().getOutputDirectory();
			for (String actType : activityTypes) {
				String actSpecificWorkingDirectory = workingDirectory + actType + "/";

				for ( Modes4Accessibility mode : Modes4Accessibility.values()) {
//					if !(actType.equals("drinking_water") ) {
//						log.error("skipping everything except work for debugging purposes; remove in production code. kai, feb'14") ;
//						continue ;
//					}
					VisualizationUtils.createQGisOutput(actType, mode, boundingBox, workingDirectory, crs, includeDensityLayer,
							lowerBound, upperBound, range, symbolSize, populationThreshold);
					VisualizationUtils.createSnapshot(actSpecificWorkingDirectory, mode, osName);
				}
			}  
		}
	}
}