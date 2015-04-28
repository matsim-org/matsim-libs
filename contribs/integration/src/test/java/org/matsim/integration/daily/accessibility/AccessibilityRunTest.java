package org.matsim.integration.daily.accessibility;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.GridBasedAccessibilityControlerListenerV3;
import org.matsim.contrib.accessibility.Labels;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.analysis.vsp.qgis.QGisConstants;
import org.matsim.contrib.analysis.vsp.qgis.QGisMapnikFileCreator;
import org.matsim.contrib.analysis.vsp.qgis.QGisWriter;
import org.matsim.contrib.analysis.vsp.qgis.RasterLayer;
import org.matsim.contrib.analysis.vsp.qgis.VectorLayer;
import org.matsim.contrib.analysis.vsp.qgis.layerTemplates.AccessibilityDensitiesRenderer;
import org.matsim.contrib.analysis.vsp.qgis.layerTemplates.AccessibilityRenderer;
import org.matsim.contrib.analysis.vsp.qgis.layerTemplates.AccessibilityXmlRenderer;
import org.matsim.contrib.matrixbasedptrouter.MatrixBasedPtRouterConfigGroup;
import org.matsim.contrib.matrixbasedptrouter.PtMatrix;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.ExeRunner;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.testcases.MatsimTestUtils;

public class AccessibilityRunTest {
	public static final Logger log = Logger.getLogger( AccessibilityRunTest.class ) ;
	
	private static final double cellSize = 1000.;

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
	
	
	@Test
	public void doAccessibilityTest() {
		System.out.println("Working Directory = " + System.getProperty("user.dir"));
		System.out.println("package input directory = " + utils.getPackageInputDirectory());
		System.out.println("class input directory = " + utils.getClassInputDirectory());
		System.out.println("input directory = " + utils.getInputDirectory());

		String folderStructure = "../../"; // local
			
		String networkFile = folderStructure + "matsimExamples/countries/za/nmbm/network/NMBM_Network_CleanV7.xml.gz";
		String facilitiesFile = folderStructure + "matsimExamples/countries/za/nmbm/facilities/20121010/facilities.xml.gz";
		String minibusPtTravelTimeMatrix = folderStructure + "matsimExamples/countries/za/nmbm/minibus-pt/JTLU_14i/travelTimeMatrix.csv.gz";
		String minibusPtTravelDistanceMatrix = folderStructure + "matsimExamples/countries/za/nmbm/minibus-pt/JTLU_14i/travelDistanceMatrix.csv.gz";
		String measuringPointsAsPtStops = folderStructure + "matsimExamples/countries/za/nmbm/minibus-pt/measuringPointsAsStops/stops.csv.gz";


//		Config config = ConfigUtils.createConfig( new AccessibilityConfigGroup() ) ;
		Config config = ConfigUtils.createConfig( new AccessibilityConfigGroup(), new MatrixBasedPtRouterConfigGroup()) ;
		
		AccessibilityConfigGroup acg = (AccessibilityConfigGroup) config.getModule( AccessibilityConfigGroup.GROUP_NAME ) ;
		MatrixBasedPtRouterConfigGroup mbpcg = (MatrixBasedPtRouterConfigGroup) config.getModule( MatrixBasedPtRouterConfigGroup.GROUP_NAME);
		
		config.network().setInputFile(networkFile);
		config.facilities().setInputFile(facilitiesFile);

		config.controler().setLastIteration(0);

		config.vspExperimental().setVspDefaultsCheckingLevel( VspExperimentalConfigGroup.ABORT );

		// some (otherwise irrelevant) settings to make the vsp check happy:
		config.timeAllocationMutator().setMutationRange(7200.);
		config.timeAllocationMutator().setAffectingDuration(false);
		config.vspExperimental().setRemovingUnneccessaryPlanAttributes(true);
		config.plans().setActivityDurationInterpretation( PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration );

		{
			StrategySettings stratSets = new StrategySettings( ConfigUtils.createAvailableStrategyId(config) );
			stratSets.setStrategyName( DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString() );
			stratSets.setWeight(1.);
			config.strategy().addStrategySettings(stratSets);
		}
		
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		
//		scenario.getConfig().scenario().setUseTransit(true);
		
		mbpcg.setUsingTravelTimesAndDistances(true);
		mbpcg.setPtStopsInputFile(measuringPointsAsPtStops);
		mbpcg.setPtTravelTimesInputFile(minibusPtTravelTimeMatrix);
		mbpcg.setPtTravelDistancesInputFile(minibusPtTravelDistanceMatrix);
		
		PlansCalcRouteConfigGroup plansCalcRoute = config.plansCalcRoute();
        
        System.out.println("teleported mode speed factors (1): " + plansCalcRoute.getTeleportedModeFreespeedFactors());
        System.out.println("teleported mode speeds (1): " + plansCalcRoute.getTeleportedModeSpeeds());
        System.out.println("beeline distance factors (1): " + plansCalcRoute.getBeelineDistanceFactors());
          
        // teleported mode speed for pt also required, see PtMatrix:120
//        ModeRoutingParams ptParameters = new ModeRoutingParams(TransportMode.pt);
//        ptParameters.setTeleportedModeSpeed(50./3.6);
//        plansCalcRoute.addModeRoutingParams(ptParameters );
        
        // by adding ModeRoutingParams (as done above for pt), the other parameters are deleted
        // those parameters which are needed, have to be set again to be available.

        // teleported mode speed for walking also required, see PtMatrix:141
        ModeRoutingParams walkParameters = new ModeRoutingParams(TransportMode.walk);
        walkParameters.setTeleportedModeSpeed(3./3.6);
        plansCalcRoute.addModeRoutingParams(walkParameters );
        
        // teleported mode speed for bike also required, see AccessibilityControlerListenerImpl:168
        ModeRoutingParams bikeParameters = new ModeRoutingParams(TransportMode.bike);
        bikeParameters.setTeleportedModeSpeed(15./3.6);
        plansCalcRoute.addModeRoutingParams(bikeParameters );	
		
		System.out.println("teleported mode speed factors (2): " + plansCalcRoute.getTeleportedModeFreespeedFactors());
		System.out.println("teleported mode speeds (2): " + plansCalcRoute.getTeleportedModeSpeeds());
		System.out.println("beeline distance factors (2): " + plansCalcRoute.getBeelineDistanceFactors());
		
		assertNotNull(config);
		
		
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
				if ( option.getType().equals("h") ) {
					homes.addActivityFacility(fac);
				}
			}
		}
		
		BoundingBox boundingBox = BoundingBox.createBoundingBox(scenario.getNetwork());
        
        // extends of the network are (as they can looked up by using the bounding box):
        // minX = 111083.9441831379, maxX = 171098.03695045778, minY = -3715412.097693177,	maxY = -3668275.43481496
        // choose map view a bit bigger
        double[] mapViewExtent = {100000,-3720000,180000,-3675000};
			
		PtMatrix ptMatrix = PtMatrix.createPtMatrix(plansCalcRoute, boundingBox, mbpcg);
		// end new
		
		Map<String, ActivityFacilities> activityFacilitiesMap = new HashMap<String, ActivityFacilities>();
		Controler controler = new Controler(scenario) ;
		controler.setOverwriteFiles(true);

		log.warn( "found activity types: " + activityTypes );
		// yyyy there is some problem with activity types: in some algorithms, only the first letter is interpreted, in some
		// other algorithms, the whole string.  BEWARE!  This is not good software design and should be changed.  kai, feb'14

		// loop over activity types to add one GridBasedAccessibilityControlerListenerV3 for each combination
		for ( String actType : activityTypes ) {
			if ( !actType.equals("w") ) {
				log.error("skipping everything except work for debugging purposes; remove in production code. kai, feb'14") ;
				continue ;
			}

			config.controler().setOutputDirectory( utils.getOutputDirectory());
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
//					new GridBasedAccessibilityControlerListenerV3(activityFacilitiesMap.get(actType), config, scenario.getNetwork());
					new GridBasedAccessibilityControlerListenerV3(activityFacilitiesMap.get(actType), ptMatrix, config, scenario.getNetwork());
				
			// define the mode that will be considered
			listener.setComputingAccessibilityForMode(Modes4Accessibility.freeSpeed, true);
			listener.setComputingAccessibilityForMode(Modes4Accessibility.car, true);
			listener.setComputingAccessibilityForMode(Modes4Accessibility.walk, true);
			listener.setComputingAccessibilityForMode(Modes4Accessibility.bike, true);
			listener.setComputingAccessibilityForMode(Modes4Accessibility.pt, true);
				
			listener.addAdditionalFacilityData(homes) ;
			listener.generateGridsAndMeasuringPointsByNetwork(cellSize);
				
			listener.writeToSubdirectoryWithName(actType);
				
			listener.setUrbansimMode(false); // avoid writing some (eventually: all) files that related to matsim4urbansim

			controler.addControlerListener(listener);
		}

	controler.run();
			
	String workingDirectory =  config.controler().getOutputDirectory();
	
	createQGisOutput(activityTypes, mapViewExtent, workingDirectory);
	}

	
	/**
	 * Create QGis project files
	 * 
	 * @param activityTypes
	 * @param mapViewExtent
	 * @param workingDirectory
	 */
	private static void createQGisOutput(List<String> activityTypes, double[] mapViewExtent, String workingDirectory) {
		// create Mapnik file that is needed to have OSM layer in QGis project
		QGisMapnikFileCreator.writeMapnikFile(workingDirectory + "osm_mapnik.xml");
		
		// loop over activity types to produce one QGIs project file for each combination
		for (String actType : activityTypes) {
			for ( Modes4Accessibility mode : Modes4Accessibility.values()) {
				if ( !actType.equals("w") ) {
					log.error("skipping everything except work for debugging purposes; remove in production code. kai, feb'14") ;
					continue ;
				}
//				if ( !mode.equals(Modes4Accessibility.freeSpeed)) {
//					log.error("skipping everything except freespeed and pt for debugging purposes; remove in production code. dz, nov'14") ;
//					continue ;
//				}
		
				// Write QGis project file
				QGisWriter writer = new QGisWriter(TransformationFactory.WGS84_SA_Albers, workingDirectory);
				String qGisProjectFile = "QGisProjectFile_" + mode + ".qgs";
				writer.setExtent(mapViewExtent);
					
				// osm raster layer
				// working directory needs to be the storage location of the file
				writer.changeWorkingDirectory(workingDirectory);
				
				RasterLayer mapnikLayer = new RasterLayer("osm_mapnik_xml", workingDirectory + "/osm_mapnik.xml");
				new AccessibilityXmlRenderer(mapnikLayer);
				mapnikLayer.setSrs("WGS84_Pseudo_Mercator");
				writer.addLayer(0,mapnikLayer);

				// working directory needs to be the storage location of the file
				String actSpecificWorkingDirectory =  workingDirectory + actType + "/";
				writer.changeWorkingDirectory(actSpecificWorkingDirectory);
				
				// density layer
				VectorLayer densityLayer = new VectorLayer(
						"density", actSpecificWorkingDirectory + "accessibilities.csv", QGisConstants.geometryType.Point, true);
				densityLayer.setXField(Labels.X_COORDINATE);
				densityLayer.setYField(Labels.Y_COORDINATE);
				AccessibilityDensitiesRenderer dRenderer = new AccessibilityDensitiesRenderer(densityLayer);
				dRenderer.setRenderingAttribute(Labels.POPULATION_DENSITIY);
				writer.addLayer(densityLayer);
				
				// accessibility layer
				VectorLayer accessibilityLayer = new VectorLayer(
						"accessibility", actSpecificWorkingDirectory + "accessibilities.csv", QGisConstants.geometryType.Point, true);
				// there are two ways to set x and y fields for csv geometry files
				// 1) if there is a header, you can set the members xField and yField to the name of the column headers
				// 2) if there is no header, you can write the column index into the member (e.g. field_1, field_2,...), but works also if there is a header
				accessibilityLayer.setXField(Labels.X_COORDINATE);
				accessibilityLayer.setYField(Labels.Y_COORDINATE);
				AccessibilityRenderer renderer = new AccessibilityRenderer(accessibilityLayer);
				if (mode.equals(Modes4Accessibility.freeSpeed)) {
					renderer.setRenderingAttribute(Labels.ACCESSIBILITY_BY_FREESPEED); // choose column/header to visualize
				} else if (mode.equals(Modes4Accessibility.car)) {
					renderer.setRenderingAttribute(Labels.ACCESSIBILITY_BY_CAR); // choose column/header to visualize
				} else if (mode.equals(Modes4Accessibility.bike)) {
					renderer.setRenderingAttribute(Labels.ACCESSIBILITY_BY_BIKE); // choose column/header to visualize
				} else if (mode.equals(Modes4Accessibility.walk)) {
					renderer.setRenderingAttribute(Labels.ACCESSIBILITY_BY_WALK); // choose column/header to visualize
				} else if (mode.equals(Modes4Accessibility.pt)) {
					renderer.setRenderingAttribute(Labels.ACCESSIBILITY_BY_PT); // choose column/header to visualize
				} else {
					throw new RuntimeException("Other modes not yet considered!");
				}
				writer.addLayer(accessibilityLayer);

				// write the project file
				writer.write(qGisProjectFile);
			
				String osName = System.getProperty("os.name");
				createSnapshot(actSpecificWorkingDirectory, mode, osName);
			}
		}		
	}

	
	/**
	 * This method creates a snapshot of the accessibility map that is held in the created QGis file.
	 * The syntax within the method is different dependent on the operating system.
	 * 
	 * @param workingDirectory The directory where the QGisProjectFile (the data source of the to-be-created snapshot) is stored
	 * @param mode
	 * @param osName
	 */
	private static void createSnapshot(String workingDirectory, Modes4Accessibility mode, String osName) {
		
		//TODO adapt this method so that maps for different modes are created.
		
		// if OS is Windows
		// example (daniel r) // os.arch=amd64 // os.name=Windows 7 // os.version=6.1
		if ( osName.contains("Win") || osName.contains("win")) {
			// On Windows, the PATH variables need to be set correctly to be able to call "qgis.bat" on the command line
			// This needs to be done manually. It does not seem to be set automatically when installing QGis
			String cmd = "qgis.bat " + workingDirectory + "QGisProjectFile_" + mode + ".qgs" +
					" --snapshot " + workingDirectory + "snapshot_" + mode + ".png";

			String stdoutFileName = workingDirectory + "snapshot_" + mode + ".log";
			int timeout = 99999;

			ExeRunner.run(cmd, stdoutFileName, timeout);
		
		// if OS is Macintosh
		// example (dominik) // os.arch=x86_64 // os.name=Mac OS X // os.version=10.10.2
		} else if ( osName.contains("Mac") || osName.contains("mac") ) {
			String cmd = "/Applications/QGIS.app/Contents/MacOS/QGIS " + workingDirectory + "QGisProjectFile_" + mode + ".qgs" +
					" --snapshot " + workingDirectory + "snapshot_" + mode + ".png";

					String stdoutFileName = workingDirectory + "snapshot_" + mode + ".log";
			
			int timeout = 99999;

			ExeRunner.run(cmd, stdoutFileName, timeout);
		
		// if OS is Linux
		// example (benjamin) // os.arch=amd64 // os.name=Linux	// os.version=3.13.0-45-generic
		//} else if ( osName.contains("Lin") || osName.contains("lin") ) {
			// TODO for linux
			
		// if OS is other
		} else {
			log.warn("generating png files not implemented for os.arch=" + System.getProperty("os.arch") );
		}
	}
}
