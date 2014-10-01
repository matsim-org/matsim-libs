/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.io.DigicoreVehicleReader_v1;
import playground.southafrica.freight.digicore.utils.DigicoreUtils;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

/**
 * @author jwjoubert
 *
 */
public class NormalDayChainCounts {
	private static List<Integer> abnormalDays;
	private static List<File> vehicleFiles;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(NormalDayChainCounts.class.toString(), args);
		
		String inputfolder = args[0];
		String abnormalDaysFile = args[1];
		String outputFile = args[2];
		
		vehicleFiles = FileUtils.sampleFiles(new File(inputfolder), Integer.MAX_VALUE, FileUtils.getFileFilter(".xml.gz"));
		abnormalDays = DigicoreUtils.readDayOfYear(abnormalDaysFile);		
		
		analyseNormalWeekdayVehicleCounts(outputFile);
		
		Header.printFooter();
	}
	
	public static void analyseNormalWeekdayVehicleCounts(String output){
		Map<String, Integer> map = new TreeMap<String, Integer>();
		Counter counter = new Counter("   vehicles # ");
		
		for(File f : vehicleFiles){
			DigicoreVehicleReader_v1 dvr = new DigicoreVehicleReader_v1();
			dvr.parse(f.getAbsolutePath());
			DigicoreVehicle vehicle = dvr.getVehicle();
			
			for(DigicoreChain chain : vehicle.getChains()){
				int dayOfYear = chain.getFirstMajorActivity().getEndTimeGregorianCalendar().get(Calendar.DAY_OF_YEAR);
				if(!abnormalDays.contains(dayOfYear)){
					
					/* Check that it is a weekday */
					int dayOfWeek = chain.getFirstMajorActivity().getEndTimeGregorianCalendar().get(Calendar.DAY_OF_WEEK);
					if(dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY){
						String dayId = String.valueOf(dayOfYear);
						
						if(!map.containsKey(dayId)){
							map.put(dayId, new Integer(1));
						} else{
							int oldValue = map.get(dayId);
							map.put(dayId, oldValue + 1);
						}
					}
				}
			}
			counter.incCounter();
		}
		counter.printCounter();
		
		/* Write the output. */
		BufferedWriter bw  = IOUtils.getBufferedWriter(output);
		try{
			bw.write("day,numberOfChains");
			bw.newLine();
			for(String day : map.keySet()){
				bw.write(String.format("%s,%d\n", day.toString(), map.get(day)));
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

}
