package playground.dziemke.accessibility;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.AccessibilityContributionCalculator;
import org.matsim.contrib.accessibility.AccessibilityStartupListener;
import org.matsim.contrib.accessibility.ConstantSpeedModeProvider;
import org.matsim.contrib.accessibility.FacilityTypes;
import org.matsim.contrib.accessibility.FreeSpeedNetworkModeProvider;
import org.matsim.contrib.accessibility.NetworkModeProvider;
import org.matsim.contrib.accessibility.utils.AccessibilityUtils;
import org.matsim.contrib.accessibility.utils.VisualizationUtils;
import org.matsim.contrib.matrixbasedptrouter.MatrixBasedPtRouterConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacilities;

import com.google.inject.Key;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;
import com.vividsolutions.jts.geom.Envelope;

import playground.dziemke.utils.LogToOutputSaver;

public class AccessibilityComputationBerlin {
	public static final Logger log = Logger.getLogger( AccessibilityComputationBerlin.class ) ;
	
	private static final Double cellSize = 500.;

	public static void main(String[] args) {
		// Input and output
//		String networkFile = "../../shared-svn/projects/bvg_3_bln_inputdata/rev554B-bvg00-0.1sample/network/network.all.xml";
//		String networkFile = "../../shared-svn/studies/countries/de/berlin/counts/iv_counts/network.xml";
		// same network at alternate location
//		String networkFile = "../../../../SVN/shared-svn/projects/accessibility_berlin/iv_counts/network.xml";
		String networkFile = "../../../shared-svn/projects/accessibility_berlin/iv_counts/network.xml";
		
//		String facilitiesFile = "../../shared-svn/projects/accessibility_berlin/osm/facilities_amenities_modified.xml";
		// now using work facilities
//		String facilitiesFile = "../../../../SVN/shared-svn/projects/accessibility_berlin/osm/berlin/combined/01/facilities.xml";
		String facilitiesFile = "../../../shared-svn/projects/accessibility_berlin/osm/berlin/combined/01/facilities.xml";

//		String outputDirectory = "../../../../SVN/shared-svn/projects/accessibility_berlin/output/08_test/";
		String outputDirectory = "../../../shared-svn/projects/accessibility_berlin/output/08_test_2016-11-09/";
//		String travelTimeMatrix = "/Users/dominik/Workspace/shared-svn/projects/accessibility_berlin/pt/be_04/travelTimeMatrix.csv.gz";
//		String travelDistanceMatrix = "/Users/dominik/Workspace/shared-svn/projects/accessibility_berlin/pt/be_04/travelDistanceMatrix.csv.gz";
//		String ptStops = "/Users/dominik/Workspace/shared-svn/projects/accessibility_berlin/pt/be_04/stops.csv.gz";
		
		LogToOutputSaver.setOutputDirectory(outputDirectory);
		
		// Parameters
		String crs = TransformationFactory.DHDN_GK4;
//		String crs = "EPSG:31468"; // = DHDN GK4
		Envelope envelope = new Envelope(4574000-1000, 5802000-1000, 4620000+1000, 5839000+1000);
//		final String runId = "de_berlin_" + PathUtils.getDate() + "_" + cellSize.toString().split("\\.")[0];
		final String runId = "de_berlin_" + "106-11-10" + "_" + cellSize.toString().split("\\.")[0];
		final boolean push2Geoserver = false;
		
		// QGis parameters
		boolean createQGisOutput = true;
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
		
		// Storage objects
		final List<String> modes = new ArrayList<>();

		// Config and scenario
		final Config config = ConfigUtils.createConfig(new AccessibilityConfigGroup(), new MatrixBasedPtRouterConfigGroup()) ;
		config.network().setInputFile(networkFile);
		config.facilities().setInputFile(facilitiesFile);
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
		config.controler().setOutputDirectory(outputDirectory);
		config.controler().setLastIteration(0);

		final Scenario scenario = ScenarioUtils.loadScenario( config ) ;

		// ##### Matrix-based pt
//		MatrixBasedPtRouterConfigGroup mbpcg = (MatrixBasedPtRouterConfigGroup) config.getModule(MatrixBasedPtRouterConfigGroup.GROUP_NAME);
//		mbpcg.setPtStopsInputFile(ptStopsFilePT);
//		mbpcg.setUsingTravelTimesAndDistances(true);
//		mbpcg.setPtTravelDistancesInputFile(travelDistanceMatrixFilePT);
//		mbpcg.setPtTravelTimesInputFile(travelTimeMatrixFilePT);

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
//		BoundingBox boundingBox = BoundingBox.createBoundingBox(scenario.getNetwork());
//		final PtMatrix ptMatrix = PtMatrix.createPtMatrix(plansCalcRoute, boundingBox, mbpcg);
		
		// ##### end of PT block

		// Collect activity types
//		final List<String> activityTypes = AccessibilityRunUtils.collectAllFacilityTypes(scenario);
//		log.warn( "found activity types: " + activityTypes );
		// yyyy there is some problem with activity types: in some algorithms, only the first letter is interpreted, in some
		// other algorithms, the whole string.  BEWARE!  This is not good software design and should be changed.  kai, feb'14
		List<String> activityTypes = new ArrayList<String>();
		activityTypes.add("s");
		log.error("Only using s as activity type to speed up for testing");

		// Collect homes for density layer
		String activityFacilityType = FacilityTypes.HOME;
		final ActivityFacilities densityFacilities = AccessibilityUtils.collectActivityFacilitiesWithOptionOfType(scenario, activityFacilityType);

		// Controller
		final Controler controler = new Controler(scenario);
		controler.addControlerListener(new AccessibilityStartupListener(activityTypes, densityFacilities, crs, runId, envelope, cellSize, push2Geoserver));
		
		// Add calculators
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				MapBinder<String,AccessibilityContributionCalculator> accBinder = MapBinder.newMapBinder(this.binder(), String.class, AccessibilityContributionCalculator.class);
				{
					String mode = "freeSpeed";
					this.binder().bind(AccessibilityContributionCalculator.class).annotatedWith(Names.named(mode)).toProvider(new FreeSpeedNetworkModeProvider(TransportMode.car));
					accBinder.addBinding(mode).to(Key.get(AccessibilityContributionCalculator.class, Names.named(mode)));
					modes.add(mode);
				}
				{
					String mode = TransportMode.car;
					this.binder().bind(AccessibilityContributionCalculator.class).annotatedWith(Names.named(mode)).toProvider(new NetworkModeProvider(mode));
					accBinder.addBinding(mode).to(Key.get(AccessibilityContributionCalculator.class, Names.named(mode)));
					modes.add(mode);
				}
				{ 
					String mode = TransportMode.bike;
					this.binder().bind(AccessibilityContributionCalculator.class).annotatedWith(Names.named(mode)).toProvider(new ConstantSpeedModeProvider(mode));
					accBinder.addBinding(mode).to(Key.get(AccessibilityContributionCalculator.class, Names.named(mode)));
					modes.add(mode);
				}
				{
					final String mode = TransportMode.walk;
					this.binder().bind(AccessibilityContributionCalculator.class).annotatedWith(Names.named(mode)).toProvider(new ConstantSpeedModeProvider(mode));
					accBinder.addBinding(mode).to(Key.get(AccessibilityContributionCalculator.class, Names.named(mode)));
					modes.add(mode);
				}
			}
		});
		controler.run();

		// QGis
		if (createQGisOutput == true) {
			String osName = System.getProperty("os.name");
			String workingDirectory = config.controler().getOutputDirectory();
			for (String actType : activityTypes) {
				String actSpecificWorkingDirectory = workingDirectory + actType + "/";
				for (String mode : modes) {
					VisualizationUtils.createQGisOutput(actType, mode, envelope, workingDirectory, crs, includeDensityLayer,
							lowerBound, upperBound, range, symbolSize, populationThreshold);
					VisualizationUtils.createSnapshot(actSpecificWorkingDirectory, mode, osName);
				}
			}  
		}
	}
}