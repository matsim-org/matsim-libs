/* *********************************************************************** *
 * project: org.matsim.*
 * ChainCounter.java
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
package playground.southafrica.freight.digicore.analysis.chain;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.vehicles.Vehicle;

import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.containers.DigicoreVehicles;
import playground.southafrica.freight.digicore.io.DigicoreVehiclesReader;
import playground.southafrica.utilities.Header;

/**
 * Class to read a container of {@link DigicoreVehicles} and count the number
 * of unique vehicles, and the total number of activity chains. The original
 * reason for this was to see if the number of chains differ substantially when
 * looking at vehicles over a 12 month-period on a month to month basis, as
 * opposed to a container of vehicles covering the entire 12 month-period.
 * 
 * @author jwjoubert
 */
public class ChainCounter {
	final private static Logger LOG = Logger.getLogger(ChainCounter.class);
	
	final static String[] months = {
			"201306", "201307", "201308", "201309", "201310", "201311", 
			"201312", "201401", "201402", "201403", "201404", "201405"};

	private static Map<Id<Vehicle>, Map<String, Integer>> monthlyMap = new TreeMap<>();
	private static Map<Id<Vehicle>, Map<String, Integer>> yearlyMap = new TreeMap<>();
	
	/** Hide constructor */
	private ChainCounter() {
	}
	
	public static void main(String[] args){
		Header.printHeader(ChainCounter.class.toString(), args);
		String path = args[0];
		path += path.endsWith("/") ? "" : "/";
		checkMonthByMonth(path);
		checkWholeYear(path);
		
		writeChainCountComparison(path);
		
		Header.printFooter();
	}
	
	private static void checkMonthByMonth(String path){
		int chains = 0;
		DigicoreVehicles vehicles = null;
		
		for(String month : months){
			vehicles = null; vehicles = new DigicoreVehicles();
			new DigicoreVehiclesReader(vehicles).readFile(path + "digicoreVehicles_" + month + ".xml.gz");
			int count = countChains(vehicles, monthlyMap);
			LOG.info("Number of chains for " + month + ": " + count);
		}

		LOG.info("Total number of chains on month-by-month basis: " + chains);
	}
	
	private static void checkWholeYear(String path){
		int chains = 0;
		DigicoreVehicles vehicles = new DigicoreVehicles();
		new DigicoreVehiclesReader(vehicles).readFile(path + "digicoreVehicles_201306_201405.xml.gz");
		chains += countChains(vehicles, yearlyMap);
		LOG.info("Total number of chains on yearly basis: " + chains);
	}
	
	private static int countChains(DigicoreVehicles vehicles, 
			Map<Id<Vehicle>, Map<String, Integer>> map){
		
		int total = 0;
		Counter counter = new Counter("  vehicles # ");
		for(DigicoreVehicle vehicle : vehicles.getVehicles().values()){
			if(!map.containsKey(vehicle.getId())){
				map.put(vehicle.getId(), new TreeMap<>());
			}
			Map<String, Integer> thisMap = map.get(vehicle.getId());
			
			for(DigicoreChain chain : vehicle.getChains()){
				int chainYear = chain.getFirstMajorActivity().getEndTimeGregorianCalendar().get(Calendar.YEAR);
				int chainMonth = chain.getFirstMajorActivity().getEndTimeGregorianCalendar().get(Calendar.MONTH)+1;
				String monthString = String.format("%d%02d", chainYear, chainMonth);
				if(!thisMap.containsKey(monthString)){
					thisMap.put(monthString, 1);
				} else{
					int oldValue = thisMap.get(monthString);
					thisMap.put(monthString, oldValue + 1);
				}
			}
			
			total += vehicle.getChains().size();
			counter.incCounter();
		}
		counter.printCounter();
		
		return total;
	}
	
	private static void writeChainCountComparison(String path){
		LOG.info("Writing chain count comparison to file...");
		
		/* Get a consolidated list of vehicle IDs. */
		List<Id<Vehicle>> vIds = new ArrayList<Id<Vehicle>>();
		for(Id<Vehicle> vId : monthlyMap.keySet()){
			if(!vIds.contains(vId)){
				vIds.add(vId);
			}
		}
		for(Id<Vehicle> vId : yearlyMap.keySet()){
			if(!vIds.contains(vId)){
				vIds.add(vId);
			}
		}
		
		BufferedWriter bw = IOUtils.getBufferedWriter(path + "chainCountComparison.csv.gz");
		try{
			/* Header */
			bw.write("vid");
			for(int i = 0; i < months.length; i++){
				bw.write(",");
				bw.write("m");
				bw.write(months[i]);
			}
			for(int i = 0; i < months.length; i++){
				bw.write(",");
				bw.write("y");
				bw.write(months[i]);
			}
			bw.newLine();
			
			/* Write each vehicle's number of chains in each of the considered 
			 * months. */
			for(Id<Vehicle> vid : vIds){
				bw.write(vid.toString());
				
				/* Monthly. */
				for(String month : months){
					int count = 0;
					if(monthlyMap.containsKey(vid)){
						if(monthlyMap.get(vid).containsKey(month)){
							count = monthlyMap.get(vid).get(month);
						}
					}
					bw.write(String.format(",%d", count));
				}
				
				/* Yearly. */
				for(String month : months){
					int count = 0;
					if(yearlyMap.containsKey(vid)){
						if(yearlyMap.get(vid).containsKey(month)){
							count = yearlyMap.get(vid).get(month);
						}
					}
					bw.write(String.format(",%d", count));
				}
				
				bw.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + bw.toString());
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + bw.toString());
			}
		}
		LOG.info("Done writing chain count comparison to file.");
	}
	
	
	
	
	
	
	
}
