/* *********************************************************************** *
 * project: org.matsim.*
 * ExtraZonalTripsAnalyzer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.christoph.agglobern;

import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.Facility;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/*
 * Identifies home to work and work to home trip from or to swiss municipalities
 * listed in zonesArray.
 */
public class ExtraZonalTripsAnalyzer implements ActivityStartEventHandler, ActivityEndEventHandler,
	PersonDepartureEventHandler {

	private static final Logger log = Logger.getLogger(ExtraZonalTripsAnalyzer.class);
	
	private static final String separator = "\t";
	
	private static String outputDirectory = "/data/matsim/cdobler/sandbox00/census2000V2_100pct_kti_run5/";
	private static String shapeFile = outputDirectory + "GIS/G3G07.shp";
	private static String facilitiesFile = outputDirectory + "kti.5.output_facilities.xml.gz";
	private static String eventsFile = outputDirectory + "ITERS/it.100/kti.5.100.events.xml.gz";
	
	private int[] zonesArray = new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 21, 22, 23, 24, 25, 26, 27,
			28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 51, 52, 53, 54, 55, 56, 57, 58,
			59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91,
			92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121,
			131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 151, 152, 153, 154, 155, 156, 157, 158,
			159, 160, 161, 171, 172, 173, 174, 175, 176, 177, 178, 179, 180, 181, 182, 191, 192, 193, 194, 195,
			196, 197, 198, 199, 200, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223, 224, 225,
			226, 227, 228, 229, 230, 231, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 261};
	
	private final Map<Id, ActivityEndEvent> previousActivityEndEvents = new HashMap<Id, ActivityEndEvent>();
	private final Map<Id, PersonDepartureEvent> previousDepartureEvents = new HashMap<Id, PersonDepartureEvent>();
	private final Map<Id, Integer> facilityMunicipalities = new HashMap<Id, Integer>();
	private final Map<Id, Coord> facilityCoords = new HashMap<Id, Coord>();
	private final Map<Integer, SimpleFeature> municipalities = new HashMap<Integer, SimpleFeature>();
	private final Set<Integer> zones = new HashSet<Integer>();
	
	private final Counter home2workCounter = new Counter("# home to work trips: ");
	private final Counter work2homeCounter = new Counter("# work to home trips: ");
	
	private BufferedWriter home2workWriter;
	private BufferedWriter work2homeWriter;
	
	public static void main(String[] args) throws Exception {
		
		Config config = ConfigUtils.createConfig();
		config.facilities().setInputFile(facilitiesFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		new ExtraZonalTripsAnalyzer(scenario, shapeFile, eventsFile, outputDirectory);
	}
	
	public ExtraZonalTripsAnalyzer(Scenario scenario, String shpFile, String eventsFile, String outputDirectory) throws Exception {
		
		for (int zone : zonesArray) zones.add(zone);
		
		log.info("Reading municipalities from shp file...");
		for (SimpleFeature municipality : ShapeFileReader.getAllFeatures(shapeFile)) {
			municipalities.put((Integer) municipality.getAttribute(1), municipality);
		}
		log.info("done.");		
		log.info("Found " + municipalities.size() + " municipalities.");
		
		log.info("Mapping facilities to municipalities...");
		GeometryFactory factory = new GeometryFactory();
		for (Facility facility : ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().values()) {
			Coord coord = facility.getCoord();
			Point point = factory.createPoint(new Coordinate(coord.getX(), coord.getY()));
			for (Entry<Integer, SimpleFeature> entry : municipalities.entrySet()) {
				SimpleFeature municipality = entry.getValue();
				Geometry polygon = (Geometry) municipality.getDefaultGeometry();
				if (polygon.contains(point)) {
					facilityMunicipalities.put(facility.getId(), entry.getKey());
					facilityCoords.put(facility.getId(), facility.getCoord());
					break;
				}
			}
		}
		log.info("done.");
		log.info("Mapped " + facilityMunicipalities.size() + " out of " + 
		((ScenarioImpl) scenario).getActivityFacilities().getFacilities().size() + " facilities to municipalities.");
		
		log.info("Reading events...");
		home2workWriter = IOUtils.getBufferedWriter(outputDirectory + "home2workTrips.txt.gz");
		work2homeWriter = IOUtils.getBufferedWriter(outputDirectory + "work2homeTrips.txt.gz");
		
		writeFileHeader(home2workWriter);
		writeFileHeader(work2homeWriter);
		
		readEvents(eventsFile);
		
		home2workWriter.flush();
		work2homeWriter.flush();
		
		home2workWriter.close();
		work2homeWriter.close();
		
		home2workCounter.printCounter();
		work2homeCounter.printCounter();
		log.info("done.");
	}
	
	private void readEvents(final String eventFileName) {
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(this);
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(eventFileName);
	}
	
	private void writeFileHeader(BufferedWriter bw) throws Exception {
		bw.write("fromMunicipality");
		bw.write(separator);
		bw.write("toMunicipality");
		bw.write(separator);
		bw.write("departuretime");
		bw.write(separator);
		bw.write("traveltime");
		bw.write(separator);
		bw.write("fromX");
		bw.write(separator);
		bw.write("fromY");
		bw.write(separator);
		bw.write("toX");
		bw.write(separator);
		bw.write("toY");
		bw.write(separator);
		bw.write("mode");
		bw.newLine();
		bw.flush();
	}
	
	private void writeFileRow(BufferedWriter bw, ActivityEndEvent endEvent, 
			PersonDepartureEvent departureEvent, ActivityStartEvent startEvent) throws Exception {
		
		Coord fromCoord = facilityCoords.get(endEvent.getFacilityId());
		Coord toCoord = facilityCoords.get(startEvent.getFacilityId());		
		
		bw.write(Integer.toString(facilityMunicipalities.get(endEvent.getFacilityId())));
		bw.write(separator);
		bw.write(Integer.toString(facilityMunicipalities.get(startEvent.getFacilityId())));
		bw.write(separator);
		bw.write(Double.toString(endEvent.getTime()));
		bw.write(separator);
		bw.write(Double.toString(startEvent.getTime() - endEvent.getTime()));
		bw.write(separator);
		bw.write(Double.toString(fromCoord.getX()));
		bw.write(separator);
		bw.write(Double.toString(fromCoord.getY()));
		bw.write(separator);
		bw.write(Double.toString(toCoord.getX()));
		bw.write(separator);
		bw.write(Double.toString(toCoord.getY()));
		bw.write(separator);
		bw.write(departureEvent.getLegMode());
		bw.newLine();
	}
	
	private boolean checkLocation(ActivityStartEvent startEvent, ActivityEndEvent endEvent) {
		
		Integer startMunicipality = facilityMunicipalities.get(startEvent.getFacilityId());
		Integer endMunicipality = facilityMunicipalities.get(endEvent.getFacilityId());
		
		if (startMunicipality == null || endMunicipality == null) return false;
		
		return zones.contains(startMunicipality) || zones.contains(endMunicipality);
	}
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void handleEvent(ActivityEndEvent event){
		previousActivityEndEvents.put(event.getPersonId(), event);
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		
		PersonDepartureEvent previousDepartureEvent = previousDepartureEvents.remove(event.getPersonId());
		
		boolean relevantTrip = false;
		
		try {
			if (event.getActType().startsWith("work")) {
				ActivityEndEvent previousActivityEndEvent = previousActivityEndEvents.remove(event.getPersonId());
				
				// if it is a work -> home trip
				if (previousActivityEndEvent.getActType().equals("home")) {
					relevantTrip = checkLocation(event, previousActivityEndEvent);
					
					if (relevantTrip) {
						writeFileRow(home2workWriter, previousActivityEndEvent, previousDepartureEvent, event);
						home2workCounter.incCounter();
					}
				}
				
			} else if (event.getActType().equals("home")) {
				ActivityEndEvent previousActivityEndEvent = previousActivityEndEvents.remove(event.getPersonId());
				
				// if it is a home -> work trip
				if (previousActivityEndEvent.getActType().startsWith("work")) {
					relevantTrip = checkLocation(event, previousActivityEndEvent);
					
					if (relevantTrip) {
						writeFileRow(work2homeWriter, previousActivityEndEvent, previousDepartureEvent, event);
						work2homeCounter.incCounter();	
					}
				}
			}			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		previousDepartureEvents.put(event.getPersonId(), event);
	}
}
