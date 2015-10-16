///* *********************************************************************** *
// * project: org.matsim.*
// * TripComparison.java
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2007 by the members listed in the COPYING,        *
// *                   LICENSE and WARRANTY file.                            *
// * email           : info at matsim dot org                                *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// *   This program is free software; you can redistribute it and/or modify  *
// *   it under the terms of the GNU General Public License as published by  *
// *   the Free Software Foundation; either version 2 of the License, or     *
// *   (at your option) any later version.                                   *
// *   See also COPYING, LICENSE and WARRANTY file                           *
// *                                                                         *
// * *********************************************************************** */
//
//package playground.christoph.evacuation.analysis;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.TreeMap;
//import java.util.Map.Entry;
//
//import javax.xml.parsers.ParserConfigurationException;
//
//import net.opengis.kml._2.DocumentType;
//import net.opengis.kml._2.FolderType;
//import net.opengis.kml._2.KmlType;
//import net.opengis.kml._2.ObjectFactory;
//
//import org.apache.log4j.Logger;
//import org.matsim.api.core.v01.Coord;
//import org.matsim.api.core.v01.Id;
//import org.matsim.api.core.v01.Scenario;
//import org.matsim.api.core.v01.network.Link;
//import org.matsim.api.core.v01.network.Network;
//import org.matsim.core.api.experimental.events.ActivityStartEvent;
//import org.matsim.core.api.experimental.events.AgentArrivalEvent;
//import org.matsim.core.api.experimental.events.AgentDepartureEvent;
//import org.matsim.core.api.experimental.events.EventsManager;
//import org.matsim.core.api.experimental.events.LinkEnterEvent;
//import org.matsim.core.api.experimental.events.LinkLeaveEvent;
//import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
//import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
//import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
//import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
//import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
//import org.matsim.core.api.experimental.facilities.ActivityFacilities;
//import org.matsim.core.api.experimental.facilities.ActivityFacility;
//import org.matsim.core.events.EventsManagerImpl;
//import org.matsim.core.events.EventsReaderXMLv1;
//import org.matsim.core.events.EventsUtils;
//import org.matsim.core.facilities.MatsimFacilitiesReader;
//import org.matsim.core.network.MatsimNetworkReader;
//import org.matsim.core.utils.collections.Tuple;
//import org.matsim.core.utils.geometry.CoordinateTransformation;
//import org.matsim.core.utils.geometry.transformations.CH1903LV03toWGS84;
//import org.matsim.vis.kml.KMZWriter;
//import org.xml.sax.SAXException;
//
//import playground.christoph.evacuation.config.EvacuationConfig;
//
//public class TripComparison {
//
//	private static final Logger log = Logger.getLogger(TripComparison.class);
//	
//	private String basePath = "../../matsim/mysimulations/kt-zurich-evacuation/";
//	
//	private String nashRunPath = "output_iterative_without_ExpBeta_4_Plans/";
//	private String systemRunPath = "output_iterative_v5/";
//	
//	private String networkFile = basePath + nashRunPath + "output_network.xml.gz";
//	private String facilitiesFile = basePath + nashRunPath + "output_facilities.xml.gz";
//	
//	private String nashEventsFile = basePath + nashRunPath + "ITERS/it.200/200.events.xml.gz";
//	private String systemEventsFile = basePath + systemRunPath + "ITERS/it.500/500.events.xml.gz";
//
//	private String kmzFile = basePath + systemRunPath + "network.kml";
//	
//	private double evacuationStartTime = EvacuationConfig.evacuationTime;
//	
//	private Scenario nashScenario;
//	private Scenario systemScenario;
//	private Network network;
//	private ActivityFacilities facilities;
//	
//	/*
//	 * Mapping between Persons and the changes in their TravelTime and TravelDistances.
//	 */
//	private Map<Id, Double> timeWinners;
//	private Map<Id, Double> timeDraws;
//	private Map<Id, Double> timeLosers;
//	private Map<Id, Double> distanceWinners;
//	private Map<Id, Double> distanceDraws;
//	private Map<Id, Double> distanceLosers;
//		
//	public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException {
//		new TripComparison();
//	}
//	
//	public TripComparison() throws SAXException, ParserConfigurationException, IOException {
//		
//		nashScenario = new ScenarioImpl();
//		systemScenario = new ScenarioImpl();
//		
//		// load network
//		new MatsimNetworkReader(nashScenario).readFile(networkFile);
//		new MatsimNetworkReader(systemScenario).readFile(networkFile);
//		
//		network = nashScenario.getNetwork();
//		
//		// load facilities
//		new MatsimFacilitiesReader((ScenarioImpl)nashScenario).readFile(facilitiesFile);
//		new MatsimFacilitiesReader((ScenarioImpl)systemScenario).readFile(facilitiesFile);
//		
//		facilities = ((ScenarioImpl)nashScenario).getActivityFacilities();
//		
//		// parse Events
//		Data nashData = readEventsFile(nashEventsFile);
//		Data systemData = readEventsFile(systemEventsFile);
//		log.info(nashData.travelDistances.size());
//		log.info(nashData.travelTimes.size());
//		log.info(systemData.travelDistances.size());
//		log.info(systemData.travelTimes.size());
//		
//		// compare Results
//		compareResults(nashData, systemData);
//		
//		// write KML Network
//		createNetworkKML(systemData);
//	}
//	
//	private Data readEventsFile(String fileName) throws SAXException, ParserConfigurationException, IOException {
//		Data data = new Data();
//		
//		EventsManager eventsManager = EventsUtils.createEventsManager();
//
//		TravelTimeCollector travelTimeCollector = new TravelTimeCollector(data, network);
//		eventsManager.addHandler(travelTimeCollector);
//		
//		PositionCollector positionCollector = new PositionCollector(data, network, facilities, evacuationStartTime);
//		eventsManager.addHandler(positionCollector);
//		
//		EventsReaderXMLv1 reader = new EventsReaderXMLv1(eventsManager);
//		reader.parse(fileName);
//		
//		return data;
//	}
//	
//	private void compareResults(Data nashData, Data systemData) {
//		
//		timeWinners = new TreeMap<Id, Double>();
//		timeDraws = new TreeMap<Id, Double>();
//		timeLosers = new TreeMap<Id, Double>();
//		distanceWinners = new TreeMap<Id, Double>();
//		distanceDraws = new TreeMap<Id, Double>();
//		distanceLosers = new TreeMap<Id, Double>();
//		
//		for (Id id : nashData.travelTimes.keySet()) {
//			if (systemData.travelTimes.get(id) == null) {
//				log.warn("Agent not in both data sets included: " + id);
//				continue;
//			}
//			
//			double nashTime = nashData.travelTimes.get(id);
//			double systemTime = systemData.travelTimes.get(id);
//			double nashDistance = nashData.travelDistances.get(id);
//			double systemDistance = systemData.travelDistances.get(id);
//			
//			double timeDiff = nashTime - systemTime;
//			if (timeDiff > 0.0) timeLosers.put(id, timeDiff);
//			else if (timeDiff < 0.0) timeWinners.put(id, timeDiff);
//			else timeDraws.put(id, timeDiff);
//			
//			double distanceDiff = nashDistance - systemDistance;
//			if (distanceDiff > 0.0) distanceLosers.put(id, distanceDiff);
//			else if (distanceDiff < 0.0) distanceWinners.put(id, distanceDiff);
//			else distanceDraws.put(id, distanceDiff);
//		}
//		
//		log.info("------------------------------");
//		log.info("compare results");
//		log.info("");
//		log.info("time winners: " + timeWinners.size());
//		log.info("time draws:   " + timeDraws.size());
//		log.info("time losers:  " + timeLosers.size());
//		log.info("");
//		log.info("distance winners: " + distanceWinners.size());
//		log.info("distance draws:   " + distanceDraws.size());
//		log.info("distance losers:  " + distanceLosers.size());
//		
//		log.info("------------------------------");
//	}
//	
//	private void createNetworkKML(Data data) throws IOException {
//		/*
//		 * Mapping between Links/Facilities and the Persons which are located there when the Evacuation starts.
//		 * The Tuple contains the PersonId and the amount of the change (time or distance).
//		 */
//		Map<Id, List<Tuple<Id, Double>>> linkWinners;
//		Map<Id, List<Tuple<Id, Double>>> linkDraws;
//		Map<Id, List<Tuple<Id, Double>>> linkLosers;
//		Map<Id, List<Tuple<Id, Double>>> facilityWinners;
//		Map<Id, List<Tuple<Id, Double>>> facilityDraws;
//		Map<Id, List<Tuple<Id, Double>>> facilityLosers;
//		
//		linkWinners = new HashMap<Id, List<Tuple<Id, Double>>>();
//		linkDraws = new HashMap<Id, List<Tuple<Id, Double>>>();
//		linkLosers = new HashMap<Id, List<Tuple<Id, Double>>>();
//		facilityWinners = new HashMap<Id, List<Tuple<Id, Double>>>();
//		facilityDraws = new HashMap<Id, List<Tuple<Id, Double>>>();
//		facilityLosers = new HashMap<Id, List<Tuple<Id, Double>>>();
//		
//		/*
//		 * Init Maps
//		 */
//		for (Link link : network.getLinks().values()) {
//			linkWinners.put(link.getId(), new ArrayList<Tuple<Id, Double>>());
//			linkDraws.put(link.getId(), new ArrayList<Tuple<Id, Double>>());
//			linkLosers.put(link.getId(), new ArrayList<Tuple<Id, Double>>());
//		}
//		
//		for (ActivityFacility facility : facilities.getFacilities().values()) {
//			facilityWinners.put(facility.getId(), new ArrayList<Tuple<Id, Double>>());
//			facilityDraws.put(facility.getId(), new ArrayList<Tuple<Id, Double>>());
//			facilityLosers.put(facility.getId(), new ArrayList<Tuple<Id, Double>>());
//		}
//		
//		/*
//		 * Fill Maps
//		 */
//		fillMapForKML(data, timeWinners, linkWinners, facilityWinners);
//		fillMapForKML(data, timeDraws, linkDraws, facilityDraws);
//		fillMapForKML(data, timeLosers, linkLosers, facilityLosers);
//
//
//		/*
//		 * Write KML File
//		 */
//		ObjectFactory kmlObjectFactory = new ObjectFactory();
//		KMZWriter kmzWriter = new KMZWriter(kmzFile);
//		
//		KmlType mainKml = kmlObjectFactory.createKmlType();
//		DocumentType mainDoc = kmlObjectFactory.createDocumentType();
//		mainKml.setAbstractFeatureGroup(kmlObjectFactory.createDocument(mainDoc));
//		
//		CoordinateTransformation coordTransform = new CH1903LV03toWGS84();
//		TripComparisonWriter writer = new TripComparisonWriter(coordTransform, kmzWriter, mainDoc);
////		KmlNetworkWriter kmlNetworkWriter = new KmlNetworkWriter(network, new GeotoolsTransformation("CH1903_LV03", "WGS84"), kmzWriter, mainDoc);
//		
//		FolderType linkFolderType = writer.getLinkFolder("Travel Times", network, linkWinners, linkDraws, linkLosers);
//		mainDoc.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(linkFolderType));
//		
//		FolderType facilityFolderType = writer.getFacilityCountFolder("Travel Times", facilities, facilityWinners, facilityDraws, facilityLosers);
//		mainDoc.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(facilityFolderType));
//		
//		kmzWriter.writeMainKml(mainKml);
//		kmzWriter.close();
//
//		
////		fillMapForKML(data, distanceWinners, linkWinners, facilityWinners);
////		fillMapForKML(data, distanceDraws, linkDraws, facilityDraws);
////		fillMapForKML(data, distanceLosers, linkLosers, facilityLosers);
//	}
//	
//	private void fillMapForKML(Data data, Map<Id, Double> inputMap, Map<Id, List<Tuple<Id, Double>>> linkMap, 
//			Map<Id, List<Tuple<Id, Double>>> facilityMap) {
//		for (Entry<Id, Double> entry : inputMap.entrySet()) {
//			Id id = entry.getKey();
//			double value = entry.getValue();
//			
//			Tuple<Id, Double> tuple = new Tuple<Id, Double>(id, value);
//			
//			PositionInfo positionInfo = data.positions.get(id);
//			if (positionInfo.isLink) {
//				linkMap.get(positionInfo.id).add(tuple);
//			} else if (positionInfo.isFacility) {
//				facilityMap.get(positionInfo.id).add(tuple);
//			} else {
//				log.warn("Unexpected position!");
//			}
//		}
//	}
//	
//	private static class Data {
//		Map<Id, Double> travelTimes;
//		Map<Id, Double> travelDistances;
//		Map<Id, PositionInfo> positions;
//	}
//	
//	private static class PositionInfo {
//		Id id = null;
//		boolean isLink = false;
//		boolean isFacility = false;
//		Coord coord = null;
//	}
//	
//	private static class TravelTimeCollector implements AgentDepartureEventHandler, AgentArrivalEventHandler,
//		LinkEnterEventHandler, LinkLeaveEventHandler {
//
//		private Network network;
//		
//		private Map<Id, Double> activeTrips;
//		private Set<Id> justDeparted;
//		private Data data;
//		
//		public TravelTimeCollector(Data data, Network network) {
//			this.data = data;
//			this.network = network;
//			
//			justDeparted = new HashSet<Id>();
//			activeTrips = new HashMap<Id, Double>();
//			data.travelTimes = new HashMap<Id, Double>();
//			data.travelDistances = new HashMap<Id, Double>();
//		}
//		
//		@Override
//		public void handleEvent(AgentDepartureEvent event) {
//			Id personId = event.getDriverId();
//			
//			activeTrips.put(personId, event.getTime());
//			justDeparted.add(personId);
//			
//			if (!data.travelTimes.containsKey(personId)) data.travelTimes.put(personId, 0.0);
//			if (!data.travelDistances.containsKey(personId)) data.travelDistances.put(personId, 0.0);
//		}
//
//		@Override
//		public void handleEvent(AgentArrivalEvent event) {
//			Id personId = event.getDriverId();
//			
//			double travelTime = event.getTime() - activeTrips.remove(personId);
//			double sumTravelTime = data.travelTimes.get(personId) + travelTime;
//			data.travelTimes.put(personId, sumTravelTime);
//
//			/*
//			 * If the Agent has started his Leg on the same Link, we do not
//			 * take the travel distance into account - this has already been done. 
//			 */
//			boolean removed = justDeparted.remove(personId);
//			if (!removed) {
//				Link link = network.getLinks().get(event.getLinkId());
//				double travelDistance = link.getLength();
//				double sumTravelDistance = data.travelDistances.get(personId) + travelDistance;
//				data.travelDistances.put(personId, sumTravelDistance);
//			}
//		}
//
//		@Override
//		public void handleEvent(LinkEnterEvent event) {
//			activeTrips.put(event.getDriverId(), event.getTime());
//		}
//
//		@Override
//		public void handleEvent(LinkLeaveEvent event) {
//			Id personId = event.getDriverId();
//
//			double travelTime = event.getTime() - activeTrips.get(personId);
//			
//			Link link = network.getLinks().get(event.getLinkId());
//			double travelDistance = link.getLength();
//						
//			double sumTravelTime = data.travelTimes.get(personId) + travelTime;
//			data.travelTimes.put(personId, sumTravelTime);
//			
//			/*
//			 * If the Agent has started his Leg on the same Link, we do not
//			 * take the travel distance into account - this has already been done. 
//			 */
//			boolean removed = justDeparted.remove(personId);
//			if (!removed) {
//				double sumTravelDistance = data.travelDistances.get(personId) + travelDistance;
//				data.travelDistances.put(personId, sumTravelDistance);				
//			}
//		}
//		
//		@Override
//		public void reset(int iteration) {
//		}
//	}
//	
//	private static class PositionCollector implements LinkEnterEventHandler, ActivityStartEventHandler, AgentDepartureEventHandler {
//
//		private Network network;
//		private ActivityFacilities facilities;
//		private Data data;
//		private double evacuationStartTime;
//		
//		public PositionCollector(Data data, Network network, ActivityFacilities facilities) {
//			this(data, network, facilities, Double.MAX_VALUE);
//		}
//		
//		public PositionCollector(Data data, Network network, ActivityFacilities facilities, double evacuationStartTime) {
//			this.data = data;
//			this.network = network;
//			this.facilities = facilities;
//			this.evacuationStartTime = evacuationStartTime;
//			
//			data.positions = new HashMap<Id, PositionInfo>();
//		}
//		
//		@Override
//		public void handleEvent(LinkEnterEvent event) {
//			if (event.getTime() > evacuationStartTime) return;
//			
//			Id linkId = event.getLinkId();
//			Link link = network.getLinks().get(linkId);
//			PositionInfo positionInfo = new PositionInfo();
//			positionInfo.id = linkId;
//			positionInfo.isLink = true;
//			positionInfo.coord = link.getCoord();
//			data.positions.put(event.getDriverId(), positionInfo);
//		}
//
//		@Override
//		public void handleEvent(ActivityStartEvent event) {
//			if (event.getTime() > evacuationStartTime) return;
//			
//			Id facilityId = event.getFacilityId();
//			if (facilityId != null) {
//				ActivityFacility facility = facilities.getFacilities().get(facilityId);
//				PositionInfo positionInfo = new PositionInfo();
//				positionInfo.id = facilityId;
//				positionInfo.isFacility = true;
//				positionInfo.coord = facility.getCoord();
//				data.positions.put(event.getDriverId(), positionInfo);
//			} 
//			else {
//				Id linkId = event.getLinkId();
//				Link link = network.getLinks().get(linkId);
//				PositionInfo positionInfo = new PositionInfo();
//				positionInfo.id = linkId;
//				positionInfo.isLink = true;
//				positionInfo.coord = link.getCoord();
//				data.positions.put(event.getDriverId(), positionInfo);
//			}
//		}
//
//		@Override
//		public void handleEvent(AgentDepartureEvent event) {
//			if (event.getTime() > evacuationStartTime) return;
//			
//			Id linkId = event.getLinkId();
//			Link link = network.getLinks().get(linkId);
//			PositionInfo positionInfo = new PositionInfo();
//			positionInfo.id = linkId;
//			positionInfo.isLink = true;
//			positionInfo.coord = link.getCoord();
//			data.positions.put(event.getDriverId(), positionInfo);
//		}
//		
//		@Override
//		public void reset(int iteration) {
//		}
//	}
//}
