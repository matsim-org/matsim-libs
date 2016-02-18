/* *********************************************************************** *
 * project: org.matsim.*
 * SiteVisitIdentifier.java                                                                        *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.jjoubert.projects.wasteCollection.dataHandling;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.vehicles.Vehicle;

import playground.southafrica.utilities.Header;

public class SiteVisitIdentifier {
	private final static Logger LOG = Logger.getLogger(SiteVisitIdentifier.class);
	private final static Double DISTANCE_THRESHOLD = 50.0;
	private final static Double MINUTE_THRESHOLD = 90.0;
	
	private static CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_SA_Albers");
	private Map<Id<Vehicle>, Map<String, List<GregorianCalendar>>> map = new TreeMap<Id<Vehicle>, Map<String, List<GregorianCalendar>>>();
	private static Map<String,Coord> coordMap = new TreeMap<String,Coord>();

	public static void main(String[] args) {
		Header.printHeader(SiteVisitIdentifier.class.toString(), args);
		
		String gpsFile = args[0];
		int maxLines = Integer.parseInt(args[1]);
		String siteCoordinatesFile = args[2];
		String output = args[3];
		
		/* Parse the site coordinates. */
		BufferedReader br = IOUtils.getBufferedReader(siteCoordinatesFile);
		try {
			String line = null;
			while ((line = br.readLine()) != null) {
				String[] sa = line.split(",");
				String site = sa[0];
				double lat = Double.parseDouble(sa[1]);
				double lon = Double.parseDouble(sa[2]);
				Coord coord = CoordUtils.createCoord(lon, lat);
				coordMap.put(site, ct.transform(coord));
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read from " + siteCoordinatesFile);
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + siteCoordinatesFile);
			}
		}
		LOG.info("Sites considered (SA-Albers CRS): ");
		for(String site : coordMap.keySet()){
			LOG.info(String.format("   \\_ %s (%.0f;%.0f)", site, coordMap.get(site).getX(), coordMap.get(site).getY()));
		}
		
		SiteVisitIdentifier svi = new SiteVisitIdentifier();
		svi.ProcessGpsRecords(gpsFile, maxLines);
		svi.writeSiteVisitsToFile(output);
		
		Header.printFooter();
	}
	
	public SiteVisitIdentifier() {
		// TODO Auto-generated constructor stub
	}
	
	public void writeSiteVisitsToFile(String filename){
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try{
			bw.write("VehId,date,dayOfWeek,site");
			bw.newLine();
			
			/* Convert each site visit to a line in the output. */
			for(Id<Vehicle> vehicleId : map.keySet()){
				Map<String, List<GregorianCalendar>> vehicleVisits = map.get(vehicleId);
				for(String site : vehicleVisits.keySet()){
					List<GregorianCalendar> visits = vehicleVisits.get(site);
					for(GregorianCalendar visit : visits){
						bw.write(convertVisitToString(vehicleId, site, visit));
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + filename);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + filename);
			}
		}
	}
	
	private String convertVisitToString(Id<Vehicle> vehicleId, String site, GregorianCalendar cal){
		String date = WasteUtils.convertGregorianCalendarToDate(cal);
		int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
		String s = String.format("%s,%s,%d,%s\n", vehicleId.toString(), date, dayOfWeek, site);
		return s;
	}
	
	public void processGpsRecords(String filename){
		this.ProcessGpsRecords(filename, Integer.MAX_VALUE);
	}
	
	public void ProcessGpsRecords(String filename, int maxLines){
		LOG.info("Processing the GPS file " + filename);
		BufferedReader br = IOUtils.getBufferedReader(filename);
		Counter counter = new Counter("   lines # ");
		try{
			String line = null;
			while((line=br.readLine()) != null && counter.getCounter() < maxLines){
				if(line.startsWith("RT")){
					processNewVisit(line);
					counter.incCounter();
				} else{
					/* Ignore the header lines. */
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read from " + filename);
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + filename);
			}
		}
		counter.printCounter();
		
		LOG.info("Done processing GPS file.");
		
		/* Report the number of visits. */
		LOG.info("Number of vehicles: " + map.size());
		int visits = 0;
		for(Id<Vehicle> vehicle : map.keySet()){
			Map<String, List<GregorianCalendar>> vehicleMap = map.get(vehicle);
			for(String site : vehicleMap.keySet()){
				visits += vehicleMap.get(site).size();
			}
		}
		LOG.info("Number of visits: " + visits);
	}
	
	private void processNewVisit(String line){
		
		boolean addRecord = false;
		
		/* Check distance */
		boolean closeToSite = false;
		String closestSite = null;
		Coord c = ct.transform( CoordUtils.createCoord(getLongitude(line), getLatitude(line)) );
		Iterator<String> iterator = coordMap.keySet().iterator();
		while(iterator.hasNext() && !closeToSite){
			String site = iterator.next();
			Coord siteCoord = coordMap.get(site);
			double distance = CoordUtils.calcEuclideanDistance(c, siteCoord);
			if(distance <= DISTANCE_THRESHOLD){
				closeToSite = true;
				closestSite = site;
			}
		}
		
		/* Check if the vehicle has been spotted at the specific site before,
		 * and create the necessary map entries if not. */
		Id<Vehicle> vehicleId = Id.create(getVehicleId(line), Vehicle.class);
		if(closeToSite){
			if(!map.containsKey(vehicleId)){
				map.put(vehicleId, new TreeMap<String, List<GregorianCalendar>>());
			}
			Map<String, List<GregorianCalendar>> vehicleMap = map.get(vehicleId);
			if(!vehicleMap.containsKey(closestSite)){
				vehicleMap.put(closestSite, new ArrayList<GregorianCalendar>());
			}			
		}
		
		/* Check time. */
		if(closeToSite){
			List<GregorianCalendar> visits = map.get(vehicleId).get(closestSite);
			GregorianCalendar date = getTimeAsGregorianCalendar(line);
			boolean insideRange = false;
			Iterator<GregorianCalendar> visitIterator = visits.iterator();
			while(visitIterator.hasNext() && !insideRange){
				GregorianCalendar visit = visitIterator.next();
				double minutesDifference = compareCalendars(visit, date);
				if(minutesDifference <= MINUTE_THRESHOLD){
					insideRange = true;
				}
			}
			
			if(!insideRange){
				/* It is a new visit! */
				map.get(vehicleId).get(closestSite).add(date);
			}
		}
	}
	
	private double compareCalendars(GregorianCalendar c1, GregorianCalendar c2){
		double min1 = convertGregorianCalendarToMinutes(c1);
		double min2 = convertGregorianCalendarToMinutes(c2);
		double minutes = Math.abs(min1 - min2);
		
		return minutes;
	}
	
	private double convertGregorianCalendarToMinutes(GregorianCalendar cal){
		return ((double)cal.getTimeInMillis()) / (1000.0*60.0);
	}
	
	private String getVehicleId (String line){
		return line.substring(9, 29).replace(" ", "");
	}
	
	private String getTimeAsString(String line){
		return line.substring(30, 55);
	}
	
	private GregorianCalendar getTimeAsGregorianCalendar(String line){
		GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH);
		String dateTime = getTimeAsString(line);
		int year = Integer.parseInt(dateTime.substring(0, 4));
		int month = Integer.parseInt(dateTime.substring(5, 7)) - 1;
		int day = Integer.parseInt(dateTime.substring(8, 10));
		int hour = Integer.parseInt(dateTime.substring(11, 13));
		int min = Integer.parseInt(dateTime.substring(14, 16));
		int sec = Integer.parseInt(dateTime.substring(17, 19));
		
		cal.set(year, month, day, hour, min, sec);
		return cal;
	}
	
	private double getLongitude(String line){
		return Double.parseDouble(line.substring(56, 79));
	}
	
	private double getLatitude(String line){
		return Double.parseDouble(line.substring(80, 103));
	}
	
	private boolean getIgnitionstatus(String line){
		String ignition = line.substring(104, 105);
		if(ignition.equalsIgnoreCase("T")){
			return true;
		} else{
			return false;
		}
	}
	

}
