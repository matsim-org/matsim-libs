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
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesUtils;

import playground.dziemke.utils.LogToOutputSaver;

public class AccessibilityComputationKibera {
	public static final Logger log = Logger.getLogger( AccessibilityComputationKibera.class ) ;
	
	private static final double cellSize = 100.;

	public static void main(String[] args) {
		// Input and output
		String networkFile = "../../../../Workspace/data/accessibility/nairobi/network/2015-11-05_kibera_paths_detailed.xml";
		String facilitiesFile = "../../../../Workspace/data/accessibility/nairobi/facilities/03/facilities.xml";
		String outputDirectory = "../../../../Workspace/data/accessibility/nairobi/output/02/";
		LogToOutputSaver.setOutputDirectory(outputDirectory);

		// Parameters
		boolean createQGisOutpur = false;
		boolean includeDensityLayer = false;
		String crs = "EPSG:21037"; // = Arc 1960 / UTM zone 37S, for Nairobi, Kenya
		
//		Double lowerBound = 1.75;
//		Double upperBound = 7.;
//		Double lowerBound = 0.;
//		Double upperBound = 9.;
		Double lowerBound = 2.25;
		Double upperBound = 7.5;
		Integer range = 9; // in the current implementation, this need always be 9
		int symbolSize = 110;
		int populationThreshold = (int) (200 / (1000/cellSize * 1000/cellSize));

		Config config = ConfigUtils.createConfig( new AccessibilityConfigGroup(), new MatrixBasedPtRouterConfigGroup()) ;
		
		config.network().setInputFile(networkFile);
		config.facilities().setInputFile(facilitiesFile);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(outputDirectory);
		config.controler().setLastIteration(0);
		
		Scenario scenario = ScenarioUtils.loadScenario(config) ;

		List<String> activityTypes = new ArrayList<String>() ;
		ActivityFacilities homes = FacilitiesUtils.createActivityFacilities("homes") ;
		for ( ActivityFacility fac : scenario.getActivityFacilities().getFacilities().values()  ) {
			for ( ActivityOption option : fac.getActivityOptions().values() ) {
				// figure out all activity types
				if ( !activityTypes.contains(option.getType()) ) {
					activityTypes.add( option.getType() ) ;
				}
				// figure out where the homes are
				if ( option.getType().equals(FacilityTypes.HOME) ) {
					homes.addActivityFacility(fac);
				}
			}
		}
		
		BoundingBox boundingBox = BoundingBox.createBoundingBox(scenario.getNetwork());
		double xMin = boundingBox.getXMin();
		double xMax = boundingBox.getXMax();
		double yMin = boundingBox.getYMin();
		double yMax = boundingBox.getYMax();
		double[] mapViewExtent = {xMin, yMin, xMax, yMax};
		
		Map<String, ActivityFacilities> activityFacilitiesMap = new HashMap<String, ActivityFacilities>();
		Controler controler = new Controler(scenario) ;

		log.warn( "found activity types: " + activityTypes );
		// yyyy there is some problem with activity types: in some algorithms, only the first letter is interpreted, in some
		// other algorithms, the whole string.  BEWARE!  This is not good software design and should be changed.  kai, feb'14

		// loop over activity types to add one GridBasedAccessibilityControlerListenerV3 for each combination
		for ( String actType : activityTypes ) {

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
				
			// define the mode that will be considered
			listener.setComputingAccessibilityForMode(Modes4Accessibility.freeSpeed, true);
//			listener.setComputingAccessibilityForMode(Modes4Accessibility.car, true);
			listener.setComputingAccessibilityForMode(Modes4Accessibility.walk, true);
			listener.setComputingAccessibilityForMode(Modes4Accessibility.bike, true);
//			listener.setComputingAccessibilityForMode(Modes4Accessibility.pt, true);
				
//			listener.addAdditionalFacilityData(homes) ;
			listener.generateGridsAndMeasuringPointsByNetwork(cellSize);

			listener.writeToSubdirectoryWithName(actType);
				
			listener.setUrbansimMode(false); // avoid writing some (eventually: all) files that related to matsim4urbansim

			controler.addControlerListener(listener);
		}

		controler.run();
		
		
		if (createQGisOutpur == true) {
			String workingDirectory =  config.controler().getOutputDirectory();
		
			String osName = System.getProperty("os.name");
		
			for (String actType : activityTypes) {
				String actSpecificWorkingDirectory =  workingDirectory + actType + "/";
			
				for ( Modes4Accessibility mode : Modes4Accessibility.values()) {
					VisualizationUtilsDZ.createQGisOutput(actType, mode, mapViewExtent, workingDirectory, crs, includeDensityLayer,
							lowerBound, upperBound, range, symbolSize, populationThreshold);
					VisualizationUtilsDZ.createSnapshot(actSpecificWorkingDirectory, mode, osName);
				}
			}
		}
	}
}