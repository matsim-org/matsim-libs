package playground.dziemke.accessibility;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.matrixbasedptrouter.MatrixBasedPtRouterConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacilitiesFactoryImpl;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;


/**
 * @author dziemke
 */
public class CreateMatrixBasedPtInputsBasedOnSchedule {

	public static void main(String[] args) {
		
		// Input and output
		if (args.length < 3 || args.length > 3) {
			throw new RuntimeException("Four argument must be passed.");
		}
		
		String networkFile = args[0];
		String transitScheduleFile = args[1];
		String outputFileRoot = args[2];
		
//		String networkFile = "../../matsimExamples/countries/za/nmbm/network/NMBM_Network_CleanV7.xml.gz";
//		String transitScheduleFile = "../../runs-svn/nmbm_minibuses/nmbm/output/jtlu14i/ITERS/it.300/jtlu14i.300.transitScheduleScored.xml.gz";
//		String outputFileRoot = "";
		
		// Parameters
//		double cellSize = 1000.;
		double departureTime = 8. * 60 * 60;
		String separator = " ";
		int numberOfThreads = 10;		
		
		// Infrastructure
		Config config = ConfigUtils.createConfig( new AccessibilityConfigGroup(), new MatrixBasedPtRouterConfigGroup()) ;
		config.network().setInputFile(networkFile);
		
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		scenario.getConfig().scenario().setUseTransit(true);
		
		// Read in public transport schedule
		TransitScheduleReader reader = new TransitScheduleReader(scenario);
		reader.readFile(transitScheduleFile);
		
		Map<Id<TransitStopFacility>, TransitStopFacility> transitStopFacilitiesMap = scenario.getTransitSchedule().getFacilities();
		ActivityFacilitiesFactory activityFacilitiesFactory = new ActivityFacilitiesFactoryImpl();
		
		// Convert TransitStopFacilities into ActivityFacilities
		// TODO the other way around would make more sense actually
		ActivityFacilities measuringPoints = new ActivityFacilitiesImpl();
		for (TransitStopFacility transitStopFacility : transitStopFacilitiesMap.values()) {			
			Id<ActivityFacility> id = Id.create(transitStopFacility.getId(), ActivityFacility.class);
			Coord coord = transitStopFacility.getCoord();
			ActivityFacility activityFacility = activityFacilitiesFactory.createActivityFacility(id, coord);
			measuringPoints.addActivityFacility(activityFacility);
		}
				
		// Split up measuring points into (approximately) equal parts
		// measuringPoints.getFacilities() yields a LinkedHashMap a LinkedHash Map. A LinkedHashMap is a hash table and
		// linked list implementation of the Map interface, with predictable iteration order. It differs from HashMap in
		// that it maintains a doubly-linked list running through all of its entries. This linked list defines the
		// iteration ordering, which is normally the order in which keys were inserted into the map insertion order.

		int numberOfMeasuringPoints = measuringPoints.getFacilities().size();
		System.out.println("numberOfMeasuringPoints = " + numberOfMeasuringPoints);
		int arrayNumber = 0;
		int stopsAdded = 0;
		
		Map<Integer, ActivityFacilities> measuringPointsMap = new TreeMap<Integer, ActivityFacilities>();
		for(int i = 0; i < numberOfThreads; i++) {
			ActivityFacilities currentMeasuringPoints = new ActivityFacilitiesImpl();
			measuringPointsMap.put(i, currentMeasuringPoints);
		}
		
		// Now iterate over all measuringPoints=ActivityFacilities and copy them into separate ActivityFacilitiesImpls.
		// Once the number of copied measuringPoints reaches a certain share of all measuringPoints, they are copied
		// to a different ActivityFacilitiesImpl. This separation is necessary to be able to do the travel matrix
		// computation based on multithreading.
		for (ActivityFacility measuringPoint : measuringPoints.getFacilities().values()) {
			measuringPointsMap.get(arrayNumber).addActivityFacility(measuringPoint);
			stopsAdded++;
			if (stopsAdded == (numberOfMeasuringPoints / numberOfThreads) + 1 ) {
				System.out.println("arrayNumber = " + arrayNumber + " -- stopsAdded = " + stopsAdded);
				arrayNumber++;
				stopsAdded = 0;
			}
		}
		System.out.println("arrayNumber = " + arrayNumber + " -- stopsAdded = " + stopsAdded);

		for (int currentThreadNumber = 0; currentThreadNumber < numberOfThreads ; currentThreadNumber++) {
//			String outputFileTravelMatrix = outputFileRoot + "travelmatrix_" + currentThreadNumber + ".csv";
//			String threadName = "thread_" + currentThreadNumber;

			MatrixBasesPtInputUtils.createTravelMatrixFileBasedOnMeasuringPoints(
					scenario, measuringPointsMap.get(currentThreadNumber), measuringPoints, departureTime,
					outputFileRoot, separator, currentThreadNumber);
		}

		// Create pt stops file
//		String outputFileStops = outputFileRoot + "measuringPointsAsStops.csv";
		separator = ",";
		MatrixBasesPtInputUtils.createStopsFileBasedOnMeasuringPoints(scenario,
				measuringPoints, outputFileRoot, separator);
	}
}