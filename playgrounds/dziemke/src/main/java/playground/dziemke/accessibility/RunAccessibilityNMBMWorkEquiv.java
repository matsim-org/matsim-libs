package playground.dziemke.accessibility;

import java.util.ArrayList;
import java.util.List;

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
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesUtils;


public class RunAccessibilityNMBMWorkEquiv {
	public static final Logger log = Logger.getLogger(RunAccessibilityNMBMWorkEquiv.class);
	
	private static final double cellSize = 1000.;

	
	public static void main(String[] args) {
	
		String networkFile = "../../matsimExamples/countries/za/nmbm/network/NMBM_Network_CleanV7.xml.gz";
		String facilitiesFile = "../../matsimExamples/countries/za/nmbm/facilities/20121010/facilities.xml.gz";
		String outputDirectory = "../../data/accessibility/nmbm/001/";
//		String travelTimeMatrix = folderStructure + "matsimExamples/countries/za/nmbm/minibus-pt/JTLU_14i_06/travelTimeMatrix.csv.gz";
//		String travelDistanceMatrix = folderStructure + "matsimExamples/countries/za/nmbm/minibus-pt/JTLU_14i_06/travelDistanceMatrix.csv.gz";
//		String ptStops = folderStructure + "matsimExamples/countries/za/nmbm/minibus-pt/JTLU_14i_06/measuringPointsAsStops.csv.gz";
//		String minibusPtTravelTimeMatrix = folderStructure + "matsimExamples/countries/za/nmbm/minibus-pt/JTLU_14i_07/travelTimeMatrix.csv";
//		String minibusPtTravelDistanceMatrix = folderStructure + "matsimExamples/countries/za/nmbm/minibus-pt/JTLU_14i_07/travelDistanceMatrix.csv";
//		String measuringPointsAsPtStops = folderStructure + "matsimExamples/countries/za/nmbm/minibus-pt/JTLU_14i_07/measuringPointsAsStops.csv";
		

		Config config = ConfigUtils.createConfig( new AccessibilityConfigGroup() ) ;
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
		config.controler().setOutputDirectory(outputDirectory);
		config.network().setInputFile(networkFile);
		config.facilities().setInputFile(facilitiesFile);

		config.controler().setLastIteration(0);

		Scenario scenario = ScenarioUtils.loadScenario(config) ;

		String typeWEQ = "w-eq";
		List<String> activityTypes = new ArrayList<String>() ;
		activityTypes.add(typeWEQ);

		ActivityFacilities homes = FacilitiesUtils.createActivityFacilities("homes") ;
		ActivityFacilities amenities = FacilitiesUtils.createActivityFacilities("amenities") ;
		for ( ActivityFacility fac : scenario.getActivityFacilities().getFacilities().values()  ) {
			for ( ActivityOption option : fac.getActivityOptions().values() ) {
				// figure out all activity types
				if ( !activityTypes.contains(option.getType()) ) {
					activityTypes.add( option.getType() ) ;
				}

				if ( !option.getType().equals("h") && !option.getType().equals("w") ) {
					if (!amenities.getFacilities().containsKey(fac.getId())) {
						amenities.addActivityFacility(fac);
					}
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

		Controler controler = new Controler(scenario);

		GridBasedAccessibilityControlerListenerV3 listener = 
				new GridBasedAccessibilityControlerListenerV3(amenities, config, scenario.getNetwork());

		listener.setComputingAccessibilityForMode(Modes4Accessibility.freeSpeed, true);
		listener.addAdditionalFacilityData(homes) ;
		listener.generateGridsAndMeasuringPointsByNetwork(cellSize);
		listener.writeToSubdirectoryWithName("w-eq");
		listener.setUrbansimMode(false); // avoid writing some (eventually: all) files that related to matsim4urbansim

		controler.addControlerListener(listener);

		controler.run();

		String workingDirectory =  config.controler().getOutputDirectory();

		String osName = System.getProperty("os.name");

		for (String actType : activityTypes) {
			String actSpecificWorkingDirectory =  workingDirectory + actType + "/";

			for ( Modes4Accessibility mode : Modes4Accessibility.values()) {
				VisualizationUtils.createQGisOutput(actType, mode, mapViewExtent, workingDirectory);
				VisualizationUtils.createSnapshot(actSpecificWorkingDirectory, mode, osName);
			}
		}
	}
}
