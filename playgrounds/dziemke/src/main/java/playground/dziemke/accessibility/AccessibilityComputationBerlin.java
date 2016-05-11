package playground.dziemke.accessibility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.FacilityTypes;
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

import javax.inject.Inject;
import javax.inject.Provider;

public class AccessibilityComputationBerlin {
	public static final Logger log = Logger.getLogger( AccessibilityComputationBerlin.class ) ;
	
//	private static final double cellSize = 1000.;
//	private static final double cellSize = 200.;
	private static final double cellSize = 500.;

	public static void main(String[] args) {
		// Input and output
//		String networkFile = "../../shared-svn/projects/bvg_3_bln_inputdata/rev554B-bvg00-0.1sample/network/network.all.xml";
//		String networkFile = "../../shared-svn/studies/countries/de/berlin/counts/iv_counts/network.xml";
		// same network at alternate location
		String networkFile = "../../../../SVN/shared-svn/projects/accessibility_berlin/iv_counts/network.xml";
		
//		String facilitiesFile = "../../shared-svn/projects/accessibility_berlin/osm/facilities_amenities_modified.xml";
		// now using work facilities
		String facilitiesFile = "../../../../SVN/shared-svn/projects/accessibility_berlin/osm/berlin/combined/01/facilities.xml";
		
		String outputDirectory = "../../../../SVN/shared-svn/projects/accessibility_berlin/output/08/";
//		String travelTimeMatrix = "/Users/dominik/Workspace/shared-svn/projects/accessibility_berlin/pt/be_04/travelTimeMatrix.csv.gz";
//		String travelDistanceMatrix = "/Users/dominik/Workspace/shared-svn/projects/accessibility_berlin/pt/be_04/travelDistanceMatrix.csv.gz";
//		String ptStops = "/Users/dominik/Workspace/shared-svn/projects/accessibility_berlin/pt/be_04/stops.csv.gz";
		
		String travelTimeMatrixFilePT = "../../matsimExamples/countries/za/nmb/regular-pt/travelTimeMatrix_space.csv";
		String travelDistanceMatrixFilePT = "../../matsimExamples/countries/za/nmb/regular-pt/travelDistanceMatrix_space.csv";
		String ptStopsFilePT = "../../matsimExamples/countries/za/nmb/regular-pt/ptStops.csv";
		
		LogToOutputSaver.setOutputDirectory(outputDirectory);
		
		// Parameters
		String crs = TransformationFactory.DHDN_GK4;
//		String crs = "EPSG:31468"; // = DHDN GK4		
		
		// QGis
		boolean createQGisOutput = true;
//		boolean includeDensityLayer = true;
		boolean includeDensityLayer = false;
//		Double lowerBound = 1.75;
//		Double upperBound = 7.;
//		Double lowerBound = 0.;
//		Double upperBound = 9.;
		Double lowerBound = 2.25;
		Double upperBound = 7.5;
		Integer range = 9;
//		int symbolSize = 1010;
//		int symbolSize = 210;
		int symbolSize = 510;
		int populationThreshold = (int) (200 / (1000/cellSize * 1000/cellSize));
		
        double[] mapViewExtent = {4574000-1000, 5802000-1000, 4620000+1000, 5839000+1000};
				
		// Config and scenario
		final Config config = ConfigUtils.createConfig(new AccessibilityConfigGroup(), new MatrixBasedPtRouterConfigGroup()) ;
		config.network().setInputFile(networkFile);
		config.facilities().setInputFile(facilitiesFile);
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
		config.controler().setOutputDirectory(outputDirectory);
		config.controler().setLastIteration(0);

		final Scenario scenario = ScenarioUtils.loadScenario( config ) ;

		// Matrix-based pt
		MatrixBasedPtRouterConfigGroup mbpcg = (MatrixBasedPtRouterConfigGroup) config.getModule(MatrixBasedPtRouterConfigGroup.GROUP_NAME);
		mbpcg.setPtStopsInputFile(ptStopsFilePT);
		mbpcg.setUsingTravelTimesAndDistances(true);
		mbpcg.setPtTravelDistancesInputFile(travelDistanceMatrixFilePT);
		mbpcg.setPtTravelTimesInputFile(travelTimeMatrixFilePT);

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
		log.error("Only using s as activity type to speed up for testing");

		// collect homes
		String activityFacilityType = FacilityTypes.HOME;
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
//							listener.generateGridsAndMeasuringPointsByNetwork(cellSize);
							// Boundaries of Berlin are approx.: 4570000, 4613000, 5836000, 5806000
							listener.generateGridsAndMeasuringPointsByCustomBoundary(4574000, 5802000, 4620000, 5839000, cellSize);
//							listener.generateGridsAndMeasuringPointsByCustomBoundary(4590000, 5815000, 4595000, 5820000, cellSize);
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
					if ( !actType.equals("w") ) {
						log.error("skipping everything except work for debugging purposes; remove in production code. kai, feb'14") ;
						continue ;
					}
					VisualizationUtilsDZ.createQGisOutput(actType, mode, mapViewExtent, workingDirectory, crs, includeDensityLayer,
							lowerBound, upperBound, range, symbolSize, populationThreshold);
					VisualizationUtilsDZ.createSnapshot(actSpecificWorkingDirectory, mode, osName);
				}
			}  
		}
	}
}