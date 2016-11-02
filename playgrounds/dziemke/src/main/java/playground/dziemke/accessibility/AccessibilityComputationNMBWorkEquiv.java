package playground.dziemke.accessibility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jts.geom.Envelope;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accessibility.AccessibilityCalculator;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.GridBasedAccessibilityShutdownListenerV3;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.gis.GridUtils;
import org.matsim.contrib.accessibility.utils.VisualizationUtils;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesUtils;

import javax.inject.Inject;
import javax.inject.Provider;


public class AccessibilityComputationNMBWorkEquiv {
	public static final Logger log = Logger.getLogger(AccessibilityComputationNMBWorkEquiv.class);
	
//	private static final double cellSize = 1000.;
	private static final double cellSize = 10000.;

	
	public static void main(String[] args) {
		// Input and output	
		String networkFile = "../../../matsimExamples/countries/za/nmb/network/NMBM_Network_CleanV7.xml.gz";
		String facilitiesFile = "../../../matsimExamples/countries/za/nmb/facilities/20121010/facilities.xml.gz";
		String outputDirectory = "../../../accessibility-sa/data/12/";
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
		int symbolSize = 1010;
		int populationThreshold = (int) (200 / (1000/cellSize * 1000/cellSize));
		

		final Config config = ConfigUtils.createConfig( new AccessibilityConfigGroup() ) ;
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
		config.controler().setOutputDirectory(outputDirectory);
		config.network().setInputFile(networkFile);
		config.facilities().setInputFile(facilitiesFile);

		config.controler().setLastIteration(0);

		final Scenario scenario = ScenarioUtils.loadScenario(config) ;

		String typeWEQ = "w-eq";
		List<String> activityTypes = new ArrayList<>() ;
		activityTypes.add(typeWEQ);

		final ActivityFacilities homes = FacilitiesUtils.createActivityFacilities("homes") ;
		final ActivityFacilities amenities = FacilitiesUtils.createActivityFacilities("amenities") ;
		for ( ActivityFacility fac : scenario.getActivityFacilities().getFacilities().values()  ) {
			for ( ActivityOption option : fac.getActivityOptions().values() ) {
				// figure out all activity types
				if ( !activityTypes.contains(option.getType()) ) {
					activityTypes.add( option.getType() ) ;
				}

				// add all facilites that are not home or work
				if ( !option.getType().equals("h") && !option.getType().equals("w") && !option.getType().equals("minor")) {
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
		
//		double[] mapViewExtent = {100000,-3720000,180000,-3675000}; // choose map view a bit bigger
		Envelope envelope = new Envelope(115000,-3718000,161000,-3679000); // what actually needs to be dran

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addControlerListenerBinding().toProvider(new Provider<ControlerListener>() {
					@Inject Scenario scenario;
					@Inject Map<String, TravelTime> travelTimes;
					@Inject Map<String, TravelDisutilityFactory> travelDisutilityFactories;

					@Override
					public ControlerListener get() {
						BoundingBox bb = BoundingBox.createBoundingBox(((Scenario) scenario).getNetwork());
						AccessibilityCalculator accessibilityCalculator = new AccessibilityCalculator(travelTimes, travelDisutilityFactories, scenario);
						accessibilityCalculator.setMeasuringPoints(GridUtils.createGridLayerByGridSizeByBoundingBoxV2(bb.getXMin(), bb.getYMin(), bb.getXMax(), bb.getYMax(), cellSize));
						GridBasedAccessibilityShutdownListenerV3 listener = new GridBasedAccessibilityShutdownListenerV3(accessibilityCalculator, (ActivityFacilities) amenities, null, config, scenario, travelTimes,
						travelDisutilityFactories,bb.getXMin(), bb.getYMin(), bb.getXMax(), bb.getYMax(), cellSize);
						accessibilityCalculator.setComputingAccessibilityForMode(Modes4Accessibility.freeSpeed, true);
						listener.addAdditionalFacilityData(homes) ;
						listener.writeToSubdirectoryWithName("w-eq");
						return listener;
					}
				});
			}
		});
		controler.run();

		String workingDirectory =  config.controler().getOutputDirectory();

		String osName = System.getProperty("os.name");

//		for (String actType : activityTypes) {
			String actSpecificWorkingDirectory =  workingDirectory + typeWEQ + "/";

			for ( Modes4Accessibility mode : Modes4Accessibility.values()) {
//				VisualizationUtils.createQGisOutput(typeWEQ, mode, mapViewExtent, workingDirectory, crs, includeDensityLayer);
				VisualizationUtils.createQGisOutput(typeWEQ, mode, envelope, workingDirectory, crs, includeDensityLayer,
						lowerBound, upperBound, range, symbolSize, populationThreshold);
				VisualizationUtils.createSnapshot(actSpecificWorkingDirectory, mode, osName);
			}
//		}
	}
}
