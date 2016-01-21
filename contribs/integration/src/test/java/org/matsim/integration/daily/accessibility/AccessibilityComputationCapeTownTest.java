package org.matsim.integration.daily.accessibility;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.utils.AccessibilityRunUtils;
import org.matsim.contrib.matrixbasedptrouter.MatrixBasedPtRouterConfigGroup;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspDefaultsCheckingLevel;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.testcases.MatsimTestUtils;

public class AccessibilityComputationCapeTownTest {
	public static final Logger log = Logger.getLogger( AccessibilityComputationCapeTownTest.class ) ;

//	private static final double cellSize = 1000.;
	private static final Double cellSize = 10000.;
	private static final double timeOfDay = 8.*60*60;

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;


	@Test
	public void doAccessibilityTest() throws IOException {
		// Input
		String folderStructure = "../../";
		String networkFile = "matsimExamples/countries/za/capetown/2015-10-15_network.xml";

		// adapt folder structure that may be different on different machines, esp. on server
		folderStructure = PathUtils.tryANumberOfFolderStructures(folderStructure, networkFile);

		networkFile = folderStructure + networkFile ;
		String facilitiesFile = folderStructure + "matsimExamples/countries/za/capetown/2015-10-15_facilities.xml";
		 
		// minibus-pt
//		String travelTimeMatrix = folderStructure + "matsimExamples/countries/za/nmbm/minibus-pt/JTLU_14i/travelTimeMatrix.csv.gz";
//		String travelDistanceMatrix = folderStructure + "matsimExamples/countries/za/nmbm/minibus-pt/JTLU_14i/travelDistanceMatrix.csv.gz";
//		String ptStops = folderStructure + "matsimExamples/countries/za/nmbm/minibus-pt/measuringPointsAsStops/stops.csv.gz";

		// regular pt
//		String travelTimeMatrixFile = folderStructure + "matsimExamples/countries/za/nmb/regular-pt/travelTimeMatrix_space.csv";
//		String travelDistanceMatrixFile = folderStructure + "matsimExamples/countries/za/nmb/regular-pt/travelDistanceMatrix_space.csv";
//		String ptStopsFile = folderStructure + "matsimExamples/countries/za/nmb/regular-pt/ptStops.csv";
		
		
		// Parameters
		boolean createQGisOutput = false;
//		boolean createQGisOutput = true;
		boolean includeDensityLayer = true;
//		boolean includeDensityLayer = false;
		String crs = TransformationFactory.WGS84_SA_Albers;
		String name = "za_capetown_" + cellSize.toString().split("\\.")[0];
		name = name + "_test";
		
		Double lowerBound = 2.;
		Double upperBound = 5.5;
		Integer range = 9;
		int symbolSize = 1010;
		int populationThreshold = (int) (200 / (1000/cellSize * 1000/cellSize));
		
		
		// config and scenario
		Config config = ConfigUtils.createConfig(new MatrixBasedPtRouterConfigGroup());
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

		Scenario scenario = ScenarioUtils.loadScenario( config );


		BoundingBox boundingBox = BoundingBox.createBoundingBox(scenario.getNetwork());
		double xMin = boundingBox.getXMin();
		double xMax = boundingBox.getXMax();
		double yMin = boundingBox.getYMin();
		double yMax = boundingBox.getYMax();
		double[] mapViewExtent = {xMin, yMin, xMax, yMax};


		// no pt block


		List<String> activityTypes = new LinkedList<String>();
		activityTypes.add("shopping");
		
		// collect activity types
//		List<String> activityTypes = AccessibilityRunUtils.collectAllFacilityTypes(scenario);
//		log.warn( "found activity types: " + activityTypes );
		// yyyy there is some problem with activity types: in some algorithms, only the first letter is interpreted, in some
		// other algorithms, the whole string.  BEWARE!  This is not good software design and should be changed.  kai, feb'14
		
		// no collection of homes for Cape Town; was necessary for density layer, instead based on network. see below
//		String activityFacilityType = "h";
//		ActivityFacilities homes = AccessibilityRunUtils.collectActivityFacilitiesOfType(scenario, activityFacilityType);

		// network density points
		ActivityFacilities measuringPoints = 
				AccessibilityRunUtils.createMeasuringPointsFromNetwork(scenario.getNetwork(), cellSize);

		double maximumAllowedDistance = 0.5 * cellSize;
		ActivityFacilities networkDensityFacilities = AccessibilityRunUtils.createNetworkDensityFacilities(
				scenario.getNetwork(), measuringPoints, maximumAllowedDistance);		


		Controler controler = new Controler(scenario) ;


		controler.addOverridingModule(new AccessibilityComputationTestModule(activityTypes, networkDensityFacilities, crs, name, cellSize));
		controler.run();
		

		// QGis
		if (createQGisOutput == true) {
			String osName = System.getProperty("os.name");
			String workingDirectory = config.controler().getOutputDirectory();

			for (String actType : activityTypes) {
				String actSpecificWorkingDirectory = workingDirectory + actType + "/";

				for ( Modes4Accessibility mode : Modes4Accessibility.values()) {
//					if ( !actType.equals(FacilityTypes.EDUCATION) ) {
//						log.error("skipping everything except work for debugging purposes; remove in production code. kai, feb'14") ;
//						continue ;
//					}
					VisualizationUtils.createQGisOutput(actType, mode, mapViewExtent, workingDirectory, crs, includeDensityLayer,
							lowerBound, upperBound, range, symbolSize, populationThreshold);
					VisualizationUtils.createSnapshot(actSpecificWorkingDirectory, mode, osName);
				}
			}  
		}
	}
}