package org.matsim.integration.daily.accessibility;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.GridBasedAccessibilityControlerListenerV3;
import org.matsim.contrib.accessibility.Labels;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.matrixbasedptrouter.MatrixBasedPtRouterConfigGroup;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspDefaultsCheckingLevel;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
<<<<<<< HEAD
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
=======
>>>>>>> rescue
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.testcases.MatsimTestUtils;
//for postgres connection
import java.sql.*;
import java.util.*;
import java.lang.*;




import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;













import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;








//for postgres connection
import java.sql.*;
import java.util.*;
import java.io.IOException;
import java.lang.*;

public class AccessibilityRunTest {
	public static final Logger log = Logger.getLogger( AccessibilityRunTest.class ) ;
<<<<<<< HEAD

	private static final double cellSize = 1000.;
=======
	
	private static final double cellSize = 10000.;
>>>>>>> rescue

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;


	@Test
<<<<<<< HEAD
	public void doAccessibilityTest() {
		// Input
		String folderStructure = "../../"; // local and server

		String networkFile = folderStructure + "matsimExamples/countries/za/nmbm/network/NMBM_Network_CleanV7.xml.gz";
		String facilitiesFile = folderStructure + "matsimExamples/countries/za/nmbm/facilities/20121010/facilities.xml.gz";
		//		String travelTimeMatrix = folderStructure + "matsimExamples/countries/za/nmbm/minibus-pt/JTLU_14i/travelTimeMatrix.csv.gz";
		//		String travelDistanceMatrix = folderStructure + "matsimExamples/countries/za/nmbm/minibus-pt/JTLU_14i/travelDistanceMatrix.csv.gz";
		//		String ptStops = folderStructure + "matsimExamples/countries/za/nmbm/minibus-pt/measuringPointsAsStops/stops.csv.gz";

		// Parameters
		boolean includeDensityLayer = true;
		String crs = TransformationFactory.WGS84_SA_Albers;
		Double lowerBound = 2.;
		Double upperBound = 5.5;
		Integer range = 9;
		int symbolSize = 1010;
		int populationThreshold = (int) (200 / (1000/cellSize * 1000/cellSize));

=======
	public void doAccessibilityTest() throws IOException {
		
		System.out.println("Working Directory = " + System.getProperty("user.dir"));
		System.out.println("package input directory = " + utils.getPackageInputDirectory());
		System.out.println("class input directory = " + utils.getClassInputDirectory());
		System.out.println("input directory = " + utils.getInputDirectory());

		String folderStructure = "../../"; // local
//		String folderStructure = "../../../"; // server
			
		String networkFile = folderStructure + "matsimExamples/countries/za/nmbm/network/NMBM_Network_CleanV7.xml.gz";
		String facilitiesFile = folderStructure + "matsimExamples/countries/za/nmbm/facilities/20121010/facilities.xml.gz";
//		String travelTimeMatrix = folderStructure + "matsimExamples/countries/za/nmbm/minibus-pt/JTLU_14i/travelTimeMatrix.csv.gz";
//		String travelDistanceMatrix = folderStructure + "matsimExamples/countries/za/nmbm/minibus-pt/JTLU_14i/travelDistanceMatrix.csv.gz";
//		String ptStops = folderStructure + "matsimExamples/countries/za/nmbm/minibus-pt/measuringPointsAsStops/stops.csv.gz";
>>>>>>> rescue

		//		Config config = ConfigUtils.createConfig( new AccessibilityConfigGroup() ) ;
		Config config = ConfigUtils.createConfig( new AccessibilityConfigGroup(), new MatrixBasedPtRouterConfigGroup()) ;

		AccessibilityConfigGroup acg = (AccessibilityConfigGroup) config.getModule( AccessibilityConfigGroup.GROUP_NAME ) ;
		MatrixBasedPtRouterConfigGroup mbpcg = (MatrixBasedPtRouterConfigGroup) config.getModule( MatrixBasedPtRouterConfigGroup.GROUP_NAME);

		config.network().setInputFile(networkFile);
		config.facilities().setInputFile(facilitiesFile);

		config.controler().setLastIteration(0);

		config.vspExperimental().setVspDefaultsCheckingLevel( VspDefaultsCheckingLevel.abort );

		// some (otherwise irrelevant) settings to make the vsp check happy:
		config.timeAllocationMutator().setMutationRange(7200.);
		config.timeAllocationMutator().setAffectingDuration(false);
		config.plans().setRemovingUnneccessaryPlanAttributes(true);
		config.plans().setActivityDurationInterpretation( PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration );

		{
			StrategySettings stratSets = new StrategySettings( ConfigUtils.createAvailableStrategyId(config) );
			stratSets.setStrategyName( DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString() );
			stratSets.setWeight(1.);
			config.strategy().addStrategySettings(stratSets);
		}

		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
<<<<<<< HEAD

		//		scenario.getConfig().transit().setUseTransit(true);

		//		mbpcg.setUsingTravelTimesAndDistances(true);
		//		mbpcg.setPtStopsInputFile(ptStops);
		//		mbpcg.setPtTravelTimesInputFile(travelTimeMatrix);
		//		mbpcg.setPtTravelDistancesInputFile(travelDistanceMatrix);

=======
		
//		scenario.getConfig().scenario().setUseTransit(true);
		
//		mbpcg.setUsingTravelTimesAndDistances(true);
//		mbpcg.setPtStopsInputFile(ptStops);
//		mbpcg.setPtTravelTimesInputFile(travelTimeMatrix);
//		mbpcg.setPtTravelDistancesInputFile(travelDistanceMatrix);
		
>>>>>>> rescue
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
<<<<<<< HEAD

		// extends of the network are (as they can looked up by using the bounding box):
		// minX = 111083.9441831379, maxX = 171098.03695045778, minY = -3715412.097693177,	maxY = -3668275.43481496
		// choose map view a bit bigger
		double[] mapViewExtent = {100000,-3720000,180000,-3675000};
		// yyyyyy the above needs to be made automatic, maybe by adding something to boundingBox.  kai, jul'15

		//		PtMatrix ptMatrix = PtMatrix.createPtMatrix(plansCalcRoute, boundingBox, mbpcg);
=======
        
        // extends of the network are (as they can looked up by using the bounding box):
        // minX = 111083.9441831379, maxX = 171098.03695045778, minY = -3715412.097693177,	maxY = -3668275.43481496
        // choose map view a bit bigger
        double[] mapViewExtent = {100000,-3720000,180000,-3675000};
			
//		PtMatrix ptMatrix = PtMatrix.createPtMatrix(plansCalcRoute, boundingBox, mbpcg);
>>>>>>> rescue
		// end new

		Map<String, ActivityFacilities> activityFacilitiesMap = new HashMap<String, ActivityFacilities>();
		Controler controler = new Controler(scenario) ;
<<<<<<< HEAD
		controler.getConfig().controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles ) ;
=======
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
//		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
>>>>>>> rescue

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
					new GridBasedAccessibilityControlerListenerV3(activityFacilitiesMap.get(actType), config, scenario.getNetwork());
<<<<<<< HEAD
			//					new GridBasedAccessibilityControlerListenerV3(activityFacilitiesMap.get(actType), ptMatrix, config, scenario.getNetwork());

=======
//					new GridBasedAccessibilityControlerListenerV3(activityFacilitiesMap.get(actType), ptMatrix, config, scenario.getNetwork());
				
>>>>>>> rescue
			// define the mode that will be considered
			listener.setComputingAccessibilityForMode(Modes4Accessibility.freeSpeed, true);
			listener.setComputingAccessibilityForMode(Modes4Accessibility.car, true);
			listener.setComputingAccessibilityForMode(Modes4Accessibility.walk, true);
			listener.setComputingAccessibilityForMode(Modes4Accessibility.bike, true);
<<<<<<< HEAD
			//			listener.setComputingAccessibilityForMode(Modes4Accessibility.pt, true);

=======
//			listener.setComputingAccessibilityForMode(Modes4Accessibility.pt, true);
				
>>>>>>> rescue
			listener.addAdditionalFacilityData(homes) ;
			listener.generateGridsAndMeasuringPointsByNetwork(cellSize);

			listener.writeToSubdirectoryWithName(actType);

			listener.setUrbansimMode(false); // avoid writing some (eventually: all) files that related to matsim4urbansim

			controler.addControlerListener(listener);
		}
<<<<<<< HEAD

		controler.run();

		String workingDirectory =  config.controler().getOutputDirectory();



		String osName = System.getProperty("os.name");

		for (String actType : activityTypes) {
			String actSpecificWorkingDirectory =  workingDirectory + actType + "/";

=======
					
		controler.run();
				
		String workingDirectory =  config.controler().getOutputDirectory();
	
		
		
		String osName = System.getProperty("os.name");
	
		for (String actType : activityTypes) {
			String actSpecificWorkingDirectory =  workingDirectory + actType + "/";
		
>>>>>>> rescue
			for ( Modes4Accessibility mode : Modes4Accessibility.values()) {
				if ( !actType.equals("w") ) {
					AccessibilityRunTest.log.error("skipping everything except work for debugging purposes; remove in production code. kai, feb'14") ;
					continue ;
				}
<<<<<<< HEAD
				VisualizationUtils.createQGisOutput(actType, mode, mapViewExtent, workingDirectory, crs, includeDensityLayer,
						lowerBound, upperBound, range, symbolSize, populationThreshold);
				VisualizationUtils.createSnapshot(actSpecificWorkingDirectory, mode, osName);
			}
		}

		// writing to PostresDB

		System.out.println("Working Directory is now = " + workingDirectory);
		// Working Directory is now = test/output/org/matsim/integration/daily/accessibility/AccessibilityRunTest/doAccessibilityTest/


		System.out.println("starting to build export csv data to VSP's opengeo server");

		System.out.println("-------- PostgreSQL "
				+ "JDBC Connection Testing ------------");

		try {

			Class.forName("org.postgresql.Driver");

		} catch (ClassNotFoundException e) {

			System.out.println("Where is your PostgreSQL JDBC Driver? "
					+ "Include in your library path!");
			e.printStackTrace();
			return;

		}

		System.out.println("PostgreSQL JDBC Driver Registered!");

		Connection connection = null;

		try {

			connection = DriverManager.getConnection(
					"jdbc:postgresql://wiki.vsp.tu-berlin.de:5432/vspgeo", "postgres",
					"jafs30_A");

		} catch (SQLException e) {

			System.out.println("Connection Failed! Check output console");
			e.printStackTrace();
			return;

		}

		if (connection != null) {
			System.out.println("connection established");

			//Execute SQL query
			//System.out.println("Inserting records into the table...");
			try {
				Statement stmt = connection.createStatement();

				String locationCSV = "test/input/org/matsim/integration/daily/Accessibility";
				// yy the above should be automatic ("utils" or "config" have directory names). kai, jul'15
				// yy the above does not feel right (results should be in "output" not in "input"). kai, jul'15
				// yyyyyy however, in reality the data should be written directly from java, and we should NOT use java in order to essentially
				// write a shell script that copies data from a file into the data base.  kai, jul'15

				String sql = "COPY accessibilities(xcoord,ycoord,freespeed_accessibility,car_accessibility,bike_accessibility,walk_accessibility,pt_accessibility,population_density1,population_density2) " +
						"FROM '" + locationCSV + "/accessibilities_WGS84_conv.csv' DELIMITERS ',' CSV HEADER;";

				System.out.println("SQL statement is  = " + sql);
				//SQL statement is  = COPY accessibilities(xcoord,ycoord,freespeed_accessibility,car_accessibility,bike_accessibility,walk_accessibility,pt_accessibility,population_density1,population_density2) \
				//FROM 'test/output/org/matsim/integration/daily/accessibility/AccessibilityRunTest/doAccessibilityTest/../../../../../../../../input/org/matsim/integration/daily/Accessibility/accessibilities_WGS84_conv.csv' DELIMITERS ',' CSV HEADER;

				//commented out due to: cannot find CSV file
				//stmt.executeUpdate(sql);

				sql = "UPDATE accessibilities " +
						"SET the_geom = ST_GeomFromText('POINT(' || xcoord || ' ' || ycoord || ')',4326)";
				//stmt.executeUpdate(sql);

				//System.out.println("Inserted data from accessibilities.csv into table vspgeo");
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}


		} else {
			System.out.println("Failed to make connection!");
=======
				VisualizationUtils.createQGisOutput(actType, mode, mapViewExtent, workingDirectory);
				VisualizationUtils.createSnapshot(actSpecificWorkingDirectory, mode, osName);
			}
>>>>>>> rescue
		}
		   
	}


	// End of writing to PostgresDB
}
	    
		// writing to PostresDB
		
//		System.out.println("Working Directory is now = " + workingDirectory);
//		// Working Directory is now = test/output/org/matsim/integration/daily/accessibility/AccessibilityRunTest/doAccessibilityTest/
//		
//				
//				System.out.println("starting to build export csv data to VSP's opengeo server");
//		
//				System.out.println("-------- PostgreSQL "
//						+ "JDBC Connection Testing ------------");
//		 
//				try {
//		 
//					Class.forName("org.postgresql.Driver");
//		 
//				} catch (ClassNotFoundException e) {
//		 
//					System.out.println("Where is your PostgreSQL JDBC Driver? "
//							+ "Include in your library path!");
//					e.printStackTrace();
//					return;
//		 
//				}
//		 
//				System.out.println("PostgreSQL JDBC Driver Registered!");
//		 
//				Connection connection = null;
//		 
//				try {
//		 
//					connection = DriverManager.getConnection(
//							"jdbc:postgresql://wiki.vsp.tu-berlin.de:5432/vspgeo", "postgres",
//							"jafs30_A");
//		 
//				} catch (SQLException e) {
//		 
//					System.out.println("Connection Failed! Check output console");
//					e.printStackTrace();
//					return;
//		 
//				}
//		 
//				if (connection != null) {
//					System.out.println("connection established");
//				    
//					//Execute SQL query
//				    //System.out.println("Inserting records into the table...");
//					try {
//						Statement stmt = connection.createStatement();
//						
//						String locationCSV = "test/input/org/matsim/integration/daily/Accessibility";
//											  
//					    String sql = "COPY accessibilities(xcoord,ycoord,freespeed_accessibility,car_accessibility,bike_accessibility,walk_accessibility,pt_accessibility,population_density1,population_density2) " +
//					    			  "FROM '" + locationCSV + "/accessibilities_WGS84_conv.csv' DELIMITERS ',' CSV HEADER;";
//					    
//					    System.out.println("SQL statement is  = " + sql);
//					    //SQL statement is  = COPY accessibilities(xcoord,ycoord,freespeed_accessibility,car_accessibility,bike_accessibility,walk_accessibility,pt_accessibility,population_density1,population_density2) \
//					    //FROM 'test/output/org/matsim/integration/daily/accessibility/AccessibilityRunTest/doAccessibilityTest/../../../../../../../../input/org/matsim/integration/daily/Accessibility/accessibilities_WGS84_conv.csv' DELIMITERS ',' CSV HEADER;
//					    
//					    //commented out due to: cannot find CSV file
//					    //stmt.executeUpdate(sql);
//					    
//					    sql = "UPDATE accessibilities " +
//					    		"SET the_geom = ST_GeomFromText('POINT(' || xcoord || ' ' || ycoord || ')',4326)";
//					    //stmt.executeUpdate(sql);
//
//					    //System.out.println("Inserted data from accessibilities.csv into table vspgeo");
//					} catch (SQLException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//				    
//				    
//				} else {
//					System.out.println("Failed to make connection!");
//				}
//			}
//		 
		
		// End of writing to PostgresDB
	
