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

import playground.southafrica.utilities.Header;

/**
 * Aggregate automated fare collection (AFC) data, like that received from City
 * of Cape Town, into given time bins.
 * 
 * @author jwjoubert
 */
public class AfcAggregator {
	final private static Logger LOG = Logger.getLogger(AfcAggregator.class);
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(AfcAggregator.class.toString(), args);
		String input = args[0];
		int seconds = Integer.parseInt(args[1]);
		String output = args[2];
		
		processInput(input, seconds, output);
		
		Header.printFooter();
	}

	private AfcAggregator(){
		/* Hide constructor. */
	}
	
	
	public static void processInput(String input, int binSize, String output){
		LOG.info("Processing input file " + input);
		int unknownLeavers = 0;
		Map<String, Integer> mapBoarding = new TreeMap<String, Integer>();
		Map<String, Integer> mapAlighting = new TreeMap<String, Integer>();
		Map<String, Integer> mapConnecting = new TreeMap<String, Integer>();
		
		Map<String,Integer> mapPerson = new HashMap<String, Integer>();
		
		Map<String, List<Integer>> mapDuration = new TreeMap<>();
		
		BufferedReader br = IOUtils.getBufferedReader(input);
		Counter counter = new Counter("  lines # ");
		try{
			String line = br.readLine(); /*Header. */
			while((line = br.readLine()) != null){
				String[] sa = line.split(",");
				
				String id = sa[0];
				String type = sa[6];
				String time = String.format("%02d%02d", 
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
						mapBoarding.put(theBin, mapBoarding.get(theBin)+1);
						mapPerson.put(id, min);
					} else{
//						LOG.warn("Card " + id + " already in the system.");
					}
				} else if (type.equalsIgnoreCase("Connection")){
					
					if(!mapPerson.containsKey(id)){
						mapConnecting.put(theBin, mapConnecting.get(theBin)+1);
						mapPerson.put(id, min);
					} else{
//						LOG.warn("Card " + id + " already in the system.");
					}
				} else if (type.equalsIgnoreCase("Alighting")){
					
					if(!mapPerson.containsKey(id)){
						LOG.warn("Unknown person " + id + " alighting.");
						unknownLeavers++;
					} else{
						mapAlighting.put(theBin, mapAlighting.get(theBin)+1);

						int duration = min - mapPerson.get(id);
						mapDuration.get(theBin).add(duration);
						mapPerson.remove(id);
					}
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
	}
	
	
	
}
