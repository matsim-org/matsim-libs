/* *********************************************************************** *
 * project: org.matsim.*
 * AfcAgregator.java
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.Time;

import playground.southafrica.utilities.Header;

/**
 * Aggregate automated fare collection (AFC) data, like that received from City
 * of Cape Town, into given time bins.
 * 
 * @author jwjoubert
 */
public class AfcAggregator {
	final private static Logger LOG = Logger.getLogger(AfcAggregator.class);
	private static Map<String, Integer> gtfsStopMap;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(AfcAggregator.class.toString(), args);
		String input = args[0];
		int seconds = Integer.parseInt(args[1]);
		String output = args[2];
		String odDurations = args[3];

		gtfsStopMap = AfcUtils.parseStopIdFromGtfs(args[4]);
		
		String network = args[5];
		String schedule = args[6];
		
		processInput(input, seconds, output, odDurations, network, schedule);
		
		Header.printFooter();
	}

	private AfcAggregator(){
		/* Hide constructor. */
	}
	
	/**
	 * 
	 * @param input the automated fare collection (AFC) file provided by the
	 * 			City of Cape Town;
	 * @param binSize in minutes;
	 * @param output where the aggregated trip data is written to;
	 * @param odFile where the person-specific origin-destination results are
	 * 			written to. 
	 */
	public static void processInput(String input, int binSize, String output, String odFile, String network, String schedule){
		LOG.info("Processing input file " + input);
		
		GtfsParser gp = new GtfsParser(network, schedule);
		
		int unknownLeavers = 0;
		Map<String, Integer> mapBoarding = new TreeMap<String, Integer>();
		Map<String, Integer> mapAlighting = new TreeMap<String, Integer>();
		Map<String, Integer> mapConnecting = new TreeMap<String, Integer>();
		
		Map<String,Integer> mapPerson = new HashMap<String, Integer>();
		Map<String,Integer> mapLocation = new HashMap<>();
		
		Map<String, List<Integer>> mapDuration = new TreeMap<>();
		List<String> odPairDurations = new ArrayList<>();
		Map<Integer, String> stopMap = new HashMap<>();
		
		
		BufferedReader br = IOUtils.getBufferedReader(input);
		Counter counter = new Counter("  lines # ");
		int alightAtZeroCost = 0;
		int alightAtCost = 0;
		int fullChains = 0;
		int staffTransactions = 0;
		
		try{
			String line = br.readLine(); /*Header. */
			while((line = br.readLine()) != null){
				String[] sa = line.split(",");
				
				String id = sa[0];
				int station = Integer.parseInt(sa[4]);
				if(!stopMap.containsKey(station)){
					String stationName = sa[5];
					stopMap.put(station, stationName);
				}
				
				/* Only consider cards numbers that are longer than 8 digits
				 * as that will ignore staff cards. */
				if(id.length() > 8){
					String type = sa[6];
					double amount = Double.parseDouble(sa[11]);
					String time = String.format("%02d:%02d", 
							Integer.parseInt(sa[9]), 
							Integer.parseInt(sa[12]));
					int min = Integer.parseInt(sa[9])*60 + Integer.parseInt(sa[12]);
					int bin = (int) Math.ceil(((double)min)/((double)binSize));
					String theBin = String.format("%04d", bin);
					if(!mapBoarding.containsKey(theBin)){
						mapBoarding.put(theBin, 0);
						mapAlighting.put(theBin, 0);
						mapConnecting.put(theBin, 0);
						mapDuration.put(theBin, new ArrayList<Integer>());
					}
					
					if(type.equalsIgnoreCase("1st boarding")){
						if(!mapPerson.containsKey(id)){
							mapPerson.put(id, min);
							mapLocation.put(id, station);
							mapBoarding.put(theBin, mapBoarding.get(theBin)+1);
						} else{
//						LOG.warn("Card " + id + " already in the system.");
						}
					} else if (type.equalsIgnoreCase("Connection")){
						if(!mapPerson.containsKey(id)){
							mapPerson.put(id, min);
							mapLocation.put(id, station);
							mapConnecting.put(theBin, mapConnecting.get(theBin)+1);
						} else{
//						LOG.warn("Card " + id + " already in the system.");
						}
					} else if (type.equalsIgnoreCase("Alighting")){
						if( amount > 0){
							/* It is really getting out of the system. */
							alightAtCost++;
						} else{
							/* It is alighting and will probably connect again. */
							alightAtZeroCost++;
						}
						if(!mapPerson.containsKey(id)){
							LOG.warn("Unknown person " + id + " alighting.");
							unknownLeavers++;
						} else{
							mapAlighting.put(theBin, mapAlighting.get(theBin)+1);
							
							/* Calculate the duration. */
							int duration = min - mapPerson.get(id);
							mapDuration.get(theBin).add(duration);
							mapPerson.remove(id);
							
							/* Determine the OD pair. */
							int o = mapLocation.get(id);
							String oName = stopMap.get(o);
							int oGtfs = 0;
							if(gtfsStopMap.containsKey(oName)){
								oGtfs = gtfsStopMap.get(oName);
							} else{
								oGtfs = AfcUtils.getGtfsStationIdFromName(oName);
							}
							
							int d = station;
							String dName = stopMap.get(d);
							int dGtfs = 0;
							if(gtfsStopMap.containsKey(dName)){
								dGtfs = gtfsStopMap.get(dName);
							} else{
								dGtfs = AfcUtils.getGtfsStationIdFromName(dName);
							}
							
							/* Calculate the OD pair distance. */
							double dist = gp.findRouteDistance(oGtfs, dGtfs, Time.parseTime(time));
							
							String entry = String.format("%s,%d,%s,%d,%s,%s,%d,%.0f", 
									id, oGtfs, oName, dGtfs, dName, time, duration, dist);
							odPairDurations.add(entry);
							
							fullChains++;
						}
					} else{
						LOG.error("What is this person doing?!");
						throw new RuntimeException("Cannot handle transaction type '" + type + "'.");
					}
					
				} else{
					staffTransactions++;
				}
				counter.incCounter();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Ooops...");
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Ooops...");
			}
		}
		counter.printCounter();
		LOG.info("Done processing.");
		LOG.info("Number of people alighting without boarding: " + unknownLeavers);
		LOG.info("Number of people still in system: " + mapPerson.size());
		LOG.info("Number of alightings with zero cost: " + alightAtZeroCost);
		LOG.info("Number of alightings with cost: " + alightAtCost);
		LOG.info("Number of complete chains: " + fullChains);
		LOG.info("Number of staff transactions: " + staffTransactions);
		
		/* Try and estimate trip-chains. */
		
		
		
		
		
		LOG.info("Writing bin data to file...");
		BufferedWriter bw = IOUtils.getBufferedWriter(output);
		try{
			bw.write("Time,Boarding,Alighting,Connecting,MinTime,MaxTime,AvgTime");
			bw.newLine();
			for(String s : mapBoarding.keySet()){
				
				/* Calculate average trip duration. */
				double min = Double.POSITIVE_INFINITY;
				double max = Double.NEGATIVE_INFINITY;
				double total = 0.0;
				double avg = 0.0;
				List<Integer> list = mapDuration.get(s);
				if(list.size() > 0){
					for(Integer i : list){
						min = Math.min(min, (double)i);
						max = Math.max(max, (double)i);
						total += (double)i;
					}
					avg = total / (double)list.size();
				} 			
				
				bw.write(String.format("%d,%d,%d,%d,%.2f,%.2f,%.2f\n", 
						Integer.parseInt(s),
						mapBoarding.get(s),
						mapAlighting.get(s),
						mapConnecting.get(s),
						min, max, avg));
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Oops...");
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Oops...");
			}
		}
		LOG.info("Done writing output.");
		
		LOG.info("Writing stuck-in-system persons.");
		for(String id : mapPerson.keySet()){
			LOG.info("  " + id);
		}
		
		LOG.info("Writing OD durations to file.");
		bw = IOUtils.getBufferedWriter(odFile);
		try{
			bw.write("id,o,oName,d,dName,time,dur,dist");
			bw.newLine();
			for(String s : odPairDurations){
				bw.write(s);
				bw.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + odFile);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + odFile);
			}
		}
		
		
		LOG.info("Done writing OD durations.");
	}
	
	
	
}
