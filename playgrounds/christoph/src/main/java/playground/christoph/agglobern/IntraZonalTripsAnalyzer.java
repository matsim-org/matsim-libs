/* *********************************************************************** *
 * project: org.matsim.*
 * IntraZonalTripsAnalyzer.java
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
import java.util.Map;
import java.util.Map.Entry;

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
 * Identifies home to work and work to home trip within one of the swiss municipalities.
 */
public class IntraZonalTripsAnalyzer implements ActivityStartEventHandler, ActivityEndEventHandler,
	PersonDepartureEventHandler {

	private static final Logger log = Logger.getLogger(IntraZonalTripsAnalyzer.class);
	
	private static final String separator = "\t";
	
	private static String outputDirectory = "/data/matsim/cdobler/sandbox00/census2000V2_100pct_kti_run5/";
	private static String shapeFile = outputDirectory + "GIS/G3G07.shp";
	private static String facilitiesFile = outputDirectory + "kti.5.output_facilities.xml.gz";
	private static String eventsFile = outputDirectory + "ITERS/it.100/kti.5.100.events.xml.gz";
		
	private final Map<Id, ActivityEndEvent> previousActivityEndEvents = new HashMap<Id, ActivityEndEvent>();
	private final Map<Id, PersonDepartureEvent> previousDepartureEvents = new HashMap<Id, PersonDepartureEvent>();
	private final Map<Id, Integer> facilityMunicipalities = new HashMap<Id, Integer>();
	private final Map<Id, Coord> facilityCoords = new HashMap<Id, Coord>();
	private final Map<Integer, SimpleFeature> municipalities = new HashMap<Integer, SimpleFeature>();
	
	private final Counter home2workCounter = new Counter("# home to work trips: ");
	private final Counter work2homeCounter = new Counter("# work to home trips: ");
	
	private BufferedWriter home2workWriter;
	private BufferedWriter work2homeWriter;
	
	public static void main(String[] args) throws Exception {
		
		Config config = ConfigUtils.createConfig();
		config.facilities().setInputFile(facilitiesFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		new IntraZonalTripsAnalyzer(scenario, shapeFile, eventsFile, outputDirectory);
	}
	
	public IntraZonalTripsAnalyzer(Scenario scenario, String shpFile, String eventsFile, String outputDirectory) throws Exception {
		
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
		home2workWriter = IOUtils.getBufferedWriter(outputDirectory + "home2workTrips.txt");
		work2homeWriter = IOUtils.getBufferedWriter(outputDirectory + "work2homeTrips.txt");
		
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
		bw.write("municipality");
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
		
		return startMunicipality == endMunicipality;		
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
		
		boolean intraZonalTrip = false;
		
		try {
			if (event.getActType().startsWith("work")) {
				ActivityEndEvent previousActivityEndEvent = previousActivityEndEvents.remove(event.getPersonId());
				
				// if it is a work -> home trip
				if (previousActivityEndEvent.getActType().equals("home")) {
					intraZonalTrip = checkLocation(event, previousActivityEndEvent);
					
					if (intraZonalTrip) {
						writeFileRow(home2workWriter, previousActivityEndEvent, previousDepartureEvent, event);
						home2workCounter.incCounter();
					}
				}
				
			} else if (event.getActType().equals("home")) {
				ActivityEndEvent previousActivityEndEvent = previousActivityEndEvents.remove(event.getPersonId());
				
				// if it is a home -> work trip
				if (previousActivityEndEvent.getActType().startsWith("work")) {
					intraZonalTrip = checkLocation(event, previousActivityEndEvent);
					
					if (intraZonalTrip) {
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
