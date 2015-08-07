/* *********************************************************************** *
 * project: org.matsim.*
 * VehicleOccurenceReporter.java                                                                        *
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
/**
 * 
 */
package playground.nmviljoen.grain.analyses;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.nmviljoen.grain.GrainUtils;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

/**
 * Class to check the occurrence of vehicles over a multi-month period.
 * 
 * @author jwjoubert
 */
public class VehicleOccurenceReporter {
	final private static Logger LOG = Logger.getLogger(VehicleOccurenceReporter.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(VehicleOccurenceReporter.class.toString(), args);
		
		String processedFolder = args[0];
		String outputFile = args[1];
		
		List<File> folders = GrainUtils.getVehicleFolders(processedFolder);
		
		Map<String, List<String>> vehicleMap = new TreeMap<String, List<String>>();
		
		LOG.info("Analysing the vehicle files...");
		for(File month : folders){
			String monthName = month.getParentFile().getName();
			LOG.info("  ... " + monthName);
			Counter counter = new Counter("  ... vehicles # ");
			List<File> vehicles = FileUtils.sampleFiles(month, Integer.MAX_VALUE, FileUtils.getFileFilter(".txt"));
			for(File vehicle : vehicles){
				/* Get the vehicle Id. */
				String vehicleId = vehicle.getName().substring(0, vehicle.getName().indexOf("."));
				
				/* Add the month to the vehicle. */
				if(!vehicleMap.containsKey(vehicleId)){
					vehicleMap.put(vehicleId, new ArrayList<String>(12));
				}
				if(vehicleMap.get(vehicleId).contains(monthName)){
					LOG.error("This shouldn't happen: same vehicle (" + vehicleId + ") in month " + monthName);
				} else{
					vehicleMap.get(vehicleId).add(monthName);
				}
				counter.incCounter();
			}
			counter.printCounter();
		}
		
		LOG.info("Writing analysis to file...");
		BufferedWriter bw = IOUtils.getBufferedWriter(outputFile);
		try{
			bw.write("VehicleId,Month,Total");
			bw.newLine();
			
			/* Write each vehicle. */
			for(String vehicle : vehicleMap.keySet()){
				for(String month : vehicleMap.get(vehicle)){
					bw.write(vehicle);
					bw.write(",");
					bw.write(month);
					bw.write(",");
					bw.write(String.valueOf(vehicleMap.get(vehicle).size()));
					bw.newLine();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + outputFile);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + outputFile);
			}
		}
		LOG.info("Done writing to file.");
		
		Header.printFooter();
	}

}
