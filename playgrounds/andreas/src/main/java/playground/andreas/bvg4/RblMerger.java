package playground.andreas.bvg4;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleImpl;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.andreas.utils.pt.TransitScheduleCleaner;

public class RblMerger {
	
	private static final Logger log = Logger.getLogger(RblMerger.class);

	private ScenarioImpl scenario;

	public static void main(String[] args) {
		String networkFile ="D:/berlin_bvg3/bvg_3_bln_inputdata/rev554B-bvg00-0.1sample/network/network.final.xml.gz";
		String transitScheduleInFile = "D:/berlin_bvg3/bvg_3_bln_inputdata/rev554B-bvg00-0.1sample/network/transitSchedule.xml.gz";
		String newTransitScheduleOutFilename = "F:/bvg4/output/transitSchedule.xml";
		String rblStopsDumpFile = "F:/bvg4/output/rblStopsDumpFile.txt";
		String filteredTransitScheduleDumpFile = "F:/bvg4/output/filteredTransitScheduleDumpFile.txt";
		
		String stopNamesMapFilename = "F:/bvg4/input/stopNameMap.txt";
		
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
				
		
		RblMerger rblMerger = new RblMerger();		
		rblMerger.init(networkFile, transitScheduleInFile);
		
		List<FahrtEvent> fahrtEvents = rblMerger.readFahrtData(fahrtDataFilenames);
		List<FahrzeitEvent> fahrzeitEvents = rblMerger.readFahrzeitData(fahrzeitDataFilenames);
		
		rblMerger.addFahrtInfoToFahrzeitEvents(fahrtEvents, fahrzeitEvents);
		
		rblMerger.dumpRblStops(rblStopsDumpFile, fahrzeitEvents);
		rblMerger.dumpFilteredTransitSchedule(filteredTransitScheduleDumpFile, fahrtEvents);
		
		
		rblMerger.substituteStopNames(stopNamesMapFilename, fahrzeitEvents);
		
		
//		Map<Integer, Map<Integer, Map<Double, FahrzeitEvent>>> fahrzeitMap = rblMerger.createMap(fahrzeitEvents);
//		Map<Id, Map<Id, StopStatsContainer>> kurs2stop2StatsMap = rblMerger.accumulateData(fahrzeitMap);
		
		Map<Id, Map<Id, Map<Id, StopStatsContainer>>> line2route2stop2StatsMap = rblMerger.accumulateData(fahrzeitEvents);
		
		TransitSchedule newTransitSchedule = new TransitScheduleFactoryImpl().createTransitSchedule();
		rblMerger.addStopsToSchedule(newTransitSchedule, line2route2stop2StatsMap);
		rblMerger.createLinesAndRoutes(newTransitSchedule, line2route2stop2StatsMap);
		HashMap<Id, HashMap<Id, List<TransitRouteStop>>> line2route2TransitRouteStopsList = rblMerger.createTransitRouteStops(newTransitSchedule, line2route2stop2StatsMap, fahrzeitEvents);
		
		rblMerger.writeTransitSchedule(newTransitSchedule, newTransitScheduleOutFilename);
		
//		rblMerger.createSchedule(networkFile, transitScheduleInFile, fahrzeitMap);
		
		
	}

	private void init(String networkFile, String transitScheduleInFile) {
		Config config = ConfigUtils.createConfig();
		config.scenario().setUseTransit(true);
		config.network().setInputFile(networkFile);
		config.transit().setTransitScheduleFile(transitScheduleInFile);
		this.scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);;
	}

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

	private void addFahrtInfoToFahrzeitEvents(List<FahrtEvent> fahrtEvents, List<FahrzeitEvent> fahrzeitEvents) {
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
			} else {
				fahrzeitEvent.add(fahrtEvent);
			}
		}
		
		log.info(numberOfMissingFahrtEvents + " fahrzeit events could not be linked to a fahrt event");
	}

	private void substituteStopNames(String stopNamesMapFilename, List<FahrzeitEvent> fahrzeitEvents) {
		Map<String, String> old2newStopNamesMap = ReadStopNameMap.readStopNameMap(stopNamesMapFilename);
		int stopsNotFound = 0;
		
		for (FahrzeitEvent fahrzeitEvent : fahrzeitEvents) {
			String oldStopName = fahrzeitEvent.getStopId().toString();
			String newStopName = old2newStopNamesMap.get(oldStopName);
			
			if (newStopName == null) {
				stopsNotFound++;
			} else {
				fahrzeitEvent.setNewStopId(newStopName);
			}
		}
		
		if(stopsNotFound == 0){
			log.info("Could substitute all old stop names by new ones");
		} else {
			log.warn("Could not substitute " + stopsNotFound + " old stop names by new ones");
		}		
	}

	/**
	 * Accumulate arrival and departure offsets for all lines 2 routes 2 stops
	 * 
	 * @param fahrzeitEvents
	 * @return
	 */
	private Map<Id, Map<Id, Map<Id, StopStatsContainer>>> accumulateData(List<FahrzeitEvent> fahrzeitEvents) {
		Map<Id, Map<Id, Map<Id, StopStatsContainer>>> line2route2stop2StatsMap = new HashMap<Id, Map<Id,Map<Id,StopStatsContainer>>>();
		int notSameDay = 0;
		int fahrzeitEventsDropped = 0;
		
		for (FahrzeitEvent event : fahrzeitEvents) {
			if (event.getFahrtEvent() == null) {
				fahrzeitEventsDropped++;
				continue;
			}
			
			Id lineId = event.getFahrtEvent().getLineId();
			Id routeId = event.getFahrtEvent().getRouteId();
			Id stopId = event.getStopId();
			
			if (line2route2stop2StatsMap.get(lineId) == null) {
				line2route2stop2StatsMap.put(lineId, new HashMap<Id, Map<Id,StopStatsContainer>>());
			}
			
			if (line2route2stop2StatsMap.get(lineId).get(routeId) == null) {
				line2route2stop2StatsMap.get(lineId).put(routeId, new HashMap<Id, StopStatsContainer>());
			}
			
			if (line2route2stop2StatsMap.get(lineId).get(routeId).get(stopId) == null) {
				line2route2stop2StatsMap.get(lineId).get(routeId).put(stopId, new StopStatsContainer(stopId));
			}
			
			double arrivalOffset = event.getArrivalTimeIstAtStop() - event.getDepartureTimeIst();
			double departureOffset = event.getDepartureTimeIstAtStop() - event.getDepartureTimeIst();
			
			if(event.getArrivalDateIstAtStop().equalsIgnoreCase(event.getDepartureDateIst())){
				// same day - proceed
				line2route2stop2StatsMap.get(lineId).get(routeId).get(event.getStopId()).addArrival(arrivalOffset);
				line2route2stop2StatsMap.get(lineId).get(routeId).get(event.getStopId()).addDeparture(departureOffset);
			} else {
				notSameDay++;
				// TODO Should consider +/- 86400 seconds
			}		
			
		}
		
		log.info("Finished accumulating arrival and departure times...");
		log.info("Dropped " + fahrzeitEventsDropped + " entries, since they are not linked to a fahrt");
		log.info("Dropped another " + notSameDay + " entries, since their departure at terminus and arrival/departure at stop are on different days");
		return line2route2stop2StatsMap;	
	}
	
	private void addStopsToSchedule(TransitSchedule newTransitSchedule,	Map<Id, Map<Id, Map<Id, StopStatsContainer>>> line2route2stop2StatsMap) {
		
		int stopFacilitiesNotFound = 0;
		
		for (Map<Id, Map<Id, StopStatsContainer>> route2stop2StatsMap : line2route2stop2StatsMap.values()) {
			
			for (Map<Id, StopStatsContainer> stop2StatsMap : route2stop2StatsMap.values()) {

				for (Id stopId : stop2StatsMap.keySet()) {
					// get the transit schedule from the reference file and add it to the new transit schedule file
					TransitStopFacility transitStopFacility = scenario.getTransitSchedule().getFacilities().get(stopId);
					if(transitStopFacility == null){
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
		
		log.info(stopFacilitiesNotFound + " transit stop facilities could not be added.");
	}

	private void createLinesAndRoutes(TransitSchedule newTransitSchedule, Map<Id, Map<Id, Map<Id, StopStatsContainer>>> line2route2stop2StatsMap) {
		
		for (Entry<Id, Map<Id, Map<Id, StopStatsContainer>>> line2route2stop2StatsMapEntry : line2route2stop2StatsMap.entrySet()) {
			Id lineId = line2route2stop2StatsMapEntry.getKey();
			if (newTransitSchedule.getTransitLines().get(lineId) == null) {
				// new line - create one
				newTransitSchedule.addTransitLine(newTransitSchedule.getFactory().createTransitLine(lineId));
			}
			
			for (Entry<Id, Map<Id, StopStatsContainer>> route2stop2StatsMapEntry : line2route2stop2StatsMapEntry.getValue().entrySet()) {
				Id routeId = route2stop2StatsMapEntry.getKey();
				if (newTransitSchedule.getTransitLines().get(lineId).getRoutes().get(routeId) == null) {
					// new route - create one
//					newTransitSchedule.getTransitLines().get(lineId).addRoute(newTransitSchedule.getFactory().createTransitRoute(routeId, route, stops, mode));
				}
			}
		}
	}

	private HashMap<Id,HashMap<Id,List<TransitRouteStop>>> createTransitRouteStops(TransitSchedule newTransitSchedule, Map<Id, Map<Id, Map<Id, StopStatsContainer>>> line2route2stop2StatsMap,	List<FahrzeitEvent> fahrzeitEvents) {
		HashMap<Id, HashMap<Id, List<TransitRouteStop>>> line2route2TransitRouteStopsList = new HashMap<Id, HashMap<Id, List<TransitRouteStop>>>();
		
		Id lastLineId = null;
		Id lastRouteId = null;
		List<TransitRouteStop> lastTransitRouteStops = null;
		
		// for each event
		for (FahrzeitEvent fahrzeitEvent : fahrzeitEvents) {
			
			// check, if we have a complete fahrzeit event
			if (fahrzeitEvent.getFahrtEvent() == null) {
				// incomplete - skip
				continue;
			}			
			
			// check, if we need to process this event
			if(line2route2TransitRouteStopsList.get(fahrzeitEvent.getFahrtEvent().getLineId()) != null){
				if(line2route2TransitRouteStopsList.get(fahrzeitEvent.getFahrtEvent().getLineId()).get(fahrzeitEvent.getFahrtEvent().getRouteId()) != null){
					// this route of this line was porcessed - skip this event
					continue;
				}
			}
			
			// we need to process this event			
			
			if(fahrzeitEvent.getFahrtEvent().getLineId() != lastLineId || fahrzeitEvent.getFahrtEvent().getRouteId() != lastRouteId){
				// different line or route - save the old one first
				
				if(line2route2TransitRouteStopsList.get(lastLineId) == null){
					line2route2TransitRouteStopsList.put(lastLineId, new HashMap<Id, List<TransitRouteStop>>());
				}
				
				if(line2route2TransitRouteStopsList.get(lastLineId).get(lastRouteId) == null){
					line2route2TransitRouteStopsList.get(lastLineId).put(lastRouteId, lastTransitRouteStops);
				}
				
				lastTransitRouteStops = new LinkedList<TransitRouteStop>();
				lastLineId = fahrzeitEvent.getFahrtEvent().getLineId();
				lastRouteId = fahrzeitEvent.getFahrtEvent().getRouteId();
			}				
			
			// same line and route - ok add the current stop to the route
			TransitStopFacility stopFacility = newTransitSchedule.getFacilities().get(fahrzeitEvent.getStopId());
			StopStatsContainer stopStats = line2route2stop2StatsMap.get(lastLineId).get(lastRouteId).get(fahrzeitEvent.getStopId());
			double arrivalDelay = stopStats.getArithmeticMeanArrival();
			double departureDelay = stopStats.getArithmeticMeanDeparture();
			lastTransitRouteStops.add(newTransitSchedule.getFactory().createTransitRouteStop(stopFacility, arrivalDelay, departureDelay));
		}
				
		log.info("Finished creating transit route stops... - WARNING: If the very last fahrzeit event belongs to a new line and route, it is not processed. In reality, this should not happen.");
		return line2route2TransitRouteStopsList;
	}

	private void createSchedule(String networkFile, Map<Integer, Map<Integer, Map<Double, FahrzeitEvent>>> fahrzeitMap) {
		TransitSchedule referenceTransitSchedule = this.scenario.getTransitSchedule();
		
		TransitSchedule newTransitSchedule = new TransitScheduleFactoryImpl().createTransitSchedule();
		

		// for each kurs
		for (Entry<Integer, Map<Integer, Map<Double, FahrzeitEvent>>> kursMapEntry : fahrzeitMap.entrySet()) {
			
			Id lineId = new IdImpl(String.valueOf(kursMapEntry.getKey()).substring(0, 3));
			// check if line already exists
			if (newTransitSchedule.getTransitLines().get(lineId) == null) {
				newTransitSchedule.addTransitLine(newTransitSchedule.getFactory().createTransitLine(lineId));
			}
			
			// for each rblDate
			for (Entry<Integer, Map<Double, FahrzeitEvent>> rblDateMapEntry : kursMapEntry.getValue().entrySet()) {
				
				Integer rblDate = rblDateMapEntry.getKey();
				
				// for each departure time ist
				for (FahrzeitEvent event : rblDateMapEntry.getValue().values()) {
					
					// get the transit schedule from the reference file and add it to the new transit schedule file
					TransitStopFacility stopFacility = referenceTransitSchedule.getFacilities().get(event.getStopId());
					if(stopFacility == null){
						log.error("Stop facility " + event.getStopId() + " - " + event.getStopName() + " not found");
					} else {
						if(!newTransitSchedule.getFacilities().containsKey(stopFacility.getId())){
							newTransitSchedule.addStopFacility(stopFacility);
						}						
					}
					
					TransitLine line = newTransitSchedule.getTransitLines().get(lineId);
					
					// check if kurs already exists
					if(line.getRoutes().get(new IdImpl(event.getKurs())) == null){
//						line.addRoute(newTransitSchedule.getFactory().createTransitRoute(new IdImpl(event.getKurs()), route, stops, mode))
					}
					
					
					
					
					
					
					
					
					
					
					
					
					
					
				}
			}
			
		}
		
		log.info("Finished");
		
	}

	private void writeTransitSchedule(TransitSchedule newTransitSchedule, String newTransitScheduleOutFilename) {
		new TransitScheduleWriterV1(newTransitSchedule).write(newTransitScheduleOutFilename);
		log.info("Transit schedule written to " + newTransitScheduleOutFilename);		
	}

	/**
	 * Dump transit schedule to file ignoring all lines not mentioned by the given rbl data
	 *  
	 * @param filteredTransitScheduleDumpFile
	 * @param fahrtEvents
	 */
	private void dumpFilteredTransitSchedule(String filteredTransitScheduleDumpFile, List<FahrtEvent> fahrtEvents) {
		TransitSchedule filteredTransitSchedule = new TransitScheduleFactoryImpl().createTransitSchedule();
		
		Set<Id> lineIds = new TreeSet<Id>();
		for (FahrtEvent event : fahrtEvents) {
			lineIds.add(event.getLineId());
		}
		log.info("Found " + lineIds.size() + " lines to filter");
		
		// copy lines
		for (Entry<Id, TransitLine> lineEntry : scenario.getTransitSchedule().getTransitLines().entrySet()) {
			for (Id lineId : lineIds) {
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

//	private Map<Id, Map<Id, StopStatsContainer>> accumulateData(Map<Integer, Map<Integer, Map<Double, FahrzeitEvent>>> fahrzeitMap) {
//		
//		Map<Id, Map<Id, StopStatsContainer>> kurs2stop2StatsMap = new HashMap<Id, Map<Id,StopStatsContainer>>();
//		int notSameDay = 0;
//		
//		// for each kurs
//		for (Entry<Integer, Map<Integer, Map<Double, FahrzeitEvent>>> kursMapEntry : fahrzeitMap.entrySet()) {
//			
//			Id kursId = new IdImpl(kursMapEntry.getKey());
//			
//			// check if entry exists
//			if (kurs2stop2StatsMap.get(kursId) == null) {
//				kurs2stop2StatsMap.put(kursId, new HashMap<Id, StopStatsContainer>());
//			}
//						
//			// for each rblDate
//			for (Entry<Integer, Map<Double, FahrzeitEvent>> rblDateMapEntry : kursMapEntry.getValue().entrySet()) {
//				
//				Integer rblDate = rblDateMapEntry.getKey();
//				
//				// for each departure time ist
//				for (FahrzeitEvent event : rblDateMapEntry.getValue().values()) {
//					Map<Id, StopStatsContainer> kurs = kurs2stop2StatsMap.get(kursId);
//					if(kurs.get(event.getStopId()) == null){
//						kurs.put(event.getStopId(), new StopStatsContainer(event.getStopId()));
//					}
//					
//					double arrivalOffset = event.getArrivalTimeIstAtStop() - event.getDepartureTimeIst();
//					double departureOffset = event.getDepartureTimeIstAtStop() - event.getDepartureTimeIst();
//					
//					if(event.getArrivalDateIstAtStop().equalsIgnoreCase(event.getDepartureDateIst())){
//						// same day - proceed
//						kurs.get(event.getStopId()).addArrival(arrivalOffset);
//						kurs.get(event.getStopId()).addDeparture(departureOffset);
//					} else {
//						notSameDay++;
//						// TODO Should consider +/- 86400 seconds
//					}					
//				}
//			}
//		}
//		
//		log.info("Finished accumulating arrival and departure times...");
//		log.info("Dropped " + notSameDay + " entries, since their departure at terminus and arrival/departure at stop are on different days");
//		return kurs2stop2StatsMap;
//	}
//
//	private Map<Id, List<TransitRouteStop>> createTransitRouteStops(Map<Id, Map<Id, Map<Id, StopStatsContainer>>> line2route2stop2StatsMap) {
//		Map<Id, List<TransitRouteStop>> kurs2TransitRouteStopsMap = new HashMap<Id, List<TransitRouteStop>>();
//		int notSameDay = 0;
//		
//		Id currentKursId = null;
//		List<TransitRouteStop> currentTransitRouteStops = null;
//		
//		// for each event
//		for (FahrzeitEvent fahrzeitEvent : fahrzeitEvents) {
//			
//			if(currentKursId.equals(new IdImpl(fahrzeitEvent.getKurs()))){
//				// still on same kurs
//			}
//			
//			if (kurs2TransitRouteStopsMap.get(new IdImpl(fahrzeitEvent.getKurs())) == null) {
//				
//			} else {
//				// kurs already processed - skip it
//				
//			}
//		}
//				
//		log.info("Finished creating transit route stops...");
//		log.info("Dropped " + notSameDay + " entries, since their departure at terminus and arrival/departure at stop are on different days");
//		return kurs2TransitRouteStopsMap;
//	}

//	/**
//	 * Sort every event into the map LSK2rblDate2departureIST2event
//	 * @param fahrzeitEvents 
//	 */
//	private HashMap<Integer,Map<Integer,Map<Double,FahrzeitEvent>>> createMap(List<FahrzeitEvent> fahrzeitEvents) {
//		HashMap<Integer,Map<Integer,Map<Double,FahrzeitEvent>>> rblDate2LskMap  = new HashMap<Integer, Map<Integer, Map<Double, FahrzeitEvent>>>();
//		int cnt = 0;
//	
//		for (FahrzeitEvent event : fahrzeitEvents) {
//			
//			if (rblDate2LskMap.get(event.getKurs()) == null) {
//				rblDate2LskMap.put(event.getKurs(), new HashMap<Integer, Map<Double, FahrzeitEvent>>());
//			}
//			
//			if(rblDate2LskMap.get(event.getKurs()).get(event.getRblDate()) == null){
//				rblDate2LskMap.get(event.getKurs()).put(event.getRblDate(), new HashMap<Double, FahrzeitEvent>());
//			}
//			
//			rblDate2LskMap.get(event.getKurs()).get(event.getRblDate()).put(event.getDepartureTimeIst(), event);
//			cnt++;
//		}
//		
//		log.info("Added " + cnt + " events to map");
//		return rblDate2LskMap;
//	}
	
}
