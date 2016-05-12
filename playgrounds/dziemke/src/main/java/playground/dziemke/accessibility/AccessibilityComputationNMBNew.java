package playground.dziemke.accessibility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.GridBasedAccessibilityControlerListenerV3;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.utils.AccessibilityRunUtils;
import org.matsim.contrib.matrixbasedptrouter.MatrixBasedPtRouterConfigGroup;
import org.matsim.contrib.matrixbasedptrouter.PtMatrix;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacilities;

import playground.dziemke.utils.LogToOutputSaver;


public class AccessibilityComputationNMBNew {
	public static final Logger log = Logger.getLogger(AccessibilityComputationNMBNew.class);
	
	private static final double cellSize = 500.;
	
	public static void main(String[] args) {
		// Input and output	
		String networkFile = "../../../matsimExamples/countries/za/nmb/network/NMBM_Network_CleanV7.xml.gz";
		String facilitiesFile = "../../../matsimExamples/countries/za/nmb/facilities/20121010/facilities.xml.gz";
		String outputDirectory = "../../../shared-svn/projects/maxess/data/nmb/output/44/";
//		String travelTimeMatrix = folderStructure + "matsimExamples/countries/za/nmbm/minibus-pt/JTLU_14i_06/travelTimeMatrix.csv.gz";
//		String travelDistanceMatrix = folderStructure + "matsimExamples/countries/za/nmbm/minibus-pt/JTLU_14i_06/travelDistanceMatrix.csv.gz";
//		String ptStops = folderStructure + "matsimExamples/countries/za/nmbm/minibus-pt/JTLU_14i_06/measuringPointsAsStops.csv.gz";
//		String minibusPtTravelTimeMatrix = folderStructure + "matsimExamples/countries/za/nmbm/minibus-pt/JTLU_14i_07/travelTimeMatrix.csv";
//		String minibusPtTravelDistanceMatrix = folderStructure + "matsimExamples/countries/za/nmbm/minibus-pt/JTLU_14i_07/travelDistanceMatrix.csv";
//		String measuringPointsAsPtStops = folderStructure + "matsimExamples/countries/za/nmbm/minibus-pt/JTLU_14i_07/measuringPointsAsStops.csv";
		
		// Regular pt
		String travelTimeMatrixFile = "../../../matsimExamples/countries/za/nmb/regular-pt/travelTimeMatrix_space.csv";
		String travelDistanceMatrixFile = "../../../matsimExamples/countries/za/nmb/regular-pt/travelDistanceMatrix_space.csv";
		String ptStopsFile = "../../../matsimExamples/countries/za/nmb/regular-pt/ptStops.csv";

		// Minibus pt
//		String travelTimeMatrixFile = "../../../shared-svn/projects/maxess/data/nmb/minibus-pt/jtlu14b/matrix_grid_500/travelTimeMatrix_0.csv";
//		String travelDistanceMatrixFile = "../../../shared-svn/projects/maxess/data/nmb/minibus-pt/jtlu14b/matrix_grid_500/travelDistanceMatrix_0.csv";
//		String ptStopsFile = "../../../shared-svn/projects/maxess/data/nmb/minibus-pt/jtlu14b/matrix_grid_500/ptStops.csv";
//		String travelTimeMatrixFile = "../../../matsimExamples/countries/za/nmb/minibus-pt/jtlu14b/matrix_grid_1000/travelTimeMatrix_0.csv.gz";
//		String travelDistanceMatrixFile = "../../../matsimExamples/countries/za/nmb/minibus-pt/jtlu14b/matrix_grid_1000/travelDistanceMatrix_0.csv.gz";
//		String ptStopsFile = "../../../matsimExamples/countries/za/nmb/minibus-pt/jtlu14b/matrix_grid_1000/ptStops.csv";

		LogToOutputSaver.setOutputDirectory(outputDirectory);
		
		// Parameters
		String crs = TransformationFactory.WGS84_SA_Albers;

		// QGis
		boolean createQGisOutput = true;
		boolean includeDensityLayer = true;
		Double lowerBound = -3.5;
		Double upperBound = 3.5;
		Integer range = 9;
		int symbolSize = 525;
		int populationThreshold = (int) (120 / (1000/cellSize * 1000/cellSize));

		/* Extent of the network are (as they can looked up by using the bounding box):
		/* minX = 111083.9441831379, maxX = 171098.03695045778, minY = -3715412.097693177,	maxY = -3668275.43481496 */
//		double[] mapViewExtent = {100000,-3720000,180000,-3675000}; // choose map view a bit bigger
		double[] mapViewExtent = {115000,-3718000,161000,-3679000};

		// Config and scenario
//		final Config config = ConfigUtils.createConfig( new AccessibilityConfigGroup() ) ;
		Config config = ConfigUtils.createConfig(new AccessibilityConfigGroup(), new MatrixBasedPtRouterConfigGroup());
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
		config.controler().setOutputDirectory(outputDirectory);
		config.network().setInputFile(networkFile);
		config.facilities().setInputFile(facilitiesFile);
		config.controler().setLastIteration(0);

		final Scenario scenario = ScenarioUtils.loadScenario(config);

		// Matrix-based pt
		MatrixBasedPtRouterConfigGroup mbpcg = (MatrixBasedPtRouterConfigGroup) config.getModule(MatrixBasedPtRouterConfigGroup.GROUP_NAME);
		mbpcg.setPtStopsInputFile(ptStopsFile);
		mbpcg.setUsingTravelTimesAndDistances(true);
		mbpcg.setPtTravelDistancesInputFile(travelDistanceMatrixFile);
		mbpcg.setPtTravelTimesInputFile(travelTimeMatrixFile);

		// plansClacRoute parameters
		PlansCalcRouteConfigGroup plansCalcRoute = config.plansCalcRoute();

		// if no travel matrix (distances and times) is provided, the teleported mode speed for pt needs to be set
		// teleported mode speed for pt also required, see PtMatrix:120
//		ModeRoutingParams ptParameters = new ModeRoutingParams(TransportMode.pt);
//		ptParameters.setTeleportedModeSpeed(50./3.6);
//		plansCalcRoute.addModeRoutingParams(ptParameters);

		// by adding ModeRoutingParams (as done above for pt), the other parameters are deleted
		// the walk and bike parameters are needed, however. This is why they have to be set here again

		// teleported mode speed for walking also required, see PtMatrix:141
		ModeRoutingParams walkParameters = new ModeRoutingParams(TransportMode.walk);
		walkParameters.setTeleportedModeSpeed(3./3.6);
		plansCalcRoute.addModeRoutingParams(walkParameters );

		// teleported mode speed for bike also required, see AccessibilityControlerListenerImpl:168
		ModeRoutingParams bikeParameters = new ModeRoutingParams(TransportMode.bike);
		bikeParameters.setTeleportedModeSpeed(15./3.6);
		plansCalcRoute.addModeRoutingParams(bikeParameters );

		// pt matrix
		BoundingBox boundingBox = BoundingBox.createBoundingBox(scenario.getNetwork());
		final PtMatrix ptMatrix = PtMatrix.createPtMatrix(plansCalcRoute, boundingBox, mbpcg);


		// collect activity types
//		final List<String> activityTypes = AccessibilityRunUtils.collectAllFacilityTypes(scenario);
//		log.warn( "found activity types: " + activityTypes );
		// yyyy there is some problem with activity types: in some algorithms, only the first letter is interpreted, in some
		// other algorithms, the whole string.  BEWARE!  This is not good software design and should be changed.  kai, feb'14
		List<String> activityTypes = new ArrayList<String>();
		activityTypes.add("s");
		activityTypes.add("w");
		activityTypes.add("l");
		activityTypes.add("e");
		log.error("Only using s as activity type to speed up for testing");

		// collect homes
		String activityFacilityType = "h";
		final ActivityFacilities homes = AccessibilityRunUtils.collectActivityFacilitiesWithOptionOfType(scenario, activityFacilityType);

		final Controler controler = new Controler(scenario);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				// Loop over activity types to add one GridBasedAccessibilityControlerListenerV3 for each
				for ( final String actType : activityTypes ) {
					addControlerListenerBinding().toProvider(new Provider<ControlerListener>() {
						@Inject Scenario scenario;
						@Inject Map<String, TravelTime> travelTimes;
						@Inject Map<String, TravelDisutilityFactory> travelDisutilityFactories;

						@Override
						public ControlerListener get() {
							GridBasedAccessibilityControlerListenerV3 listener =
									new GridBasedAccessibilityControlerListenerV3(AccessibilityRunUtils.collectActivityFacilitiesWithOptionOfType(scenario, actType), ptMatrix, config, scenario, travelTimes, travelDisutilityFactories);
							listener.setComputingAccessibilityForMode(Modes4Accessibility.freeSpeed, true);
							listener.setComputingAccessibilityForMode(Modes4Accessibility.car, true);
							listener.setComputingAccessibilityForMode(Modes4Accessibility.walk, true);
							listener.setComputingAccessibilityForMode(Modes4Accessibility.bike, true);
							listener.setComputingAccessibilityForMode(Modes4Accessibility.pt, true);

							listener.addAdditionalFacilityData(homes) ;
							listener.generateGridsAndMeasuringPointsByNetwork(cellSize);
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