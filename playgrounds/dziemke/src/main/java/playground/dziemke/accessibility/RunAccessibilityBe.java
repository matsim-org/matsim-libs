package playground.dziemke.accessibility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.FacilityTypes;
import org.matsim.contrib.accessibility.GridBasedAccessibilityControlerListenerV3;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.matrixbasedptrouter.MatrixBasedPtRouterConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesUtils;

public class RunAccessibilityBe {
	public static final Logger log = Logger.getLogger( RunAccessibilityBe.class ) ;
	
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

		// Parameters
//		boolean includeDensityLayer = true;
		boolean includeDensityLayer = false;
		String crs = TransformationFactory.DHDN_GK4;
//		String crs = "EPSG:31468"; // = DHDN GK4
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
				
				
//		Config config = ConfigUtils.createConfig( new AccessibilityConfigGroup() ) ;
		Config config = ConfigUtils.createConfig( new AccessibilityConfigGroup(), new MatrixBasedPtRouterConfigGroup()) ;
		
//		AccessibilityConfigGroup acg = (AccessibilityConfigGroup) config.getModule( AccessibilityConfigGroup.GROUP_NAME ) ;
//		MatrixBasedPtRouterConfigGroup mbpcg = (MatrixBasedPtRouterConfigGroup) config.getModule( MatrixBasedPtRouterConfigGroup.GROUP_NAME);
		
		config.network().setInputFile(networkFile);
		config.facilities().setInputFile(facilitiesFile);
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
		config.controler().setOutputDirectory(outputDirectory);
		config.controler().setLastIteration(0);

//		config.vspExperimental().setVspDefaultsCheckingLevel( VspDefaultsCheckingLevel.abort );

		// some (otherwise irrelevant) settings to make the vsp check happy:
//		config.timeAllocationMutator().setMutationRange(7200.);
//		config.timeAllocationMutator().setAffectingDuration(false);
//		config.plans().setRemovingUnneccessaryPlanAttributes(true);
//		config.plans().setActivityDurationInterpretation( PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration );
//
//		{
//			StrategySettings stratSets = new StrategySettings( ConfigUtils.createAvailableStrategyId(config) );
//			stratSets.setStrategyName( DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString() );
//			stratSets.setWeight(1.);
//			config.strategy().addStrategySettings(stratSets);
//		}
		
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		
//		scenario.getConfig().transit().setUseTransit(true);
		
//		mbpcg.setUsingTravelTimesAndDistances(true);
//		mbpcg.setPtStopsInputFile(ptStops);
//		mbpcg.setPtTravelTimesInputFile(travelTimeMatrix);
//		mbpcg.setPtTravelDistancesInputFile(travelDistanceMatrix);
		
//		PlansCalcRouteConfigGroup plansCalcRoute = config.plansCalcRoute();
//        
//        System.out.println("teleported mode speed factors (1): " + plansCalcRoute.getTeleportedModeFreespeedFactors());
//        System.out.println("teleported mode speeds (1): " + plansCalcRoute.getTeleportedModeSpeeds());
//        System.out.println("beeline distance factors (1): " + plansCalcRoute.getBeelineDistanceFactors());
          
        // teleported mode speed for pt also required, see PtMatrix:120
//        ModeRoutingParams ptParameters = new ModeRoutingParams(TransportMode.pt);
//        ptParameters.setTeleportedModeSpeed(50./3.6);
//        plansCalcRoute.addModeRoutingParams(ptParameters );
        
        // by adding ModeRoutingParams (as done above for pt), the other parameters are deleted
        // those parameters which are needed, have to be set again to be available.

        // teleported mode speed for walking also required, see PtMatrix:141
//        ModeRoutingParams walkParameters = new ModeRoutingParams(TransportMode.walk);
//        walkParameters.setTeleportedModeSpeed(3./3.6);
//        plansCalcRoute.addModeRoutingParams(walkParameters );
//        
//        // teleported mode speed for bike also required, see AccessibilityControlerListenerImpl:168
//        ModeRoutingParams bikeParameters = new ModeRoutingParams(TransportMode.bike);
//        bikeParameters.setTeleportedModeSpeed(15./3.6);
//        plansCalcRoute.addModeRoutingParams(bikeParameters );	
//		
//		System.out.println("teleported mode speed factors (2): " + plansCalcRoute.getTeleportedModeFreespeedFactors());
//		System.out.println("teleported mode speeds (2): " + plansCalcRoute.getTeleportedModeSpeeds());
//		System.out.println("beeline distance factors (2): " + plansCalcRoute.getBeelineDistanceFactors());

		
		// new
//		// adding pt matrix, based on transit stops
//		// should actually work. Problem is, however, that there are just too many pt stops (for minibus; ca. 36000),
//		// So, the computation takes forever
//		// Therefore, use different approach below (only create pt matrix for measuring points)
//		MatrixBasedPtRouterConfigGroup mbpcg = (MatrixBasedPtRouterConfigGroup) config.getModule( MatrixBasedPtRouterConfigGroup.GROUP_NAME);
//		// add transit stops
//		mbpcg.setPtStopsInputFile(ptStopsFile);
//
//        PlansCalcRouteConfigGroup plansCalcRoute = config.plansCalcRoute();
//        
//        System.out.println("teleported mode speed factors (1): " + plansCalcRoute.getTeleportedModeFreespeedFactors());
//        System.out.println("teleported mode speeds (1): " + plansCalcRoute.getTeleportedModeSpeeds());
//        System.out.println("beeline distance factors (1): " + plansCalcRoute.getBeelineDistanceFactors());
//          
//        // if no travel matrix (distances and times) is provided, the teleported mode speed for pt needs to be set
//        ModeRoutingParams ptParameters = new ModeRoutingParams(TransportMode.pt);
//        ptParameters.setTeleportedModeSpeed(50./3.6);
//        plansCalcRoute.addModeRoutingParams(ptParameters );
//        
//        // by adding ModeRoutingParams (as done above for pt), the other parameters are deleted
//        // the walk parameters are needed, however. This is why they have to be set here again
//
//        ModeRoutingParams walkParameters = new ModeRoutingParams(TransportMode.walk);
//        plansCalcRoute.addModeRoutingParams(walkParameters );	
//		
//		System.out.println("teleported mode speed factors (2): " + plansCalcRoute.getTeleportedModeFreespeedFactors());
//		System.out.println("teleported mode speeds (2): " + plansCalcRoute.getTeleportedModeSpeeds());
//		System.out.println("beeline distance factors (2): " + plansCalcRoute.getBeelineDistanceFactors());
//
//        BoundingBox nbb = BoundingBox.createBoundingBox(scenario.getNetwork());
//			
//		PtMatrix ptMatrix = PtMatrix.createPtMatrix(plansCalcRoute, nbb, mbpcg);
		// end adding pt matrix

		
		List<String> activityTypes = new ArrayList<String>() ;
		ActivityFacilities homes = FacilitiesUtils.createActivityFacilities("homes") ;
		for ( ActivityFacility fac : scenario.getActivityFacilities().getFacilities().values()  ) {
			for ( ActivityOption option : fac.getActivityOptions().values() ) {
				// figure out all activity types
				if ( !activityTypes.contains(option.getType()) ) {
					activityTypes.add( option.getType() ) ;
				}
				// figure out where the homes are
//				if ( option.getType().equals("h") ) {
				if ( option.getType().equals(FacilityTypes.HOME) ) {
					homes.addActivityFacility(fac);
				}
			}
		}
		
//		BoundingBox boundingBox = BoundingBox.createBoundingBox(scenario.getNetwork());
	
        
        
        double[] mapViewExtent = {4574000-1000, 5802000-1000, 4620000+1000, 5839000+1000};
			
//		PtMatrix ptMatrix = PtMatrix.createPtMatrix(plansCalcRoute, boundingBox, mbpcg);
		// end new
		
		Map<String, ActivityFacilities> activityFacilitiesMap = new HashMap<String, ActivityFacilities>();
		Controler controler = new Controler(scenario) ;

		log.warn( "found activity types: " + activityTypes );
		// yyyy there is some problem with activity types: in some algorithms, only the first letter is interpreted, in some
		// other algorithms, the whole string.  BEWARE!  This is not good software design and should be changed.  kai, feb'14

		// loop over activity types to add one GridBasedAccessibilityControlerListenerV3 for each combination
		for ( String actType : activityTypes ) {
//			if ( !actType.equals("s") ) {
//				log.error("skipping everything except work for debugging purposes; remove in production code. kai, feb'14") ;
//				continue ;
//			}

			ActivityFacilities opportunities = FacilitiesUtils.createActivityFacilities() ;
			for ( ActivityFacility fac : scenario.getActivityFacilities().getFacilities().values()  ) {
				for ( ActivityOption option : fac.getActivityOptions().values() ) {
					if ( option.getType().equals(actType) ) {
						opportunities.addActivityFacility(fac);
					}
				}
			}

			activityFacilitiesMap.put(actType, opportunities);

			GridBasedAccessibilityControlerListenerV3 listener = 
					new GridBasedAccessibilityControlerListenerV3(activityFacilitiesMap.get(actType), config, scenario.getNetwork());
//					new GridBasedAccessibilityControlerListenerV3(activityFacilitiesMap.get(actType), ptMatrix, config, scenario.getNetwork());
				
			// define the mode that will be considered
			listener.setComputingAccessibilityForMode(Modes4Accessibility.freeSpeed, true);
			listener.setComputingAccessibilityForMode(Modes4Accessibility.car, true);
			listener.setComputingAccessibilityForMode(Modes4Accessibility.walk, true);
			listener.setComputingAccessibilityForMode(Modes4Accessibility.bike, true);
//			listener.setComputingAccessibilityForMode(Modes4Accessibility.pt, true);
				
			listener.addAdditionalFacilityData(homes) ;
//			listener.generateGridsAndMeasuringPointsByNetwork(cellSize);
			// Boundaries of Berlin are approx.: 4570000, 4613000, 5836000, 5806000
			
			
			// new; more additional data
	
			
//			listener.addAdditionalFacilityData(lakes);
			
			// end new
			
			listener.generateGridsAndMeasuringPointsByCustomBoundary(4574000, 5802000, 4620000, 5839000, cellSize);
//			listener.generateGridsAndMeasuringPointsByCustomBoundary(4590000, 5815000, 4595000, 5820000, cellSize);
			
				
			listener.writeToSubdirectoryWithName(actType);
				
			listener.setUrbansimMode(false); // avoid writing some (eventually: all) files that related to matsim4urbansim

			controler.addControlerListener(listener);
		}

		controler.run();
			
		String workingDirectory =  config.controler().getOutputDirectory();
	
		String osName = System.getProperty("os.name");
	
		for (String actType : activityTypes) {
			String actSpecificWorkingDirectory =  workingDirectory + actType + "/";
		
			for ( Modes4Accessibility mode : Modes4Accessibility.values()) {
//				if ( !actType.equals("s") ) {
//					RunAccessibilityBe.log.error("skipping everything except work for debugging purposes; remove in production code. kai, feb'14") ;
//					continue ;
//				}
//				if ( !mode.equals(Modes4Accessibility.freeSpeed) ) {
//					RunAccessibilityBe.log.error("skipping everything except work for debugging purposes; remove in production code. kai, feb'14") ;
//					continue ;
//				}
//				VisualizationUtilsDZ.createQGisOutput(actType, mode, mapViewExtent, workingDirectory, crs, includeDensityLayer);
				VisualizationUtilsDZ.createQGisOutput(actType, mode, mapViewExtent, workingDirectory, crs, includeDensityLayer,
						lowerBound, upperBound, range, symbolSize, populationThreshold);
				VisualizationUtilsDZ.createSnapshot(actSpecificWorkingDirectory, mode, osName);
			}
		}
	}
}