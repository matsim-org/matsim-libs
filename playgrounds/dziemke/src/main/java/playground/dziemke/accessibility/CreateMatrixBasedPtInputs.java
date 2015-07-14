package playground.dziemke.accessibility;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.gis.GridUtils;
import org.matsim.contrib.matrixbasedptrouter.MatrixBasedPtRouterConfigGroup;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author dziemke
 */
public class CreateMatrixBasedPtInputs {
	private static final Logger log = Logger.getLogger(CreateMatrixBasedPtInputs.class);

	public static void main(String[] args) {
		
		// Input, output, and parameters
		
		// ... for use on cluster
//		if (args.length < 8 || args.length > 8) {
//			throw new RuntimeException("Four argument must be passed.");
//		}
//
//		String networkFile = args[0];
//		String transitScheduleFile = args[1];
//		String outputFileRoot = args[2];
//		Boolean measuringPointsAsPTStops = Boolean.parseBoolean(args[3]);
//		Double cellSize = Double.parseDouble(args[4]);
//		Double departureTime = Double.parseDouble(args[5]);
//		Integer numberOfThreads = Integer.parseInt(args[6]);
//		String bounds = args[7];
		
		// ... for local use
		String networkFile = "../../shared-svn/projects/bvg_3_bln_inputdata/rev554B-bvg00-0.1sample/network/network.final.xml.gz";
//		String networkFile = "../../runs-svn/nmbm_minibuses/nmbm/output/jtlu14i/jtlu14i.output_network.xml.gz";
		String transitScheduleFile = "../../shared-svn/projects/bvg_3_bln_inputdata/rev554B-bvg00-0.1sample/network/transitSchedule.xml.gz";
//		String transitScheduleFile = "../../runs-svn/nmbm_minibuses/nmbm/output/jtlu14i/ITERS/it.300/jtlu14i.300.transitScheduleScored.xml.gz";
		String outputFileRoot = "../../data/accessibility/be_002/";
		Boolean measuringPointsAsPTStops = false;
		Double cellSize = 1000.;
		Double departureTime = 8. * 60 * 60;
		Integer numberOfThreads = 1;
		String bounds = "4550000,5790000,4630000,5850000";

		log.info("networkFile = " + networkFile + " -- transitScheduleFile = " + transitScheduleFile + " -- outputFileRoot = " + outputFileRoot
				+ " -- measuringPointsAsPTStops = " + measuringPointsAsPTStops + " -- cellSize = " + cellSize + " -- departureTime = "
				+ departureTime + " -- numberOfThreads = " + numberOfThreads);


		// Infrastructure
		Config config = ConfigUtils.createConfig(new AccessibilityConfigGroup(), new MatrixBasedPtRouterConfigGroup());
		config.network().setInputFile(networkFile);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.getConfig().transit().setUseTransit(true);


		// Read in public transport schedule
		TransitScheduleReader reader = new TransitScheduleReader(scenario);
		reader.readFile(transitScheduleFile);


		// A LinkedHashMap is a hash table and linked list implementation of the Map interface, with predictable iteration order.
		// It differs from HashMap in that it maintains a doubly-linked list running through all of its entries. This linked list
		// defines the iteration ordering, which is normally the order in which keys were inserted into the map insertion order.
		Map<Id<Coord>, Coord> locationFacilitiesMap = new LinkedHashMap<Id<Coord>, Coord>();
		if (measuringPointsAsPTStops == true) {
			log.info("measuringPointsAsPTStops = " + measuringPointsAsPTStops);
			
			BoundingBox boundingBox;
			if (bounds == "network") {
				boundingBox = BoundingBox.createBoundingBox(scenario.getNetwork());
			} else {
				String boundsArray[] = bounds.split(",");				
				boundingBox = BoundingBox.createBoundingBox(Double.parseDouble(boundsArray[0]),
						Double.parseDouble(boundsArray[1]), Double.parseDouble(boundsArray[2]), Double.parseDouble(boundsArray[3]));
			}
			ActivityFacilitiesImpl measuringPoints;
			measuringPoints = GridUtils.createGridLayerByGridSizeByBoundingBoxV2(
					boundingBox.getXMin(), boundingBox.getYMin(), boundingBox.getXMax(), boundingBox.getYMax(), cellSize);

			// Conversion from ActivityFacilities=measuringPoints to coordinates
			for (ActivityFacility activityFacility : measuringPoints.getFacilities().values()) {
				Id<Coord> id = Id.create(activityFacility.getId(), Coord.class);
				Coord coord = activityFacility.getCoord();
				locationFacilitiesMap.put(id, coord);
			}

			// Create pt stops file
			MatrixBasesPtInputUtils.createStopsFileBasedOnMeasuringPoints(scenario,	measuringPoints, outputFileRoot + "measuringPointsAsStops.csv", ",");
			
		} else { // i.e. measuringPointsAsPTStops == false
			log.info("measuringPointsAsPTStops = " + measuringPointsAsPTStops);
			
			// Conversion from TranstiFacilities to coordinates
			for (TransitStopFacility transitStopFacility : scenario.getTransitSchedule().getFacilities().values()) {		
				Id<Coord> id = Id.create(transitStopFacility.getId(), Coord.class);
				Coord coord = transitStopFacility.getCoord();
				locationFacilitiesMap.put(id, coord);
			}

			// Create pt stops file
			MatrixBasesPtInputUtils.createStopsFileBasedOnSchedule(scenario, outputFileRoot + "ptStops.csv", ",");
		}


		// Split up map of locations into (approximately) equal parts
		int numberOfMeasuringPoints = locationFacilitiesMap.size();
		log.info("numberOfMeasuringPoints = " + numberOfMeasuringPoints);
		int arrayNumber = 0;
		int locationsAdded = 0;

		Map<Integer, Map<Id<Coord>, Coord>> mapOfLocationFacilitiesMaps = new HashMap<Integer, Map<Id<Coord>, Coord>>();
		for (int i = 0; i < numberOfThreads; i++) {
			Map<Id<Coord>, Coord> locationFacilitiesPartialMap = new LinkedHashMap<Id<Coord>, Coord>();
			mapOfLocationFacilitiesMaps.put(i, locationFacilitiesPartialMap);
		}


		// Now iterate over all locations and copy them into separate maps. Once the number of copied locations reaches a certain
		// share of all measuringPoints, they are copied to the next map. This separation is necessary to be able to do the travel
		// matrix computation multi-threaded.
		for (Id<Coord> locationFacilityId : locationFacilitiesMap.keySet()) {
			mapOfLocationFacilitiesMaps.get(arrayNumber).put(locationFacilityId, locationFacilitiesMap.get(locationFacilityId));
			locationsAdded++;
			if (locationsAdded == (numberOfMeasuringPoints / numberOfThreads) + 1) {
				arrayNumber++;
				locationsAdded = 0;
			}
		}


		for (int currentThreadNumber = 0; currentThreadNumber < numberOfThreads ; currentThreadNumber++) {
			new ThreadedMatrixCreator(scenario, mapOfLocationFacilitiesMaps.get(currentThreadNumber), 
					locationFacilitiesMap, departureTime, outputFileRoot, "\t", currentThreadNumber);
		}
	}
}