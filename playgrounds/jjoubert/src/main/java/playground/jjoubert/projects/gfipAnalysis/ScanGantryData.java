/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
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
package playground.jjoubert.projects.gfipAnalysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.counts.Volume;

import playground.southafrica.utilities.Header;

/**
 * Class to initially scan gantry data.
 * 
 * @author jwjoubert
 */
public class ScanGantryData {
	private final static Logger LOG = Logger.getLogger(ScanGantryData.class);
	private static List<String> vln = new ArrayList<String>();
	private static List<String> gantries = new ArrayList<String>();
	private static List<String> days = new ArrayList<String>();

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(ScanGantryData.class.toString(), args);
		
		String input = args[0];
		String output = args[1];
//		parse(input);
		buildCounts(input, output);
		
		Header.printFooter();
	}
	
	public static void parse(String inputfile){
		LOG.info("Parsing " + inputfile);
		Counter counter = new Counter("   lines # ");

		BufferedReader br = IOUtils.getBufferedReader(inputfile);
		try{
			String line = br.readLine(); /* Header */
			String s_vln = null;
			String s_day = null;
			String s_gantry = null;
			while((line = br.readLine()) != null){
				String[] sa = line.split(",");
				s_vln = sa[0];
				if(!vln.contains(s_vln)){
					vln.add(s_vln);
				}
				
				s_day = sa[1].substring(0, 10);
				if(!days.contains(s_day)){
					days.add(s_day);
				}
				
				s_gantry = sa[2];
				if(!gantries.contains(s_gantry)){
					gantries.add(s_gantry);
				}
				counter.incCounter();
			}
			counter.printCounter();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read from " + inputfile);
		}finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + inputfile);
			}
		}
		LOG.info("Done parsing " + inputfile);
		
		LOG.info("---  Statistics  ---------------------------");
		LOG.info("  Number of unique IDs: " + vln.size());
		LOG.info("  Number of unique days: " + days.size());
		for(String s : days){
			LOG.info("       " + s);
		}
		LOG.info("  Number of unique gantries: " + gantries.size());
		for(String s : gantries){
			LOG.info("       " + s);
		}
	}
	
	public static void buildCounts(String inputfile, String outputFile){
		Map<String, Counts> countMap = new HashMap<String, Counts>();
		LOG.info("Parsing " + inputfile);
		Counter counter = new Counter("   lines # ");
		
		BufferedReader br = IOUtils.getBufferedReader(inputfile);
		try{
			String line = br.readLine(); /* Header */
			String s_vlnClass = null;
			String s_gantry = null;
			while((line = br.readLine()) != null){
				String[] sa = line.split(",");
				s_vlnClass = sa[2];
				if(s_vlnClass == " " | s_vlnClass == "" || s_vlnClass == null){
					LOG.debug("What's going on here?");
				}

				/* Initialise the Counts container if it doesn't exist yet. */
				if(!countMap.containsKey(s_vlnClass)){
					Counts counts = new Counts();
					counts.setYear(2014);
					counts.setName(s_vlnClass);
					counts.setDescription("GFIP gantry counts.");
					countMap.put(s_vlnClass, counts);
				}
				Counts counts = countMap.get(s_vlnClass);
				
				s_gantry = sa[3];
				/* The name of the gantry starts with '1'. This portion of the 
				 * code converts it to start with 'TG'. */
				s_gantry = "TG" + s_gantry.substring(1, 4);
				
				Id<Link> gantryId = Id.create(s_gantry, Link.class);
				/* Initialise the gantry if it doesn't exist yet. */
				Count count = null;
				if(counts.getCount(gantryId) == null){
					count = counts.createAndAddCount(gantryId, s_gantry);
				}
				count = counts.getCount(gantryId);
				
				/* Establish the hour. MATSim assume 1 is the 'first' hour. */
				int hour = Integer.parseInt(sa[1].substring(11, 13)) + 1;

				/* Initialise the hour count if it doesn't exist yet. */
				Volume volume = null;
				if(count.getVolume(hour) == null){
					volume = count.createVolume(hour, 0.0);
				}
				volume = count.getVolume(hour);
				volume.setValue(volume.getValue() + 1.0);
				
				counter.incCounter();
			}
			counter.printCounter();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read from " + inputfile);
		}finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + inputfile);
			}
		}
		LOG.info("Done parsing " + inputfile);
		
		LOG.info("Writing counts to file");
		for(String vehicleClass : countMap.keySet()){
			Counts counts = countMap.get(vehicleClass);
			new CountsWriter(counts).write(outputFile + (outputFile.endsWith("/") ? "" : "/") + "counts_" + vehicleClass + ".xml");
		}
		
		
	}

}
