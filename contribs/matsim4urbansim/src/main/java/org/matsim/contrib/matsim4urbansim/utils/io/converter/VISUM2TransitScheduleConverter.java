/* *********************************************************************** *
 * project: org.matsim.													   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,     *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 * 
 */
package org.matsim.contrib.matsim4urbansim.utils.io.converter;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.matsim4urbansim.utils.io.HeaderParser;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.transitSchedule.TransitStopFacilityImpl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;


/**
 * @author thomas
 *
 */
public class VISUM2TransitScheduleConverter {
	
	// Logger
	private static final Logger log = Logger.getLogger(VISUM2TransitScheduleConverter.class);
	
	private BufferedReader zoneAttributeReader;
	private BufferedReader reiseZeitenReader;
	private BufferedReader startWarteZeitenReader;
	private TransitScheduleWriterV1 writer;
	
//	private Dijkstra dijkstra;
	
//	private NetworkImpl network;
	private Map<Long, Coord> transitStop;
	private Map<Long, ArrayList<VISUMObject>> visumData;
	
	private final int secondsOfDay;
	private final double EMPTY_ENTRY	= 999999.;
	private final String VISUM_END_TAG 	= "* Netzobjektnamen";
	private final String SEPARATOR 		= ";";
	
	private String destinationFile;
	private NumberFormat numberFormat;
	private AverageTime avgWaitingTime;
	
	public VISUM2TransitScheduleConverter(String zoneAttributesFile, String reiseZeitFile, String startWarteZeitFile, String outputFile){
		
//		String networkFile 		  = "/Users/thomas/Development/opus_home/data/zurich_parcel/data/data/network/ivtch-osm.xml";
//		String zoneAttributesFile = "/Users/thomas/Development/opus_home/data/zurich_parcel/data/Matrizen__OeV/Zones_Attributes.csv";
//		String reiseZeitFile      =	"/Users/thomas/Development/opus_home/data/zurich_parcel/data/Matrizen__OeV/OeV_2007_7_8.JRT";
//		String startWarteZeitFile = "/Users/thomas/Development/opus_home/data/zurich_parcel/data/Matrizen__OeV/OeV_2007_7_8.OWTA";
//		destinationFile	  		  = "/Users/thomas/Development/opus_home/data/zurich_parcel/data/transitSchedule.xml";
		
//		Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		new MatsimNetworkReader(scenario).readFile(networkFile);
//		network = (NetworkImpl) scenario.getNetwork();
//
//		FreespeedTravelTimeAndDisutility cost = new FreespeedTravelTimeAndDisutility(-1, 0, 0);
//		dijkstra = new Dijkstra(network, cost, cost);
		
		destinationFile = outputFile;
		
		zoneAttributeReader = IOUtils.getBufferedReader(zoneAttributesFile);
		reiseZeitenReader	= IOUtils.getBufferedReader(reiseZeitFile);
		startWarteZeitenReader=IOUtils.getBufferedReader(startWarteZeitFile);
		
		transitStop 	= new ConcurrentHashMap<Long, Coord>();
		visumData			= new ConcurrentHashMap<Long, ArrayList<VISUMObject>>();
		
		secondsOfDay	 = 24 * 60 * 60;
		numberFormat	= new DecimalFormat("000");
		avgWaitingTime = new AverageTime();
	}
	
	////////////////////////////////
	// Initialization
	////////////////////////////////
	
	public void init(){
		initTransitStops();
		initVISUMData();
		consistencyCheck();
		System.gc();
	}
	
	private void initTransitStops() {
		try {
			// read zone attribute table and store as hashmap
			String line = zoneAttributeReader.readLine();
			// get and initialize the column number of each header element
			Map<String,Integer> idxFromKey = HeaderParser.createIdxFromKey( line, SEPARATOR);
			final int bezirkNummerIDX = idxFromKey.get("$BEZIRK:NR");
			final int xCoordIDX = idxFromKey.get("XKOORD");
			final int yCoordIDX = idxFromKey.get("YKOORD");
			
			// temporary variables
			int lengths = line.split(SEPARATOR).length;
			Coord coord;
			String[] parts;
			
			while ( (line = zoneAttributeReader.readLine()) != null ) {
				
				parts = line.split(SEPARATOR);
				if( parts.length < lengths )
					continue;
				
				Long bezirkNummer = Long.parseLong( parts[ bezirkNummerIDX ] );
				// get the coordinates
				coord = new Coord(Double.parseDouble(parts[xCoordIDX]), Double.parseDouble(parts[yCoordIDX]));
				
				if( ! transitStop.containsKey( bezirkNummer ) )
					transitStop.put(bezirkNummer, coord);
				else
					log.warn("A transit stop with a BEZIRK:NR " + bezirkNummer + " already exists!");
			}
			
			log.info("Done creating transit stop HashMap.");
			
		} catch (IOException e) { 
			e.printStackTrace(); 
		} catch (NumberFormatException nfe) {
			nfe.printStackTrace();
		}
	}

	private void initVISUMData() {
		
		int lineNumber 			 		= 0;
		final int bezikNummerOriginIDX 	= 0;
		final int bezikNummerDestinationIDX = 1;
		final int valueIDX 				= 2;
		String[] parts = null;
		String line  = null;
		
		try {
			log.info("Reading VISUM station 2 station travel times ...");
			boolean skipLine = true;
			Counter reiseZeitenCounter = new Counter();
			while ( (line = reiseZeitenReader.readLine()) != null ) {
				lineNumber ++;
				// skip lines
				if(skipLine){
					if(line.length() == 0)
						skipLine = false;
					continue;
				}
				else if(line.startsWith(VISUM_END_TAG))
					break;
				
				line = line.trim().replaceAll("\\s+", SEPARATOR);
				parts = line.split(SEPARATOR);
				
				Long bezirkNummerOrigin 	= Long.parseLong(parts[bezikNummerOriginIDX]);
				long bezirkNummerDestination= Long.parseLong(parts[bezikNummerDestinationIDX]);
				double reiseZeit  			= Double.parseDouble(parts[valueIDX]);
				
				if(reiseZeit == EMPTY_ENTRY){
					reiseZeitenCounter.countEmpty();
					continue;
				}
				else if(visumData.containsKey(bezirkNummerOrigin)){
					VISUMObject vo = new VISUMObject(bezirkNummerDestination, reiseZeit);
					ArrayList<VISUMObject> list = visumData.get(bezirkNummerOrigin);
					list.add(vo);
					reiseZeitenCounter.countTotal();
				}
				else{
					VISUMObject vo = new VISUMObject(bezirkNummerDestination, reiseZeit);
					ArrayList<VISUMObject> list = new ArrayList<VISUMObject>();
					list.add(vo);
					visumData.put(bezirkNummerOrigin, list);
					reiseZeitenCounter.countOrigins();
				}
			}
			
			log.info("Done reading VISUM station 2 station travel times.");
			log.info("Reading VISUM waiting times ...");
			skipLine = true;
			Counter warteZeitenCounter = new Counter();
			while ( (line = startWarteZeitenReader.readLine()) != null ) {
				// skip lines
				if(skipLine){
					if(line.length() == 0)
						skipLine = false;
					continue;
				}
				else if(line.startsWith(VISUM_END_TAG))
					break;

				line = line.trim().replaceAll("\\s+", SEPARATOR);
				parts = line.split(SEPARATOR);
				
				Long bezirkNummerOrigin 	= Long.parseLong(parts[bezikNummerOriginIDX]);
				long bezirkNummerDestination= Long.parseLong(parts[bezikNummerDestinationIDX]);
				int warteZeit  				= Integer.parseInt(parts[valueIDX]);
				
				if(warteZeit == EMPTY_ENTRY){
					warteZeitenCounter.countEmpty();
					continue;
				}
				else if(visumData.containsKey(bezirkNummerOrigin)){
					ArrayList<VISUMObject> list = visumData.get(bezirkNummerOrigin);
					for(int i = 0; i <list.size(); i++){
						if(list.get(i).getDestinationBezirkNummer() == bezirkNummerDestination){
							VISUMObject vo = list.get(i);
							vo.addWarteZeit(warteZeit);
							break;
						}
					}
					warteZeitenCounter.countTotal();
				}
				else
					log.warn("BezirkNummer " + bezirkNummerOrigin + " not found!");
				
				avgWaitingTime.addTime(warteZeit);
			}

			log.info("Reise Zeiten Counter: Total="+reiseZeitenCounter.getTotalCounter() + " Empty="+reiseZeitenCounter.getEmptyCounter() + " Origins="+reiseZeitenCounter.getOriginCounter());
			log.info("Warte Zeiten Counter: Total="+warteZeitenCounter.getTotalCounter() + " Empty="+warteZeitenCounter.getEmptyCounter() );
			log.info("Average Waiting Time:"+ avgWaitingTime.getAverageTime());
			log.info("Done reading VISUM waiting times.");

		} catch (IOException e) { 
			e.printStackTrace(); 
		} catch (NumberFormatException nfe) {
			log.error(line);
			for(int i = 0; i < parts.length; i++)
				System.out.println(parts[i]);
			log.error("LineNumber: " + lineNumber);
			nfe.printStackTrace();
		}
	}
	
	/**
	 * this checks if the transit stops are used in VISUM and vice versa
	 */
	private void consistencyCheck(){
		
		log.info("Checking for missing waiting times ...");
		Iterator<Long> transitStopKeyIterator = transitStop.keySet().iterator();
		
		int removedVISUMDataEntries	= 0;
		
		ArrayList<Long> removeTransitStops = new ArrayList<Long>();
		
		while(transitStopKeyIterator.hasNext()){
			
			long key = transitStopKeyIterator.next();
			if(visumData.containsKey(key)){
				// check if each VISUM Object in list if it contains travel and waiting times ...
				ArrayList<VISUMObject> list = visumData.get(key);
				int localCounter = 0;
				for(int i = 0; i < list.size(); i++){
					
					VISUMObject vo = list.get(i);
					
					long destinationKey = vo.getDestinationBezirkNummer();
					
					if(!transitStop.containsKey(destinationKey)){
						if(!removeTransitStops.contains(destinationKey))
							removeTransitStops.add(destinationKey);
						list.remove(i);
						i--;
						localCounter++;
					}
					else if( (vo.getWarteZeit() < 0) || (vo.getReiseZeit() < 0.) ){
						// missing waiting time remove VISUMObject
						list.remove(i);
						i--;
						// alternatively:
						// add avg waiting time
						// vo.addWarteZeit(avgWaitingTime.getAverageTime());
						localCounter ++;
					}
				}
				removedVISUMDataEntries += localCounter;
				log.warn("Removed " + localCounter + " entries in transit stop " + key + ". These entries had no waiting- or travel times ...");
			}
			else{
				// Apparently the VISUM data does not contain this transit stop. hence it will be removed
				if(!removeTransitStops.contains(key))
					removeTransitStops.add(key);
			}
		}
		
		for(int i = 0; i < removeTransitStops.size(); i++){
			transitStop.remove(removeTransitStops.get(i));
			log.warn("Removed transit stop:" + removeTransitStops.get(i) + ". It is not used in VISUM ...");
		}	
		log.info(removeTransitStops.size() + " transit stops and " + removedVISUMDataEntries + " VISUM entries are removed.");
	}
	
	////////////////////////////////
	// TransitSchedule Dump 
	////////////////////////////////
	
	private void createTransitSchedule() {
		
		log.info("Creating transit schedule ...");

		ArrayList<Long> transitStopKeyList = new ArrayList<Long>(transitStop.keySet());
		
		TransitScheduleFactoryImpl tsfi = new TransitScheduleFactoryImpl();
		TransitSchedule transitSchedule = tsfi.createTransitSchedule();
		
		// create transit stops
		log.info("Creating transit stops ...");
		for (int i = 0; i < transitStopKeyList.size(); i++) {

			// get transit stop id (origin)
			Long transitStopKey = transitStopKeyList.get(i);
			Coord coord = transitStop.get(transitStopKey);
//			Link nearestLink = network.getNearestLinkExactly(coord);
			
			TransitStopFacility stop = tsfi.createTransitStopFacility(Id.create(transitStopKey, TransitStopFacility.class), coord, false);
//			((TransitStopFacilityImpl) stop).setLinkId(nearestLink.getId());
			((TransitStopFacilityImpl) stop).setLinkId(Id.create(transitStopKey +""+ transitStopKey, Link.class));
			transitSchedule.addStopFacility(stop);
		}
		log.info("Creating transit stops done.");
		
		// add transit lines
		log.info("Creating transit lines ...");
		long transitLineId = 1;
		String mode = "pt".intern();
		for(int i = 0; i < transitStopKeyList.size(); i++){
			
			
//			// for testing 
//			if(i == 10)
//				break;
			
			// get transit stop id (origin)
			long transitStopA = transitStopKeyList.get(i);
			log.info("Processing transit stop " + transitStopA);
			
			// get related destinations, travel- and waiting times
			ArrayList<VISUMObject> list = visumData.get(transitStopA);
			Iterator<VISUMObject> listIterator = list.iterator();
			
			while(listIterator.hasNext()){
				
				VISUMObject voA2B 	= listIterator.next();
				
				long transitStopB 	= voA2B.getDestinationBezirkNummer();
				
				// transit line A -> B & B -> A
				TransitLine transitLine = tsfi.createTransitLine(Id.create(transitLineId, TransitLine.class));
				
				if(! voA2B.isHandeled() ){ 				// direction A -> B
					
					long waitTimeA2B 	= voA2B.getWarteZeit() * 60;	// get as seconds
					double travelTimeA2B= voA2B.getReiseZeit() * 60.; 	// get as seconds 
					// mark as handled
					voA2B.markAsHandled();
					
					// line A -> B
					createAndAddTransitStopsAndDepartures(tsfi, transitSchedule, mode, 
								transitStopA, transitStopB, transitLine,
								travelTimeA2B, waitTimeA2B);
				}

				VISUMObject voB2A = getVISUMObject(transitStopB, transitStopA);
				if(voB2A != null && !voB2A.isHandeled()){// direction B -> A
					
					long waitTimeB2A	= voB2A.getWarteZeit() * 60;	// get as seconds
					double travelTimeB2A= voB2A.getReiseZeit() * 60.;	// get as seconds
					// mark as handled
					voB2A.markAsHandled();
					
					// line B -> A (get opposite direction)
					createAndAddTransitStopsAndDepartures(tsfi, transitSchedule, mode, 
							transitStopB, transitStopA, transitLine,
							travelTimeB2A, waitTimeB2A);
				}
				
				// add transit line to schedule if not empty
				if(transitLine.getRoutes().size() > 0){
					transitLineId++;
					transitSchedule.addTransitLine(transitLine);
				}
			}
			log.info("Created Transit Lines: " + transitLineId);
		}
		
		log.info("Writing TransitSchedule ...");
		writer = new TransitScheduleWriterV1(transitSchedule);
		writer.write(destinationFile);
		log.info("Writing done!");
	}

	/**
	 * @param tsfi
	 * @param transitSchedule
	 * @param mode
	 * @param transitStopOrigin
	 * @param transitStopDestination
	 * @param transitLine
	 * @param travelTime
	 */
	private void createAndAddTransitStopsAndDepartures(TransitScheduleFactoryImpl tsfi,
			TransitSchedule transitSchedule, String mode, long transitStopOrigin,
			long transitStopDestination, TransitLine transitLine, double travelTime, long waitTime) {
		// create transit route stops
		TransitStopFacility origin 	    = transitSchedule.getFacilities().get(Id.create(transitStopOrigin, TransitStopFacility.class));
		TransitStopFacility destination = transitSchedule.getFacilities().get(Id.create(transitStopDestination, TransitStopFacility.class));
		
		TransitRouteStop transitRouteStopOrigin 	= tsfi.createTransitRouteStop(origin, 0., 0.);
		TransitRouteStop transitRouteStopDestination= tsfi.createTransitRouteStop(destination, travelTime, 0.);
		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		stops.add(transitRouteStopOrigin);
		stops.add(transitRouteStopDestination);
		
//		Link linkOrigin 	 = network.getLinks().get(origin.getLinkId());
//		Link linkDestination = network.getLinks().get(destination.getLinkId());
//		Node nodeOrigin 	 = linkOrigin.getToNode();
//		Node nodeDestination = linkDestination.getToNode();
//		
//		Path path = dijkstra.calcLeastCostPath(nodeOrigin, nodeDestination, 0, null, null);
//		List<Id> linkIds = new ArrayList<Id>();
//		for(Link l: path.links)
//			linkIds.add(l.getId());
		
		List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
		linkIds.add(Id.create(origin.getId() +""+ destination.getId(), Link.class));
		
		NetworkRoute route = new LinkNetworkRouteImpl(origin.getLinkId(), destination.getLinkId());
		route.setLinkIds(origin.getLinkId(), linkIds, destination.getLinkId());

		TransitRoute transitRoute = tsfi.createTransitRoute(Id.create(transitStopOrigin+"to"+transitStopDestination, TransitRoute.class), route, stops, mode);
		
		// set departures
		int departureId = 1;
		for(int t = 0; t < secondsOfDay; t += (waitTime*2)){
			Departure departure = tsfi.createDeparture(Id.create(getNumberFormat(departureId), Departure.class), t);
			departure.setVehicleId(Id.create(departureId, Vehicle.class));
			transitRoute.addDeparture(departure);
			
			departureId++;
		}
		
		transitLine.addRoute(transitRoute);
	}
	
	private VISUMObject getVISUMObject(long origin, long destination){
		
		ArrayList<VISUMObject> list = visumData.get( origin );
		VISUMObject vo = null;
		
		for(int i = 0; i < list.size(); i++)
			if (list.get(i).getDestinationBezirkNummer() == destination){
				vo = list.get(i);
				list.remove(i);
				break;
			}
		return vo;
	}
	
	private String getNumberFormat(int number){
		return numberFormat.format(number);
	}
	
	////////////////////////////////
	// Entry Point
	////////////////////////////////
	
	public static void main(String args[]){
		
		if(args != null && args.length == 4){
			
			String zoneAttributesFile = args[0];
			String reiseZeiten		  = args[1];
			String warteZeiten		  = args[2];
			String outputFile		  = args[3];
			
			VISUM2TransitScheduleConverter instance = new VISUM2TransitScheduleConverter(zoneAttributesFile, reiseZeiten, warteZeiten, outputFile);
			instance.init();
			instance.createTransitSchedule();
		}
		log.info("Done!");
	}
	
	////////////////////////////////
	// Inner Class
	////////////////////////////////
	
	public class VISUMObject{
		
		private long zielBezirkNummer;
		private int warteZeit;
		private double reiseZeit;
		private boolean handeled = false;
		
		public VISUMObject(long bezirkNummer, double reiseZeit){
			this.zielBezirkNummer 	= bezirkNummer;
			this.warteZeit			= -1;
			this.reiseZeit 			= reiseZeit;
		}
		
		public void addWarteZeit(int warteZeit){
			this.warteZeit 			= warteZeit;
		}
		
		public long getDestinationBezirkNummer(){
			return this.zielBezirkNummer;
		}
		
		public int getWarteZeit(){
			return this.warteZeit;
		}
		
		public double getReiseZeit(){
			return this.reiseZeit;
		}
		
		public void markAsHandled(){
			this.handeled = false;
		}
		
		public boolean isHandeled(){
			return this.handeled;
		}
	}

	public class Counter{
		
		private int emptyCounter;
		private int origins;
		private int total;
		
		public Counter(){
			this.emptyCounter 	= 0;
			this.origins		= 0;
			this.total			= 0;
		}
		
		public void countEmpty(){
			this.total++;
			this.emptyCounter++;
		}
		
		public void countOrigins(){
			this.total++;
			this.origins++;
		}
		
		public void countTotal(){
			this.total++;
		}
		
		public int getEmptyCounter(){
			return this.emptyCounter;
		}
		
		public int getOriginCounter(){
			return this.origins;
		}
		
		public int getTotalCounter(){
			return this.total;
		}
	}
	
	public class AverageTime{
		
		private long sum;
		private long counter;
		
		public AverageTime(){
			this.sum 	= 0;
			this.counter= 0;
		}
		
		public void addTime(long time){
			sum += time;
			counter++;
		}
		
		public int getAverageTime(){
			return (int) (sum/counter);
		}
	}
}
