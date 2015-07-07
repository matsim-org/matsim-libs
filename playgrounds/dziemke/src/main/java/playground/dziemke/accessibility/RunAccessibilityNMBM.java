package playground.dziemke.accessibility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.GridBasedAccessibilityControlerListenerV3;
import org.matsim.contrib.accessibility.Modes4Accessibility;
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


public class RunAccessibilityNMBM {
	public static final Logger log = Logger.getLogger(RunAccessibilityNMBM.class);
	
	private static final double cellSize = 1000.;

	
	public static void main(String[] args) {
		// Input and output	
		String networkFile = "../../matsimExamples/countries/za/nmbm/network/NMBM_Network_CleanV7.xml.gz";
		String facilitiesFile = "../../matsimExamples/countries/za/nmbm/facilities/20121010/facilities.xml.gz";
		String outputDirectory = "../../accessibility-sa/data/03/";
//		String travelTimeMatrix = folderStructure + "matsimExamples/countries/za/nmbm/minibus-pt/JTLU_14i_06/travelTimeMatrix.csv.gz";
//		String travelDistanceMatrix = folderStructure + "matsimExamples/countries/za/nmbm/minibus-pt/JTLU_14i_06/travelDistanceMatrix.csv.gz";
//		String ptStops = folderStructure + "matsimExamples/countries/za/nmbm/minibus-pt/JTLU_14i_06/measuringPointsAsStops.csv.gz";
//		String minibusPtTravelTimeMatrix = folderStructure + "matsimExamples/countries/za/nmbm/minibus-pt/JTLU_14i_07/travelTimeMatrix.csv";
//		String minibusPtTravelDistanceMatrix = folderStructure + "matsimExamples/countries/za/nmbm/minibus-pt/JTLU_14i_07/travelDistanceMatrix.csv";
//		String measuringPointsAsPtStops = folderStructure + "matsimExamples/countries/za/nmbm/minibus-pt/JTLU_14i_07/measuringPointsAsStops.csv";
		
		// Parameters
		boolean includeDensityLayer = true;
		String crs = TransformationFactory.WGS84_SA_Albers;
		Double lowerBound = 2.;
		Double upperBound = 5.5;
		Integer range = 9;
		

		Config config = ConfigUtils.createConfig( new AccessibilityConfigGroup() ) ;
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
		config.controler().setOutputDirectory(outputDirectory);
		config.network().setInputFile(networkFile);
		config.facilities().setInputFile(facilitiesFile);

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
				if ( option.getType().equals("h") ) {
					homes.addActivityFacility(fac);
				}
			}
		}

		// extends of the network are (as they can looked up by using the bounding box):
		// minX = 111083.9441831379, maxX = 171098.03695045778, minY = -3715412.097693177,	maxY = -3668275.43481496
		// choose map view a bit bigger
		double[] mapViewExtent = {100000,-3720000,180000,-3675000};

		Map<String, ActivityFacilities> activityFacilitiesMap = new HashMap<String, ActivityFacilities>();
		Controler controler = new Controler(scenario) ;

		log.warn( "found activity types: " + activityTypes );
		
		// loop over activity types to add one GridBasedAccessibilityControlerListenerV3 for each combination
		for ( String actType : activityTypes ) {
			if ( !actType.equals("w") ) {
				log.error("skipping everything except work for debugging purposes; remove in production code. kai, feb'14") ;
				continue ;
			}

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
			listener.generateGridsAndMeasuringPointsByNetwork(cellSize);
				
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
				if ( !actType.equals("w") ) {
					log.error("skipping everything except work for debugging purposes; remove in production code. kai, feb'14") ;
					continue ;
				}
				VisualizationUtilsDZ.createQGisOutput(actType, mode, mapViewExtent, workingDirectory, crs, includeDensityLayer,
						lowerBound, upperBound, range);
				VisualizationUtilsDZ.createSnapshot(actSpecificWorkingDirectory, mode, osName);
			}
		}
	}
}
