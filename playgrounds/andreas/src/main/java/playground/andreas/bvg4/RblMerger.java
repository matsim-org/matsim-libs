package playground.andreas.bvg4;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility.Builder;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.*;
import playground.andreas.utils.pt.TransitScheduleCleaner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public class RblMerger {
	
	private static final Logger log = Logger.getLogger(RblMerger.class);

	private MutableScenario scenario;

	public static void main(String[] args) {
		String networkFilename ="D:/berlin_bvg3/bvg_3_bln_inputdata/rev554B-bvg00-0.1sample/network/network.final.xml.gz";
		String transitScheduleInFilename = "D:/berlin_bvg3/bvg_3_bln_inputdata/rev554B-bvg00-0.1sample/network/transitSchedule.xml.gz";
		
		String stopNamesMapFilename = "F:/bvg4/input/stopNameMap.csv";
		String newStopsFilename = "F:/bvg4/input/newStops.csv";
		
		String statsOutFilename = "F:/bvg4/output/statsOut.txt";
		String transitScheduleOutFilename = "F:/bvg4/output/transitScheduleOut.xml";
		String vehiclesOutFilename = "F:/bvg4/output/vehiclesOut.xml";
		String eventsFromTransitScheduleOutFilename = "F:/bvg4/output/eventsFromTransitScheduleOut.xml";
		
		Set<String> onlyTakeDeparturesFromTheseRblDates = new TreeSet<String>();
		onlyTakeDeparturesFromTheseRblDates.add("4811");		

		double timeBinSize = Double.MAX_VALUE;
		
		String[] fahrtDataFilenames = new String[]{
				"F:/bvg4/input/IST_und_SOLL_03-05-Januar2012/fahrt_ist_187_478847894790csv.lst",
				"F:/bvg4/input/IST_und_SOLL_10-12-Januar2012/fahrt_ist_187_479547964797.csv.lst", 
				"F:/bvg4/input/IST_und_SOLL_17-19-Januar2012/fahrt_ist_187_480248034805csv.lst", 
				"F:/bvg4/input/IST_und_SOLL_24-26-Januar2012/fahrt_ist_187_481148124814csv.lst",
				"F:/bvg4/input/IST_und_SOLL_03-05-Januar2012/fahrt_ist_M41_478847894790csv.lst",
				"F:/bvg4/input/IST_und_SOLL_10-12-Januar2012/fahrt_ist_M41_479547964797.csv.lst", 
				"F:/bvg4/input/IST_und_SOLL_17-19-Januar2012/fahrt_ist_M41_480248034805csv.lst", 
				"F:/bvg4/input/IST_und_SOLL_24-26-Januar2012/fahrt_ist_M41_481148124814csv.lst"};
		
		
		String[] fahrzeitDataFilenames = new String[]{
				"F:/bvg4/input/IST_und_SOLL_03-05-Januar2012/fahrzeit_ist_187_478847894790csv.lst",
				"F:/bvg4/input/IST_und_SOLL_10-12-Januar2012/fahrzeit_ist_187_479547964797.csv.lst", 
				"F:/bvg4/input/IST_und_SOLL_17-19-Januar2012/fahrzeit_ist_187_480248034805csv.lst", 
				"F:/bvg4/input/IST_und_SOLL_24-26-Januar2012/fahrzeit_ist_187_481148124814csv.lst",
				"F:/bvg4/input/IST_und_SOLL_03-05-Januar2012/fahrzeit_ist_M41_478847894790csv.lst",
				"F:/bvg4/input/IST_und_SOLL_10-12-Januar2012/fahrzeit_ist_M41_479547964797.csv.lst", 
				"F:/bvg4/input/IST_und_SOLL_17-19-Januar2012/fahrzeit_ist_M41_480248034805csv.lst", 
				"F:/bvg4/input/IST_und_SOLL_24-26-Januar2012/fahrzeit_ist_M41_481148124814csv.lst"};
		
		String rblStopsDumpFilename = "F:/bvg4/output/rblStopsDumpFile.txt";
		String filteredTransitScheduleDumpFilename = "F:/bvg4/output/filteredTransitScheduleDumpFile.txt";

		// BVG Model 10% sample
//		String scenBase = "D:/balmermi/documents/eclipse/input/BVG/scen0008/";
//		String rawBase = "D:/balmermi/documents/eclipse/input/BVG/stability/raw/delivery_cleaned/";
//		String inBase = "D:/balmermi/documents/eclipse/input/BVG/stability/";
//		String outBase = "D:/balmermi/documents/eclipse/output/BVG/stability/ist/";
//		String networkFilename = scenBase+"network/network.final.xml.gz";
//		String transitScheduleInFilename = scenBase+"network/transitSchedule.xml.gz";
//		String stopNamesMapFilename = inBase+"ist20120404/stopNameMap.csv";
//		String newStopsFilename = inBase+"ist20120404/newStops.csv";
//		String statsOutFilename = outBase+"stats.ist.txt";
//		String transitScheduleOutFilename = outBase+"transitSchedule.ist.xml";
//		String vehiclesOutFilename = outBase+"vehicles.ist.xml";
//		String eventsFromTransitScheduleOutFilename = outBase+"events.transitSchedule.ist.xml";
//		Set<String> onlyTakeDeparturesFromTheseRblDates = new TreeSet<String>();
//		onlyTakeDeparturesFromTheseRblDates.add("4811");
////		double timeBinSize = Double.MAX_VALUE;
//		double timeBinSize = 3600.0;
//		String[] fahrtDataFilenames = new String[]{
//				rawBase+"IST_und_SOLL_03-05-Januar2012/fahrt_ist_187_478847894790csv.lst",
//				rawBase+"IST_und_SOLL_10-12-Januar2012/fahrt_ist_187_479547964797.csv.lst", 
//				rawBase+"IST_und_SOLL_17-19-Januar2012/fahrt_ist_187_480248034805csv.lst", 
//				rawBase+"IST_und_SOLL_24-26-Januar2012/fahrt_ist_187_481148124814csv.lst",
//				rawBase+"IST_und_SOLL_03-05-Januar2012/fahrt_ist_M41_478847894790csv.lst",
//				rawBase+"IST_und_SOLL_10-12-Januar2012/fahrt_ist_M41_479547964797.csv.lst", 
//				rawBase+"IST_und_SOLL_17-19-Januar2012/fahrt_ist_M41_480248034805csv.lst", 
//				rawBase+"IST_und_SOLL_24-26-Januar2012/fahrt_ist_M41_481148124814csv.lst"
//		};
//		String[] fahrzeitDataFilenames = new String[]{
//				rawBase+"IST_und_SOLL_03-05-Januar2012/fahrzeit_ist_187_478847894790csv.lst",
//				rawBase+"IST_und_SOLL_10-12-Januar2012/fahrzeit_ist_187_479547964797.csv.lst", 
//				rawBase+"IST_und_SOLL_17-19-Januar2012/fahrzeit_ist_187_480248034805csv.lst", 
//				rawBase+"IST_und_SOLL_24-26-Januar2012/fahrzeit_ist_187_481148124814csv.lst",
//				rawBase+"IST_und_SOLL_03-05-Januar2012/fahrzeit_ist_M41_478847894790csv.lst",
//				rawBase+"IST_und_SOLL_10-12-Januar2012/fahrzeit_ist_M41_479547964797.csv.lst", 
//				rawBase+"IST_und_SOLL_17-19-Januar2012/fahrzeit_ist_M41_480248034805csv.lst", 
//				rawBase+"IST_und_SOLL_24-26-Januar2012/fahrzeit_ist_M41_481148124814csv.lst"
//		};
//		String rblStopsDumpFilename = outBase+"rblStopsDump.ist.txt";
//		String filteredTransitScheduleDumpFilename = outBase+"filteredTransitScheduleDump.ist.txt";
		// done. (BVG Model 10% sample)
		
		RblMerger rblMerger = new RblMerger();	
		rblMerger.init(networkFilename, transitScheduleInFilename);
		rblMerger.addNewStops(newStopsFilename);
		
		List<FahrtEvent> fahrtEvents = rblMerger.readFahrtData(fahrtDataFilenames);
		List<FahrzeitEvent> fahrzeitEvents = rblMerger.readFahrzeitData(fahrzeitDataFilenames);
		
		fahrzeitEvents = rblMerger.addFahrtInfoToFahrzeitEvents(fahrtEvents, fahrzeitEvents, false);

		// Only for debug information
		rblMerger.dumpRblStops(rblStopsDumpFilename, fahrzeitEvents);
		rblMerger.dumpFilteredTransitSchedule(filteredTransitScheduleDumpFilename, fahrtEvents);
		
		fahrzeitEvents = rblMerger.substituteStopNames(stopNamesMapFilename, fahrzeitEvents, false);
		HashMap<Id<TransitLine>,Map<Id<TransitRoute>,Map<Integer,Map<Id<TransitStopFacility>,StopStatsContainer>>>> line2route2stop2StatsMap = rblMerger.accumulateData(fahrzeitEvents, timeBinSize);
		rblMerger.writeStats(line2route2stop2StatsMap, statsOutFilename);
		
		TransitSchedule newTransitSchedule = new TransitScheduleFactoryImpl().createTransitSchedule();
		rblMerger.addStopsToSchedule(newTransitSchedule, line2route2stop2StatsMap);
		
		HashMap<Id<TransitLine>,HashMap<Id<TransitRoute>,HashMap<Integer,List<TransitRouteStop>>>> line2route2timeBin2TransitRouteStopsList = rblMerger.createTransitRouteStops(newTransitSchedule, line2route2stop2StatsMap, fahrzeitEvents, timeBinSize);
		HashMap<Id<TransitLine>,HashMap<Id<TransitRoute>,HashMap<Integer,NetworkRoute>>> line2route2timeBin2networkRouteMap = rblMerger.createNetworkRoutes(line2route2timeBin2TransitRouteStopsList);
		rblMerger.createLinesAndRoutes(newTransitSchedule, line2route2stop2StatsMap, line2route2timeBin2TransitRouteStopsList, line2route2timeBin2networkRouteMap);
		rblMerger.addDepartures(newTransitSchedule, fahrtEvents, onlyTakeDeparturesFromTheseRblDates, timeBinSize);
		
		newTransitSchedule = rblMerger.cleanUpSchedule(newTransitSchedule);
		rblMerger.writeTransitSchedule(newTransitSchedule, transitScheduleOutFilename, vehiclesOutFilename);		
		rblMerger.createEventsFromTransitSchedule(newTransitSchedule, eventsFromTransitScheduleOutFilename);		
	}

	/**
	 * Read reference files
	 * 
	 * @param networkFile
	 * @param transitScheduleInFile
	 */
	private void init(String networkFile, String transitScheduleInFile) {
		Config config = ConfigUtils.createConfig();
		config.transit().setUseTransit(true);
		config.network().setInputFile(networkFile);
		config.transit().setTransitScheduleFile(transitScheduleInFile);
		this.scenario = (MutableScenario) ScenarioUtils.loadScenario(config);;
	}

	private void addNewStops(String newStopsFilename) {
		List<TransitStopFacility> newStops = ReadNewStops.readNewStopsList(newStopsFilename);
		for (TransitStopFacility transitStopFacility : newStops) {
			scenario.getTransitSchedule().addStopFacility(transitStopFacility);
		}
		log.info("Added " + newStops.size() + " new stops to the original transit schedule");
	}

	/**
	 * Read fahrt events
	 * 
	 * @param fahrtDataFilenames
	 * @return
	 */
	private List<FahrtEvent> readFahrtData(String[] fahrtDataFilenames) {
		List<FahrtEvent> fahrtData = new LinkedList<FahrtEvent>();		
		try {
			for (String filename : fahrtDataFilenames) {
				LinkedList<FahrtEvent> tempData = ReadRBLfahrt.readFahrtEvents(filename);
				fahrtData.addAll(tempData);
			}			
			log.info("Finished reading input data...");
			return fahrtData;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}		
	}

	/**
	 * Read fahrzeit events
	 * 
	 * @param fahrzeitDataFilenames
	 * @return
	 */
	private List<FahrzeitEvent> readFahrzeitData(String[] fahrzeitDataFilenames) {
		List<FahrzeitEvent> fahrzeitData = new LinkedList<FahrzeitEvent>();		
		try {
			for (String filename : fahrzeitDataFilenames) {
				LinkedList<FahrzeitEvent> tempData = ReadRBLfahrzeit.readFahrzeitEvents(filename);
				fahrzeitData.addAll(tempData);
			}
			log.info("Finished reading input data...");
			return fahrzeitData;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Link fahrzeit events to the corresponding fahrt event
	 * 
	 * @param fahrtEvents
	 * @param fahrzeitEvents
	 * @param keepFahrzeitEventsWithoutFahrtEvent If set false, each fahrzeit event without a linked fahrt event is deleted
	 * @return
	 */
	private List<FahrzeitEvent> addFahrtInfoToFahrzeitEvents(List<FahrtEvent> fahrtEvents, List<FahrzeitEvent> fahrzeitEvents, boolean keepFahrzeitEventsWithoutFahrtEvent) {
		List<FahrzeitEvent> fahrzeitEventsOut= new LinkedList<FahrzeitEvent>();
		
		// make fahrt events searchable
		Map<String, FahrtEvent> rblDateKursDateTimeIst2FahrtEventMap = new HashMap<String, FahrtEvent>();
		Set<String> searchStrings = new TreeSet<String>();
		
		for (FahrtEvent fahrtEvent : fahrtEvents) {
			String searchId = String.valueOf(fahrtEvent.getRblDate()) + String.valueOf(fahrtEvent.getKurs()) + fahrtEvent.getDepartureDateIst() + fahrtEvent.getDepartureTimeIst();
			if (searchStrings.contains(searchId)) {
				log.warn("Search string already exists. This should not happen. " + searchId);
			} else {
				searchStrings.add(searchId);
				rblDateKursDateTimeIst2FahrtEventMap.put(searchId, fahrtEvent);
			}			
		}
		
		int numberOfMissingFahrtEvents = 0;
		for (FahrzeitEvent fahrzeitEvent : fahrzeitEvents) {
			String searchId = String.valueOf(fahrzeitEvent.getRblDate()) + String.valueOf(fahrzeitEvent.getKurs()) + fahrzeitEvent.getDepartureDateIst() + fahrzeitEvent.getDepartureTimeIst();
			FahrtEvent fahrtEvent = rblDateKursDateTimeIst2FahrtEventMap.get(searchId);
			if (fahrtEvent == null) {
				numberOfMissingFahrtEvents++;
				if(keepFahrzeitEventsWithoutFahrtEvent){
					fahrzeitEventsOut.add(fahrzeitEvent);
				}
			} else {
				fahrzeitEvent.add(fahrtEvent);
				fahrzeitEventsOut.add(fahrzeitEvent);
			}
		}
		
		log.info(numberOfMissingFahrtEvents + " fahrzeit events could not be linked to a fahrt event");
		log.info("Returning " + fahrzeitEventsOut.size() + " fahrzeitEvents");
		return fahrzeitEventsOut;
	}

	/**
	 * Substitute all rbl stop point ids by the corresponding matsim stop ids.
	 * 
	 * @param stopNamesMapFilename Lookup table
	 * @param fahrzeitEvents
	 * @param keepRoutesWithMissingStops If set false, each fahrzeit event belonging to one route with at least one stop, which could not be substituted, is deleted
	 * @return
	 */
	private List<FahrzeitEvent> substituteStopNames(String stopNamesMapFilename, List<FahrzeitEvent> fahrzeitEvents, boolean keepRoutesWithMissingStops) {
		List<FahrzeitEvent> fahrzeitEventsOut = new LinkedList<FahrzeitEvent>();
		
		Map<String, String> old2newStopNamesMap = ReadStopNameMap.readStopNameMap(stopNamesMapFilename);
		
		Set<Id<TransitStopFacility>> stopsNotFound = new TreeSet<>();
		Map<Id<TransitLine>, Set<Id<TransitRoute>>> incompleteRoutes = new HashMap<>();
		
		for (FahrzeitEvent fahrzeitEvent : fahrzeitEvents) {
			String oldStopName = fahrzeitEvent.getStopId().toString();
			String newStopName = old2newStopNamesMap.get(oldStopName);
			
			if(incompleteRoutes.get(fahrzeitEvent.getFahrtEvent().getLineId()) == null){
				incompleteRoutes.put(fahrzeitEvent.getFahrtEvent().getLineId(), new TreeSet<Id<TransitRoute>>());
			}
			
			if (newStopName == null) {
				stopsNotFound.add(Id.create(oldStopName, TransitStopFacility.class));
				incompleteRoutes.get(fahrzeitEvent.getFahrtEvent().getLineId()).add(fahrzeitEvent.getFahrtEvent().getRouteId());
			} else {
				fahrzeitEvent.setNewStopId(newStopName);
			}
		}
		
		int nIncompleteRoutes = 0;
		for (Set<Id<TransitRoute>> set : incompleteRoutes.values()) {
			nIncompleteRoutes += set.size();
		}
		
		log.info("Tagged " + nIncompleteRoutes + " routes as incomplete...");
		log.info(incompleteRoutes);		
		
		if(stopsNotFound.size() == 0){
			log.info("Could substitute all old stop names by new ones");
		} else {
			log.warn("Could not substitute " + stopsNotFound.size() + " old stop names by new ones");
		}
		
		for (FahrzeitEvent event : fahrzeitEvents) {
			if (keepRoutesWithMissingStops) {
				fahrzeitEventsOut.add(event);
			} else {
				if (!incompleteRoutes.get(event.getFahrtEvent().getLineId()).contains(event.getFahrtEvent().getRouteId())) {
					fahrzeitEventsOut.add(event);
				}
			}
		}		
		
		log.info("Returning " + fahrzeitEventsOut.size() + " fahrzeitEvents");
		return fahrzeitEventsOut;
	}


	/**
	 * Accumulate arrival and departure offsets for all lines 2 routes 2 time bins 2 stops
	 * 
	 * @param fahrzeitEvents
	 * @return
	 */
	private HashMap<Id<TransitLine>,Map<Id<TransitRoute>,Map<Integer,Map<Id<TransitStopFacility>,StopStatsContainer>>>> accumulateData(List<FahrzeitEvent> fahrzeitEvents, double timeBinSize) {
		HashMap<Id<TransitLine>,Map<Id<TransitRoute>,Map<Integer,Map<Id<TransitStopFacility>,StopStatsContainer>>>> line2route2timeBin2stop2StatsMap = new HashMap<>();
		int fahrzeitEventsDropped = 0;
		
		for (FahrzeitEvent event : fahrzeitEvents) {
			if (event.getFahrtEvent() == null) {
				fahrzeitEventsDropped++;
				continue;
			}
			
			Id<TransitLine> lineId = event.getFahrtEvent().getLineId();
			Id<TransitRoute> routeId = event.getFahrtEvent().getRouteId();
			Id<TransitStopFacility> stopId = event.getStopId();
			int firstStopDepartureTimeBin = (int) (event.getDepartureTimeIst() / timeBinSize);
			
			if (line2route2timeBin2stop2StatsMap.get(lineId) == null) {
				line2route2timeBin2stop2StatsMap.put(lineId, new HashMap<Id<TransitRoute>, Map<Integer, Map<Id<TransitStopFacility>,StopStatsContainer>>>());
			}
			
			if (line2route2timeBin2stop2StatsMap.get(lineId).get(routeId) == null) {
				line2route2timeBin2stop2StatsMap.get(lineId).put(routeId, new HashMap<Integer, Map<Id<TransitStopFacility>, StopStatsContainer>>());
			}
			
			if (line2route2timeBin2stop2StatsMap.get(lineId).get(routeId).get(firstStopDepartureTimeBin) == null) {
				line2route2timeBin2stop2StatsMap.get(lineId).get(routeId).put(firstStopDepartureTimeBin, new HashMap<Id<TransitStopFacility>, StopStatsContainer>());
			}
			
			if (line2route2timeBin2stop2StatsMap.get(lineId).get(routeId).get(firstStopDepartureTimeBin).get(stopId) == null) {
				line2route2timeBin2stop2StatsMap.get(lineId).get(routeId).get(firstStopDepartureTimeBin).put(stopId, new StopStatsContainer(stopId));
			}
			
			double arrivalOffset = event.getArrivalTimeIstAtStop() - event.getDepartureTimeIst();
			double departureOffset = event.getDepartureTimeIstAtStop() - event.getDepartureTimeIst();
			
			if (arrivalOffset < 0) {
				// departure at terminus was yesterday
				arrivalOffset += 86400.0;
			}
			
			if (departureOffset < 0) {
				// departure at terminus was yesterday
				departureOffset += 86400.0;
			}
			
			line2route2timeBin2stop2StatsMap.get(lineId).get(routeId).get(firstStopDepartureTimeBin).get(event.getStopId()).addArrival(arrivalOffset);
			line2route2timeBin2stop2StatsMap.get(lineId).get(routeId).get(firstStopDepartureTimeBin).get(event.getStopId()).addDeparture(departureOffset);
		}
		
		log.info("Finished accumulating arrival and departure times...");
		log.info("Dropped " + fahrzeitEventsDropped + " entries, since they are not linked to a fahrt");
		return line2route2timeBin2stop2StatsMap;	
	}
	
	/**
	 * Copies each stop referenced by the map from the transit schedule to the new output transit schedule
	 * 
	 * @param newTransitSchedule
	 * @param line2route2stop2StatsMap
	 * @return
	 */
	private Set<Id<TransitStopFacility>> addStopsToSchedule(TransitSchedule newTransitSchedule,	HashMap<Id<TransitLine>,Map<Id<TransitRoute>,Map<Integer,Map<Id<TransitStopFacility>,StopStatsContainer>>>> line2route2stop2StatsMap) {
		
		Set<Id<TransitStopFacility>> stopIdsNotFound = new TreeSet<>();
		int stopFacilitiesNotFound = 0;
		
		for (Map<Id<TransitRoute>, Map<Integer, Map<Id<TransitStopFacility>, StopStatsContainer>>> route2timeBin2stop2StatsMap : line2route2stop2StatsMap.values()) {
			
			for (Map<Integer, Map<Id<TransitStopFacility>, StopStatsContainer>> timeBin2stop2StatsMap : route2timeBin2stop2StatsMap.values()) {

				for (Map<Id<TransitStopFacility>, StopStatsContainer> stop2StatsMap : timeBin2stop2StatsMap.values()) {

					for (Id<TransitStopFacility> stopId : stop2StatsMap.keySet()) {
						// get the transit schedule from the reference file and add it to the new transit schedule file
						TransitStopFacility transitStopFacility = scenario.getTransitSchedule().getFacilities().get(stopId);
						if(transitStopFacility == null){
							stopIdsNotFound.add(stopId);
							stopFacilitiesNotFound++;
							log.error("Stop facility " + stopId + " not found");
						} else {
							if(!newTransitSchedule.getFacilities().containsKey(transitStopFacility.getId())){
								newTransitSchedule.addStopFacility(transitStopFacility);
							}
						}
					}
				}
			}
		}
		
		log.info(stopFacilitiesNotFound + " occurences of " + stopIdsNotFound.size()+ " different transit stop facilities could not be added.");
		return stopIdsNotFound;
	}

	/**
	 * Creates a list of {@link TransitStopFacility} for each line and route found in the map
	 * 
	 * @param newTransitSchedule
	 * @param line2route2stop2StatsMap
	 * @param fahrzeitEvents
	 * @param timeBinSize 
	 * @return
	 */
	private HashMap<Id<TransitLine>,HashMap<Id<TransitRoute>,HashMap<Integer,List<TransitRouteStop>>>> createTransitRouteStops(TransitSchedule newTransitSchedule, HashMap<Id<TransitLine>, Map<Id<TransitRoute>, Map<Integer, Map<Id<TransitStopFacility>, StopStatsContainer>>>> line2route2stop2StatsMap,	List<FahrzeitEvent> fahrzeitEvents, double timeBinSize) {
		HashMap<Id<TransitLine>,HashMap<Id<TransitRoute>,HashMap<Integer,List<TransitRouteStop>>>> line2route2timeBin2TransitRouteStopsList = new HashMap<>();
		
		Id<TransitLine> lastLineId = null;
		Id<TransitRoute> lastRouteId = null;
		Integer lastTimeBin = null;
		List<TransitRouteStop> lastTransitRouteStops = null;
		
		// for each event
		for (FahrzeitEvent fahrzeitEvent : fahrzeitEvents) {
			
			Integer currentTimeBinOfFirstStop = (int) (fahrzeitEvent.getDepartureTimeIst() / timeBinSize);
			
			// check, if we have a complete fahrzeit event
			if (fahrzeitEvent.getFahrtEvent() == null) {
				// incomplete - skip
				continue;
			}			
			
			// check, if we need to process this event
			if(line2route2timeBin2TransitRouteStopsList.get(fahrzeitEvent.getFahrtEvent().getLineId()) != null){
				if(line2route2timeBin2TransitRouteStopsList.get(fahrzeitEvent.getFahrtEvent().getLineId()).get(fahrzeitEvent.getFahrtEvent().getRouteId()) != null){
					if(line2route2timeBin2TransitRouteStopsList.get(fahrzeitEvent.getFahrtEvent().getLineId()).get(fahrzeitEvent.getFahrtEvent().getRouteId()).get(currentTimeBinOfFirstStop) != null){
						// this route of this line was porcessed - skip this event
						continue;
					}
				}
			}
			
			// we need to process this event			
			
			if(fahrzeitEvent.getFahrtEvent().getLineId() != lastLineId || fahrzeitEvent.getFahrtEvent().getRouteId() != lastRouteId || !currentTimeBinOfFirstStop.equals(lastTimeBin)){
				// different line or route - save the old one first
				
				if (lastLineId != null) {
					if(line2route2timeBin2TransitRouteStopsList.get(lastLineId) == null){
						line2route2timeBin2TransitRouteStopsList.put(lastLineId, new HashMap<Id<TransitRoute>, HashMap<Integer, List<TransitRouteStop>>>());
					}
					
					if(line2route2timeBin2TransitRouteStopsList.get(lastLineId).get(lastRouteId) == null && lastRouteId != null){
						line2route2timeBin2TransitRouteStopsList.get(lastLineId).put(lastRouteId, new HashMap<Integer, List<TransitRouteStop>>());
					}
					
					if(line2route2timeBin2TransitRouteStopsList.get(lastLineId).get(lastRouteId).get(lastTimeBin) == null && lastTimeBin != null){
						line2route2timeBin2TransitRouteStopsList.get(lastLineId).get(lastRouteId).put(lastTimeBin, lastTransitRouteStops);
					}
				}				
				
				lastTransitRouteStops = new LinkedList<TransitRouteStop>();
				lastLineId = fahrzeitEvent.getFahrtEvent().getLineId();
				lastRouteId = fahrzeitEvent.getFahrtEvent().getRouteId();
				lastTimeBin = currentTimeBinOfFirstStop;
			}				
			
			// same line and route - ok add the current stop to the route
			TransitStopFacility stopFacility = newTransitSchedule.getFacilities().get(fahrzeitEvent.getStopId());
			StopStatsContainer stopStats = line2route2stop2StatsMap.get(lastLineId).get(lastRouteId).get(currentTimeBinOfFirstStop).get(fahrzeitEvent.getStopId());
			double arrivalDelay = stopStats.getArithmeticMeanArrival();
			double departureDelay = stopStats.getArithmeticMeanDeparture();
			lastTransitRouteStops.add(newTransitSchedule.getFactory().createTransitRouteStop(stopFacility, arrivalDelay, departureDelay));

			if (lastTransitRouteStops.size() > 1) {
				// last and second last are the same - remove last stop
				if (lastTransitRouteStops.get(lastTransitRouteStops.size() - 1).getStopFacility().getId() == lastTransitRouteStops.get(lastTransitRouteStops.size() - 2).getStopFacility().getId()){
					lastTransitRouteStops.remove(lastTransitRouteStops.size() - 1);
				}
			}
		}
				
		log.info("Finished creating transit route stops...");
		log.warn("WARNING: If the very last fahrzeit event belongs to a new line and route, it is not processed. In reality, this should not happen.");
		return line2route2timeBin2TransitRouteStopsList;
	}

	/**
	 * Creates a {@link NetworkRoute} for each line and route of the list
	 * 
	 * @param line2route2timeBin2TransitRouteStopsList
	 * @return
	 */
	private HashMap<Id<TransitLine>,HashMap<Id<TransitRoute>,HashMap<Integer,NetworkRoute>>> createNetworkRoutes(HashMap<Id<TransitLine>,HashMap<Id<TransitRoute>,HashMap<Integer,List<TransitRouteStop>>>> line2route2timeBin2TransitRouteStopsList){
		HashMap<Id<TransitLine>,HashMap<Id<TransitRoute>,HashMap<Integer,NetworkRoute>>> line2route2timeBin2networkRouteMap = new HashMap<>();

		TravelTimeCalculator travelTimeCalculator = TravelTimeCalculator.create(scenario.getNetwork(), scenario.getConfig().travelTimeCalculator());
		TravelDisutility travelCostCalculator = new Builder( TransportMode.car, scenario.getConfig().planCalcScore() ).createTravelDisutility(travelTimeCalculator.getLinkTravelTimes());
		LeastCostPathCalculator routeAlgo = new DijkstraFactory().createPathCalculator(scenario.getNetwork(), travelCostCalculator, travelTimeCalculator.getLinkTravelTimes());
		
		// for all lines
		for (Entry<Id<TransitLine>, HashMap<Id<TransitRoute>, HashMap<Integer, List<TransitRouteStop>>>> line2route2timeBin2TransitRouteStopsListEntry : line2route2timeBin2TransitRouteStopsList.entrySet()) {
			if(line2route2timeBin2networkRouteMap.get(line2route2timeBin2TransitRouteStopsListEntry.getKey()) == null){
				line2route2timeBin2networkRouteMap.put(line2route2timeBin2TransitRouteStopsListEntry.getKey(), new HashMap<Id<TransitRoute>, HashMap<Integer, NetworkRoute>>());
			}
			
			// for all routes
			for (Entry<Id<TransitRoute>, HashMap<Integer, List<TransitRouteStop>>> route2timeBin2TransitRouteStopsListEntry : line2route2timeBin2TransitRouteStopsListEntry.getValue().entrySet()) {
				
				if(line2route2timeBin2networkRouteMap.get(line2route2timeBin2TransitRouteStopsListEntry.getKey()).get(route2timeBin2TransitRouteStopsListEntry.getKey()) == null){
					line2route2timeBin2networkRouteMap.get(line2route2timeBin2TransitRouteStopsListEntry.getKey()).put(route2timeBin2TransitRouteStopsListEntry.getKey(), new HashMap<Integer, NetworkRoute>());
				}
				
				for (Entry<Integer, List<TransitRouteStop>> timeBin2TransitRouteStopsListEntry : route2timeBin2TransitRouteStopsListEntry.getValue().entrySet()) {
					Id<Link> startLinkId = null;
					Id<Link> lastLinkId = null;
					
					List<Id<Link>> links = new LinkedList<Id<Link>>();
					
					// for each stop
					for (TransitRouteStop stop : timeBin2TransitRouteStopsListEntry.getValue()) {
						if(startLinkId == null){
							startLinkId = stop.getStopFacility().getLinkId();
						}
						
						if(lastLinkId != null){
							links.add(lastLinkId);
							Path path = routeAlgo.calcLeastCostPath(scenario.getNetwork().getLinks().get(lastLinkId).getToNode(), scenario.getNetwork().getLinks().get(stop.getStopFacility().getLinkId()).getFromNode(), 0.0, null, null);
							for (Link link : path.links) {
								links.add(link.getId());
							}
						}
						
						lastLinkId = stop.getStopFacility().getLinkId();
					}
					
					if(links.size() != 0){
						links.remove(0);
					}
					
					LinkNetworkRouteImpl networkRoute = new LinkNetworkRouteImpl(startLinkId, lastLinkId);
					networkRoute.setLinkIds(startLinkId, links, lastLinkId);
					
					line2route2timeBin2networkRouteMap.get(line2route2timeBin2TransitRouteStopsListEntry.getKey()).get(route2timeBin2TransitRouteStopsListEntry.getKey()).put(timeBin2TransitRouteStopsListEntry.getKey(), networkRoute);				
					
				}
				
			}			
		}
		
		int nOfRouteCreated = 0;
		for (HashMap<Id<TransitRoute>, HashMap<Integer, NetworkRoute>> route2timeBin2networkRouteMap : line2route2timeBin2networkRouteMap.values()) {
			for (HashMap<Integer, NetworkRoute> timeBin2networkRouteMap : route2timeBin2networkRouteMap.values()) {
				nOfRouteCreated += timeBin2networkRouteMap.size();
			}
		}
		
		log.info("Finished creating " + nOfRouteCreated + " network routes...");		
		return line2route2timeBin2networkRouteMap;
	}

	/**
	 * Creates a line and route for each one mentioned by line2route2stop2StatsMap
	 * 
	 * @param newTransitSchedule
	 * @param line2route2stop2StatsMap
	 * @param line2route2timeBin2TransitRouteStopsList
	 * @param line2route2timeBin2networkRouteMap
	 */
	private void createLinesAndRoutes(TransitSchedule newTransitSchedule, 
			HashMap<Id<TransitLine>,Map<Id<TransitRoute>,Map<Integer,Map<Id<TransitStopFacility>,StopStatsContainer>>>> line2route2timeBin2stop2StatsMap, 
			HashMap<Id<TransitLine>,HashMap<Id<TransitRoute>,HashMap<Integer,List<TransitRouteStop>>>> line2route2timeBin2TransitRouteStopsList, 
			HashMap<Id<TransitLine>,HashMap<Id<TransitRoute>,HashMap<Integer,NetworkRoute>>> line2route2timeBin2networkRouteMap) {
		
		int routesDropped = 0;
		
		for (Entry<Id<TransitLine>, Map<Id<TransitRoute>, Map<Integer, Map<Id<TransitStopFacility>, StopStatsContainer>>>> line2route2timeBin2stop2StatsMapEntry : line2route2timeBin2stop2StatsMap.entrySet()) {
			Id<TransitLine> lineId = line2route2timeBin2stop2StatsMapEntry.getKey();
			if (newTransitSchedule.getTransitLines().get(lineId) == null) {
				// new line - create one
				newTransitSchedule.addTransitLine(newTransitSchedule.getFactory().createTransitLine(lineId));
			}
			
			for (Entry<Id<TransitRoute>, Map<Integer, Map<Id<TransitStopFacility>, StopStatsContainer>>> route2timeBin2stop2StatsMapEntry : line2route2timeBin2stop2StatsMapEntry.getValue().entrySet()) {
				
				for (Entry<Integer, Map<Id<TransitStopFacility>, StopStatsContainer>> timeBin2stop2StatsMapEntry : route2timeBin2stop2StatsMapEntry.getValue().entrySet()) {
				
					Id<TransitRoute> routeId = route2timeBin2stop2StatsMapEntry.getKey();
					if (newTransitSchedule.getTransitLines().get(lineId).getRoutes().get(routeId) == null) {
						// new route - create one
						NetworkRoute route = line2route2timeBin2networkRouteMap.get(lineId).get(routeId).get(timeBin2stop2StatsMapEntry.getKey());
						List<TransitRouteStop> stops = line2route2timeBin2TransitRouteStopsList.get(lineId).get(routeId).get(timeBin2stop2StatsMapEntry.getKey());
						if (route != null && stops != null) {
							// can happen, if there is only one departure at a one stop route, e.g. from depot to service
							newTransitSchedule.getTransitLines().get(lineId).addRoute(newTransitSchedule.getFactory().createTransitRoute(Id.create(route2timeBin2stop2StatsMapEntry.getKey().toString() + "-" + timeBin2stop2StatsMapEntry.getKey().toString(), TransitRoute.class), route, stops, TransportMode.pt));
						} else {
							routesDropped++;
						}
						
					}
				}
			}
		}
		
		log.info("Dropped " + routesDropped + " routes, due to missing stops and/or route");
	}
	
	/**
	 * Adds departures to each line and route
	 * 
	 * @param newTransitSchedule
	 * @param fahrtEvents
	 * @param rblDates Only departures of rbl dates specified in the set will be added.
	 */
	private void addDepartures(TransitSchedule newTransitSchedule, List<FahrtEvent> fahrtEvents, Set<String> rblDates, double timeBinSize) {
		int nOfDeparture = 0;
		for (FahrtEvent fahrtEvent : fahrtEvents) {
			if(rblDates.contains(String.valueOf(fahrtEvent.getRblDate()))){
				TransitLine line = newTransitSchedule.getTransitLines().get(fahrtEvent.getLineId());
				if (line != null) {
					Id<TransitRoute> routeId = Id.create(fahrtEvent.getRouteId().toString() + "-" + ((int) (fahrtEvent.getDepartureTimeIst() / timeBinSize)), TransitRoute.class);
					TransitRoute route = line.getRoutes().get(routeId);
					if(route != null){
						nOfDeparture++;
						Departure departure = newTransitSchedule.getFactory().createDeparture(Id.create(nOfDeparture, Departure.class), fahrtEvent.getDepartureTimeIst());
						departure.setVehicleId(fahrtEvent.getVehId());
						route.addDeparture(departure);
					}
				}
			}
		}
		log.info("Added " + nOfDeparture + " departures");
	}

	/**
	 * Removes all routes without any departure from the schedule. Removes all lines without any route from the schedule. Removes all stops not referenced by any line from the schedule.  
	 * 
	 * @param newTransitSchedule
	 * @return
	 */
	private TransitSchedule cleanUpSchedule(TransitSchedule newTransitSchedule) {
		newTransitSchedule = TransitScheduleCleaner.removeRoutesWithoutDepartures(newTransitSchedule);
		newTransitSchedule = TransitScheduleCleaner.removeEmptyLines(newTransitSchedule);
		newTransitSchedule = TransitScheduleCleaner.removeStopsNotUsed(newTransitSchedule);
		return newTransitSchedule;
	}

	/**
	 * Writes the transit schedule to file.
	 * 
	 * @param newTransitSchedule
	 * @param newTransitScheduleOutFilename
	 * @param vehiclesOutFilename 
	 */
	private void writeTransitSchedule(TransitSchedule newTransitSchedule, String newTransitScheduleOutFilename, String vehiclesOutFilename) {
		new TransitScheduleWriterV1(newTransitSchedule).write(newTransitScheduleOutFilename);
		log.info("Transit schedule written to " + newTransitScheduleOutFilename);
		
		Set<Id<Vehicle>> vehIds = new TreeSet<>();
		
		for (TransitLine line : newTransitSchedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure departure : route.getDepartures().values()) {
					vehIds.add(departure.getVehicleId());
				}
			}
		}
		
		Vehicles vehicles = VehicleUtils.createVehiclesContainer();
		Id<VehicleType> vehTypeId = Id.create("rbl", VehicleType.class);
		VehicleType vehType = vehicles.getFactory().createVehicleType(vehTypeId);
		VehicleCapacity vehCap = vehicles.getFactory().createVehicleCapacity();
		vehCap.setSeats(1);
		vehCap.setStandingRoom(1);
		vehType.setCapacity(vehCap);
		vehicles.addVehicleType(vehType);
		
		Map<Id<Vehicle>, Vehicle> vehMap = vehicles.getVehicles();
		for (Id<Vehicle> vehId : vehIds) {
			Vehicle veh = vehicles.getFactory().createVehicle(vehId, vehType);
			vehMap.put(vehId, veh);
		}
		
		VehicleWriterV1 writer = new VehicleWriterV1(vehicles);
		writer.writeFile(vehiclesOutFilename);
		log.info("Vehicles written to " + vehiclesOutFilename);		
	}

	/**
	 * Creates events from a given transit schedule. Currently only {@link TransitDriverStartsEvent}, {@link VehicleArrivesAtFacilityEvent} and {@link VehicleDepartsAtFacilityEvent} are supported.
	 * 
	 * @param newTransitSchedule
	 * @param eventsFromTransitScheduleOutFile
	 */
	private void createEventsFromTransitSchedule(TransitSchedule newTransitSchedule, String eventsFromTransitScheduleOutFile) {
		
		Collection<Event> events = new ArrayList<Event>();

		for (Entry<Id<TransitLine>, TransitLine> lineEntry : newTransitSchedule.getTransitLines().entrySet()) {
			for (Entry<Id<TransitRoute>, TransitRoute> routeEntry : lineEntry.getValue().getRoutes().entrySet()) {
				
				for (Departure departure : routeEntry.getValue().getDepartures().values()) {
					
					events.add(new TransitDriverStartsEvent(departure.getDepartureTime(), Id.create(departure.getVehicleId(), Person.class), departure.getVehicleId(), lineEntry.getKey(), routeEntry.getKey(), departure.getId()));
					// not visible from playground
//					events.add(new PersonEntersVehicleEventImpl(departure.getDepartureTime(), driverId, departure.getVehicleId()));
					
//					double departureAtLastStop = Double.NaN;
					for (TransitRouteStop stop : routeEntry.getValue().getStops()) {
						events.add(new VehicleArrivesAtFacilityEvent(departure.getDepartureTime() + stop.getArrivalOffset(), departure.getVehicleId(), stop.getStopFacility().getId(), 0.0));
						events.add(new VehicleDepartsAtFacilityEvent(departure.getDepartureTime() + stop.getDepartureOffset(), departure.getVehicleId(), stop.getStopFacility().getId(), 0.0));
//						departureAtLastStop = departure.getDepartureTime() + stop.getDepartureOffset();
					}
					
					// not visible from playground
//					events.add(new PersonLeavesVehicleEventImpl(departureAtLastStop, driverId, departure.getVehicleId()));					
				}				
			}
		}
		
		EventWriterXML eventWriter = new EventWriterXML(eventsFromTransitScheduleOutFile);
		for (Event event : events) {
			eventWriter.handleEvent(event);
		}
		eventWriter.closeFile();
		log.info(events.size() + " events written to " + eventsFromTransitScheduleOutFile);
	}

	private void writeStats(HashMap<Id<TransitLine>,Map<Id<TransitRoute>,Map<Integer,Map<Id<TransitStopFacility>,StopStatsContainer>>>> line2route2stop2StatsMap, String statsOutFilename) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(statsOutFilename)));
			writer.write("# line id; route id; time bin; stop id; " + StopStatsContainer.toStringHeader); writer.newLine();
			
			for (Entry<Id<TransitLine>, Map<Id<TransitRoute>, Map<Integer, Map<Id<TransitStopFacility>, StopStatsContainer>>>> line2route2timeBin2stop2StatsMapEntry : line2route2stop2StatsMap.entrySet()) {
				Id<TransitLine> lineId = line2route2timeBin2stop2StatsMapEntry.getKey();
				for (Entry<Id<TransitRoute>, Map<Integer, Map<Id<TransitStopFacility>, StopStatsContainer>>> route2timeBin2stop2StatsMapEntry : line2route2timeBin2stop2StatsMapEntry.getValue().entrySet()) {
					Id<TransitRoute> routeId = route2timeBin2stop2StatsMapEntry.getKey();
					for (Entry<Integer, Map<Id<TransitStopFacility>, StopStatsContainer>> timeBin2stop2StatsMapEntry : route2timeBin2stop2StatsMapEntry.getValue().entrySet()) {
						for (Entry<Id<TransitStopFacility>, StopStatsContainer> stop2StatsMapEntry : timeBin2stop2StatsMapEntry.getValue().entrySet()) {
							Id<TransitStopFacility> stopId = stop2StatsMapEntry.getKey();
							writer.write(lineId + "; " + routeId + "; " + timeBin2stop2StatsMapEntry.getKey() + "; " + stopId + "; " + stop2StatsMapEntry.getValue().toString());
							writer.newLine();
						}
					}
				}
			}			
			
			writer.flush();
			writer.close();			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	/**
	 * Dump transit schedule to file ignoring all lines not mentioned by the given rbl data
	 *  
	 * @param filteredTransitScheduleDumpFile
	 * @param fahrtEvents
	 */
	private void dumpFilteredTransitSchedule(String filteredTransitScheduleDumpFile, List<FahrtEvent> fahrtEvents) {
		TransitSchedule filteredTransitSchedule = new TransitScheduleFactoryImpl().createTransitSchedule();
		
		Set<Id<TransitLine>> lineIds = new TreeSet<>();
		for (FahrtEvent event : fahrtEvents) {
			lineIds.add(event.getLineId());
		}
		log.info("Found " + lineIds.size() + " lines to filter");
		
		// copy lines
		for (Entry<Id<TransitLine>, TransitLine> lineEntry : scenario.getTransitSchedule().getTransitLines().entrySet()) {
			for (Id<TransitLine> lineId : lineIds) {
				if (lineEntry.getKey().toString().contains(lineId.toString())) {
					filteredTransitSchedule.addTransitLine(lineEntry.getValue());
				}
			}
		}
		
		// copy all stops
		for (TransitStopFacility stop : scenario.getTransitSchedule().getFacilities().values()) {
			filteredTransitSchedule.addStopFacility(stop);
		}
		
		filteredTransitSchedule = TransitScheduleCleaner.removeStopsNotUsed(filteredTransitSchedule);
		new TransitScheduleWriterV1(filteredTransitSchedule).write(filteredTransitScheduleDumpFile);		
	}

	/**
	 * Dump all stops mentioned by rbl data to file
	 * 
	 * @param rblStopsDumpFile
	 * @param fahrzeitEvents
	 */
	private void dumpRblStops(String rblStopsDumpFile, List<FahrzeitEvent> fahrzeitEvents) {
		Set<String> dumpLines = new TreeSet<String>();
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(rblStopsDumpFile)));
			writer.write("# matsim stop id\trbl stop id\trbl short\tline\troute\tLfdNr\trbl name"); writer.newLine();
			for (FahrzeitEvent event : fahrzeitEvents) {
				if (event.getFahrtEvent() == null) {
					continue;
				}
				String line2write = "\t" + event.getStopId() + "\t" + event.getStopNameShort() + "\t" + event.getFahrtEvent().getLineId() + "\t" + event.getFahrtEvent().getRouteId() + "\t" + event.getRunningNumber() + "\t" + event.getStopName();
				if(!dumpLines.contains(line2write)){
					dumpLines.add(line2write);
					writer.write(line2write); writer.newLine();
				}
			}
			writer.flush();
			writer.close();			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}	
}