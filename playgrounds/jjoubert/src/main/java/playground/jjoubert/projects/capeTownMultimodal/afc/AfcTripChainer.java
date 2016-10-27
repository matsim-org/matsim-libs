/* *********************************************************************** *
 * project: org.matsim.*
 * AfcTripChainer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.jjoubert.projects.capeTownMultimodal.afc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacilitiesFactoryImpl;
import org.matsim.facilities.ActivityFacility;

import playground.southafrica.freight.digicore.containers.DigicoreNetwork;
import playground.southafrica.utilities.Header;

/**
 * Class to try and chain trips together.
 * 
 * @author jwjoubert
 */
public class AfcTripChainer {
	final private static Logger LOG = Logger.getLogger(AfcTripChainer.class);
	final private Map<String, Integer> stopMap;
	final private GtfsParser gp;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(AfcTripChainer.class.toString(), args);
		
		String input = args[0];
		String gtfsStops = args[1];
		String gtfsNetwork = args[2];
		String gtfsSchedule = args[3];
		String output = args[4];
		String network = args[5];
		
		AfcTripChainer atc = new AfcTripChainer(gtfsStops, gtfsNetwork, gtfsSchedule);
		
		atc.extractTripChains(input, output, network);
		
		Header.printFooter();
	}
	
	private AfcTripChainer(String gtfsStops, String gtfsNetwork, String gtfsSchedule) {
		this.stopMap = AfcUtils.parseStopIdFromGtfs(gtfsStops);
		this.gp = new GtfsParser(gtfsNetwork, gtfsSchedule);
	}
	
	
	public void extractTripChains(String input, String output, String network){
		Map<String, Map<String, String>> personMap = groupRecordsByPerson(input);
		
		/* Clean each person's records. */
		LOG.info("Cleaning connections...");
		Counter personCounter = new Counter("  person # ");
		for(String cid : personMap.keySet()){
			Map<String, String> oldMap = personMap.get(cid);
			personMap.put(cid, cleanConnections(oldMap));
			personCounter.incCounter();
		}
		personCounter.printCounter();
		LOG.info("Done cleaning records.");
		
		/* Extract the trips. */
		LOG.info("Extracting trips...");
		personCounter.reset();
		List<String> trips = new ArrayList<>();
		for(String cid : personMap.keySet()){
			trips.addAll(extractTrips(personMap.get(cid)));
			personCounter.incCounter();
		}
		personCounter.printCounter();
		LOG.info("Done extracting trips.");
		
		writeTrips(trips, output);
	}
	
	
	private void writeTrips(List<String> trips, String output){
		BufferedWriter bw = IOUtils.getBufferedWriter(output);
		
		try{
			bw.write("id,o,d,timeO,timeD,oBin,dBin,duration,distO,distD");
			bw.newLine();
			for(String s : trips){
				bw.write(s);
				bw.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + output);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + output);
			}
		}
	}
	
	
	private Map<String, Map<String, String>> groupRecordsByPerson(String filename){
		LOG.info("Parsing records from " + filename);
		
		Map<String, Map<String, String>> completePersonMap = new TreeMap<>();
		
		BufferedReader br = IOUtils.getBufferedReader(filename);
		Counter counter = new Counter("  records # ");
		try{
			String line = br.readLine(); /* Header */
			while((line = br.readLine()) != null){
				String[] sa = line.split(",");
				String id = sa[0];
				
				/* Only use card number with more than 8 digits. */
				if(id.length() > 8){
					
					/* Add the person if not already there. */
					if(!completePersonMap.containsKey(id)){
						completePersonMap.put(id, new TreeMap<>());
					}
					Map<String, String> thisPersonMap = completePersonMap.get(id);
					
					String time = getTimeFromRecord(line);
					if(thisPersonMap.containsKey(time)){
//						throw new RuntimeException("Multiple transactions at the same time for card " + id);
						LOG.error("Multiple transactions at the same time for card " + id);
					} else{
						thisPersonMap.put(time, line);
					}
				}
				counter.incCounter();
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
		LOG.info("Done parsing records. Total number of unique cards: " + completePersonMap.size());
		return completePersonMap;
	}
	
	private Map<String, String> cleanConnections(Map<String, String> map){
		map = cleanToFirstBoarding(map);
		map = removeSameLocationConnections(map);
		
		return map;
	}
	
	
	private Map<String, String> cleanToFirstBoarding(Map<String,String> map){
		List<String> recordsToRemove = new ArrayList<>();
		
		boolean foundFirstBoarding = false;
		Iterator<String> keyIterator = map.keySet().iterator();
		
		while(!foundFirstBoarding && keyIterator.hasNext()){
			String key = keyIterator.next();
			String record = map.get(key);
			String type = getActivityTypeFromRecord(record);
			if(type.equalsIgnoreCase("B")){
				foundFirstBoarding = true;
			} else{
				recordsToRemove.add(key);
			}
		}
		
		for(String keyToRemove : recordsToRemove){
			map.remove(keyToRemove);
		}
		
		return map;
	}
	
	private Map<String, String> removeSameLocationConnections(Map<String, String> map){
		
		/* Build list from Map. */
		List<String> list = new ArrayList<>();
		for(String key : map.keySet()){
			list.add(map.get(key));
		}
		
		List<String> recordsToRemove = new ArrayList<>();
		
		/* Check all consecutive pairs for connection pattern A-C. */
		for(int i = 1; i < list.size(); i++){
			String previousType = getActivityTypeFromRecord(list.get(i-1));
			String currentType = getActivityTypeFromRecord(list.get(i));
			
			String previousStation = getLocationFromRecord(list.get(i-1));
			String currentStation = getLocationFromRecord(list.get(i));
			
			if(previousType.equalsIgnoreCase("A") && currentType.equalsIgnoreCase("C")){
				if(previousStation.equalsIgnoreCase(currentStation)){
					/* Can remove both transactions. */
					recordsToRemove.add(getTimeFromRecord(list.get(i-1)));
					recordsToRemove.add(getTimeFromRecord(list.get(i)));
				}
			}
		}
		
		/* Remove the records. */
		for(String key : recordsToRemove){
			map.remove(key);
		}
		
		return map;
	}
	
	
	private List<String> extractTrips(Map<String, String> map){
		List<String> trips = new ArrayList<>();
		
		/* Ignore incomplete trips. */
		if(map.size() > 1){
			
			Iterator<String> iterator = map.keySet().iterator();
			String startRecord = null;
			while(iterator.hasNext()){
				String key = iterator.next();
				String record = map.get(key);
				
				String type = getActivityTypeFromRecord(record);
				if(type.equalsIgnoreCase("B")){
					startRecord = record;
				} else if(type.equalsIgnoreCase("A")){
					if(startRecord == null){
						LOG.warn("Alighting without a boarding!");
						/*TODO Find out what to do...*/
					} else{
						/* Use the trip. */
						trips.add(convertOdRecordsToString(startRecord, record));
						startRecord = null;
					}
				} else{
					/*  It is a connection. */
					startRecord = record;
				}
			}
		}
		
		return trips;
	}
	
	private String convertOdRecordsToString(String o, String d){
		String s = "";
		String[] oa = o.split(",");
		
		String id = oa[0];
		
		/* Duration. */
		double t1 = Time.parseTime(getTimeFromRecord(o));
		double t2 = Time.parseTime(getTimeFromRecord(d));
		int duration = (int) Math.round((t2 - t1) / 60.0);
		
		/* Time bin */
		int oBin = getTimeBinFromRecord(o);
		int dBin = getTimeBinFromRecord(d);
		
		/* Origin */
		int oGtfs = getGtfsLocationFromRecord(o);
		
		/* Destination */
		int dGtfs = getGtfsLocationFromRecord(d);
		
		/* Route */
		double distO = gp.findRouteDistance(oGtfs, dGtfs, t1);
		double distD = gp.findRouteDistance(oGtfs, dGtfs, t2);
		
		s = String.format("%s,%d,%d,%s,%s,%d,%d,%d,%.0f,%.0f", 
				id, 
				oGtfs, 
				dGtfs, 
				getTimeFromRecord(o), 
				getTimeFromRecord(d),
				oBin,
				dBin,
				duration,
				distO,
				distD);
		
		return s;
	}
	
	private int getGtfsLocationFromRecord(String record){
		String[] sa = record.split(",");
		int stopId = 0;
		String name = sa[5];
		if(this.stopMap.containsKey(name)){
			stopId = this.stopMap.get(name);
		} else{
			stopId = AfcUtils.getGtfsStationIdFromName(name);
		}
		return stopId;
	}
	
	
	private static String getActivityTypeFromRecord(String record){
		String[] sa = record.split(",");
		return AfcUtils.getTransactionAbbreviation(sa[6]);
	}
	
	private static String getLocationFromRecord(String record){
		String[] sa = record.split(",");
		String stationName = sa[5];
		return stationName;
	}
	
	private static String getTimeFromRecord(String record){
		String[] sa = record.split(",");
		String time = String.format("%02d:%02d", 
				Integer.parseInt(sa[9]), 
				Integer.parseInt(sa[12]));
		return time;
	}

	
	private int getTimeBinFromRecord(String record){
		double time = Time.parseTime(getTimeFromRecord(record));
		int bin = (int) Math.ceil((time / 60.0) / 5.0);
		return bin;
	}
}
